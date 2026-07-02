/**
 * 模拟器共享基类头文件。
 * <p>定义 Emulator 抽象基类和 CThreadLock 线程锁工具类。
 * Emulator 封装了帧渲染、音频缓冲、历史状态管理、双缓冲交换等通用逻辑，
 * 具体平台（NES/GBC/GG）通过继承实现平台特有的模拟功能。</p>
 * <p>各平台通过实现 getPixel() 虚函数提供不同的像素格式转换。</p>
 */
#ifndef EMULATOR_H_
#define EMULATOR_H_

#include "jni.h"
#include "settings_common.h"
#include <pthread.h>
#include <string>

namespace emudroid {

    /**
     * 线程互斥锁封装。
     * <p>基于 pthread_mutex 实现，用于保护图形和音频缓冲区的并发访问。</p>
     */
    class CThreadLock {
    public:
        CThreadLock();

        virtual ~CThreadLock();

        void Lock();

        void Unlock();

    private:
        pthread_mutex_t mutexlock;
    };


    /**
     * 模拟器抽象基类。
     * <p>提供帧模拟、渲染、存档、历史状态、金手指等通用接口。
     * 采用三缓冲图形交换机制（working/working_copy/stable），
     * 双缓冲音频交换机制，确保模拟线程和渲染线程的安全并发。</p>
     * <p>子类必须实现 getPixel() 以提供平台特有的像素格式转换。</p>
     */
    class Emulator {

    public:
        Emulator();

        virtual bool start(int gfx, int sfx, int general) = 0;

        bool emulateFrame(int keys, int turbos, int numFramesToSkip);

        virtual bool setBaseDir(const char *path) = 0;

        bool loadGame(const char *path, const char *batterySaveDir, const char *strippedName);

        bool loadState(const char *path, int slot);

        virtual bool saveState(const char *path, int slot) = 0;

        virtual bool doLoadGame(const char *path, const char *batterySaveDir,
                                const char *strippedName) = 0;

        virtual bool doLoadHistoryState(int idx) = 0;

        virtual bool doSaveHistoryState(int idx) = 0;

        virtual bool doLoadState(const char *path, int slot) = 0;

        virtual int getHistoryItemCount();

        virtual bool renderHistory(JNIEnv *env, jobject bitmap, int pos, int vw, int wh) = 0;

        bool saveToHistory();

        virtual bool loadHistoryState(int pos);

        virtual bool enableCheat(const char *cheat, int type);

        virtual bool enableRawCheat(int addr, int val, int comp);

        virtual bool reset() = 0;

        virtual bool stop() = 0;

        virtual bool fireZapper(int x, int y);

        virtual bool render(JNIEnv *env, jobject bitmap, int vw, int wh, BUFFER_TYPE *force);

        virtual bool renderGL();

        virtual bool readPalette(JNIEnv *env, jintArray result);

        virtual int readSfxBuffer(JNIEnv *env, jobject obj, jshortArray data);

        virtual bool setViewPortSize(int w, int h);

        virtual ~Emulator();

    protected:

        /** 历史状态环形缓冲区大小（帧数） */
        static const int HIS_SIZE = 40;

        virtual bool emulate(int keys, int turbos, int numFramesToSkip) = 0;

        /**
         * 将原始缓冲区数据转换为 ARGB 像素值。
         * <p>各平台实现不同：NES 使用调色板查找，GBC/GG 直接组合 RGB 通道。</p>
         * @param buf 图形缓冲区指针
         * @param idx 缓冲区偏移索引
         * @return 32 位 ARGB 像素值
         */
        virtual unsigned int getPixel(const BUFFER_TYPE *buf, int idx) const = 0;

        CThreadLock sfxLock;
        CThreadLock gfxLock;
        std::string lastPath;

        int viewPortWidth;
        int viewPortHeight;
        int origWidth;
        int origHeight;
        int offsetIdx;
        int historyIndex;
        int historySize;

        int stableGfx;
        int workingGfx;
        int workingGfx_copy;
        bool workingCopyDirty;
        bool historyEnabled;
        int curSfx;
        short *sfxBufs[2];
        BUFFER_TYPE *gfxBufs[3];
        PALETTE_TYPE *emuPalette;
        unsigned int sfxBufPos[2];

        virtual int getGfxBufferSize() = 0;

        virtual int getSfxBufferSize() = 0;

        bool setHistoryEnabled(bool enable);

        int posToIdx(int delta);

        void swapBuffersAfterWrite();

        int swapBuffersBeforeRead();

        void initBuffers();

    private:

        int xd, yr, yd, xr;
    };

}
#endif
