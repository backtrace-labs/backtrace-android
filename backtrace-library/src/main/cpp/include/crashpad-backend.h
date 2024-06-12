#ifndef BACKTRACE_ANDROID_CRASHPAD_BACKEND_H
#define BACKTRACE_ANDROID_CRASHPAD_BACKEND_H

#include "base/logging.h"
#include "client/crashpad_client.h"
#include "client/crashpad_info.h"
#include "client/crash_report_database.h"
#include "client/settings.h"

#include "client-side-unwinding.h"

#include <jni.h>

bool InitializeCrashpad(jstring url,
                        jstring database_path,
                        jstring handler_path,
                        jobjectArray attributeKeys,
                        jobjectArray attributeValues,
                        jobjectArray attachmentPaths = nullptr,
                        jboolean enableClientSideUnwinding = false,
                        jint unwindingMode = UNWINDING_MODE_DEFAULT);

bool InitializeCrashpadJavaCrashHandler(jstring url,
                                        jstring database_path,
                                        jstring class_path,
                                        jobjectArray attributeKeys,
                                        jobjectArray attributeValues,
                                        jobjectArray attachmentPaths = nullptr,
                                        jobjectArray environmentVariables = nullptr);

bool CaptureCrashCrashpad(jobjectArray args);

void DumpWithoutCrashCrashpad(jstring message, jboolean set_main_thread_as_faulting_thread);

void AddAttributeCrashpad(jstring key, jstring value);

void DisableCrashpad();

void ReEnableCrashpad();

#endif //BACKTRACE_ANDROID_CRASHPAD_BACKEND_H
