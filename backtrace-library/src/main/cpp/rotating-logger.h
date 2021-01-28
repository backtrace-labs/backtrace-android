#ifndef BACKTRACE_ANDROID_ROTATING_LOGGER_H
#define BACKTRACE_ANDROID_ROTATING_LOGGER_H

#include <stdio.h>
#include <stdexcept>
#include <string>
#include <stdlib.h>
#include <android/log.h>
#include <memory>

namespace Backtrace {

    class RotatingLogger {
    public:
        // Store a max of numFiles rotating log files of capacityInBytes bytes each
        // in the specified directory
        RotatingLogger(const char* directory, const int capacityInBytes, const int maxNumFiles)
                : directory(directory),
                  capacityInBytes(capacityInBytes),
                  maxNumFiles(maxNumFiles) {
            if (maxNumFiles < 1)
                throw std::invalid_argument("Backtrace::RotatingLogger needs maxNumFiles of at least 1");
            if (capacityInBytes < 0)
                throw std::invalid_argument("Backtrace::RotatingLogger needs capacityInBytes of at least 1");

            std::string filePath = this->directory + "/" + baseFileName + std::to_string(numFiles);
            __android_log_print(ANDROID_LOG_DEBUG, "Backtrace-Android", "Opening breadcrumb log file at directory %s", filePath.c_str());
            activeFile = fopen(filePath.c_str(), "w");

            if (activeFile == nullptr) {
                throw std::runtime_error(std::string("Could not open a log file at directory ") + filePath);
            }
        }

        ~RotatingLogger() {
            fclose(activeFile);
            activeFile = nullptr;
        }

        void Write(const char* str)
        {
            fprintf(activeFile, "%s\n", str);
        }

        void Flush()
        {
            fflush(activeFile);
        }

        long int GetPosition()
        {
            return ftell(activeFile);
        }

        void SetPosition(long int pos)
        {
            fseek(activeFile, pos, SEEK_SET);
        }

        void RotateLogs()
        {
            // TODO: implement
        }

    private:
        int numFiles = 0;
        int maxNumFiles = 0;

        int capacityInBytes = 0;
        // Tracking the number of bytes written to the active file
        // allows us to avoid clunky fseek/ftell calls to determine
        // the size every time we want to write the file
        int bytesWritten = 0;

        std::string directory;
        FILE** files = nullptr;
        FILE* activeFile = nullptr;

        const char* baseFileName = "bt-breadcrumbs-";
    };
}

#endif //BACKTRACE_ANDROID_ROTATING_LOGGER_H
