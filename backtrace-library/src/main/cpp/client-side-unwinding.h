#ifndef BACKTRACE_ANDROID_CLIENT_SIDE_UNWINDING_H
#define BACKTRACE_ANDROID_CLIENT_SIDE_UNWINDING_H

#if __NDK_MAJOR__ >= 21 && __ANDROID_API__ >= 23
#include <sys/syscall.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/ptrace.h>

#include "bun/bun.h"
#include "bun/stream.h"
#include "bun/utils.h"
#include "libbun/src/bun_internal.h"
#include "bcd.h"

extern std::atomic_bool initialized;

// Default size of bun buffer.
#define BUFFER_SIZE	65536

// bun handle
struct bun_handle handle;
static struct bun_buffer buf;

// This descriptor is shared between the child and parent.
static int buffer_fd;
static char *buffer_child;

// Is bun initialized?
static std::atomic_bool bun_initialized;

// bcd instance for remote unwinding
static bcd_t bcd;

// Possible modes for client side unwinding
enum class UnwindingMode {
    INVALID = -1,
    LOCAL = 0,
    REMOTE,
    REMOTE_DUMPWITHOUTCRASH,
    LOCAL_DUMPWITHOUTCRASH,
    LOCAL_CONTEXT
};

// Store the unwinding mode
UnwindingMode unwinding_mode = UnwindingMode::INVALID;

static void *
buffer_reader(int fd, int flags) {
    void *r;

    r = mmap(NULL, BUFFER_SIZE, flags, MAP_SHARED, fd, 0);
    if (r == MAP_FAILED) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Could not create memory mapped file for client side unwinding");
    }

    return r;
}

static void *
buffer_create(void) {
    void *r;
    int fd;

    fd = bun_memfd_create("_backtrace_buffer", MFD_CLOEXEC);
    if (fd == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Could not create anonymous file for client side unwinding");
        return nullptr;
    }

    if (ftruncate(fd, BUFFER_SIZE) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Could not truncate anonymous file to desired size for client side unwinding");
        return nullptr;
    }

    buffer_fd = fd;

    r = buffer_reader(buffer_fd, PROT_READ|PROT_WRITE);

    return r;
}

static int
request_handler(pid_t tid) {
    if (!bun_handle_init(&handle, BUN_BACKEND_LIBUNWINDSTACK)) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "bun_handle_init failed");
        return -1;
    }

    bun_buffer buf = { buffer_child, BUFFER_SIZE };

    auto written = bun_unwind_remote(&handle, &buf, tid);

    (void) written;

    // We return -1 on purpose to tell bcd to not employ its own ptrace dumping mechanism
    return -1;
}

static void
monitor_init(void) {
    /*
     * This is called after the parent process has set buffer_fd. Set
     * a memory mapping to the same descriptor.
     */
    buffer_child = (char *)buffer_reader(buffer_fd, PROT_READ | PROT_WRITE);
    if (buffer_child == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Could not create memory mapped file for client side unwinding");
        return;
    }

    pid_t child_pid = getpid();

    memcpy(buffer_child, &child_pid, sizeof(child_pid));

    return;
}

bool LocalUnwindingHandler(int signum, siginfo_t *info, ucontext_t *context) {
    (void) signum;
    (void) info;
    (void) context;
    size_t bytes_written = bun_unwind(&handle, &buf);

    return false;
}

bool LocalContextUnwindingHandler(int signum, siginfo_t *info, ucontext_t *context) {
    (void) signum;
    (void) info;
    (void) context;
    size_t bytes_written = bun_unwind_context(&handle, &buf, context);

    return false;
}

bool LocalUnwindingHandlerDumpWithoutCrash(int signum, siginfo_t *info, ucontext_t *context) {
    (void) signum;
    (void) info;
    (void) context;

    thread_local bool flag;

    if (flag == false) {
        flag = true;
        size_t bytes_written = bun_unwind(&handle, &buf);

        crashpad::CrashpadClient::DumpWithoutCrash(context);

        return true;
    } else {
        return false;
    }

    return true;
}

bool RemoteUnwindingHandler(int signum, siginfo_t *info, ucontext_t *context) {
    (void) signum;
    (void) info;
    (void) context;

    bcd_emit(&bcd, "1");
    return false;
}

bool RemoteUnwindingHandlerDumpWithoutCrash(int signum, siginfo_t *info, ucontext_t *context) {
    (void) signum;
    (void) info;
    (void) context;

    thread_local bool flag;

    if (flag == true) {
        return false;
    }

    flag = true;
    bcd_emit(&bcd, "1");

    crashpad::CrashpadClient::DumpWithoutCrash(context);

    return true;
}

bool InitializeLocalClientSideUnwinding(JNIEnv* env) {
    // Initialize a shared memory region.
    static const char *buffer = static_cast<char *>(buffer_create());

    bun_buffer_init(&buf, (char*) buffer, BUFFER_SIZE);

    bun_initialized.store(bun_handle_init(&handle, BUN_BACKEND_LIBUNWINDSTACK));

    if (!bun_initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Local client side unwinding initialization failed");
        return false;
    }

    return true;
}

bool InitializeRemoteClientSideUnwinding(JNIEnv* env, jstring path) {
    struct bcd_config cf;
    bcd_error_t e;

    // Initialize a shared memory region.
    static const char *buffer = static_cast<char *>(buffer_create());

    // Initialize the BCD configuration file.
    if (bcd_config_init(&cf, &e) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "bcd_config_init failed, cannot start client side unwinding, error message %s, error code %d",
                            e.message, e.errnum);
        return false;
    }

    // Request handler to be called when processing errors by BCD worker.
    cf.request_handler = request_handler;

    // Set a function to be called by the child for setting permissions.
    cf.monitor_init = monitor_init;

    const char *path_cstr = env->GetStringUTFChars(path, 0);

    // Create the fully resolved path to the bcd socket file
    const char* file_name = "/bcd.socket";
    char* full_path = (char*) malloc((strlen(path_cstr) + strlen(file_name) + 1) * sizeof(char));
    strcpy(full_path, path_cstr);
    strcat(full_path, file_name);

    cf.ipc.us.path = full_path;

    if (bcd_init(&cf, &e) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "bcd_init failed, cannot start client side unwinding, error message %s, error code %d",
                            e.message, e.errnum);
        return false;
    }

    // Initialize the BCD handler.
    if (bcd_attach(&bcd, &e) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "bcd_attach failed, cannot start client side unwinding, error message %s, error code %d",
                            e.message, e.errnum);
        return false;
    }

    buf.data = (char *)buffer;
    buf.size = BUFFER_SIZE;

    pid_t child_pid;
    memcpy(&child_pid, buffer, sizeof(child_pid));

    prctl(PR_SET_PTRACER, child_pid, 0, 0, 0);
    prctl(PR_SET_DUMPABLE, 1);

    env->ReleaseStringUTFChars(path, path_cstr);
    bun_initialized = true;
    return true;
}

bool EnableClientSideUnwinding(JNIEnv *env, jstring path, jint unwindingMode) {
    if (initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Client side unwinding needs to be enabled BEFORE crashpad initialization");
        return false;
    }

    unwinding_mode = static_cast<UnwindingMode>((int) unwindingMode);

    switch (unwinding_mode) {
        case UnwindingMode::LOCAL:
        case UnwindingMode::LOCAL_DUMPWITHOUTCRASH:
        case UnwindingMode::LOCAL_CONTEXT:
            return InitializeLocalClientSideUnwinding(env);
        case UnwindingMode::REMOTE:
        case UnwindingMode::REMOTE_DUMPWITHOUTCRASH:
            return InitializeRemoteClientSideUnwinding(env, path);
        default:
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Invalid unwinding mode for client side unwinding");
            return false;
    }
}

void SetCrashpadHandlerForClientSideUnwinding() {
    if (initialized && bun_initialized) {
        // Set the Minidump data stream to our buffer.
        crashpad::CrashpadInfo::GetCrashpadInfo()->AddUserDataMinidumpStream(
                                                      BUN_STREAM_ID, buf.data, buf.size);

        // Set signal/exception handler for the libbun stream.
        switch (unwinding_mode) {
            case UnwindingMode::LOCAL:
                crashpad::CrashpadClient::SetFirstChanceExceptionHandler(LocalUnwindingHandler);
                break;
            case UnwindingMode::REMOTE:
                crashpad::CrashpadClient::SetFirstChanceExceptionHandler(RemoteUnwindingHandler);
                break;
            case UnwindingMode::REMOTE_DUMPWITHOUTCRASH: {
                bool success = bun_register_signal_handler(
                        +[](int sig, siginfo_t *info, void *ctx) {
                            RemoteUnwindingHandlerDumpWithoutCrash(sig, info,
                                                                   (ucontext_t *) ctx);
                            std::exit(0);
                        });
                if (!success) {
                    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                        "Remote first chance handler failed");
                }
                break;
            }
            case UnwindingMode::LOCAL_DUMPWITHOUTCRASH: {
                bool success = bun_register_signal_handler(
                        +[](int sig, siginfo_t *info, void *ctx) {
                            LocalUnwindingHandlerDumpWithoutCrash(sig, info,
                                                                   (ucontext_t *) ctx);
                            std::exit(0);
                        });
                if (!success) {
                    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                        "Local first chance handler failed");
                }
                break;
            }
            case UnwindingMode::LOCAL_CONTEXT: {
                bool success = bun_register_signal_handler(
                        +[](int sig, siginfo_t *info, void *ctx) {
                            LocalContextUnwindingHandler(sig, info,
                                                                  (ucontext_t *) ctx);
                            std::exit(0);
                        });
                if (!success) {
                    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                                        "Local context first chance handler failed");
                }
                break;
            }
            default:
                __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Invalid client side unwinding mode");
                return;
        }
    } else if (!initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Crashpad not initialized properly, cannot enable client side unwinding");
    }
}

jint ExtractClientSideUnwindingMode(JNIEnv *env, jobject unwindingMode) {
    if (unwindingMode != nullptr) {
        jmethodID unwindingModeGetValueMethod = env->GetMethodID(env->FindClass(
                "backtraceio/library/enums/UnwindingMode"), "ordinal", "()I");
        return env->CallIntMethod(unwindingMode, unwindingModeGetValueMethod);
    }
    return -1;
}

#define UNWINDING_MODE_DEFAULT (jint) UnwindingMode::REMOTE_DUMPWITHOUTCRASH

#else

void LogClientSideUnwindingUnavailable() {
#if defined(__NDK_MAJOR__) && __NDK_MAJOR__ < 21
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Client side unwinding not supported on NDK version %d",
                        __NDK_MAJOR__);
#endif

#if __ANDROID_API__ < 23
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Client side unwinding not supported on API version %d",
                        __ANDROID_API__);
#endif
}

bool EnableClientSideUnwinding(JNIEnv *env, jstring path, jint unwindingMode) {
    LogClientSideUnwindingUnavailable();
    return false;
}

void SetCrashpadHandlerForClientSideUnwinding() {
    LogClientSideUnwindingUnavailable();
    return;
}

jint ExtractClientSideUnwindingMode(JNIEnv *env, jobject unwindingMode) {
    return -1;
}

#define UNWINDING_MODE_DEFAULT -1

#endif  // __NDK_MAJOR__ >= 21 && __ANDROID_API__ >= 23

#endif //BACKTRACE_ANDROID_CLIENT_SIDE_UNWINDING_H
