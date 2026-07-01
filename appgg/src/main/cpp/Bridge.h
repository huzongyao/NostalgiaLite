/**
 * @file Bridge.h
 * @brief Game Gear JNI 桥接头文件。
 * 定义 Bridge 类，负责将 JNI 调用转发到 Emulator 实例。
 */

#ifndef BRIDGE_H_
#define BRIDGE_H_


#include "jni.h"
namespace emudroid {

/**
 * @brief JNI 桥接类。
 * 持有 Emulator 指针，将 Java 层 JNI 调用转发到具体模拟器实现。
 */
class Bridge {
public:
    /**
     * 构造函数。
     * @param emu 模拟器实例指针
     */
    Bridge(Emulator *emu);
};

}
#endif

