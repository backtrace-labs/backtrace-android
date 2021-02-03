#ifndef BACKTRACE_ANDROID_BREADCRUMBS_H
#define BACKTRACE_ANDROID_BREADCRUMBS_H

#include "rotating-logger.h"
#include <memory>
#include <unordered_map>

extern "C" {
namespace Backtrace {
    enum class BreadcrumbType {
        Manual,
        Log,
        Navigation,
        HTTP,
        System,
        User,
        Configuration
    };

    enum class BreadcrumbLevel {
        Debug,
        Info,
        Warning,
        Error,
        Fatal
    };

    class Breadcrumbs {

    public:
        Breadcrumbs(const char *directory);

        // Prefer to add breadcrumbs with this function from NDK
        void addBreadcrumb(const long long int timestamp,
                           const BreadcrumbType type,
                           const BreadcrumbLevel level,
                           const char *message,
                           std::__ndk1::unordered_map<std::string, std::string> &attributes);

        // We use this function to add breadcrumbs coming from the managed layer
        void addBreadcrumb(const long long int timestamp,
                           const BreadcrumbType type,
                           const BreadcrumbLevel level,
                           const char *message,
                           const char *serializedAttributes);

        void flushLog();

        // Get the number of breadcrumbs in the log
        int getNumBreadcrumbs();

    private:
        std::unique_ptr<RotatingLogger> logger;
        // Track the breadcrumb ID number
        int breadcrumbId = 0;
        // Track the current file position so we can rewind to always maintain valid JSON
        off_t filePosition = -1;
    };
}
}
#endif //BACKTRACE_ANDROID_BREADCRUMBS_H
