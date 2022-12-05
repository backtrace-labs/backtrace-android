#include "backend.h"
#include "client-side-unwinding.h"
#include <android/log.h>
#include <atomic>
#include <mutex>

#ifdef CRASHPAD_BACKEND
#include "crashpad-backend.h"
#elif BREAKPAD_BACKEND
#include "breakpad-backend.h"
#endif

extern std::atomic_bool initialized;

extern "C" {
bool Initialize(jstring url,
                jstring database_path,
                jstring handler_path,
                jobjectArray attributeKeys,
                jobjectArray attributeValues,
                jobjectArray attachmentPaths,
                jboolean enableClientSideUnwinding,
                jint unwindingMode) {
    static std::once_flag initialize_flag;

    std::call_once(initialize_flag, [&] {
#ifdef CRASHPAD_BACKEND
        initialized = InitializeCrashpad(url,
                                         database_path, handler_path,
                                         attributeKeys, attributeValues,
                                         attachmentPaths, enableClientSideUnwinding,
                                         unwindingMode);
#elif BREAKPAD_BACKEND
        initialized = InitializeBreakpad(url,
                                         database_path, handler_path,
                                         attributeKeys, attributeValues,
                                         attachmentPaths, enableClientSideUnwinding,
                                         unwindingMode);
#else
        initialized = false;
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "No native crash reporting backend defined");
#endif
    });

#ifdef CRASHPAD_BACKEND
    if (initialized) {
        ReEnableCrashpad();
    }
#endif

    return initialized;
}

void DumpWithoutCrash(jstring message, jboolean set_main_thread_as_faulting_thread) {
#ifdef CRASHPAD_BACKEND
    DumpWithoutCrashCrashpad(message, set_main_thread_as_faulting_thread);
#elif BREAKPAD_BACKEND
    DumpWithoutCrashBreakpad(message, set_main_thread_as_faulting_thread);
#else
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "DumpWithoutCrash not supported on this backend");
#endif
}

void AddAttribute(jstring key, jstring value) {
#ifdef CRASHPAD_BACKEND
    AddAttributeCrashpad(key, value);
#elif BREAKPAD_BACKEND
    AddAttributeBreakpad(key, value);
#else
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "AddAttribute not supported on this backend");
#endif
}

void Disable() {
#ifdef CRASHPAD_BACKEND
    DisableCrashpad();
#else
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Disable not supported on this backend");
#endif
}

bool EnableCrashLoopDetectionBackend() {
#ifdef CRASHPAD_BACKEND
    return EnableCrashLoopDetectionCrashpad();
#elif BREAKPAD_BACKEND
    return false;
#endif
}

bool IsSafeModeRequiredBackend() {
#ifdef CRASHPAD_BACKEND
    return IsSafeModeRequiredCrashpad();
#elif BREAKPAD_BACKEND
    return false;
#endif
}

int ConsecutiveCrashesCountBackend() {
#ifdef CRASHPAD_BACKEND
    return ConsecutiveCrashesCountCrashpad();
#elif BREAKPAD_BACKEND
    return 0;
#endif
}

}