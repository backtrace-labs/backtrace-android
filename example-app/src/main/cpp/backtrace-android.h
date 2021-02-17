#ifndef BACKTRACE_ANDROID_BACKTRACE_ANDROID_H
#define BACKTRACE_ANDROID_BACKTRACE_ANDROID_H

#include <jni.h>
#include <string>
#include <unordered_map>
#include <android/log.h>
// This is a header including additional Backtrace functionality
// for NDK applications using the `backtrace-android` crash-reporting
// library

////////////////// BEGIN API ///////////////////////

namespace Backtrace {
    /**
     * @see: backtraceio.library.enums.BacktraceBreadcrumbType
     */
    enum class BreadcrumbType {
        MANUAL,
        LOG,
        NAVIGATION,
        HTTP,
        SYSTEM,
        USER,
        CONFIGURATION
    };
    /**
     * @see: backtraceio.library.enums.BacktraceBreadcrumbLevel
     */
    enum class BreadcrumbLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        FATAL
    };

    /**
     * Registers a BacktraceBase object to handle future AddBreadcrumb requests.
     *
     * @param env - The JNI environment
     * @param backtrace_base - A BacktraceBase object
     * @return true if the Breadcrumbs object was registered successfully
     */
    bool InitializeNativeBreadcrumbs(JNIEnv *env, jobject backtrace_base);

    /**
     * Cleanup BacktraceBreadcrumbs
     */
    void Cleanup(JNIEnv *env);

    /**
     * Adds a breadcrumb to the registered native breadcrumb handler
     * @param env - The JNI environment
     * @param message - The message of the breadcrumb
     * @param attributes - Key-value pair of attributes to add to the breadcrumb
     * @return true if the breadcrumb was added successfully, otherwise false
     */
    bool AddBreadcrumb(JNIEnv *env,
                       const char *message,
                       std::unordered_map<std::string, std::string> *attributes = nullptr,
                       Backtrace::BreadcrumbType type = Backtrace::BreadcrumbType::MANUAL,
                       Backtrace::BreadcrumbLevel level = Backtrace::BreadcrumbLevel::INFO);

    ////////////////// END API ///////////////////////

    // The BacktraceBase object
    jweak btBaseWeakGlobalRef = nullptr;
    // The BacktraceBreadcrumb::addBreadcrumb method
    jmethodID addBreadcrumbMethodId = nullptr;

    // Pointer representing Java HashMap class
    jclass mapClassGlobalRef = nullptr;
    // HashMap::init method
    jmethodID initMap = nullptr;
    // HashMap::put method
    jmethodID putMap = nullptr;

    // Pointer representing the BacktraceBreadcrumbType Java enum class
    jclass breadcrumbTypeClass = nullptr;
    // Pointer representing the BacktraceBreadcrumbLevel Java enum class
    jclass breadcrumbLevelClass = nullptr;

    // BreadcrumbType field ids
    jfieldID breadcrumbTypeManualId = nullptr;
    jfieldID breadcrumbTypeLogId = nullptr;
    jfieldID breadcrumbTypeNavigationId = nullptr;
    jfieldID breadcrumbTypeHttpId = nullptr;
    jfieldID breadcrumbTypeSystemId = nullptr;
    jfieldID breadcrumbTypeUserId = nullptr;
    jfieldID breadcrumbTypeConfigurationId = nullptr;

    // BreadcrumbLevel field ids
    jfieldID breadcrumbLevelDebugId = nullptr;
    jfieldID breadcrumbLevelInfoId = nullptr;
    jfieldID breadcrumbLevelWarningId = nullptr;
    jfieldID breadcrumbLevelErrorId = nullptr;
    jfieldID breadcrumbLevelFatalId = nullptr;

    bool InitializeNativeBreadcrumbs(JNIEnv *env, jobject backtrace_base) {
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "JNI env is null");
            return false;
        }

        // We cache the results of these methods for use later because they are expensive:
        // See: https://developer.ibm.com/languages/java/articles/j-jni/#notc
        jclass backtraceBaseCls = env->GetObjectClass(backtrace_base);
        addBreadcrumbMethodId = env->GetMethodID(backtraceBaseCls, "addBreadcrumb",
                                                 "(Ljava/lang/String;Ljava/util/Map;Lbacktraceio/library/enums/BacktraceBreadcrumbType;Lbacktraceio/library/enums/BacktraceBreadcrumbLevel;)Z");
        if (addBreadcrumbMethodId == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Could not find addBreadcrumb method in the supplied breadcrumb class");
            goto except;
        }

        mapClassGlobalRef = static_cast<jclass>(env->NewGlobalRef(
                env->FindClass("java/util/HashMap")));
        initMap = env->GetMethodID(mapClassGlobalRef, "<init>", "()V");
        putMap = env->GetMethodID(mapClassGlobalRef, "put",
                                  "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

        btBaseWeakGlobalRef = env->NewWeakGlobalRef(backtrace_base);
        if (btBaseWeakGlobalRef == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Could not create a JNI global reference to the supplied breadcrumb class");
            goto except;
        }

        breadcrumbTypeClass = static_cast<jclass>(env->NewGlobalRef(
                env->FindClass("backtraceio/library/enums/BacktraceBreadcrumbType")));
        breadcrumbLevelClass = static_cast<jclass>(env->NewGlobalRef(
                env->FindClass("backtraceio/library/enums/BacktraceBreadcrumbLevel")));

        breadcrumbTypeManualId = env->GetStaticFieldID(breadcrumbTypeClass, "MANUAL", "Lbacktraceio/library/enums/BacktraceBreadcrumbType;");
        breadcrumbTypeLogId = env->GetStaticFieldID(breadcrumbTypeClass, "LOG", "Lbacktraceio/library/enums/BacktraceBreadcrumbType;");
        breadcrumbTypeNavigationId = env->GetStaticFieldID(breadcrumbTypeClass, "NAVIGATION", "Lbacktraceio/library/enums/BacktraceBreadcrumbType;");
        breadcrumbTypeHttpId = env->GetStaticFieldID(breadcrumbTypeClass, "HTTP", "Lbacktraceio/library/enums/BacktraceBreadcrumbType;");
        breadcrumbTypeSystemId = env->GetStaticFieldID(breadcrumbTypeClass, "SYSTEM", "Lbacktraceio/library/enums/BacktraceBreadcrumbType;");
        breadcrumbTypeUserId = env->GetStaticFieldID(breadcrumbTypeClass, "USER", "Lbacktraceio/library/enums/BacktraceBreadcrumbType;");
        breadcrumbTypeConfigurationId = env->GetStaticFieldID(breadcrumbTypeClass, "CONFIGURATION", "Lbacktraceio/library/enums/BacktraceBreadcrumbType;");

        breadcrumbLevelDebugId = env->GetStaticFieldID(breadcrumbLevelClass, "DEBUG", "Lbacktraceio/library/enums/BacktraceBreadcrumbLevel;");
        breadcrumbLevelInfoId = env->GetStaticFieldID(breadcrumbLevelClass, "INFO", "Lbacktraceio/library/enums/BacktraceBreadcrumbLevel;");
        breadcrumbLevelWarningId = env->GetStaticFieldID(breadcrumbLevelClass, "WARNING", "Lbacktraceio/library/enums/BacktraceBreadcrumbLevel;");
        breadcrumbLevelErrorId = env->GetStaticFieldID(breadcrumbLevelClass, "ERROR", "Lbacktraceio/library/enums/BacktraceBreadcrumbLevel;");
        breadcrumbLevelFatalId = env->GetStaticFieldID(breadcrumbLevelClass, "FATAL", "Lbacktraceio/library/enums/BacktraceBreadcrumbLevel;");

        // JNI functions should always check for exceptions before returning
        // See: https://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.vm.80.doc/docs/jni_exceptions.html#jni_exceptions
        except:
        jboolean flag = env->ExceptionCheck();
        if (flag) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Detected JNI exception");
            return false;
        }
        return true;
    }

    // Attribution: https://stackoverflow.com/a/53624436/15063264
    jobject StlStringStringMapToJavaHashMap(JNIEnv *env,
                                            const std::unordered_map<std::string, std::string> &map) {
        if (mapClassGlobalRef == nullptr || initMap == nullptr || putMap == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Required object(s) are null");
            return nullptr;
        }
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "JNI env is null");
            return nullptr;
        }

        jobject hashMap = env->NewObject(mapClassGlobalRef, initMap);

        std::unordered_map<std::string, std::string>::const_iterator citr = map.begin();
        for (; citr != map.end(); ++citr) {
            jstring keyJava = env->NewStringUTF(citr->first.c_str());
            jstring valueJava = env->NewStringUTF(citr->second.c_str());

            env->CallObjectMethod(hashMap, putMap, keyJava, valueJava);

            env->DeleteLocalRef(keyJava);
            env->DeleteLocalRef(valueJava);
        }

        jobject hashMapGlobal = static_cast<jobject>(env->NewGlobalRef(hashMap));
        env->DeleteLocalRef(hashMap);

        jboolean flag = env->ExceptionCheck();
        if (flag) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Detected JNI exception");
            return nullptr;
        }
        return hashMapGlobal;
    }

    jobject ConvertToJavaBreadcrumbType(JNIEnv* env, Backtrace::BreadcrumbType type) {
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "JNI env is null");
            return nullptr;
        }

        switch (type) {
            case BreadcrumbType::CONFIGURATION:
                return env->GetStaticObjectField(breadcrumbTypeClass, breadcrumbTypeConfigurationId);
            case BreadcrumbType::HTTP:
                return env->GetStaticObjectField(breadcrumbTypeClass, breadcrumbTypeHttpId);
            case BreadcrumbType::LOG:
                return env->GetStaticObjectField(breadcrumbTypeClass, breadcrumbTypeLogId);
            case BreadcrumbType::NAVIGATION:
                return env->GetStaticObjectField(breadcrumbTypeClass, breadcrumbTypeNavigationId);
            case BreadcrumbType::SYSTEM:
                return env->GetStaticObjectField(breadcrumbTypeClass, breadcrumbTypeSystemId);
            case BreadcrumbType::USER:
                return env->GetStaticObjectField(breadcrumbTypeClass, breadcrumbTypeUserId);
            case BreadcrumbType::MANUAL:
            default:
                return env->GetStaticObjectField(breadcrumbTypeClass, breadcrumbTypeManualId);
        }
    }

    jobject ConvertToJavaBreadcrumbLevel(JNIEnv* env, Backtrace::BreadcrumbLevel Level) {
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "JNI env is null");
            return nullptr;
        }

        switch (Level) {
            case BreadcrumbLevel::DEBUG:
                return env->GetStaticObjectField(breadcrumbLevelClass, breadcrumbLevelDebugId);
            case BreadcrumbLevel::WARNING:
                return env->GetStaticObjectField(breadcrumbLevelClass, breadcrumbLevelWarningId);
            case BreadcrumbLevel::ERROR:
                return env->GetStaticObjectField(breadcrumbLevelClass, breadcrumbLevelErrorId);
            case BreadcrumbLevel::FATAL:
                return env->GetStaticObjectField(breadcrumbLevelClass, breadcrumbLevelFatalId);
            case BreadcrumbLevel::INFO:
            default:
                return env->GetStaticObjectField(breadcrumbLevelClass, breadcrumbLevelInfoId);
        }
    }


    // Add a breadcrumb from C++
    bool AddBreadcrumb(JNIEnv *env,
                       const char *message,
                       std::unordered_map<std::string, std::string> *attributes,
                       Backtrace::BreadcrumbType type,
                       Backtrace::BreadcrumbLevel level) {
        if (btBaseWeakGlobalRef == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Breadcrumbs object is null");
            return false;
        }
        if (message == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "message is null");
            return false;
        }
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "JNI env is null");
            return false;
        }

        jobject jAttributes = nullptr;
        if (attributes != nullptr) {
            // Convert a C++ hash map of attributes to Java
            jAttributes = StlStringStringMapToJavaHashMap(env, *attributes);
        }

        jobject jbreadcrumbType = ConvertToJavaBreadcrumbType(env, type);
        jobject jbreadcrumbLevel = ConvertToJavaBreadcrumbLevel(env, level);

        jobject backtraceBreadcrumbsLocal = env->NewLocalRef(btBaseWeakGlobalRef);
        jstring jMessage = env->NewStringUTF(message);

        bool success = false;
        success = env->CallBooleanMethod(backtraceBreadcrumbsLocal, addBreadcrumbMethodId, jMessage,
                                         jAttributes, jbreadcrumbType, jbreadcrumbLevel);
        if (success == false) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Java addBreadcrumbs call failed");
            goto except;
        }
        env->DeleteLocalRef(jMessage);
        env->DeleteLocalRef(backtraceBreadcrumbsLocal);

        if (attributes != nullptr) {
            env->DeleteGlobalRef(jAttributes);
        }

        except:
        jboolean flag = env->ExceptionCheck();
        if (flag) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "Detected JNI exception");
            return false;
        }
        return success;
    }

    void Cleanup(JNIEnv *env) {
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                                "JNI env is null");
            return;
        }
        env->DeleteWeakGlobalRef(btBaseWeakGlobalRef);
        env->DeleteGlobalRef(mapClassGlobalRef);
        env->DeleteGlobalRef(breadcrumbTypeClass);
        env->DeleteGlobalRef(breadcrumbLevelClass);
        btBaseWeakGlobalRef = nullptr;
        mapClassGlobalRef = nullptr;
        breadcrumbTypeClass = nullptr;
        breadcrumbLevelClass = nullptr;
    }
}
#endif //BACKTRACE_ANDROID_BACKTRACE_ANDROID_H
