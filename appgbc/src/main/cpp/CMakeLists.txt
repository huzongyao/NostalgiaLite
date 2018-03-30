# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.
# by huzongyao

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-exceptions -fno-rtti -DINLINE=inline -DHAVE_STDINT_H -DINT_LEAST_32 -DHAVE_INTTYPES_H -DLSB_FIRST -Wreturn-type")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.
file(GLOB_RECURSE NATIVE_SRCS *.c *.cpp)

include_directories(./ common/
                    include/
                    libgambatte/include/
                    libgambatte/src/
                    src/file/
                    src/mem/
                    libgambatte/src/file/
                    src/file/unzip/
                    common/resample/)

add_library(gbc SHARED ${NATIVE_SRCS})

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries(gbc z log jnigraphics GLESv1_CM)
