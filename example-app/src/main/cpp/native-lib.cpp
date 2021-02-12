#include <jni.h>
#include <string>
#include <signal.h>
#include <unordered_map>

// GetMethodId is expensive so let's do it once and keep the result here
jweak backtraceBase = nullptr;
jmethodID addBreadcrumbMethodId = nullptr;

bool AddBreadcrumb(JNIEnv *env,
                   const char* message,
                   std::unordered_map<std::string, std::string>* attributes = nullptr);

extern "C"
JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_cppCrash(JNIEnv *env, jobject thiz) {
    raise(SIGSEGV);
}

extern "C"
JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_addNativeBreadcrumb(JNIEnv *env, jobject thiz) {
    std::unordered_map<std::string, std::string> attributes;
    attributes["C++"] = "true";
    attributes["Java"] = "false";

    AddBreadcrumb(env, "MyNativeBreadcrumb", &attributes);
}

extern "C"
JNIEXPORT void JNICALL
Java_backtraceio_backtraceio_MainActivity_registerNativeBreadcrumbs(JNIEnv *env, jobject thiz,
                                                                    jobject backtrace_base) {
    backtraceBase = env->NewWeakGlobalRef(backtrace_base);
    jclass backtraceBaseCls = env->GetObjectClass(backtrace_base);
    addBreadcrumbMethodId = env->GetMethodID(backtraceBaseCls, "addBreadcrumb", "(Ljava/lang/String;Ljava/util/Map;)Z");
}



// Attribution: https://stackoverflow.com/a/53624436/15063264
jobject StlStringStringMapToJavaHashMap(JNIEnv *env, const std::unordered_map<std::string, std::string>& map);

// Add a breadcrumb from C++
bool AddBreadcrumb(JNIEnv *env,
                   const char* message,
                   std::unordered_map<std::string, std::string>* attributes) {
    if (backtraceBase == nullptr) {
        return false;
    }

    // Convert a C++ hash map of attributes to Java
    jobject jAttributes = StlStringStringMapToJavaHashMap(env, *attributes);

    jstring jMessage = env->NewStringUTF(message);

    return env->CallBooleanMethod(backtraceBase, addBreadcrumbMethodId, jMessage, jAttributes);
}

// Attribution: https://stackoverflow.com/a/53624436/15063264
jobject StlStringStringMapToJavaHashMap(JNIEnv *env, const std::unordered_map<std::string, std::string>& map) {
    jclass mapClass = env->FindClass("java/util/HashMap");
    if(mapClass == NULL)
        return NULL;

    jmethodID init = env->GetMethodID(mapClass, "<init>", "()V");
    jobject hashMap = env->NewObject(mapClass, init);
    jmethodID put = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    std::unordered_map<std::string, std::string>::const_iterator citr = map.begin();
    for( ; citr != map.end(); ++citr) {
        jstring keyJava = env->NewStringUTF(citr->first.c_str());
        jstring valueJava = env->NewStringUTF(citr->second.c_str());

        env->CallObjectMethod(hashMap, put, keyJava, valueJava);

        env->DeleteLocalRef(keyJava);
        env->DeleteLocalRef(valueJava);
    }

    jobject hashMapGobal = static_cast<jobject>(env->NewGlobalRef(hashMap));
    env->DeleteLocalRef(hashMap);
    env->DeleteLocalRef(mapClass);

    return hashMapGobal;
}