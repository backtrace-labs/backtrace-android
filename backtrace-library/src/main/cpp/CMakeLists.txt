# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.13)

# Determine native crash backend
# ANDROID_NDK_MAJOR not defined until ndk 17+
# https://github.com/android/ndk/issues/596
if (ANDROID_ABI STREQUAL "x86")
    message("Native crash reporting not supported for x86 emulator")
elseif(ANDROID_ABI STREQUAL "x86_64" AND (NOT ANDROID_NDK_MAJOR OR ANDROID_NDK_MAJOR LESS 17))
    message("Breakpad not supported for x86_64 emulator")
elseif (NOT ANDROID_NDK_MAJOR)
    set(BACKEND "BREAKPAD_BACKEND")
elseif(ANDROID_NDK_MAJOR LESS 17)
    set(BACKEND "BREAKPAD_BACKEND")
elseif(ANDROID_NATIVE_API_LEVEL LESS 21)
    set(BACKEND "CRASHPAD_BACKEND")
    # 64 bit architectures will always have min API level 21
    # https://stackoverflow.com/a/56467008
    if (NOT ANDROID_ABI STREQUAL "armeabi-v7a")
        set(CLIENT_SIDE_UNWINDING TRUE)
    endif()
else()
    set(BACKEND "CRASHPAD_BACKEND")
    set(CLIENT_SIDE_UNWINDING TRUE)
endif()

cmake_policy(SET CMP0077 NEW)
set(ANDROID_SSL_MODE "OPENSSL")

# Sources
list(APPEND SOURCES backtrace-native.cpp)
list(APPEND SOURCES backends/backend.cpp)
list(APPEND SOURCES client-side-unwinding.cpp)
if(BACKEND STREQUAL "CRASHPAD_BACKEND")
    list(APPEND SOURCES backends/crashpad-backend.cpp)
elseif(BACKEND STREQUAL "BREAKPAD_BACKEND")
    list(APPEND SOURCES backends/breakpad-backend.cpp)
else()
    message("No native debugging backend selected")
endif()

add_library(# Sets the name of the library.
            backtrace-native

            # Sets the library as a shared library.
            SHARED

            # Provides a relative path to your source file(s).
            ${SOURCES})

target_compile_features(backtrace-native PRIVATE cxx_std_17)

if(BACKEND STREQUAL "CRASHPAD_BACKEND")
    target_compile_definitions(backtrace-native PRIVATE -DCRASHPAD_BACKEND)

elseif(BACKEND STREQUAL "BREAKPAD_BACKEND")
    target_compile_definitions(backtrace-native PRIVATE -DBREAKPAD_BACKEND)

    # Breakpad Libraries
    add_library(breakpad_client STATIC IMPORTED)
    set_property(TARGET breakpad_client PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/breakpad-builds/${ANDROID_ABI}/libbreakpad_client.a)

    # Curl Libraries
    add_library(curl SHARED IMPORTED)
    set_property(TARGET curl PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/curl-builds/${ANDROID_ABI}/libcurl.so)

    # Breakpad Headers
    include_directories(${PROJECT_SOURCE_DIR}/breakpad-builds/${ANDROID_ABI} ${PROJECT_SOURCE_DIR}/breakpad-builds/${ANDROID_ABI}/src ${PROJECT_SOURCE_DIR}/breakpad-builds/${ANDROID_ABI}/src/common/android/include)
else()
    message("No native debugging backend selected")
endif()

# Includes
include_directories(${PROJECT_SOURCE_DIR}/include)

if (CLIENT_SIDE_UNWINDING)
    target_compile_definitions(backtrace-native PRIVATE -DCLIENT_SIDE_UNWINDING)

    # Bun Libraries
    set(LIBUNWINDSTACK_ENABLED TRUE)
    add_subdirectory(libbun)
endif()

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log
            )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

list(APPEND LIBS backtrace-native)
list(APPEND LIBS ${log-lib})

if(BACKEND STREQUAL "CRASHPAD_BACKEND")
    set(HANDLER "INTEGRATED")
    add_subdirectory(crashpad)
    target_link_libraries(backtrace-native client)
    target_link_libraries(backtrace-native handler)

elseif(BACKEND STREQUAL "BREAKPAD_BACKEND")
    list(APPEND LIBS breakpad_client)
    list(APPEND LIBS curl)
else()
    message("No native debugging backend selected")
endif()

if (CLIENT_SIDE_UNWINDING)
    list(APPEND LIBS bun)
endif()

target_link_libraries(${LIBS})


