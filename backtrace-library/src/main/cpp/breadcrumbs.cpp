#include "breadcrumbs.h"

namespace Backtrace {

    Breadcrumbs::Breadcrumbs(const char* directory)
    {
        // Creating a RotatingLogger can throw
        logger = std::make_unique<RotatingLogger>(directory, 64000, 2);
    }

    // We use this function to add breadcrumbs coming from the managed layer
    void Breadcrumbs::addBreadcrumb(const long long int timestamp,
                                    const BreadcrumbType type,
                                    const BreadcrumbLevel level,
                                    const char* message,
                                    const char* serializedAttributes) {
        int myId = this->breadcrumbId++;

        std::string messageStr = message;
        sanitizeString(messageStr);

        const char* typeString = [&] {
            switch (type)
            {
                case BreadcrumbType::Manual:
                    return "manual";
                case BreadcrumbType::Configuration:
                    return "configuration";
                case BreadcrumbType::HTTP:
                    return "http";
                case BreadcrumbType::Log:
                    return "log";
                case BreadcrumbType::Navigation:
                    return "navigation";
                case BreadcrumbType::System:
                    return "system";
                case BreadcrumbType::User:
                    return "user";
                default:
                    assert(0);
                    return "invalid";
            }
        }();

        const char* levelString = [&] {
            switch (level)
            {
                case BreadcrumbLevel::Info:
                    return "info";
                case BreadcrumbLevel::Debug:
                    return "debug";
                case BreadcrumbLevel::Warning:
                    return "warning";
                case BreadcrumbLevel::Error:
                    return "error";
                case BreadcrumbLevel::Fatal:
                    return "fatal";
                default:
                    assert(0);
                    return "invalid";
            }
        }();

        std::string breadcrumb;
        breadcrumb += "timestamp ";
        breadcrumb += std::to_string(timestamp);
        breadcrumb += " id ";
        breadcrumb += std::to_string(myId);
        breadcrumb += " level ";
        breadcrumb += levelString;
        breadcrumb += " type ";
        breadcrumb += typeString;
        breadcrumb += " attributes ";
        breadcrumb += serializedAttributes;
        breadcrumb += " message ";
        breadcrumb += messageStr;
        breadcrumb += "\n";

        logger->Write(breadcrumb.c_str(), breadcrumb.size());
    }

    // Prefer to add breadcrumbs with this function from NDK
    void Breadcrumbs::addBreadcrumb(const long long int timestamp,
                                    const BreadcrumbType type,
                                    const BreadcrumbLevel level,
                                    const char* message,
                                    std::unordered_map<std::string, std::string>& attributes)
    {
        std::string serializedAttributes = "{";
        for (const auto& elem : attributes)
        {
            serializedAttributes += elem.first;
            serializedAttributes += " ";
            serializedAttributes += elem.second;
            serializedAttributes += " ";
        }
        addBreadcrumb(timestamp, type, level, message, serializedAttributes.c_str());
    }

    void Breadcrumbs::flushLog()
    {
        logger->Flush();
    }

    int Breadcrumbs::getNumBreadcrumbs() {
        return breadcrumbId;
    }

    void Breadcrumbs::sanitizeString(std::string& message)
    {
        for (int i = 0; i < message.size(); i++) {
            if (message[i] == '\n') {
                message.erase(i, 1);
                i--;
            }
        }
    }
}
