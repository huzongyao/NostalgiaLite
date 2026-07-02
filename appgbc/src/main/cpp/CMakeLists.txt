# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.
# by huzongyao

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-exceptions -fno-rtti -DINLINE=inline -DHAVE_STDINT_H -DINT_LEAST_32 -DHAVE_INTTYPES_H -DLSB_FIRST -Wreturn-type")

add_definitions(-Wno-narrowing)

# 收集所有本地源文件，排除已移至 framework 共享的重复文件
file(GLOB_RECURSE ALL_NATIVE_SRCS *.c *.cpp)
set(NATIVE_SRCS ${ALL_NATIVE_SRCS})
list(FILTER NATIVE_SRCS EXCLUDE REGEX ".*/Emulator\\.cpp$")
list(FILTER NATIVE_SRCS EXCLUDE REGEX ".*/Bridge\\.cpp$")

# framework 共享 C++ 源文件（模拟器基类 + JNI 桥接）
set(FRAMEWORK_CPP_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../../../../framework/src/main/cpp)
set(SHARED_SRCS
    ${FRAMEWORK_CPP_PATH}/Emulator.cpp
    ${FRAMEWORK_CPP_PATH}/Bridge.cpp
)

include_directories(./ common/
                    include/
                    libgambatte/include/
                    libgambatte/src/
                    src/file/
                    src/mem/
                    libgambatte/src/file/
                    src/file/unzip/
                    common/resample/
                    ${FRAMEWORK_CPP_PATH})

add_library(gbc SHARED ${NATIVE_SRCS} ${SHARED_SRCS})

set_target_properties(gbc PROPERTIES LINK_FLAGS "-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")

target_link_libraries(gbc z log jnigraphics GLESv1_CM)
