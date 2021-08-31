#include <atomic>
#include <iostream>

#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#include "client/linux/handler/exception_handler.h"
#include "common/linux/libcurl_wrapper.h"
#include "common/scoped_ptr.h"

#include "backtrace_handler.h"

namespace google_breakpad {

namespace {
const string kDefaultDescriptorPath = "/tmp";
};

class BacktraceHandlerContext {
 public:
  BacktraceHandlerContext(const string& url, const string& token,
                          const std::unordered_map<string, string>& attributes);

  string url_;
  string token_;
  std::shared_ptr<std::unordered_map<string, string>> attributes_;

  scoped_ptr<LibcurlWrapper> http_layer_;

  MinidumpDescriptor descriptor_;
  ExceptionHandler handler_;

  static bool MinidumpCallback(
      const google_breakpad::MinidumpDescriptor& descriptor, void* context,
      bool succeeded);
};

/* Global shared context */
std::shared_ptr<BacktraceHandlerContext> ctx_;

BacktraceHandlerContext::BacktraceHandlerContext(
    const string& url, const string& token,
    const std::unordered_map<string, string>& attributes)
    : url_(url),
      token_(token),
      attributes_(new std::unordered_map<string, string>(attributes)),
      http_layer_(new LibcurlWrapper()),
      descriptor_(kDefaultDescriptorPath),
      handler_(descriptor_, NULL, MinidumpCallback, NULL, true, -1) {}

bool isSuccessfulHttpCode(int code) { return (200 <= code && code < 300); }

bool BacktraceHandlerContext::MinidumpCallback(
    const google_breakpad::MinidumpDescriptor& descriptor, void* context,
    bool succeeded) {
  auto ctx = ctx_.get();
  if (ctx == nullptr) return false;

  if (succeeded) {
    auto http_layer = ctx->http_layer_.get();

    string minidump_pathname = descriptor.path();
    struct stat st;
    if (stat(minidump_pathname.c_str(), &st)) {
      std::cerr << minidump_pathname << " could not be found";
      return false;
    }

    auto attributes = ctx->attributes_;
    auto attrs_ = attributes.get();
    /* This shouldn't happen */
    if (attrs_ == nullptr) return false;

    /* FIXME: properly parse url and adjust query string sanely */
    std::string url = ctx->url_ + "/api/post?format=minidump";
    if (!http_layer->AddFormParameter("token", ctx->token_)) return false;
    for (auto const& kv : *(attrs_))
      if (!http_layer->AddFormParameter(kv.first, kv.second)) return false;

    if (!http_layer->AddFile(minidump_pathname, "upload_file_minidump"))
      return false;

    int http_status_code;
    string http_response_header;
    string http_response_body;
    std::map<string, string> dummy_map;
    bool send_success =
        http_layer->SendRequest(url, dummy_map, &http_status_code,
                                &http_response_header, &http_response_body);

    if (!send_success || !isSuccessfulHttpCode(http_status_code)) {
      std::cerr << "Failed to send dump to " << url << "\n Received error code "
                << http_status_code << " with request:\n\n"
                << http_response_header << "\n"
                << http_response_body;

      return false;
    }
  }

  return succeeded;
}

bool BacktraceHandler::Init(
    const string& url, const string& token,
    const std::unordered_map<string, string>& attributes) {
  if (ctx_ != nullptr) return false;

  ctx_.reset(new BacktraceHandlerContext(url, token, attributes));
  if (ctx_.get()->http_layer_.get()->Init()) return false;

  return true;
}

bool BacktraceHandler::SetOrReplaceAttribute(const string& key,
                                             const string& val) {
  if (ctx_ == nullptr) return false;

  for (;;) {
    auto old_attrs = ctx_->attributes_;
    auto old_attrs_ = old_attrs.get();
    if (old_attrs_ == nullptr) return false;

    std::shared_ptr<std::unordered_map<string, string>> new_attrs(
        new std::unordered_map<string, string>(*old_attrs_));

    new_attrs.get()->erase(key);
    new_attrs.get()->insert({key, val});

    //if (atomic_compare_exchange_weak(&ctx_->attributes_, &old_attrs, new_attrs))
    break;
  }

  return true;
}

bool BacktraceHandler::RemoveAttribute(const string& key) {
  if (ctx_ == nullptr) return false;

  for (;;) {
    auto old_attrs = ctx_->attributes_;
    auto old_attrs_ = old_attrs.get();
    if (old_attrs_ == nullptr) return false;

    std::shared_ptr<std::unordered_map<string, string>> new_attrs(
        new std::unordered_map<string, string>(*old_attrs_));

    new_attrs.get()->erase(key);

    //if (atomic_compare_exchange_weak(&ctx_->attributes_, &old_attrs, new_attrs))
    break;
  }

  return true;
}
}
