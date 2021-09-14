#ifndef BACKTRACE_ANDROID_CLIENT_SIDE_UNWINDING_H
#define BACKTRACE_ANDROID_CLIENT_SIDE_UNWINDING_H

#include <android/log.h>
#include <jni.h>

#if CLIENT_SIDE_UNWINDING
// Possible modes for client side unwinding
enum class UnwindingMode {
    INVALID = -1,
    LOCAL = 0,
    REMOTE,
    REMOTE_DUMPWITHOUTCRASH,
    LOCAL_DUMPWITHOUTCRASH,
    LOCAL_CONTEXT
};
#define UNWINDING_MODE_DEFAULT (jint) UnwindingMode::REMOTE_DUMPWITHOUTCRASH
#else
#define UNWINDING_MODE_DEFAULT -1
#endif

bool EnableClientSideUnwinding(JNIEnv *env, const char* path, jint unwindingMode);

void SetCrashpadHandlerForClientSideUnwinding();

jint ExtractClientSideUnwindingMode(JNIEnv *env, jobject unwindingMode);

#endif //BACKTRACE_ANDROID_CLIENT_SIDE_UNWINDING_H
