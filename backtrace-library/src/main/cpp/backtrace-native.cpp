#include "backtrace-native.h"

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

#include <sys/system_properties.h>

#include "backend.h"
#include "client-side-unwinding.h"

// supported JNI version
static jint JNI_VERSION = JNI_VERSION_1_6;

// Java VM
static JavaVM *javaVm;

// check if native crash client is already initialized
std::atomic_bool initialized;
// check if native crash client is disabled
std::atomic_bool disabled;
std::mutex attribute_synchronization;
std::string thread_id;

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

extern "C" {
void Crash() {
    *(volatile int *) 0 = 0;
}

#ifndef USE_BREAKPAD

#endif

JNIEXPORT void JNICALL Java_backtraceio_library_base_BacktraceBase_crash(
        JNIEnv *env,
        jobject /* this */) {
    Crash();
}

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
    jint unwindingModeInt = ExtractClientSideUnwindingMode(env, unwindingMode);
    return Initialize(url, database_path, handler_path, attributeKeys,
                      attributeValues, attachmentPaths, enableClientSideUnwinding,
                      unwindingModeInt);
}

JNIEXPORT jboolean JNICALL
Java_backtraceio_library_nativeCalls_BacktraceCrashHandler_initializeCrashHandler(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring url,
                                                                                  jstring database_path,
                                                                                  jstring handler_path,
                                                                                  jobjectArray attribute_keys,
                                                                                  jobjectArray attribute_values,
                                                                                  jobjectArray attachment_paths,
                                                                                  jboolean enable_client_side_unwinding,
                                                                                  jobject unwinding_mode) {
    return Java_backtraceio_library_BacktraceDatabase_initialize(env, thiz, url, database_path,
                                                                 handler_path, attribute_keys,
                                                                 attribute_values, attachment_paths,
                                                                 enable_client_side_unwinding,
                                                                 unwinding_mode);
}

JNIEXPORT jboolean JNICALL
Java_backtraceio_library_nativeCalls_BacktraceCrashHandler_initializeJavaCrashHandler(JNIEnv *env,
                                                                                      jobject thiz,
                                                                                      jstring url,
                                                                                      jstring database_path,
                                                                                      jstring class_path,
                                                                                      jobjectArray attributeKeys,
                                                                                      jobjectArray attributeValues,
                                                                                      jobjectArray attachmentPaths = nullptr,
                                                                                      jobjectArray environmentVariables = nullptr) {
    return InitializeJavaCrashHandler(url, database_path, class_path, attributeKeys,
                                      attributeValues, attachmentPaths, environmentVariables);
}

JNIEXPORT jboolean JNICALL
Java_backtraceio_library_BacktraceDatabase_initializeJavaCrashHandler(JNIEnv *env,
                                                                      jobject thiz,
                                                                      jstring url,
                                                                      jstring database_path,
                                                                      jstring class_path,
                                                                      jobjectArray attributeKeys,
                                                                      jobjectArray attributeValues,
                                                                      jobjectArray attachmentPaths = nullptr,
                                                                      jobjectArray environmentVariables = nullptr) {
    return InitializeJavaCrashHandler(url, database_path, class_path, attributeKeys,
                                      attributeValues, attachmentPaths, environmentVariables);
}

JNIEXPORT void JNICALL
Java_backtraceio_library_BacktraceDatabase_addAttribute(JNIEnv *env, jobject thiz,
                                                        jstring name, jstring value) {
    AddAttribute(name, value);
}

JNIEXPORT void JNICALL
Java_backtraceio_library_base_BacktraceBase_dumpWithoutCrash__Ljava_lang_String_2(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring message) {
    DumpWithoutCrash(message, false);
}

JNIEXPORT void JNICALL
Java_backtraceio_library_base_BacktraceBase_dumpWithoutCrash__Ljava_lang_String_2Z(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jstring message,
                                                                                   jboolean set_main_thread_as_faulting_thread) {
    DumpWithoutCrash(message, set_main_thread_as_faulting_thread);
}

JNIEXPORT void JNICALL
Java_backtraceio_library_BacktraceDatabase_disable(JNIEnv *env, jobject thiz) {
    Disable();
}

JNIEXPORT jboolean JNICALL
Java_backtraceio_library_nativeCalls_BacktraceCrashHandler_handleCrash(JNIEnv *env, jclass clazz,
                                                                       jobjectArray args) {
    return CaptureCrash(args);
}
}