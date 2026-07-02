/**
 * 共享 JNI 桥接头文件。
 * <p>定义 Bridge 类，持有 Emulator 实例引用，
 * 供 JNI 函数调用模拟器核心方法。</p>
 */
#ifndef BRIDGE_H_
#define BRIDGE_H_

#include "jni.h"

namespace emudroid {

    class Emulator;

    /**
     * JNI 桥接类。
     * <p>包装 Emulator 指针，供 Bridge.cpp 中的 JNI 函数统一调用。</p>
     */
    class Bridge {
    public:
        /** 构造桥接对象，绑定模拟器实例 */
        Bridge(Emulator *emu);
    };
}
#endif
