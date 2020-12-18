#include <jni.h>
#include <string>
#include <signal.h>

extern "C"
JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_cppCrash(JNIEnv *env, jobject thiz) {
    raise(SIGSEGV);
}