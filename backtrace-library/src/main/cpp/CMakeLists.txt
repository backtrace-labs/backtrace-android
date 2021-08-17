# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             backtrace-native

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
        backtrace-native.cpp)

# TODO: Gate with NDK version
if(false)
# Crashpad Libraries
add_library(crashpad_client STATIC IMPORTED)
set_property(TARGET crashpad_client PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/crashpad-builds/${ANDROID_ABI}/client/libcrashpad_client.a)

add_library(crashpad_util STATIC IMPORTED)
set_property(TARGET crashpad_util PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/crashpad-builds/${ANDROID_ABI}/util/libcrashpad_util.a)

add_library(base STATIC IMPORTED)
set_property(TARGET base PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/crashpad-builds/${ANDROID_ABI}/third_party/mini_chromium/mini_chromium/base/libbase.a)

# Crashpad Headers
include_directories(${PROJECT_SOURCE_DIR}/crashpad-builds/headers/ ${PROJECT_SOURCE_DIR}/crashpad-builds/headers/third_party/mini_chromium/mini_chromium/)
endif()

message("clang version")
message(${CMAKE_CXX_COMPILER_VERSION})
# Clang 9 is used in NDK 21+
if (CMAKE_CXX_COMPILER_VERSION VERSION_GREATER 8.0)
    # Bun Libraries
    set(LIBUNWINDSTACK_ENABLED TRUE)
    add_subdirectory(libbun)
endif()

# TODO: Gate with NDK version
if (true)
    set(CMAKE_MAP_IMPORTED_CONFIG_RELWITHDEBINFO "RelWithDebInfo;Release;")
  #  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D__STDC_LIMIT_MACROS")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -llog")

    # Breakpad Libraries
    add_library(breakpad_client STATIC IMPORTED)
    set_property(TARGET breakpad_client PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/breakpad-builds/${ANDROID_ABI}/libbreakpad_client.a)

    # Curl Libraries
    add_library(curl SHARED IMPORTED)
    set_property(TARGET curl PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/curl-builds/${ANDROID_ABI}/libcurl.so)

    # SSL Libraries
 #   add_library(ssl SHARED IMPORTED)
 #   set_property(TARGET curl PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/breakpad-builds/${ANDROID_ABI}/libssl.so)

    # Crypto Libraries
  #  add_library(crypto SHARED IMPORTED)
 #   set_property(TARGET crypto PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/breakpad-builds/${ANDROID_ABI}/libcrypto.so)

    # Breakpad Headers
    include_directories(${PROJECT_SOURCE_DIR}/breakpad-builds/headers/ ${PROJECT_SOURCE_DIR}/breakpad-builds/headers/src ${PROJECT_SOURCE_DIR}/breakpad-builds/headers/src/common/android/include)
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
# TODO: Gate with NDK version
if (true)
    list(APPEND LIBS breakpad_client)
    list(APPEND LIBS curl)
  #  list(APPEND LIBS ssl)
 #   list(APPEND LIBS crypto)
endif()
list(APPEND LIBS ${log-lib})

# TODO: Gate with NDK version
if (false)
    list(APPEND LIBS crashpad_client)
    list(APPEND LIBS crashpad_util)
    list(APPEND LIBS base)
endif()

if (CMAKE_CXX_COMPILER_VERSION VERSION_GREATER 8.0)
    list(APPEND LIBS bun)
endif()

target_link_libraries(${LIBS})


