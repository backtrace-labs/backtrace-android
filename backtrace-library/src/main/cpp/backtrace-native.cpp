#include <atomic>
#include <map>
#include <mutex>
#include <string>
#include <unistd.h>
#include <vector>

#include <jni.h>
#include <string>
#include <android/log.h>
#include <unordered_map>
#include <libgen.h>

#include "base/logging.h"
#include "client/crashpad_client.h"
#include "client/crashpad_info.h"
#include "client/crash_report_database.h"
#include "client/settings.h"

#include "bun/bun.h"
#include "bun/stream.h"
#include "libbun/src/bun_internal.h"

#include <sys/syscall.h>

using namespace base;

// supported JNI version
static jint JNI_VERSION = JNI_VERSION_1_6;

// Java VM
static JavaVM *javaVm;

// crashpad client
static crashpad::CrashpadClient *client;

// check if crashpad client is already initialized
static std::atomic_bool initialized;
static std::mutex attribute_synchronization;
static std::string thread_id;

// bun handle
struct bun_handle handle;
// bun buffer
char buf[65536];

struct bun_buffer bun_buf;

/*
 * Signal handler executed by CrashpadClient::SetFirstChanceExceptionHandler.
 */
static bool bun_sighandler(int signum, siginfo_t *info, ucontext_t *context)
{
    (void) signum;
    (void) info;
    (void) context;

    bun_unwind(&handle, &bun_buf);

    return false;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env;
    if (jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION) != JNI_OK) {

        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "%s",
                            "Cannot load the JNI env");
        return JNI_ERR;
    }
    javaVm = jvm;
    thread_id = std::to_string(gettid());
    return JNI_VERSION_1_4;
}

namespace /* anonymous */
{
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

    bool InitializeImpl(jstring url,
                        jstring database_path,
                        jstring handler_path,
                        jobjectArray attributeKeys,
                        jobjectArray attributeValues,
                        jobjectArray attachmentPaths = nullptr,
                        jboolean enableClientSideUnwinding = false) {
        using namespace crashpad;
        // avoid multi initialization
        if (initialized) {
            return true;
        }

        JNIEnv *env = GetJniEnv();
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
            return false;
        }

        std::map<std::string, std::string> attributes;
        attributes["format"] = "minidump";
        // save native main thread id
        if(!thread_id.empty()) {
            attributes["thread.main"] = thread_id;
        }

        jint keyLength = env->GetArrayLength(attributeKeys);
        jint valueLength = env->GetArrayLength(attributeValues);
        if (keyLength == valueLength) {
            for (int attributeIndex = 0; attributeIndex < keyLength; ++attributeIndex) {
                jstring jstringKey = (jstring) env->GetObjectArrayElement(attributeKeys,
                                                                          attributeIndex);
                jboolean isCopy;
                const char *convertedKey = (env)->GetStringUTFChars(jstringKey, &isCopy);

                jstring stringValue = (jstring) env->GetObjectArrayElement(attributeValues,
                                                                           attributeIndex);
                const char *convertedValue = (env)->GetStringUTFChars(stringValue, &isCopy);

                if (!convertedKey || !convertedValue)
                    continue;

                attributes[convertedKey] = convertedValue;

                env->ReleaseStringUTFChars(jstringKey, convertedKey);
                env->ReleaseStringUTFChars(stringValue, convertedValue);
            }
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                "Attribute array length doesn't match. Attributes won't be available in the Crashpad integration");
        }

        std::vector<std::string> arguments;
        arguments.push_back("--no-rate-limit");

        // Backtrace url
        const char *backtraceUrl = env->GetStringUTFChars(url, 0);

        // path to crash handler executable
        const char *handlerPath = env->GetStringUTFChars(handler_path, 0);
        FilePath handler(handlerPath);

        // path to crashpad database
        const char *filePath = env->GetStringUTFChars(database_path, 0);
        FilePath db(filePath);

        // paths to file attachments
        if (attachmentPaths != nullptr) {
            jint attachmentsLength = env->GetArrayLength(attachmentPaths);
            for (int attachmentIndex = 0; attachmentIndex < attachmentsLength; ++attachmentIndex) {
                jstring jstringAttachmentPath = (jstring) env->GetObjectArrayElement(
                        attachmentPaths,
                        attachmentIndex);
                jboolean isCopy;
                const char *convertedAttachmentPath = (env)->GetStringUTFChars(
                        jstringAttachmentPath, &isCopy);

                if (!convertedAttachmentPath)
                    continue;

                std::string attachmentBaseName = basename(convertedAttachmentPath);

                std::string attachmentArgumentString("--attachment=");
                attachmentArgumentString += attachmentBaseName;
                attachmentArgumentString += "=";
                attachmentArgumentString += convertedAttachmentPath;
                arguments.push_back(attachmentArgumentString);

                env->ReleaseStringUTFChars(jstringAttachmentPath, convertedAttachmentPath);
            }
        }

        std::unique_ptr<CrashReportDatabase> database = CrashReportDatabase::Initialize(db);
        if (database == nullptr || database->GetSettings() == NULL) {
            return false;
        }

        /* Enable automated uploads. */
        database->GetSettings()->SetUploadsEnabled(true);

        // Start crash handler
        client = new CrashpadClient();

        initialized = client->StartHandlerAtCrash(handler, db, db, backtraceUrl, attributes,
                                                  arguments);

        env->ReleaseStringUTFChars(url, backtraceUrl);
        env->ReleaseStringUTFChars(handler_path, handlerPath);
        env->ReleaseStringUTFChars(database_path, filePath);
        return initialized;
    }
}

extern "C" {
void Crash() {
    *(volatile int *) 0 = 0;
}

void DumpWithoutCrash(jstring message, jboolean set_main_thread_as_faulting_thread) {
    crashpad::NativeCPUContext context;
    crashpad::CaptureContext(&context);

    // set dump message for single report
    crashpad::SimpleStringDictionary *annotations = NULL;
    bool did_attach = false;

    if (message != NULL || set_main_thread_as_faulting_thread == true) {
        JNIEnv *env = GetJniEnv();
        if (env == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
            return;
        }
        const std::lock_guard<std::mutex> lock(attribute_synchronization);
        crashpad::CrashpadInfo *info = crashpad::CrashpadInfo::GetCrashpadInfo();
        annotations = info->simple_annotations();
        if (!annotations) {
            annotations = new crashpad::SimpleStringDictionary();
            info->set_simple_annotations(annotations);
        }
        if(set_main_thread_as_faulting_thread == true) {
            annotations->SetKeyValue("_mod_faulting_tid", thread_id);
        }
        if(message != NULL) {
            // user can't override error.message - exception message that Crashpad/crash-reporting tool
            // will set to tell user about error message. This code will set error.message only for single
            // report and after creating a dump, method will clean up this attribute.
            jboolean isCopy;
            const char *rawMessage = env->GetStringUTFChars(message, &isCopy);
            annotations->SetKeyValue("error.message", rawMessage);
            env->ReleaseStringUTFChars(message, rawMessage);
        }
    }
    client->DumpWithoutCrash(&context);

    if (annotations != NULL) {
        annotations->RemoveKey("error.message");
    }
}

JNIEXPORT void JNICALL Java_backtraceio_library_base_BacktraceBase_crash(
        JNIEnv *env,
        jobject /* this */) {
    Crash();
}

bool Initialize(jstring url,
                jstring database_path,
                jstring handler_path,
                jobjectArray attributeKeys,
                jobjectArray attributeValues,
                jobjectArray attachmentPaths = nullptr,
                jboolean enableClientSideUnwinding = false) {
    static std::once_flag initialize_flag;

    std::call_once(initialize_flag, [&] {
        initialized = InitializeImpl(url,
                                     database_path, handler_path,
                                     attributeKeys, attributeValues,
                                     attachmentPaths, enableClientSideUnwinding);
    });
    return initialized;
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
                                                      jboolean enableClientSideUnwinding = false) {
    return Initialize(url, database_path, handler_path, attributeKeys,
                      attributeValues, attachmentPaths, enableClientSideUnwinding);
}


void AddAttribute(jstring key, jstring value) {
    if (initialized == false) {
        __android_log_print(ANDROID_LOG_WARN, "Backtrace-Android",
                            "Crashpad integration isn't available. Please initialize the Crashpad integration first.");
        return;
    }
    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return;
    }

    const std::lock_guard<std::mutex> lock(attribute_synchronization);
    crashpad::CrashpadInfo *info = crashpad::CrashpadInfo::GetCrashpadInfo();
    crashpad::SimpleStringDictionary *annotations = info->simple_annotations();
    if (!annotations) {
        annotations = new crashpad::SimpleStringDictionary();
        info->set_simple_annotations(annotations);
    }

    jboolean isCopy;
    const char *crashpadKey = env->GetStringUTFChars(key, &isCopy);
    const char *crashpadValue = env->GetStringUTFChars(value, &isCopy);
    if (crashpadKey && crashpadValue)
        annotations->SetKeyValue(crashpadKey, crashpadValue);

    env->ReleaseStringUTFChars(key, crashpadKey);
    env->ReleaseStringUTFChars(value, crashpadValue);
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

bool EnableClientSideUnwinding() {
    if (!initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Crashpad needs to be initialized to enable client-side unwinding");
        return false;
    }
    if (!bun_handle_init(&handle, BUN_BACKEND_LIBUNWINDSTACK)) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Could not initialize bun_handle");
    }
    if (!bun_buffer_init(&bun_buf, buf, sizeof(buf))) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Could not initialize bun_buffer");
    }
    crashpad::CrashpadInfo::GetCrashpadInfo()
            ->AddUserDataMinidumpStream(BUN_STREAM_ID, buf, sizeof(buf));
    crashpad::CrashpadClient::SetFirstChanceExceptionHandler(bun_sighandler);
    return true;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_backtraceio_library_BacktraceDatabase_enableClientSideUnwinding(JNIEnv *env, jobject thiz) {
    return EnableClientSideUnwinding();
}
}