#ifndef BACKTRACE_ANDROID_ROTATING_LOGGER_H
#define BACKTRACE_ANDROID_ROTATING_LOGGER_H

#include <stdexcept>
#include <string>
#include <stdlib.h>
#include <android/log.h>
#include <memory>
#include <fcntl.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>

namespace Backtrace {

    class RotatingLogger {
    public:
        // Store a max of numFiles rotating log files of capacityInBytes bytes each
        // in the specified directory
        RotatingLogger(const char* directory, const unsigned int capacityInBytes, const unsigned int maxNumFiles)
                : directory(directory),
                  capacityInBytes(capacityInBytes),
                  maxNumFiles(maxNumFiles) {
            if (maxNumFiles < 1)
                throw std::invalid_argument("Backtrace::RotatingLogger needs maxNumFiles of at least 1");
            if (capacityInBytes < 0)
                throw std::invalid_argument("Backtrace::RotatingLogger needs capacityInBytes of at least 1");

            std::string filePath = this->directory + "/" + baseFileName + std::to_string(numFiles);
            __android_log_print(ANDROID_LOG_DEBUG, "Backtrace-Android", "Opening breadcrumb log file at directory %s", filePath.c_str());
            activeFile = open(filePath.c_str(), O_WRONLY | O_APPEND | O_CREAT | O_TRUNC, 0666);

            if (activeFile == -1) {
                __android_log_print(ANDROID_LOG_DEBUG, "Backtrace-Android", "Received error %d when trying to open the file", errno);
                throw std::runtime_error(std::string("Could not open a log file at directory ") + filePath);
            }
        }

        ~RotatingLogger() {
            fsync(activeFile);
            if (close(activeFile) == -1)
            {
                __android_log_print(ANDROID_LOG_DEBUG, "Backtrace-Android", "Received error %d when trying to close the file", errno);
            }
            activeFile = -1;
        }

        bool Write(const char* str, const unsigned int bytes)
        {
            write(activeFile, str, bytes);
            return true;
        }

        void Flush()
        {
            fdatasync(activeFile);
        }

        off_t GetPosition()
        {
            return lseek(activeFile, 0, SEEK_CUR);
        }

        void SetPosition(off_t pos)
        {
            lseek(activeFile, pos, SEEK_SET);
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
        int* files = nullptr;
        int activeFile = -1;

        const char* baseFileName = "bt-breadcrumbs-";
    };
}

#endif //BACKTRACE_ANDROID_ROTATING_LOGGER_H
