#include <jni.h>
#include "Emulator.h"
#include "Bridge.h"

extern "C" {
using namespace emudroid;

Emulator *emu;

#ifndef BRIDGE_PACKAGE
#define BRIDGE_PACKAGE :-)
#endif

Bridge::Bridge(Emulator *emulator) {
    emu = emulator;
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(start)(JNIEnv *env, jobject obj, jint gfx, jint sfx,
                                                 jint general) {
    return (jboolean) emu->start(gfx, sfx, general);
}


JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(readPalette)(JNIEnv *env, jobject obj, jintArray result) {
    return (jboolean) emu->readPalette(env, result);
}


JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(loadGame)(JNIEnv *env, jobject obj, jstring path,
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

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(setBaseDir)(JNIEnv *env, jobject obj, jstring path) {
    jboolean isCopy;
    const char *fname = env->GetStringUTFChars(path, &isCopy);
    bool success = emu->setBaseDir(fname);
    env->ReleaseStringUTFChars(path, fname);
    return (jboolean) success;
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(enableCheat)(JNIEnv *env, jobject obj, jstring gg,
                                                       jint type) {
    jboolean isCopy;
    const char *cheat = env->GetStringUTFChars(gg, &isCopy);
    bool success = emu->enableCheat(cheat, type);
    env->ReleaseStringUTFChars(gg, cheat);
    return (jboolean) success;
}


JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(enableRawCheat)(JNIEnv *env, jobject obj, jint addr,
                                                          jint val, jint comp) {
    bool success = emu->enableRawCheat(addr, val, comp);
    return (jboolean) success;
}


JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(emulate)(JNIEnv *env, jobject obj, jint keys, jint turbos,
                                                   jint numFramesToSkip) {
    int res = emu->emulateFrame(keys, turbos, numFramesToSkip);
    return (jboolean) res;
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(render)(JNIEnv *env, jobject obj, jobject bitmap) {
    return (jboolean) emu->render(env, bitmap, -1, -1, NULL);
}


JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(renderVP)(JNIEnv *env, jobject obj, jobject bitmap, int w,
                                                    int h) {
    return (jboolean) emu->render(env, bitmap, w, h, NULL);
}


JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(renderGL)(JNIEnv *env, jobject obj) {
    return (jboolean) emu->renderGL();
}


JNIEXPORT jint JNICALL BRIDGE_PACKAGE(getHistoryItemCount)(JNIEnv *env, jobject obj) {
    return emu->getHistoryItemCount();
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(loadHistoryState)(JNIEnv *env, jobject obj, int pos) {
    return (jboolean) emu->loadHistoryState(pos);
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(renderHistory)(JNIEnv *env, jobject obj, jobject bmp,
                                                         int pos, int w, int h) {
    return (jboolean) emu->renderHistory(env, bmp, pos, w, h);
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(setViewPortSize)(JNIEnv *env, jobject obj, jint w,
                                                           jint h) {
    return (jboolean) emu->setViewPortSize(w, h);
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(fireZapper)(JNIEnv *env, jobject obj, jint x, jint y) {
    return (jboolean) emu->fireZapper(x, y);
}


JNIEXPORT jint JNICALL BRIDGE_PACKAGE(readSfxBuffer)(JNIEnv *env, jobject obj,
                                                     jshortArray data) {
    return emu->readSfxBuffer(env, obj, data);
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(loadState)(JNIEnv *env, jobject obj,
                                                     jstring path, int slot) {
    jboolean isCopy;
    const char *fname = env->GetStringUTFChars(path, &isCopy);
    bool success = emu->loadState(fname, slot);
    env->ReleaseStringUTFChars(path, fname);
    return (jboolean) success;
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(saveState)(JNIEnv *env, jobject obj,
                                                     jstring path, int slot) {
    jboolean isCopy;
    const char *fname = env->GetStringUTFChars(path, &isCopy);
    bool success = emu->saveState(fname, slot);
    env->ReleaseStringUTFChars(path, fname);
    return (jboolean) success;
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(reset)(JNIEnv *env, jobject obj,
                                                 jstring path) {
    return (jboolean) emu->reset();
}

JNIEXPORT jboolean JNICALL BRIDGE_PACKAGE(stop)(JNIEnv *env, jobject obj) {
    return (jboolean) emu->stop();
}


}
