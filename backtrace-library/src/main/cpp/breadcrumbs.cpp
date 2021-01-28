#include "breadcrumbs.h"

namespace Backtrace {

    Breadcrumbs::Breadcrumbs(const char* directory)
    {
        // Creating a RotatingLogger can throw
        logger = std::make_unique<RotatingLogger>(directory, 64000, 2);
        // Initialize the breadcrumb log JSON
        logger->Write("[");
        // Initialize the file position tracker
        filePosition = logger->GetPosition();
        // This is the line we will rewrite in the subsequent lines. Subsequent logged breadcrumbs
        // will erase the closing bracket of the previous line and write over it.
        logger->Write("\n");
    }

    // We use this function to add breadcrumbs coming from the managed layer
    void Breadcrumbs::addBreadcrumb(const long long int timestamp,
                                    const BreadcrumbType type,
                                    const BreadcrumbLevel level,
                                    const char* message,
                                    const char* serializedAttributes) {
        int myId = this->breadcrumbId++;

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
        if (myId != 0)
            breadcrumb += ",";
        breadcrumb += "{\"timestamp\":";
        breadcrumb += std::to_string(timestamp);
        breadcrumb += ",\"id\":";
        breadcrumb += std::to_string(myId);
        breadcrumb += ",\"level\":\"";
        breadcrumb += levelString;
        breadcrumb += "\",\"type\":\"";
        breadcrumb += typeString;
        breadcrumb += "\",\"message\":\"";
        breadcrumb += message;
        breadcrumb += "\",\"attributes\":";
        breadcrumb += serializedAttributes;
        breadcrumb += "}";

        // Rewrite the previous log (we need to keep valid JSON as we go)
        logger->SetPosition(filePosition);
        logger->Write(breadcrumb.c_str());
        filePosition = logger->GetPosition();
        logger->Write("]");
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
            if (breadcrumbId != 0)
                serializedAttributes += "\",";
            serializedAttributes += "\"";
            serializedAttributes += elem.first;
            serializedAttributes += "\":\"";
            serializedAttributes += elem.second;
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

}
