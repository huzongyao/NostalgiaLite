/**
 * @file Bridge.cpp
 * @brief Game Gear JNI 桥接实现。
 * 实现所有 JNI 本地方法，将 Java 层调用转发到 Emulator 实例。
 */

#include <jni.h>
#include "Emulator.h"
#include "Bridge.h"

extern "C" {
using namespace emudroid;

/** 全局模拟器实例指针 */
Emulator *emu;

#ifndef BRIDGE_PACKAGE
#define BRIDGE_PACKAGE :-)
#endif

/**
 * 桥接构造函数，保存模拟器实例指针。
 * @param emulator 模拟器实例
 */
Bridge::Bridge(Emulator *emulator) {
    emu = emulator;
}

/** 启动模拟器引擎 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(start)(JNIEnv *env, jobject obj, jint gfx, jint sfx, jint general) {
    return (jboolean) emu->start(gfx, sfx, general);
}


/** 读取调色板数据 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(readPalette)(JNIEnv *env, jobject obj, jintArray result) {
    return (jboolean) emu->readPalette(env, result);
}


/** 加载游戏 ROM，同时传入电池存档路径 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(loadGame)(JNIEnv *env, jobject obj, jstring path,
        jstring batteryPath, jstring batteryFullPath) {
    jboolean isCopy;
    jboolean isCopy2;
    jboolean isCopy3;
    const char *fname = env->GetStringUTFChars(path, &isCopy);
    const char *fbattery = env->GetStringUTFChars(batteryPath, &isCopy2);
    const char *fbatteryFullPath = env->GetStringUTFChars(batteryFullPath, &isCopy3);
    bool success = emu->loadGame(fname, fbattery, fbatteryFullPath);
    env->ReleaseStringUTFChars(path, fname);
    env->ReleaseStringUTFChars(batteryPath, fbattery);
    env->ReleaseStringUTFChars(batteryFullPath, fbatteryFullPath);
    return (jboolean) success;
}

/** 设置模拟器基础目录路径 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(setBaseDir)(JNIEnv *env, jobject obj, jstring path) {
    jboolean isCopy;
    const char *fname = env->GetStringUTFChars(path, &isCopy);
    bool success = emu->setBaseDir(fname);
    env->ReleaseStringUTFChars(path, fname);
    return (jboolean) success;
}

/** 启用金手指（字符串格式） */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(enableCheat)(JNIEnv *env, jobject obj, jstring gg, jint type) {
    jboolean isCopy;
    const char *cheat = env->GetStringUTFChars(gg, &isCopy);
    bool success = emu->enableCheat(cheat, type);
    env->ReleaseStringUTFChars(gg, cheat);
    return (jboolean) success;
}

/** 启用原始地址金手指 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(enableRawCheat)(JNIEnv *env, jobject obj, jint addr, jint val, jint comp) {
    jboolean isCopy;
    bool success = emu->enableRawCheat(addr, val, comp);
    return (jboolean) success;
}

/** 执行一帧模拟，包含按键和连发处理 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(emulate)(JNIEnv *env, jobject obj, jint keys, jint turbos, jint numFramesToSkip) {
    int res = emu->emulateFrame(keys, turbos, numFramesToSkip);
    return (jboolean) res;
}

/** 渲染当前帧到 Bitmap */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(render)(JNIEnv *env, jobject obj, jobject bitmap) {
    return (jboolean) emu->render(env, bitmap, -1, -1, NULL);
}


/** 渲染当前帧到指定尺寸的 Bitmap */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(renderVP)(JNIEnv *env, jobject obj, jobject bitmap, int w,int h) {
    return (jboolean) emu->render(env, bitmap, w, h, NULL);
}


/** 使用 OpenGL 渲染当前帧 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(renderGL)(JNIEnv *env, jobject obj) {
    return (jboolean) emu->renderGL();
}

/** 获取时间回溯历史项数 */
JNIEXPORT jint JNICALL
BRIDGE_PACKAGE(getHistoryItemCount)(JNIEnv *env, jobject obj) {
    return emu->getHistoryItemCount();
}

/** 加载指定位置的历史状态 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(loadHistoryState)(JNIEnv *env, jobject obj, int pos) {
    return (jboolean) emu->loadHistoryState(pos);
}

/** 渲染历史状态帧到 Bitmap */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(renderHistory)(JNIEnv *env, jobject obj, jobject bmp, int pos, int w, int h) {
    return (jboolean) emu->renderHistory(env, bmp, pos, w, h);
}

/** 设置视口尺寸 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(setViewPortSize)(JNIEnv *env, jobject obj, jint w, jint h) {
    return (jboolean) emu->setViewPortSize(w, h);
}

/** 触发光枪射击事件 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(fireZapper)(JNIEnv *env, jobject obj, jint x, jint y) {
    return (jboolean) emu->fireZapper(x, y);
}


/** 读取音频缓冲区数据 */
JNIEXPORT jint JNICALL
BRIDGE_PACKAGE(readSfxBuffer)(JNIEnv *env, jobject obj, jshortArray data) {
    return emu->readSfxBuffer(env, obj, data);
}

/** 加载游戏存档状态 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(loadState)(JNIEnv *env, jobject obj, jstring path, int slot) {
    jboolean isCopy;
    const char *fname = env->GetStringUTFChars(path, &isCopy);
    bool success = emu->loadState(fname, slot);
    env->ReleaseStringUTFChars(path, fname);
    return (jboolean) success;
}

/** 保存游戏存档状态 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(saveState)(JNIEnv *env, jobject obj, jstring path, int slot) {
    jboolean isCopy;
    const char *fname = env->GetStringUTFChars(path, &isCopy);
    bool success = emu->saveState(fname, slot);
    env->ReleaseStringUTFChars(path, fname);
    return (jboolean) success;
}

/** 重置模拟器 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(reset)(JNIEnv *env, jobject obj, jstring path) {
    return (jboolean) emu->reset();
}

/** 停止模拟器 */
JNIEXPORT jboolean JNICALL
BRIDGE_PACKAGE(stop)(JNIEnv *env, jobject obj) {
    return (jboolean) emu->stop();
}


}
