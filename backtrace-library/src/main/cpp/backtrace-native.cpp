#include <atomic>
#include <map>
#include <mutex>
#include <string>
#include <unistd.h>
#include <vector>
#include <cstdio>

#include <jni.h>
#include <string>
#include <android/log.h>
#include <unordered_map>
#include <libgen.h>

#define USE_BREAKPAD 1

#ifndef USE_BREAKPAD
#include "base/logging.h"
#include "client/crashpad_client.h"
#include "client/crashpad_info.h"
#include "client/crash_report_database.h"
#include "client/settings.h"
#endif

#include "client-side-unwinding.h"

#include <sys/types.h>
#include <sys/system_properties.h>

// supported JNI version
static jint JNI_VERSION = JNI_VERSION_1_6;

// Java VM
static JavaVM *javaVm;

// crashpad client
#ifndef USE_BREAKPAD
using namespace base;
static crashpad::CrashpadClient *client;
#endif

// check if native crash client is already initialized
std::atomic_bool initialized;
static std::mutex attribute_synchronization;
static std::string thread_id;

// TODO: Gate to NDK version
#if USE_BREAKPAD
#include "exception_handler.h"
#include "common/linux/http_upload.h"
#include "cacert.h"

//std::unique_ptr<google_breakpad::ExceptionHandler*> eh;
static google_breakpad::ExceptionHandler* eh;
std::string upload_url_str;
std::map<std::string, std::string> attributes;
std::string certificate_path;

#endif

JNIEXPORT jint JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env;
    if (jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION) != JNI_OK) {

        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                            "Cannot load the JNI env");
        return JNI_ERR;
    }
    javaVm = jvm;

    char thread_id_cstr[25];
    sprintf(thread_id_cstr, "%jd", (intmax_t)gettid());
    thread_id = std::string(thread_id_cstr);
    return JNI_VERSION_1_4;
}

namespace /* anonymous */
{
    /**
     * Get JNI Environment pointer
     * Executing JNIEnv methods on background thread might cause a crash - by default we attached
     * JNIEnv to main thread - background thread wasn't available. This method allows to return
     * safety JNIEnv and detach it once we done with all native operations.
     */
    JNIEnv *GetJniEnv() {
        JNIEnv *m_pJniEnv;
        int nEnvStat = javaVm->GetEnv(reinterpret_cast<void **>(&m_pJniEnv), JNI_VERSION_1_6);

        if (nEnvStat == JNI_EDETACHED) {
            JavaVMAttachArgs args;
            args.version = JNI_VERSION_1_6;

            if (javaVm->AttachCurrentThread(&m_pJniEnv, &args) != 0) {
                return nullptr;
            }

            thread_local struct DetachJniOnExit {
                ~DetachJniOnExit() {
                    javaVm->DetachCurrentThread();
                }
            };
        } else if (nEnvStat == JNI_EVERSION) {
            return nullptr;
        }
        return m_pJniEnv;
    }

#ifndef USE_BREAKPAD
    bool InitializeImpl(jstring url,
                        jstring database_path,
                        jstring handler_path,
                        jobjectArray attributeKeys,
                        jobjectArray attributeValues,
                        jobjectArray attachmentPaths = nullptr,
                        jboolean enableClientSideUnwinding = false,
                        jint unwindingMode = UNWINDING_MODE_DEFAULT) {
        using namespace crashpad;

        // avoid multi initialization
        if (initialized) {
            return true;
        }

        JNIEnv *env = GetJniEnv();
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
            return false;
        }

        if (enableClientSideUnwinding) {
            bool success = EnableClientSideUnwinding(env, database_path, unwindingMode);
            if (!success) {
                __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot enable client side unwinding");
            }
        }

        std::map<std::string, std::string> attributes;
        attributes["format"] = "minidump";
        // save native main thread id
        if(!thread_id.empty()) {
            attributes["thread.main"] = thread_id;
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

                attributes[convertedKey] = convertedValue;

                env->ReleaseStringUTFChars(jstringKey, convertedKey);
                env->ReleaseStringUTFChars(stringValue, convertedValue);
            }
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                "Attribute array length doesn't match. Attributes won't be available in the Crashpad integration");
        }

        std::vector<std::string> arguments;
        arguments.push_back("--no-rate-limit");

        // Backtrace url
        const char *backtraceUrl = env->GetStringUTFChars(url, 0);

        // path to crash handler executable
        const char *handlerPath = env->GetStringUTFChars(handler_path, 0);
        FilePath handler(handlerPath);

        // path to crashpad database
        const char *filePath = env->GetStringUTFChars(database_path, 0);
        FilePath db(filePath);

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

                std::string attachmentBaseName = basename(convertedAttachmentPath);

                std::string attachmentArgumentString("--attachment=");
                attachmentArgumentString += attachmentBaseName;
                attachmentArgumentString += "=";
                attachmentArgumentString += convertedAttachmentPath;
                arguments.push_back(attachmentArgumentString);

                env->ReleaseStringUTFChars(jstringAttachmentPath, convertedAttachmentPath);
            }
        }

        std::unique_ptr<CrashReportDatabase> database = CrashReportDatabase::Initialize(db);
        if (database == nullptr || database->GetSettings() == NULL) {
            return false;
        }

        // Enable automated uploads.
        database->GetSettings()->SetUploadsEnabled(true);

        // Start crash handler
        client = new CrashpadClient();

        initialized = client->StartHandlerAtCrash(handler, db, db, backtraceUrl, attributes,
                                                  arguments);

        env->ReleaseStringUTFChars(url, backtraceUrl);
        env->ReleaseStringUTFChars(handler_path, handlerPath);
        env->ReleaseStringUTFChars(database_path, filePath);

        if (enableClientSideUnwinding) {
            SetCrashpadHandlerForClientSideUnwinding();
        }

        return initialized;
    }
#endif

#if USE_BREAKPAD
    static bool dumpCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                             void* context, bool succeeded) {

        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Dump path: %s\n", descriptor.path());

        upload_url_str = std::string("https://yolo.sp.backtrace.io:6098/post?format=minidump&token=d8ca0bad4874d43982241ade3d5afeebf7d6823a50ab5de6a5b508ea2beda8d0");

        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Upload URL: %s.\n", upload_url_str.c_str());

        /* try to open file to read */
        FILE *file;
        if (file = fopen(descriptor.path(), "r")) {
            fclose(file);
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","File at dump path exists");
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","File at dump path does not exist");
        }

        if (succeeded == true) {
            std::map<string, string> files;
            files["upload_file_minidump"] = descriptor.path();
            // Send it
            string response, error;
            bool success = google_breakpad::HTTPUpload::SendRequest(upload_url_str,
                                                                    attributes,
                                                                    files,
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

    std::string CreateCertificateFile(const char* directory) {
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
                           jobjectArray attachmentPaths = nullptr,
                           jboolean enableClientSideUnwinding = false,
                           jint unwindingMode = UNWINDING_MODE_DEFAULT) {
        JNIEnv *env = GetJniEnv();
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
            return false;
        }

        // path to crash handler executable
        const char *database_path_cstr = env->GetStringUTFChars(database_path, 0);
        // Backtrace url
        const char *backtrace_url_cstr = env->GetStringUTFChars(url, 0);
        upload_url_str = std::string(backtrace_url_cstr);

        google_breakpad::MinidumpDescriptor descriptor(database_path_cstr);

        CreateCertificateFile(database_path_cstr);

        attributes["format"] = "minidump";
        // save native main thread id
        if(!thread_id.empty()) {
            attributes["thread.main"] = thread_id;
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

                attributes[convertedKey] = convertedValue;

                env->ReleaseStringUTFChars(jstringKey, convertedKey);
                env->ReleaseStringUTFChars(stringValue, convertedValue);
            }
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                "Attribute array length doesn't match. Attributes won't be available in the Crashpad integration");
        }


        //std::unique_ptr<google_breakpad::ExceptionHandler, cpp_deleter>(new google_breakpad::ExceptionHandler(descriptor, NULL, dumpCallback, NULL, true, -1), cpp_deleter());
        eh = new google_breakpad::ExceptionHandler(descriptor, NULL, dumpCallback, NULL, true, -1);

        env->ReleaseStringUTFChars(url, backtrace_url_cstr);
        env->ReleaseStringUTFChars(database_path, database_path_cstr);

        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Breakpad initialized");

        return 0;
    }

#endif
}

extern "C" {
void Crash() {
    *(volatile int *) 0 = 0;
}

#ifndef USE_BREAKPAD
void DumpWithoutCrash(jstring message, jboolean set_main_thread_as_faulting_thread) {
    crashpad::NativeCPUContext context;
    crashpad::CaptureContext(&context);

    // set dump message for single report
    crashpad::SimpleStringDictionary *annotations = NULL;

    if (message != NULL || set_main_thread_as_faulting_thread == true) {
        JNIEnv *env = GetJniEnv();
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
            return;
        }
        const std::lock_guard<std::mutex> lock(attribute_synchronization);
        crashpad::CrashpadInfo *info = crashpad::CrashpadInfo::GetCrashpadInfo();
        annotations = info->simple_annotations();
        if (!annotations) {
            annotations = new crashpad::SimpleStringDictionary();
            info->set_simple_annotations(annotations);
        }
        if (set_main_thread_as_faulting_thread == true) {
            annotations->SetKeyValue("_mod_faulting_tid", thread_id);
        }
        if (message != NULL) {
            // user can't override error.message - exception message that Crashpad/crash-reporting tool
            // will set to tell user about error message. This code will set error.message only for single
            // report and after creating a dump, method will clean up this attribute.
            jboolean isCopy;
            const char *rawMessage = env->GetStringUTFChars(message, &isCopy);
            annotations->SetKeyValue("error.message", rawMessage);
            env->ReleaseStringUTFChars(message, rawMessage);
        }
    }
    client->DumpWithoutCrash(&context);

    if (annotations != NULL) {
        annotations->RemoveKey("error.message");
    }
}
#endif

JNIEXPORT void JNICALL Java_backtraceio_library_base_BacktraceBase_crash(
        JNIEnv *env,
        jobject /* this */) {
    Crash();
}

#ifndef USE_BREAKPAD
bool Initialize(jstring url,
                jstring database_path,
                jstring handler_path,
                jobjectArray attributeKeys,
                jobjectArray attributeValues,
                jobjectArray attachmentPaths = nullptr,
                jboolean enableClientSideUnwinding = false,
                jint unwindingMode = UNWINDING_MODE_DEFAULT) {
    static std::once_flag initialize_flag;

    std::call_once(initialize_flag, [&] {
        initialized = InitializeImpl(url,
                                     database_path, handler_path,
                                     attributeKeys, attributeValues,
                                     attachmentPaths, enableClientSideUnwinding,
                                     unwindingMode);
    });
    return initialized;
}
#endif

JNIEXPORT jboolean JNICALL
Java_backtraceio_library_BacktraceDatabase_initialize(JNIEnv *env,
                                                      jobject thiz,
                                                      jstring url,
                                                      jstring database_path,
                                                      jstring handler_path,
                                                      jobjectArray attributeKeys,
                                                      jobjectArray attributeValues,
                                                      jobjectArray attachmentPaths = nullptr,
                                                      jboolean enableClientSideUnwinding = false,
                                                      jobject unwindingMode = nullptr) {
#if USE_BREAKPAD
    return InitializeBreakpad(url, database_path, handler_path, attributeKeys,
                              attributeValues, attachmentPaths, enableClientSideUnwinding);
#else
    jint unwindingModeInt = ExtractClientSideUnwindingMode(env, unwindingMode);
    return Initialize(url, database_path, handler_path, attributeKeys,
                      attributeValues, attachmentPaths, enableClientSideUnwinding,
                      unwindingModeInt);
#endif
}

#ifndef USE_BREAKPAD
void AddAttribute(jstring key, jstring value) {
    if (initialized == false) {
        __android_log_print(ANDROID_LOG_WARN, "Backtrace-Android",
                            "Crashpad integration isn't available. Please initialize the Crashpad integration first.");
        return;
    }
    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return;
    }

    const std::lock_guard<std::mutex> lock(attribute_synchronization);
    crashpad::CrashpadInfo *info = crashpad::CrashpadInfo::GetCrashpadInfo();
    crashpad::SimpleStringDictionary *annotations = info->simple_annotations();
    if (!annotations) {
        annotations = new crashpad::SimpleStringDictionary();
        info->set_simple_annotations(annotations);
    }

    jboolean isCopy;
    const char *crashpadKey = env->GetStringUTFChars(key, &isCopy);
    const char *crashpadValue = env->GetStringUTFChars(value, &isCopy);
    if (crashpadKey && crashpadValue)
        annotations->SetKeyValue(crashpadKey, crashpadValue);

    env->ReleaseStringUTFChars(key, crashpadKey);
    env->ReleaseStringUTFChars(value, crashpadValue);
}
#endif

JNIEXPORT void JNICALL
Java_backtraceio_library_BacktraceDatabase_addAttribute(JNIEnv *env, jobject thiz,
                                                        jstring name, jstring value) {
#ifndef USE_BREAKPAD
    AddAttribute(name, value);
#endif
}

JNIEXPORT void JNICALL
Java_backtraceio_library_base_BacktraceBase_dumpWithoutCrash__Ljava_lang_String_2(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring message) {
#ifndef USE_BREAKPAD
    DumpWithoutCrash(message, false);
#endif
}
JNIEXPORT void JNICALL
Java_backtraceio_library_base_BacktraceBase_dumpWithoutCrash__Ljava_lang_String_2Z(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jstring message,
                                                                                   jboolean set_main_thread_as_faulting_thread) {
#ifndef USE_BREAKPAD
    DumpWithoutCrash(message, set_main_thread_as_faulting_thread);
#endif
}

}