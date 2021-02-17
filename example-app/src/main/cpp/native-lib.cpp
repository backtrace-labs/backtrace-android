#include <jni.h>
#include <signal.h>
#include "backtrace-android.h"

extern "C"
{
JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_cppCrash(JNIEnv *env, jobject thiz) {
    raise(SIGSEGV);
}

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
Java_backtraceio_backtraceio_MainActivity_registerNativeBreadcrumbs(JNIEnv *env, jobject thiz, jobject backtrace_breadcrumbs) {
    return Backtrace::InitializeNativeBreadcrumbs(env, backtrace_breadcrumbs);
}
}extern "C"
JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_cleanupNativeBreadcrumbHandler(JNIEnv *env,
                                                                         jobject thiz) {
    Backtrace::Cleanup(env);
}