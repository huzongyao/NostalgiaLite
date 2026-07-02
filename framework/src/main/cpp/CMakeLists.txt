# 共享模拟器基类静态库
# 提供 Emulator 基类、Bridge JNI 桥接、通用类型定义
# 由三个 app 模块（appnes/appgbc/appgg）链接使用

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-write-strings -Ofast -fexceptions -frtti -fno-builtin-sin -fno-builtin-cos -Wpointer-arith")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fmerge-all-constants -fvisibility=hidden -fvisibility-inlines-hidden -funroll-loops -ffast-math")

add_definitions(-Wno-narrowing)

# 共享源文件：模拟器基类 + JNI 桥接
set(SHARED_SRCS
    Emulator.cpp
    Bridge.cpp
)

include_directories(./)

# 编译为静态库供各 app 模块链接
add_library(emudroid_base STATIC ${SHARED_SRCS})
