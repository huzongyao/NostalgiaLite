# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.
# by huzongyao

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DPSS_STYLE=1 -DHAVE_ASPRINTF -DLSB_FIRST -DFRAMESKIP -DIOAPI_NO_64 -D_STLP_HAS_WCHAR_T -D_GLIBCXX_USE_WCHAR_T")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-write-strings -Ofast -fexceptions -frtti -fno-builtin-sin -fno-builtin-cos -Wpointer-arith")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fmerge-all-constants -fvisibility=hidden -fvisibility-inlines-hidden -funroll-loops  -ffast-math")

add_definitions(-Wno-narrowing)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.
file(GLOB_RECURSE NATIVE_SRCS *.c *.cpp)

include_directories(./ fceux/
                    fceux/boards/
                    fceux/input/)

add_library(nes SHARED ${NATIVE_SRCS})

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries(nes z log jnigraphics GLESv1_CM)
