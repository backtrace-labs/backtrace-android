#ifndef BACKTRACE_ANDROID_BREAKPAD_BACKEND_H
#define BACKTRACE_ANDROID_BREAKPAD_BACKEND_H

#include "exception_handler.h"
#include "common/linux/http_upload.h"
#include "cacert.h"
#include "client-side-unwinding.h"
#include <jni.h>

int InitializeBreakpad(jstring url,
                       jstring database_path,
                       jstring handler_path,
                       jobjectArray attributeKeys,
                       jobjectArray attributeValues,
                       jobjectArray attachmentPaths = nullptr,
                       jboolean enableClientSideUnwinding = false,
                       jint unwindingMode = UNWINDING_MODE_DEFAULT);

void DumpWithoutCrashBreakpad(jstring message, jboolean set_main_thread_as_faulting_thread);

void AddAttributeBreakpad(jstring key, jstring value);

#endif //BACKTRACE_ANDROID_BREAKPAD_BACKEND_H
