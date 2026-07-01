/**
 * GBC JNI 桥接头文件。
 * <p>定义 Bridge 类，持有 Emulator 实例引用。</p>
 */

#ifndef BRIDGE_H_
#define BRIDGE_H_


#include "jni.h"
namespace emudroid {
/** JNI 桥接类，包装 Emulator 指针供 JNI 函数调用 */
class Bridge {
public:
    /** 构造桥接对象，绑定模拟器实例 */
    Bridge(Emulator *emu);
};

}
#endif

