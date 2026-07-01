/**
 * @file Emulator.cpp
 * @brief Game Gear 模拟器基类实现。
 * 实现图形/音频缓冲区管理、Bresenham 缩放渲染、时间回溯、状态存档等通用功能。
 */

#include "Emulator.h"
#include <android/log.h>
#include <android/bitmap.h>
#include <stdio.h>
#include <GLES/gl.h>
#include <GLES/glext.h>
#include <string.h>
#include <vector>

#include "settings.h"

using namespace emudroid;

/** Java 虚拟机全局指针 */
JavaVM *jvm;

/** 构造函数，初始化所有成员变量 */
Emulator::Emulator() {
    viewPortHeight = 0;
    viewPortWidth = 0;
    origWidth = 0;
    offsetIdx = 0;
    origHeight = 0;
    historyIndex = -1;
    historySize = 0;
    historyEnabled = false;
    lastPath = (char*) malloc(1);
    stableGfx = 0;
    emuPalette = new PALETTE_TYPE[256];}

/** 设置视口尺寸，并计算 Bresenham 缩放参数 */
bool Emulator::setViewPortSize(int w, int h) {
    viewPortWidth = w;
    viewPortHeight = h;
    int newWidth = viewPortWidth;
    int newHeight = viewPortHeight;
    yd = (origHeight / newHeight) * origWidth - origWidth;
    yr = origHeight % newHeight;
    xd = origWidth / newWidth;
    xr = origWidth % newWidth;
    return true;
}

/** 启用金手指（默认不实现） */
bool Emulator::enableCheat(const char *cheat, int type) {
    return false;
}


/** 启用原始地址金手指（默认不实现） */
bool Emulator::enableRawCheat(int addr, int val, int comp) {
    return false;
}

/** 触发光枪（默认不实现） */
bool Emulator::fireZapper(int x, int y) {
    return false;
}

/** 读取调色板数据到 Java 数组 */
bool Emulator::readPalette(JNIEnv *env, jintArray result) {
    env->SetIntArrayRegion(result, 0, 256, (const int*) emuPalette);
    return true;
}

/** 连发计数器 */
int turboCounter = 0;

/**
 * 执行一帧模拟。
 * 处理连发按键逻辑：每隔 8 帧关闭连发按键输入。
 * @param keys 当前按键状态
 * @param turbos 连发按键状态
 * @param numFramesToSkip 跳帧数
 * @return 执行成功返回 true
 */
bool Emulator::emulateFrame(int keys, int turbos, int numFramesToSkip) {
    if (turboCounter == 0) {
        keys &= turbos;
    }

    turboCounter++;
    static const int turboC = 8;

    if (turboCounter == turboC) {
        turboCounter = 0;
    }

    bool res = emulate(keys, turbos, numFramesToSkip);
    return res;
}

/** 加载存档状态，非即时存档时清空历史 */
bool Emulator::loadState(const char*path, int slot) {
    if (slot != 0) {
        historyIndex = -1;
        historySize = 0;
    }

    return doLoadState(path, slot);
}

/** 加载游戏，切换游戏时清空历史记录 */
bool Emulator::loadGame(const char*path, const char*batteryPath, const char*strippedName) {
    if (strcmp(lastPath, path) != 0) {
        historyIndex = -1;
        historySize = 0;
    }

    free(lastPath);
    lastPath = (char*) malloc(strlen(path) + 1);
    strcpy(lastPath, path);
    return doLoadGame(path, batteryPath,strippedName);
}

/**
 * 渲染当前帧到 Android Bitmap。
 * 使用 Bresenham 算法将原始尺寸缩放到目标尺寸。
 * 参考: http://willperone.net/Code/codescaling.php
 * @param env JNI 环境
 * @param bitmap 目标 Bitmap 对象
 * @param w 目标宽度，-1 表示使用视口尺寸
 * @param h 目标高度，-1 表示使用视口尺寸
 * @param force 强制使用的图形缓冲区，NULL 表示使用稳定缓冲区
 * @return 渲染成功返回 true
 */
// based on http://willperone.net/Code/codescaling.php
bool Emulator::render(JNIEnv *env, jobject bitmap, int w, int h,
                      BUFFER_TYPE *force) {
    int stable = swapBuffersBeforeRead();
    void *pixels;

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return false;
    }

    int newHeight;
    int newWidth;
    int mxd, myd, mxr, myr;

    if (w == -1) {
        newHeight = viewPortHeight;
        newWidth = viewPortWidth;
        mxd = this->xd;
        myd = this->yd;
        mxr = this->xr;
        myr = this->yr;

    } else {
        newHeight = h;
        newWidth = w;
        myd = (origHeight / h) * origWidth - origWidth;
        myr = origHeight % h;
        mxd = origWidth / w;
        mxr = origWidth % w;
    }

    int outOffset = 0;
    int inOffset = offsetIdx;
    BUFFER_TYPE *buf = force == NULL ? gfxBufs[stable] : force;

    for (int y = newHeight, ye = 0; y > 0; y--) {
        int *data = (int*) pixels;

        for (int x = newWidth, xe = 0; x > 0; x--) {
            data[outOffset++] = GET_PIXEL(buf, inOffset);
            inOffset += mxd;
            xe += mxr;

            if (xe >= newWidth) {
                xe -= newWidth;
                inOffset++;
            }
        }

        inOffset += myd;
        ye += myr;

        if (ye >= newHeight) {
            ye -= newHeight;
            inOffset += origWidth;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return true;
}

/** 设置时间回溯功能开关 */
bool Emulator::setHistoryEnabled(bool enabled) {
    if (enabled && !historyEnabled) {
        historyIndex = -1;
        historySize = 0;
    }

    historyEnabled = enabled;
    return true;
}

/** 保存当前状态到历史缓冲区（环形缓冲区） */
bool Emulator::saveToHistory() {
    historyIndex++;

    if (historySize < HIS_SIZE) {
        historySize++;
    }

    if (historyIndex == HIS_SIZE) {
        historyIndex = 0;
    }

    return doSaveHistoryState(historyIndex);
}

/** 获取历史项数量，未启用时返回 0 */
int Emulator::getHistoryItemCount() {
    if (!historyEnabled) {
        return 0;
    }

    return historySize;
}

/** 加载指定位置的历史状态，并更新历史索引 */
bool Emulator::loadHistoryState(int delta) {
    if (delta > getHistoryItemCount()) {
        return false;
    }

    int idx = posToIdx(delta);
    bool res = doLoadHistoryState(idx);

    if (res) {
        historyIndex -= delta;

        if (historyIndex < 0) {
            historyIndex = HIS_SIZE - (-historyIndex);
        }

        historySize -= delta;
    }

    return res;
}

/** 将历史位置偏移转换为环形缓冲区数组索引 */
int Emulator::posToIdx(int delta) {
    int curPos = historyIndex;
    curPos -= delta;

    if (curPos < 0) {
        curPos = HIS_SIZE - (-curPos);
    }

    return curPos;
}

/** 使用 OpenGL 渲染当前帧，更新纹理数据 */
bool Emulator::renderGL() {
    int stable = swapBuffersBeforeRead();
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, origWidth, origHeight, GL_ALPHA,
                    GL_UNSIGNED_BYTE, gfxBufs[stable] + offsetIdx);
    return true;
}

/** 线程锁构造函数 */
CThreadLock::CThreadLock() {
    pthread_mutex_init(&mutexlock, 0);
}

/** 线程锁析构函数 */
CThreadLock::~CThreadLock() {
    pthread_mutex_destroy(&mutexlock);
}
/** 加锁 */
void CThreadLock::Lock() {
    pthread_mutex_lock(&mutexlock);
}
/** 解锁 */
void CThreadLock::Unlock() {
    pthread_mutex_unlock(&mutexlock);
}

/**
 * 读取音频缓冲区数据。
 * 双缓冲交换机制：读取当前缓冲区后切换到另一个缓冲区。
 * @param env JNI 环境
 * @param obj Java 对象
 * @param data 输出音频数据数组
 * @return 读取的采样点数
 */
int Emulator::readSfxBuffer(JNIEnv *env, jobject obj, jshortArray data) {
    CThreadLock lock = sfxLock;
    lock.Lock();
    int cur = curSfx;
    int len = sfxBufPos[cur];
    sfxBufPos[cur] = 0;
    curSfx = curSfx == 0 ? 1 : 0;
    lock.Unlock();
    env->SetShortArrayRegion(data, 0, len, sfxBufs[cur]);
    return len;
}

/** 析构函数 */
Emulator::~Emulator() {
}

/**
 * 读取前交换图形缓冲区。
 * 如果工作副本已脏，则将稳定缓冲区与副本交换。
 * @return 稳定的图形缓冲区索引
 */
int Emulator::swapBuffersBeforeRead() {
    CThreadLock lock = gfxLock;
    lock.Lock();

    if (workingCopyDirty) {
        int temp = stableGfx;
        stableGfx = workingGfx_copy;
        workingGfx_copy = temp;
        workingCopyDirty = false;
    }

    int res = stableGfx;
    lock.Unlock();
    return res;
}

/** 写入完成后交换图形缓冲区，将工作缓冲区与副本交换并标记为脏 */
void Emulator::swapBuffersAfterWrite() {
    CThreadLock lock = gfxLock;
    lock.Lock();
    int temp = workingGfx;
    workingGfx = workingGfx_copy;
    workingGfx_copy = temp;
    workingCopyDirty = true;
    lock.Unlock();
}

/** 初始化图形和音频缓冲区，分配三图形双音频缓冲 */
void Emulator::initBuffers() {
    for (int i = 0; i < 3; i++) {
        gfxBufs[i] = new BUFFER_TYPE[getGfxBufferSize()];
    }

    for (int i = 0; i < 2; i++) {
        sfxBufs[i] = new short[getSfxBufferSize()];
    }

    stableGfx = 0;
    workingGfx = 1;
    workingGfx_copy = 2;
}
