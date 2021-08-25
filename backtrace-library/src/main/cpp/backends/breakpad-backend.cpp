#include "breakpad-backend.h"
#include <android/log.h>
#include <jni.h>
#include <stdio.h>
#include <string>
#include <atomic>
#include <mutex>
#include <unordered_map>
#include <libgen.h>
#include "cacert.h"
#include "client-side-unwinding.h"
#include "backtrace-native.h"

extern std::string thread_id;
extern std::atomic_bool initialized;
extern std::mutex attribute_synchronization;

//std::unique_ptr<google_breakpad::ExceptionHandler*> eh;
static google_breakpad::ExceptionHandler* eh;
static std::string upload_url_str;
static std::map<std::string, std::string> breakpad_attributes;
std::map<std::string, std::string> breakpad_files;
static std::string certificate_path;

static std::string dump_dir;

struct dump_context {
    bool set_main_thread_as_faulting_thread;
    std::string message;
    int key;
};

// Keep track of dump context for user-generated Breakpad dumps
int user_generated_dump_counter = 0;
static std::unordered_map<int, dump_context> dump_context_map;

static bool dumpCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                         void* context, bool succeeded) {
    if (succeeded == false) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Breakpad dump callback reports failure\n");
    }

    std::map<std::string, std::string> &local_breakpad_attributes = breakpad_attributes;

    if (context == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","No context provided");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Context provided");
        dump_context *local_context = static_cast<dump_context*>(context);
        if (local_context != nullptr) {
            local_breakpad_attributes = std::map<std::string, std::string>(breakpad_attributes);
            if (!local_context->message.empty()) {
                local_breakpad_attributes["error.message"] = local_context->message;
            }
            if (local_context->set_main_thread_as_faulting_thread) {
                local_breakpad_attributes["_mod_faulting_tid"] = thread_id;
            }
            // Delete this local context when we're done
            dump_context_map.erase(local_context->key);
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Could not convert context to local dump_context");
        }
    }

    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Dump path: %s\n", descriptor.path());

    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Upload URL: %s\n", upload_url_str.c_str());

    /* try to open file to read */
    bool dump_exists = false;
    FILE *file;
    if (file = fopen(descriptor.path(), "r")) {
        dump_exists = true;
        fclose(file);
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","File at dump path exists");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","File at dump path does not exist");
    }

    if (dump_exists) {
        breakpad_files["upload_file_minidump"] = descriptor.path();

        // TODO: For debugging only
        for (auto breakpad_file : breakpad_files) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Breakpad file key %s", breakpad_file.first.c_str());
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Breakpad file value %s", breakpad_file.second.c_str());
        }

        // Send it
        string response, error;
        bool success = google_breakpad::HTTPUpload::SendRequest(upload_url_str,
                                                                breakpad_attributes,
                                                                breakpad_files,
                                                                "",
                                                                "",
                                                                certificate_path,
                                                                &response,
                                                                NULL,
                                                                &error);
        if (success) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                "Successfully sent the minidump file.\n");
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                "Failed to send minidump: %s\n", error.c_str());
        }
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Response:\n");
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s\n", response.c_str());
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Breakpad dump callback reports failure\n");
    }
    return succeeded;
}

void CreateCertificateFile(const char* directory) {
    certificate_path = std::string(directory) + "/backtrace-cacert.pem";
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Certificate path: %s\n", certificate_path.c_str());

    FILE* f = fopen(certificate_path.c_str(), "w");
    if (f) {
        fwrite(backtrace::cacert, 1, sizeof(backtrace::cacert), f);
        fclose(f);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Could not create certificate file");
    }
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Finished writing certificate file");
}

int InitializeBreakpad(jstring url,
                       jstring database_path,
                       jstring handler_path,
                       jobjectArray attributeKeys,
                       jobjectArray attributeValues,
                       jobjectArray attachmentPaths,
                       jboolean enableClientSideUnwinding,
                       jint unwindingMode) {
    // avoid multi initialization
    if (initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Breakpad is already initialized");
        return true;
    }

    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return false;
    }

    if (enableClientSideUnwinding) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Client side unwinding not available with Breakpad native crash reporting backend");
    }

    // path to crash handler executable
    const char *database_path_cstr = env->GetStringUTFChars(database_path, 0);
    // Backtrace url
    const char *backtrace_url_cstr = env->GetStringUTFChars(url, 0);
    upload_url_str = std::string(backtrace_url_cstr);

    dump_dir = std::string(database_path_cstr);
    google_breakpad::MinidumpDescriptor descriptor(database_path_cstr);

    CreateCertificateFile(database_path_cstr);

    breakpad_attributes["format"] = "minidump";
    // save native main thread id
    if(!thread_id.empty()) {
        breakpad_attributes["thread.main"] = thread_id;
    }

    jint keyLength = env->GetArrayLength(attributeKeys);
    jint valueLength = env->GetArrayLength(attributeValues);
    if (keyLength == valueLength) {
        for (int attributeIndex = 0; attributeIndex < keyLength; ++attributeIndex) {
            jstring jstringKey = (jstring) env->GetObjectArrayElement(attributeKeys,
                                                                      attributeIndex);
            jboolean isCopy;
            const char *convertedKey = (env)->GetStringUTFChars(jstringKey, &isCopy);

            jstring stringValue = (jstring) env->GetObjectArrayElement(attributeValues,
                                                                       attributeIndex);
            const char *convertedValue = (env)->GetStringUTFChars(stringValue, &isCopy);

            if (!convertedKey || !convertedValue)
                continue;

            breakpad_attributes[convertedKey] = convertedValue;

            env->ReleaseStringUTFChars(jstringKey, convertedKey);
            env->ReleaseStringUTFChars(stringValue, convertedValue);
        }
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Attribute array length doesn't match. Attributes won't be available in the Crashpad integration");
    }

    // paths to file attachments
    if (attachmentPaths != nullptr) {
        jint attachmentsLength = env->GetArrayLength(attachmentPaths);
        for (int attachmentIndex = 0; attachmentIndex < attachmentsLength; ++attachmentIndex) {
            jstring jstringAttachmentPath = (jstring) env->GetObjectArrayElement(
                    attachmentPaths,
                    attachmentIndex);
            jboolean isCopy;
            const char *convertedAttachmentPath = (env)->GetStringUTFChars(
                    jstringAttachmentPath, &isCopy);

            if (!convertedAttachmentPath)
                continue;

            std::string attachmentBaseName = std::string("attachment_") + basename(convertedAttachmentPath);

            breakpad_files[attachmentBaseName] = convertedAttachmentPath;

            env->ReleaseStringUTFChars(jstringAttachmentPath, convertedAttachmentPath);
        }
    }

    //std::unique_ptr<google_breakpad::ExceptionHandler, cpp_deleter>(new google_breakpad::ExceptionHandler(descriptor, NULL, dumpCallback, NULL, true, -1), cpp_deleter());
    eh = new google_breakpad::ExceptionHandler(descriptor, NULL, dumpCallback, NULL, true, -1);

    env->ReleaseStringUTFChars(url, backtrace_url_cstr);
    env->ReleaseStringUTFChars(database_path, database_path_cstr);

    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Breakpad initialized");

    initialized = true;
    return initialized;
}

void DumpWithoutCrashBreakpad(jstring message, jboolean set_main_thread_as_faulting_thread) {
    if (eh == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, "Backtrace-Android",
                            "Breakpad integration isn't available. Please initialize the Breakpad integration before calling DumpWithoutCrash.");
        return;
    }
#if 0
    eh->WriteMinidump();
#endif
    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return;
    }
    dump_context context;

    jboolean isCopy;
    const char *rawMessage = env->GetStringUTFChars(message, &isCopy);
    context.message = std::string(rawMessage);
    env->ReleaseStringUTFChars(message, rawMessage);

    context.set_main_thread_as_faulting_thread = set_main_thread_as_faulting_thread;
    context.key = user_generated_dump_counter++;
    dump_context_map[context.key] = context;

    google_breakpad::ExceptionHandler::WriteMinidump(dump_dir, dumpCallback, (void*) &context);
}

void AddAttributeBreakpad(jstring key, jstring value) {
    if (initialized == false) {
        __android_log_print(ANDROID_LOG_WARN, "Backtrace-Android",
                            "Breakpad integration isn't available. Please initialize the Breakpad before calling AddAttribute.");
        return;
    }
    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return;
    }

    const std::lock_guard<std::mutex> lock(attribute_synchronization);

    jboolean isCopy;
    const char *breakpadKey = env->GetStringUTFChars(key, &isCopy);
    const char *breakpadValue = env->GetStringUTFChars(value, &isCopy);
    if (breakpadKey && breakpadValue) {
        breakpad_attributes[breakpadKey] = breakpadValue;
    }

    env->ReleaseStringUTFChars(key, breakpadKey);
    env->ReleaseStringUTFChars(value, breakpadValue);
}
