#include <jni.h>
#include <signal.h>
#include <unistd.h>
#include "backtrace-android.h"

void * volatile always_null;
void anotherCrash()
{
    memset(always_null, 0x42, 1 << 20);
}

extern "C"
{
JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_cppCrash(JNIEnv *env, jobject thiz) {
    __builtin_trap();
    //anotherCrash();
}

////////////////// Begin Native Breadcrumb Examples //////////////////
// These are just some examples showing how to add breadcrumbs natively using backtrace-android.h
// You most likely will be calling these Backtrace:: functions directly from your native C++ code
JNIEXPORT jboolean JNICALL
Java_backtraceio_backtraceio_MainActivity_addNativeBreadcrumb(JNIEnv *env, jobject thiz) {
    std::unordered_map<std::string, std::string> attributes;
    attributes["C++"] = "true";
    attributes["Java"] = "false";

    return Backtrace::AddBreadcrumb(env, "My Native Breadcrumb", &attributes);
}

JNIEXPORT jboolean JNICALL
Java_backtraceio_backtraceio_MainActivity_addNativeBreadcrumbUserError(JNIEnv *env, jobject thiz) {
    std::unordered_map<std::string, std::string> attributes;

    return Backtrace::AddBreadcrumb(env, "My Native Breadcrumb", &attributes,
                                    Backtrace::BreadcrumbType::USER,
                                    Backtrace::BreadcrumbLevel::ERROR);
}

JNIEXPORT jboolean JNICALL
Java_backtraceio_backtraceio_MainActivity_registerNativeBreadcrumbs(JNIEnv *env, jobject thiz,
        jobject backtrace_base) {
    return Backtrace::InitializeNativeBreadcrumbs(env, backtrace_base);
}

JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_cleanupNativeBreadcrumbHandler(JNIEnv *env, jobject thiz) {
    Backtrace::Cleanup(env);
}
//////////////// End Native Breadcrumb Examples ////////////////
}