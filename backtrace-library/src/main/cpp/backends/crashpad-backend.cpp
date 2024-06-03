#include "crashpad-backend.h"
#include "handler/handler_main.h"
#include "backtrace-native.h"
#include <jni.h>
#include <libgen.h>

extern std::string thread_id;
extern std::atomic_bool initialized;
extern std::mutex attribute_synchronization;
extern std::atomic_bool disabled;

static crashpad::CrashpadClient *client;
static std::unique_ptr<crashpad::CrashReportDatabase> database;

std::vector<std::string>
generateInitializationArguments(JNIEnv *env, jobjectArray attachmentPaths) {
    std::vector<std::string> arguments;
    arguments.push_back("--no-rate-limit");

    // paths to file attachments
    if (attachmentPaths == nullptr) {
        return arguments;
    }
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
        env->DeleteLocalRef(jstringAttachmentPath);
    }


    return arguments;

}

std::map<std::string, std::string> generateInitializationAttributes(JNIEnv *env,
                                                                    jobjectArray attributeKeys,
                                                                    jobjectArray attributeValues) {
    std::map<std::string, std::string> attributes;
    attributes["format"] = "minidump";
    if (!thread_id.empty()) {
        attributes["thread.main"] = thread_id;
    }

    // Get lengths of the Java arrays
    jint keyLength = env->GetArrayLength(attributeKeys);
    jint valueLength = env->GetArrayLength(attributeValues);
    // Ensure the key and value arrays have the same length
    if (keyLength != valueLength) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Attribute array length doesn't match. Attributes won't be available in the Crashpad integration");
        return attributes;
    }


    for (int attributeIndex = 0; attributeIndex < keyLength; ++attributeIndex) {
        jstring jstringKey = (jstring) env->GetObjectArrayElement(attributeKeys,
                                                                  attributeIndex);
        jboolean isCopy;
        const char *convertedKey = (env)->GetStringUTFChars(jstringKey, &isCopy);

        jstring stringValue = (jstring) env->GetObjectArrayElement(attributeValues,
                                                                   attributeIndex);
        const char *convertedValue = (env)->GetStringUTFChars(stringValue, &isCopy);

        if (convertedKey && convertedValue) {
            attributes[convertedKey] = convertedValue;

            env->ReleaseStringUTFChars(jstringKey, convertedKey);
            env->ReleaseStringUTFChars(stringValue, convertedValue);
        }

        env->DeleteLocalRef(jstringKey);
        env->DeleteLocalRef(stringValue);
    }

    return attributes;
}

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
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Crashpad is already initialized");
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
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                "Cannot enable client side unwinding");
        }
    }

    // Backtrace url
    const char *backtraceUrl = env->GetStringUTFChars(url, 0);

    // path to crash handler executable
    const char *handlerPath = env->GetStringUTFChars(handler_path, 0);
    base::FilePath handler(handlerPath);

    database = crashpad::CrashReportDatabase::Initialize(db);
    if (database == nullptr || database->GetSettings() == NULL) {
        return false;
    }

    // Enable automated uploads.
    database->GetSettings()->SetUploadsEnabled(true);

    // Start crash handler
    client = new crashpad::CrashpadClient();

    std::map<std::string, std::string> attributes = generateInitializationAttributes(env,
                                                                                     attributeKeys,
                                                                                     attributeValues);

    std::vector<std::string> arguments = generateInitializationArguments(env, attachmentPaths);

    std::map<std::string, std::string>::iterator guidIterator = attributes.find("guid");
    if (guidIterator != attributes.end()) {
        client->OverrideGuid(guidIterator->second);
    }

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


bool InitializeCrashpadJavaCrashHandler(jstring url,
                                        jstring database_path,
                                        jstring class_path,
                                        jobjectArray attributeKeys,
                                        jobjectArray attributeValues,
                                        jobjectArray attachmentPaths,
                                        jobjectArray environmentVariables) {
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "InitializeCrashpadJavaCrashHandler");
    // avoid multi initialization
    if (initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Crashpad is already initialized");
        return true;
    }

    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return false;
    }


    // path to crashpad database
    const char *filePath = env->GetStringUTFChars(database_path, 0);

    // Backtrace url
    const char *backtraceUrl = env->GetStringUTFChars(url, 0);

    // path to crash handler executable
    const char *classPath = env->GetStringUTFChars(class_path, 0);
    base::FilePath db(filePath);

    database = crashpad::CrashReportDatabase::Initialize(db);
    if (database == nullptr || database->GetSettings() == NULL) {
        return false;
    }

    // Enable automated uploads.
    database->GetSettings()->SetUploadsEnabled(true);

    // Start crash handler
    client = new crashpad::CrashpadClient();

    std::map<std::string, std::string> attributes = generateInitializationAttributes(env,
                                                                                     attributeKeys,
                                                                                     attributeValues);

    std::vector<std::string> arguments = generateInitializationArguments(env, attachmentPaths);


    std::map<std::string, std::string>::iterator guidIterator = attributes.find("guid");
    if (guidIterator != attributes.end()) {
        client->OverrideGuid(guidIterator->second);
    }

    base::FilePath metrics_directory;


    std::vector<std::string> *handlerEnvVariables = nullptr;
    if (environmentVariables != nullptr) {
        handlerEnvVariables = new std::vector<std::string>;
        for (int envVariableIndex = 0;
             envVariableIndex < env->GetArrayLength(environmentVariables); ++envVariableIndex) {
            jstring envVariable = (jstring) env->GetObjectArrayElement(environmentVariables,
                                                                       envVariableIndex);
            jboolean isCopy;
            handlerEnvVariables->push_back((env)->GetStringUTFChars(envVariable, &isCopy));
        }
    }


    initialized = client->StartJavaHandlerAtCrash(classPath, handlerEnvVariables, db,
                                                  metrics_directory, backtraceUrl, attributes,
                                                  arguments);

    env->ReleaseStringUTFChars(url, backtraceUrl);
    env->ReleaseStringUTFChars(class_path, classPath);
    env->ReleaseStringUTFChars(database_path, filePath);

    return initialized;
}


bool CaptureCrashCrashpad(jobjectArray args) {
    JNIEnv *env = GetJniEnv();


    int argSize = env->GetArrayLength(args);
    char **argv = new char *[argSize];
    for (int i = 0; i < argSize; ++i) {
        jstring currentArg = (jstring) env->GetObjectArrayElement(args, i);
        argv[i] = const_cast<char *>(env->GetStringUTFChars(currentArg, 0));
    }

    return crashpad::HandlerMain(argSize, argv, nullptr) == 0;
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
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Crashpad database is null, this should not happen");
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
