# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.
# by huzongyao

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DPSS_STYLE=1 -DHAVE_ASPRINTF -DLSB_FIRST -DFRAMESKIP -DIOAPI_NO_64 -D_STLP_HAS_WCHAR_T -D_GLIBCXX_USE_WCHAR_T")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-write-strings -Ofast -fexceptions -frtti -fno-builtin-sin -fno-builtin-cos -Wpointer-arith")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fmerge-all-constants -fvisibility=hidden -fvisibility-inlines-hidden -funroll-loops  -ffast-math")

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

include_directories(./ fceux/
                    fceux/boards/
                    fceux/input/
                    ${FRAMEWORK_CPP_PATH})

add_library(nes SHARED ${NATIVE_SRCS} ${SHARED_SRCS})

set_target_properties(nes PROPERTIES LINK_FLAGS "-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")

target_link_libraries(nes z log jnigraphics GLESv1_CM)
