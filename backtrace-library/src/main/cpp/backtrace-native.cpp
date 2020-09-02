#include <atomic>
#include <map>
#include <mutex>
#include <string>
#include <unistd.h>
#include <vector>

#include <jni.h>
#include <string>
#include <android/log.h>

#include "base/logging.h"
#include "client/crashpad_client.h"
#include "client/crashpad_info.h"
#include "client/crash_report_database.h"
#include "client/settings.h"

using namespace base;

// supported JNI version
static jint JNI_VERSION = JNI_VERSION_1_6;

// JNI environment reference
static JNIEnv *env;

// crashpad client
static crashpad::CrashpadClient *client;

// check if crashpad client is already initialized
static std::atomic_bool initialized;
static std::mutex attribute_synchronization;

JNIEXPORT jint JNI_OnLoad(JavaVM* jvm, void* reserved)
{
    if (jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION) != JNI_OK) {

        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                            "Cannot load the JNI env");
        return JNI_ERR;
    }
    return JNI_VERSION_1_4;
}

namespace /* anonymous */
{
        bool InitializeImpl(jstring url,
                                jstring database_path,
                                jstring handler_path,
                                jobjectArray attributeKeys,
                                jobjectArray attributeValues) {
        using namespace crashpad;
        // avoid multi initialization
        if (initialized) {
            return true;
        }
        std::map<std::string, std::string> attributes;
        attributes["format"] = "minidump";

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
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Attribute array length doesn't match. Attributes won't be available in the Crashpad integration");
        }

        std::vector<std::string> arguments;
        arguments.push_back("--no-rate-limit");

        // Backtrace url
        const char *backtraceUrl = env->GetStringUTFChars(url, 0);

        // path to crash handler executable
        const char *handlerPath  = env->GetStringUTFChars(handler_path, 0);
        FilePath handler(handlerPath);

        // path to crashpad database
        const char *filePath = env->GetStringUTFChars(database_path, 0);
        FilePath db(filePath);

        std::unique_ptr<CrashReportDatabase> database = CrashReportDatabase::Initialize(db);
        if (database == nullptr || database->GetSettings() == NULL) {
            return false;
        }

        /* Enable automated uploads. */
        database->GetSettings()->SetUploadsEnabled(true);

        // Start crash handler
        client = new CrashpadClient();

        initialized = client->StartHandlerAtCrash(handler, db, db, backtraceUrl, attributes, arguments);

        env->ReleaseStringUTFChars(url, backtraceUrl);
        env->ReleaseStringUTFChars(handler_path, handlerPath);
        env->ReleaseStringUTFChars(database_path, filePath);
        return initialized;
    }
}

extern "C" {
    void Crash() {
        *(volatile int *) 0 = 0;
    }

    JNIEXPORT void JNICALL Java_backtraceio_library_base_BacktraceBase_Crash(
            JNIEnv *env,
            jobject /* this */) {
        Crash();
    }

    bool Initialize(jstring url,
                    jstring database_path,
                    jstring handler_path,
                    jobjectArray attributeKeys,
                    jobjectArray attributeValues) {
        static std::once_flag initialize_flag;

        std::call_once(initialize_flag, [&]{
            initialized = InitializeImpl(url,
                database_path, handler_path,
                attributeKeys, attributeValues);
        });
        return initialized;
    }

    JNIEXPORT jboolean JNICALL
    Java_backtraceio_library_BacktraceDatabase_Initialize(JNIEnv *env,
                                                          jobject thiz,
                                                          jstring url,
                                                          jstring database_path,
                                                          jstring handler_path,
                                                          jobjectArray attributeKeys,
                                                          jobjectArray attributeValues) {
        return Initialize(url, database_path, handler_path, attributeKeys, attributeValues);
    }


    void AddAttribute(jstring key, jstring value) {
        if (initialized == false) {
            __android_log_print(ANDROID_LOG_WARN, "Backtrace-Android", "Crashpad integration isn't available. Please initialize the Crashpad integration first.");
            return;
        }
        const std::lock_guard<std::mutex> lock(attribute_synchronization);
        crashpad::CrashpadInfo* info = crashpad::CrashpadInfo::GetCrashpadInfo();
        crashpad::SimpleStringDictionary* annotations = info->simple_annotations();
        if (!annotations)
        {
            annotations = new crashpad::SimpleStringDictionary();
            info->set_simple_annotations(annotations);
        }
        jboolean isCopy;
        const char* crashpadKey = env->GetStringUTFChars(key, &isCopy);
        const char* crashpadValue = env->GetStringUTFChars(value, &isCopy);
        if (crashpadKey && crashpadValue)
            annotations->SetKeyValue(crashpadKey, crashpadValue);

        env->ReleaseStringUTFChars(key, crashpadKey);
        env->ReleaseStringUTFChars(value, crashpadValue);
    }

    JNIEXPORT void JNICALL
    Java_backtraceio_library_BacktraceDatabase_AddAttribute(JNIEnv *env, jobject thiz,
                                                                     jstring name, jstring value) {
        AddAttribute(name, value);
    }
}