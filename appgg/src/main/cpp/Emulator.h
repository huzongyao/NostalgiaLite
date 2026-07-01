/**
 * @file Emulator.h
 * @brief Game Gear 模拟器基类头文件。
 * 定义线程锁类 CThreadLock 和模拟器基类 Emulator，
 * 提供图形/音频缓冲区管理、时间回溯、状态存档等通用功能。
 */

#ifndef EMULATOR_H_
#define EMULATOR_H_

#include "jni.h"
#include "settings.h"
#include <pthread.h>
#include <malloc.h>



namespace emudroid {

/**
 * @brief 线程互斥锁封装类。
 * 基于 POSIX pthread_mutex 实现，用于保护图形和音频缓冲区的并发访问。
 */
class CThreadLock
{
public:
    CThreadLock();
    virtual ~CThreadLock();

    void Lock();   /**< 加锁 */
    void Unlock(); /**< 解锁 */
private:
    pthread_mutex_t mutexlock; /**< POSIX 互斥锁 */
};



/**
 * @brief 模拟器基类。
 * 提供图形/音频双缓冲、时间回溯、状态存档、Bresenham 缩放渲染等通用功能。
 * 具体平台需继承此类并实现纯虚函数。
 */
class Emulator {

public:
    Emulator();
    virtual bool start(int gfx, int sfx, int general) = 0;  /**< 启动模拟器 */
    bool emulateFrame(int keys, int turbos, int numFramesToSkip); /**< 执行一帧模拟 */
    virtual bool setBaseDir(const char *path) = 0; /**< 设置基础目录 */
    bool loadGame(const char *path, const char*batterySaveDir,const char*strippedName); /**< 加载游戏 */
    bool loadState(const char *path, int slot); /**< 加载存档状态 */



    virtual bool saveState(const char *path, int slot) = 0; /**< 保存存档状态（纯虚） */

    virtual bool doLoadGame(const char*path, const char*batterySaveDir, const char*strippedName) = 0; /**< 实际加载游戏（纯虚） */
    virtual bool doLoadHistoryState(int idx) = 0;  /**< 加载历史状态（纯虚） */
    virtual bool doSaveHistoryState(int idx) = 0;  /**< 保存历史状态（纯虚） */
    virtual bool doLoadState(const char *path, int slot) = 0; /**< 实际加载存档（纯虚） */


    virtual int getHistoryItemCount(); /**< 获取历史项数量 */


    virtual bool renderHistory(JNIEnv *env, jobject bitmap, int pos, int vw, int wh) = 0; /**< 渲染历史帧 */

    bool saveToHistory(); /**< 保存当前状态到历史缓冲区 */
    virtual bool loadHistoryState(int pos); /**< 加载指定位置的历史状态 */


    virtual bool enableCheat(const char *cheat, int type);    /**< 启用金手指 */
    virtual bool enableRawCheat(int addr, int val, int comp); /**< 启用原始地址金手指 */
    virtual bool reset() = 0;  /**< 重置模拟器（纯虚） */
    virtual bool stop() = 0;   /**< 停止模拟器（纯虚） */

    virtual bool fireZapper(int x, int y); /**< 触发光枪 */
    virtual bool render(JNIEnv *env, jobject bitmap, int vw, int wh, BUFFER_TYPE *force); /**< 渲染到 Bitmap */


    virtual bool renderGL(); /**< OpenGL 渲染 */

    virtual bool readPalette(JNIEnv *env, jintArray result); /**< 读取调色板 */
    virtual int readSfxBuffer(JNIEnv *env, jobject obj, jshortArray data); /**< 读取音频缓冲 */
    virtual bool setViewPortSize(int w, int h); /**< 设置视口尺寸 */

    virtual ~Emulator();

    JavaVM *jvm; /**< Java 虚拟机指针 */
protected:

    static const int HIS_SIZE = 40; /**< 历史缓冲区大小 */
    virtual bool emulate(int keys, int turbos, int numFramesToSkip) = 0; /**< 实际模拟执行（纯虚） */
    CThreadLock sfxLock;  /**< 音频缓冲区锁 */
    CThreadLock gfxLock;  /**< 图形缓冲区锁 */
    char * lastPath;      /**< 上次加载的游戏路径 */

    int viewPortWidth;    /**< 视口宽度 */
    int viewPortHeight;   /**< 视口高度 */
    int origWidth;        /**< 原始游戏画面宽度 */
    int origHeight;       /**< 原始游戏画面高度 */
    int offsetIdx;        /**< 图形缓冲区偏移索引 */
    int historyIndex;     /**< 当前历史索引 */
    int historySize;      /**< 当前历史项数量 */

    int stableGfx;        /**< 稳定图形缓冲区索引（已渲染完成，可读取） */
    int workingGfx;       /**< 工作图形缓冲区索引（正在写入） */
    int workingGfx_copy;  /**< 工作缓冲区副本索引（用于交换） */
    bool workingCopyDirty; /**< 工作副本是否已脏（有写入待同步） */
    bool historyEnabled;  /**< 是否启用时间回溯 */
    int curSfx;           /**< 当前音频缓冲区索引 */
    short *sfxBufs[2];    /**< 双音频缓冲区 */
    BUFFER_TYPE *gfxBufs[3]; /**< 三图形缓冲区（稳定/工作/副本） */
    PALETTE_TYPE *emuPalette; /**< 模拟器调色板 */
    unsigned int sfxBufPos[2]; /**< 各音频缓冲区当前写入位置 */
    virtual int getGfxBufferSize() = 0; /**< 获取图形缓冲区大小（纯虚） */
    virtual int getSfxBufferSize() = 0; /**< 获取音频缓冲区大小（纯虚） */

    bool setHistoryEnabled(bool enable); /**< 设置时间回溯开关 */



    int posToIdx(int delta); /**< 将历史位置偏移转换为数组索引 */
    void swapBuffersAfterWrite();  /**< 写入完成后交换图形缓冲区 */
    int swapBuffersBeforeRead();   /**< 读取前交换图形缓冲区 */
    void initBuffers();            /**< 初始化图形和音频缓冲区 */
private:

    int xd, yr, yd, xr; /**< Bresenham 缩放算法参数 */
};







}
#endif
