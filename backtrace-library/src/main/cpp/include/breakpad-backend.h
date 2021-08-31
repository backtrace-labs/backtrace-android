#ifndef BACKTRACE_ANDROID_BREAKPAD_BACKEND_H
#define BACKTRACE_ANDROID_BREAKPAD_BACKEND_H

#include "exception_handler.h"
#include "common/linux/http_upload.h"
#include "cacert.h"
#include "client-side-unwinding.h"
#include <jni.h>
#include <mutex>
#include <atomic>
#include <unordered_map>
#include <string>

static constexpr size_t MAX_UPLOADS_PER_RUN = 3;

extern std::string thread_id;
extern std::atomic_bool initialized;
extern std::mutex attribute_synchronization;

static google_breakpad::ExceptionHandler* eh;
static std::string upload_url_str;
static std::map<std::string, std::string> breakpad_attributes;
static std::map<std::string, std::string> breakpad_files;
static std::string certificate_path;

static std::string dump_dir;
static std::string attribute_path;

struct dump_context {
    bool set_main_thread_as_faulting_thread;
    std::string message;
    int key;
};

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
