#ifndef BACKTRACE_ANDROID_BACKEND_H
#define BACKTRACE_ANDROID_BACKEND_H

#include <jni.h>
#include "client-side-unwinding.h"

extern "C" {
bool Initialize(jstring url,
                jstring database_path,
                jstring handler_path,
                jobjectArray attributeKeys,
                jobjectArray attributeValues,
                jobjectArray attachmentPaths = nullptr,
                jboolean enableClientSideUnwinding = false,
                jint unwindingMode = UNWINDING_MODE_DEFAULT);

void DumpWithoutCrash(jstring message, jboolean set_main_thread_as_faulting_thread);

void AddAttribute(jstring key, jstring value);

void Disable();

bool EnableCrashLoopDetection();

bool IsSafeModeRequired();

int ConsecutiveCrashesCount();
}

#endif //BACKTRACE_ANDROID_BACKEND_H
