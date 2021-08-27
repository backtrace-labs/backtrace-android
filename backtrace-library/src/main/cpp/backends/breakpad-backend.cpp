#include "breakpad-backend.h"
#include <dirent.h>
#include <android/log.h>
#include <jni.h>
#include <stdio.h>
#include <string>
#include <atomic>
#include <mutex>
#include <unordered_map>
#include <regex>
#include <thread>
#include <libgen.h>
#include "cacert.h"
#include "client-side-unwinding.h"
#include "backtrace-native.h"

static constexpr size_t MAX_UPLOADS_PER_RUN = 3;

extern std::string thread_id;
extern std::atomic_bool initialized;
extern std::mutex attribute_synchronization;

//std::unique_ptr<google_breakpad::ExceptionHandler*> eh;
static google_breakpad::ExceptionHandler* eh;
static std::string upload_url_str;
static std::map<std::string, std::string> breakpad_attributes;
std::map<std::string, std::string> breakpad_files;
static std::string certificate_path;

static std::string dump_dir;
static std::string attribute_path;

struct dump_context {
    bool set_main_thread_as_faulting_thread;
    std::string message;
    int key;
};

/* Purposefully making the assumption that strings will never exceed 32 bits in this scheme */
static bool serialize_string(FILE* file, const std::string& s)
{
    uint32_t len = s.size();
    if (fwrite(&len, sizeof(len), 1, file) != 1)
        return false;

    if (fwrite(s.data(), 1, s.size(), file) != s.size())
        return false;

    return true;
}

static std::string deserialize_string(FILE* file)
{
    uint32_t len;
    if (fread(&len, sizeof(len), 1, file) != 1)
        return {};

    std::string ret(len, '\0');

    if (fread(&ret[0], 1, len, file) != len)
        return {};

    return ret;
}

static bool map_serialize_to_file(const std::map<std::string, std::string>& m,
                                  const char* file_name)
{
    FILE* f = fopen(file_name, "wb");
    if (!f)
        return false;
    for (const auto& kv : m) {
        bool success = serialize_string(f, kv.first) && serialize_string(f, kv.second);
        if (!success) {
            fclose(f);
            return false;
        }
    }
    fclose(f);
    return true;
}

static std::map<std::string, std::string> map_deserialize_from_file(const char* file_name)
{
    FILE* f = fopen(file_name, "rb");
    if (!f)
        return {};

    std::map<std::string, std::string> m;
    while (true) {
        std::string key = deserialize_string(f);
        if (key.size() == 0) {
            fclose(f);
            return m;
        }
        std::string value = deserialize_string(f);
        if (value.size() == 0) {
            fclose(f);
            return m;
        }
        m.emplace(std::move(key), std::move(value));
    }

    fclose(f);
    return m;
}

static const char* get_basename(const char* file_name)
{
    const char* last_dir_separator = nullptr;

    for (const char* current = file_name; *current; ++current) {
        if (*current == '/')
            last_dir_separator = current;
    }
    if (last_dir_separator)
        return last_dir_separator + 1;
    return file_name;
}

static std::vector<std::string> get_files_pending_upload()
{
    std::vector<std::string> ret;
    std::string path = dump_dir + "/..";
    DIR* d = opendir(path.c_str());

    if (!d)
        return {};

//    const std::regex filename_regex{R"(^(........-....-....-........-........\.dmp)\.pending)"};
    const std::regex filename_regex{R"(^(.{36}\.dmp)\.pending)"};

    for (struct dirent* dir = readdir(d); dir != nullptr; dir = readdir(d)) {

//        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
//                            "Entry type %d, name %s\n", dir->d_type, dir->d_name);
        std::smatch match;
        if (!std::regex_match(std::string{dir->d_name}, match, filename_regex))
            continue;

        ret.push_back(match[1].str());

//        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
//                            "Match %s\n", ret.back().c_str());
    }

    return ret;
}


// Keep track of dump context for user-generated Breakpad dumps
int user_generated_dump_counter = 0;
static std::unordered_map<int, dump_context> dump_context_map;

static bool dumpCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                                     void* context, bool succeeded) {
    if (succeeded == false && descriptor.path()[0] == '\0') {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Breakpad dump callback reports failure, dump path string empty, cannot upload dump");
        return false;
    } else if (succeeded == false && descriptor.path()[0] != '\0') {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Breakpad dump callback reports failure, will still try to upload dump at %s", descriptor.path());
    }

    const char* path = descriptor.path();
    const char* basename = get_basename(path);

    static char computed_result_name[2048] = "";

#if DEBUG_BREAKPAD_DUMP_CALLBACK
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Breakpad dump dir: %s\n", dump_dir.c_str());
#endif
    strcpy(computed_result_name, dump_dir.c_str());
    strcat(computed_result_name, "/../");
    strcat(computed_result_name, basename);
    strcat(computed_result_name, ".pending");
#if DEBUG_BREAKPAD_DUMP_CALLBACK
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Breakpad dump old name: %s\n", path);
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Breakpad dump new name: %s\n", computed_result_name);
#endif

    rename(path, computed_result_name);

    strcpy(computed_result_name, dump_dir.c_str());
    strcat(computed_result_name, "/../");
    strcat(computed_result_name, basename);
    strcat(computed_result_name, ".attributes");
    rename(attribute_path.c_str(), computed_result_name);

    return true;
}

static void uploadSingle(const std::string& base_name)
{
    auto base_path = dump_dir + "/../" + base_name;
    auto attributes_path = base_path + ".attributes";
    auto dump_path = base_path + ".pending";
    auto attributes = map_deserialize_from_file(attributes_path.c_str());

#if DEBUG_BREAKPAD_UPLOAD
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                        "Uploading report %s\n", dump_path.c_str());
#endif

    breakpad_files["upload_file_minidump"] = dump_path;

    string response, error;
    bool success = google_breakpad::HTTPUpload::SendRequest(upload_url_str,
                                                            attributes,
                                                            breakpad_files,
                                                            "",
                                                            "",
                                                            certificate_path,
                                                            &response,
                                                            NULL,
                                                            &error);

    if (success) {
        unlink(attributes_path.c_str());
        unlink(dump_path.c_str());
    }
}

static void uploadPending()
{
    auto pending = get_files_pending_upload();

    if (pending.size() > MAX_UPLOADS_PER_RUN)
        pending.resize(MAX_UPLOADS_PER_RUN);

    for (const auto& name : pending) {
#if DEBUG_BREAKPAD_UPLOAD
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Uploading report %s\n", name.c_str());
#endif
        uploadSingle(name);
    }
}

static bool dumpWithoutCrashCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                         void* context, bool succeeded) {
    if (succeeded == false && descriptor.path()[0] == '\0') {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Breakpad dump callback reports failure, dump path string empty, cannot upload dump");
        return false;
    } else if (succeeded == false && descriptor.path()[0] != '\0') {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Breakpad dump callback reports failure, will still try to upload dump at %s", descriptor.path());
    }

    std::map<std::string, std::string> local_breakpad_attributes(breakpad_attributes);

    if (context != nullptr) {
        dump_context *local_context = static_cast<dump_context*>(context);
        if (local_context != nullptr) {
            if (!local_context->message.empty()) {
                local_breakpad_attributes["error.message"] = local_context->message;
            }
            if (local_context->set_main_thread_as_faulting_thread) {
                local_breakpad_attributes["_mod_faulting_tid"] = thread_id;
            }
            // Delete this local context when we're done
            dump_context_map.erase(local_context->key);
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Could not convert context to local dump_context");
        }
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","DumpWithoutCrash callback received null context");
    }

    breakpad_files["upload_file_minidump"] = descriptor.path();

#if DEBUG_HTTP_UPLOAD
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Dump path: %s\n", descriptor.path());
    __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","Upload URL: %s\n", upload_url_str.c_str());

    /* try to open file to read */
    bool dump_exists = false;
    FILE *file;
    if (file = fopen(descriptor.path(), "r")) {
        dump_exists = true;
        fclose(file);
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","File at dump path exists");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android","File at dump path does not exist");
    }

    for (auto breakpad_file : breakpad_files) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Breakpad file key %s", breakpad_file.first.c_str());
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Breakpad file value %s", breakpad_file.second.c_str());
    }
#endif

    // Send it
    string response, error;
    bool success = google_breakpad::HTTPUpload::SendRequest(upload_url_str,
                                                            local_breakpad_attributes,
                                                            breakpad_files,
                                                            "",
                                                            "",
                                                            certificate_path,
                                                            &response,
                                                            NULL,
                                                            &error);
    if (!success) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Failed to send the minidump file.");
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Response: %s", response.c_str());
    }

    return true;
}

void CreateCertificateFile(const char* directory) {
    certificate_path = std::string(directory) + "/backtrace-cacert.pem";
    FILE* f = fopen(certificate_path.c_str(), "w");
    if (f) {
        fwrite(backtrace::cacert, 1, sizeof(backtrace::cacert), f);
        fclose(f);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Could not create certificate file");
    }
}

int InitializeBreakpad(jstring url,
                       jstring database_path,
                       jstring handler_path,
                       jobjectArray attributeKeys,
                       jobjectArray attributeValues,
                       jobjectArray attachmentPaths,
                       jboolean enableClientSideUnwinding,
                       jint unwindingMode) {
    // avoid multi initialization
    if (initialized) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Breakpad is already initialized");
        return true;
    }

    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return false;
    }

    if (enableClientSideUnwinding) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Client side unwinding not available with Breakpad native crash reporting backend");
    }

    // path to crash handler executable
    const char *database_path_cstr = env->GetStringUTFChars(database_path, 0);
    // Backtrace url
    const char *backtrace_url_cstr = env->GetStringUTFChars(url, 0);
    upload_url_str = std::string(backtrace_url_cstr);

    dump_dir = std::string(database_path_cstr);
    google_breakpad::MinidumpDescriptor descriptor(database_path_cstr);

    CreateCertificateFile(database_path_cstr);

    breakpad_attributes["format"] = "minidump";
    // save native main thread id
    if(!thread_id.empty()) {
        breakpad_attributes["thread.main"] = thread_id;
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

            breakpad_attributes[convertedKey] = convertedValue;

            env->ReleaseStringUTFChars(jstringKey, convertedKey);
            env->ReleaseStringUTFChars(stringValue, convertedValue);
        }
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android",
                            "Attribute array length doesn't match. Attributes won't be available in the Crashpad integration");
    }

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

            std::string attachmentBaseName = std::string("attachment_") + basename(convertedAttachmentPath);

            breakpad_files[attachmentBaseName] = convertedAttachmentPath;

            env->ReleaseStringUTFChars(jstringAttachmentPath, convertedAttachmentPath);
        }
    }

    //std::unique_ptr<google_breakpad::ExceptionHandler, cpp_deleter>(new google_breakpad::ExceptionHandler(descriptor, NULL, dumpCallback, NULL, true, -1), cpp_deleter());
    eh = new google_breakpad::ExceptionHandler(descriptor, NULL, dumpCallback, NULL, true, -1);

    env->ReleaseStringUTFChars(url, backtrace_url_cstr);
    env->ReleaseStringUTFChars(database_path, database_path_cstr);

    attribute_path = dump_dir + "/breakpad_attributes";
    map_serialize_to_file(breakpad_attributes, attribute_path.c_str());

    std::thread{ [=]{ uploadPending(); } }.detach();

    initialized = true;
    return initialized;
}

void DumpWithoutCrashBreakpad(jstring message, jboolean set_main_thread_as_faulting_thread) {
    if (eh == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, "Backtrace-Android",
                            "Breakpad integration isn't available. Please initialize the Breakpad integration before calling DumpWithoutCrash.");
        return;
    }
#if 0
    eh->WriteMinidump();
#endif
    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return;
    }
    dump_context context;

    jboolean isCopy;
    if (message != nullptr) {
        const char *rawMessage = env->GetStringUTFChars(message, &isCopy);
        context.message = std::string(rawMessage);
        env->ReleaseStringUTFChars(message, rawMessage);
    } else {
        context.message = std::string();
    }

    context.set_main_thread_as_faulting_thread = set_main_thread_as_faulting_thread;
    context.key = user_generated_dump_counter++;
    dump_context_map[context.key] = context;

    google_breakpad::ExceptionHandler::WriteMinidump(dump_dir, dumpWithoutCrashCallback, (void*) &context);
}

void AddAttributeBreakpad(jstring key, jstring value) {
    if (initialized == false) {
        __android_log_print(ANDROID_LOG_WARN, "Backtrace-Android",
                            "Breakpad integration isn't available. Please initialize Breakpad before calling AddAttribute.");
        return;
    }
    JNIEnv *env = GetJniEnv();
    if (env == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "Backtrace-Android", "Cannot initialize JNIEnv");
        return;
    }

    const std::lock_guard<std::mutex> lock(attribute_synchronization);

    jboolean isCopy;
    const char *breakpadKey = env->GetStringUTFChars(key, &isCopy);
    const char *breakpadValue = env->GetStringUTFChars(value, &isCopy);
    if (breakpadKey && breakpadValue) {
        breakpad_attributes[breakpadKey] = breakpadValue;
    }

    env->ReleaseStringUTFChars(key, breakpadKey);
    env->ReleaseStringUTFChars(value, breakpadValue);

    map_serialize_to_file(breakpad_attributes, attribute_path.c_str());
}
