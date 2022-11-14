#include "crashpad-backend.h"
#include "backtrace-native.h"
#include <jni.h>
#include <libgen.h>

//#include "client/crashpad_client.h"

extern std::string thread_id;
extern std::atomic_bool initialized;
extern std::mutex attribute_synchronization;
extern std::atomic_bool disabled;

static crashpad::CrashpadClient *client;
static std::unique_ptr<crashpad::CrashReportDatabase> database;

bool InitializeCrashpad(jstring url,
                        jstring database_path,
                        jstring handler_path,
                        jobjectArray attributeKeys,
                        jobjectArray attributeValues,
                        jobjectArray attachmentPaths,
                        jboolean enableClientSideUnwinding,
                        jint unwindingMode) {
    // avoid multi initialization
    if (initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Crashpad is already initialized");
        return true;
    }

    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return false;
    }


    // path to crashpad database
    const char *filePath = env->GetStringUTFChars(database_path, 0);
    base::FilePath db(filePath);

    if (enableClientSideUnwinding) {
        bool success = EnableClientSideUnwinding(env, filePath, unwindingMode);
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
    base::FilePath handler(handlerPath);

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
            attachmentArgumentString += convertedAttachmentPath;
            arguments.push_back(attachmentArgumentString);

            env->ReleaseStringUTFChars(jstringAttachmentPath, convertedAttachmentPath);
        }
    }

    database = crashpad::CrashReportDatabase::Initialize(db);
    if (database == nullptr || database->GetSettings() == NULL) {
        return false;
    }

    // Enable automated uploads.
    database->GetSettings()->SetUploadsEnabled(true);

    // Start crash handler
    client = new crashpad::CrashpadClient();

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

void DumpWithoutCrashCrashpad(jstring message, jboolean set_main_thread_as_faulting_thread) {
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

void AddAttributeCrashpad(jstring key, jstring value) {
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

void DisableCrashpad() {
    if (database == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Crashpad database is null, this should not happen");
        return;
    }
    // Disable automated uploads.
    database->GetSettings()->SetUploadsEnabled(false);
    disabled = true;
}

void ReEnableCrashpad() {
    // Re-enable uploads if disabled
    if (disabled) {
        if (database == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                "Crashpad database is null, this should not happen");
            return;
        }
        database->GetSettings()->SetUploadsEnabled(true);
        disabled = false;
    }
}

bool IsSafeModeRequiredCrashpad(jstring database) {
    JNIEnv *env = GetJniEnv();
    base::FilePath db((env)->GetStringUTFChars(database, 0));
    client->EnableCrashLoopDetection();
//    return false;
    bool is_enabled = crashpad::CrashpadClient::IsSafeModeRequired(db);

    int count = crashpad::CrashpadClient::ConsecutiveCrashesCount(db);

    return is_enabled;
}
