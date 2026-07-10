/**
 * @file Sms.cpp
 * @brief Game Gear / SMS 模拟器 C++ 实现。
 * 封装 sms_plus 引擎，实现 SmsEmulator 类，
 * 提供游戏加载、帧模拟、图形渲染、音频处理、金手指、电池存档等功能。
 */

#include <jni.h>
#include <android/bitmap.h>
#include <cstdlib>
#include <string>
#include <GLES/gl.h>

#include "Emulator.h"
#include "Bridge.h"

extern "C" {
#include "sms_plus/shared.h"

using namespace std;
using namespace emudroid;

/** 屏幕宽度（GG 原始分辨率 160） */
#define WIDTH 160
/** 屏幕高度（GG 原始分辨率 144） */
#define HEIGHT 144
/** 图形缓冲区填充边距 */
#define PADDING 26
/** 音频缓冲区大小 */
#define SFX_BUF_SIZE 2048 * 8


/**
 * @brief GG/SMS 模拟器实现类。
 * 封装 sms_plus 引擎，实现游戏加载、帧模拟、图形/音频处理、
 * 金手指、电池存档、时间回溯等功能。
 */
class SmsEmulator : public Emulator {
private:

public:
    uint8 *gfxBuf_LQ;   /**< 低质量图形缓冲区（256x256） */
    short *sfxBuf;       /**< 音频缓冲区 */
    bool started;        /**< 引擎是否已启动 */

    /** 构造函数，初始化所有缓冲区和引擎状态 */
    SmsEmulator() {
        started = false;
        numEnabledCheats = 0;
        origHeight = HEIGHT;
        origWidth = WIDTH;
        gfxBuf_LQ = new uint8[256 * 256];
        sfxBuf = new short[SFX_BUF_SIZE];

        for (int i = 0; i < 40; i++) {
            travel[i] = new BUFFER_TYPE[160 * (144 + PADDING) * 3];
        }
        initBuffers();
        resetSfx();
    }


    bool soundEnabled;    /**< 是否启用声音 */
    int soundRate;        /**< 音频采样率 */
    bool loadSavFiles;    /**< 是否加载电池存档 */
    bool saveSavFiles;    /**< 是否保存电池存档 */

    /**
     * 启动模拟器引擎。
     * @param gfxInit 图形配置
     * @param sfxInit 音频配置（-1 表示禁用）
     * @param generalInit 通用配置（位标志组合）
     * @return 始终返回 true
     */
    bool start(int gfxInit, int sfxInit, int generalInit) {
        counter = 0;
        soundEnabled = sfxInit != -1;

        if (soundEnabled) {
            soundRate = sfxInit;
        } else {
            soundRate = 0;
        }

        int quality = generalInit / 10000;

        if (generalInit >= 10000) {
            generalInit -= quality * 10000;
        }

        saveSavFiles = generalInit >= 1000;

        if (generalInit >= 1000) {
            generalInit -= 1000;
        }

        loadSavFiles = generalInit >= 100;

        if (generalInit >= 100) {
            generalInit -= 100;
        }

        setHistoryEnabled(generalInit >= 10);

        if (generalInit >= 10) {
            generalInit -= 10;
        }
        resetSfx();
        disableAllCheats();
        return true;
    }

    BUFFER_TYPE *travel[40]; /**< 时间回溯图形缓冲区数组 */
    int counter;              /**< 帧计数器，用于控制历史保存频率 */

    /**
     * 执行一帧模拟。
     * 处理按键输入、跳帧、金手指应用、图形渲染、音频采集。
     * 图形数据按 RGB 三通道分离存储到图形缓冲区。
     * @param keys 当前按键状态
     * @param turbos 连发按键状态
     * @param numFramesToSkip 跳帧数
     * @return 始终返回 true
     */
    bool emulate(int keys, int turbos, int numFramesToSkip) {
        input.system = 0;

        if (keys & 0x80) {
            input.system = 0x01;
            keys &= ~0x80;
        }
        input.pad[0] = keys;

        for (int i = 0; i < numFramesToSkip; i++) {
            for (int c = 0; c < numEnabledCheats; c++) {
                sms.wram[cheatAddrs[c]] = cheatVals[c];
            }
            system_frame(1);
        }

        for (int c = 0; c < numEnabledCheats; c++) {
            sms.wram[cheatAddrs[c]] = cheatVals[c];
        }

        system_frame(0);
        int targetI = 0;
        int C1 = WIDTH * (HEIGHT + PADDING);
        int C2 = WIDTH * (HEIGHT + PADDING) << 1;
        BUFFER_TYPE *g = gfxBufs[workingGfx];
        uint8 *color;
        int si = (24 * 256) + 48;

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                uint8 pixel = gfxBuf_LQ[si] & PIXEL_MASK;
                color = bitmap.pal.color[pixel];
                g[targetI] = color[0];
                g[targetI + C1] = color[1];
                g[targetI + C2] = color[2];
                targetI++;
                si++;
            }

            si += (96);
        }

        swapBuffersAfterWrite();

        if (historyEnabled) {
            counter += 1 + numFramesToSkip;

            if (counter >= 15) {
                counter = 0;
                saveToHistory();
            }
        }

        if (soundEnabled) {
            sfxLock.Lock();
            int back = curSfx;
            int pos = sfxBufPos[back];
            int samples = snd.sample_count;
            int j = pos;
            short *sBuf = sfxBufs[back];
            short *l = snd.output[0];
            short *r = snd.output[1];

            for (int i = 0; (i < samples) && (j < SFX_BUF_SIZE - 2); i++) {
                sBuf[j] = l[i];
                sBuf[j + 1] = r[i];
                j += 2;
            }

            int curPos = sfxBufPos[back] + (samples * 2);

            if (curPos >= SFX_BUF_SIZE) {
                curPos = 0;
            }

            sfxBufPos[back] = curPos;
            sfxLock.Unlock();
        }
        return true;
    }


    /** 使用 OpenGL 渲染，更新 RGB 三通道分离纹理 */
    bool renderGL() {
        int stable = swapBuffersBeforeRead();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, (HEIGHT + PADDING) * 3, GL_ALPHA,
                        GL_UNSIGNED_BYTE, gfxBufs[stable]);
        return true;
    }

    /** 像素格式转换：GG 直接组合 RGB 通道分离存储的三平面数据 */
    unsigned int getPixel(const BUFFER_TYPE *buf, int idx) const {
        return 0xff000000 | (buf[idx + (160 * (144 + 26))] << 8) | (buf[idx + 160 * (144 + 26) * 2] << 16) | (buf[idx] << 0);
    }


    int cheatAddrs[100];   /**< 金手指地址数组（最多 100 个） */
    char cheatVals[100];   /**< 金手指值数组 */
    int numEnabledCheats;  /**< 已启用的金手指数量 */

    /**
     * 启用原始地址金手指。
     * 地址需减去 0xC000 偏移，并验证在有效范围内。
     * @param addr 内存地址
     * @param val 写入值
     * @param comp 比较值（未使用）
     * @return 成功返回 true
     */
    bool enableRawCheat(int addr, int val, int comp) {
        int i = numEnabledCheats;

        if (i == 100) {
            return false;
        }

        int finalAddr = addr - 0xC000;

        if (finalAddr < 0 || finalAddr >= 0x8000) {
            return false;
        }

        cheatAddrs[i] = finalAddr;
        cheatVals[i] = val;
        numEnabledCheats++;
        return true;
    }


    /** 启用金手指（字符串格式，GG 不支持） */
    bool enableCheat(const char *cheat, int type) {
        return false;
    }

    /** 禁用所有金手指 */
    bool disableAllCheats() {
        numEnabledCheats = 0;
        return true;
    }


    /** 渲染历史状态帧到 Bitmap */
    bool renderHistory(JNIEnv *env, jobject bitmap, int pos, int w, int h) {
        return this->render(env, bitmap, w, h, travel[posToIdx(pos)]);
    }

    /** 加载历史状态，并清空图形缓冲区 */
    bool doLoadHistoryState(int idx) {
        system_load_state_mem(historyItems[idx]);
        memset(gfxBuf_LQ, 0, 256 * 256 * 1);
        return true;
    }


    char historyItems[40][57815]; /**< 历史状态存档数据数组 */

    /** 保存历史状态，同时保存图形数据到回溯缓冲区 */
    bool doSaveHistoryState(int idx) {
        system_save_state_mem(historyItems[idx]);
        memcpy((void *) (travel[idx]), (void *) gfxBufs[workingGfx], (144 + PADDING) * 160 * 3);
        return true;
    }


    /** 保存游戏存档状态到文件 */
    bool saveState(const char *path, int slot) {
        FILE *fd;
        fd = fopen(path, "wb");

        if (fd) {
            system_save_state(fd);
            fclose(fd);
            return true;
        }
        return false;
    }


    /** 加载游戏存档状态，并重置音频和图形缓冲区 */
    bool doLoadState(const char *path, int slot) {
        FILE *fd = fopen(path, "rb");

        if (fd) {
            system_load_state(fd);
            fclose(fd);
            memset(gfxBuf_LQ, 0, 256 * 256);
            resetSfx();
            return true;
        }
        return false;
    }

    /** 重置模拟器（断电后重新上电） */
    bool reset() {
        system_poweroff();
        system_poweron();
        resetSfx();
        return true;
    }

    /** 停止模拟器（断电并关闭引擎） */
    bool stop() {
        system_poweroff();
        system_shutdown();
        started = false;
        return true;
    }

    /** 触发光枪（GG 不支持） */
    bool fireZapper(int x, int y) {
        return true;
    }

    /**
     * 保存电池存档到文件。
     * 将 SRAM 数据写入电池存档文件，大小为 0x8000 字节。
     * @param sram SRAM 数据指针
     */
    void saveBattery(uint8 *sram) {
        if (!saveSavFiles) {
            return;
        }

        if (sms.save) {
            FILE *fd = fopen(batterySavePath.c_str(), "wb");

            if (fd) {
                fwrite(sram, 0x8000, 1, fd);
                fclose(fd);
            }
        }
    }

    /**
     * 加载电池存档。
     * 从文件读取 SRAM 数据，文件不存在则清零。
     * @param sram SRAM 数据指针
     */
    void loadBattery(uint8 *sram) {
        if (!loadSavFiles) {
            return;
        }

        FILE *fd = fopen(batterySavePath.c_str(), "rb");

        if (fd) {
            sms.save = 1;
            fread(sram, 0x8000, 1, fd);
            fclose(fd);
        } else {
            memset(sram, 0x00, 0x8000);
        }
    }

    std::string batterySavePath; /**< 电池存档文件路径 */

    /**
     * 加载游戏 ROM。
     * 首次加载时初始化 sms_plus 引擎的 bitmap、音频、系统参数。
     * @param path ROM 文件路径
     * @param batterySaveDir 电池存档目录
     * @param batteryFullPath 电池存档完整路径
     * @return 加载成功返回 true
     */
    bool doLoadGame(const char *path, const char *batterySaveDir, const char *batteryFullPath) {
        batterySavePath = batteryFullPath;
        char *romPath = strdup(path);
        bool res = romPath != 0 && load_rom(romPath);
        free(romPath);

        if (!started) {
            memset(&bitmap, 0, sizeof(bitmap_t));
            bitmap.width = 256;
            bitmap.height = 256;
            bitmap.depth = (1) ? 8 : 16;
            bitmap.granularity = bitmap.depth / 8;
            bitmap.pitch = 256 * bitmap.granularity;
            bitmap.data = (unsigned char *) gfxBuf_LQ;
            bitmap.viewport.x = 0;
            bitmap.viewport.y = 0;
            bitmap.viewport.w = 256;
            bitmap.viewport.h = 192;
            snd.enabled = soundEnabled;
            snd.fm_which = SND_YM2413;
            snd.fps = (1) ? FPS_NTSC : FPS_PAL;
            snd.fm_clock = (1) ? CLOCK_NTSC : CLOCK_PAL;
            snd.psg_clock = (1) ? CLOCK_NTSC : CLOCK_PAL;
            snd.sample_rate = soundEnabled ? soundRate : 0;
            snd.mixer_callback = NULL;
            sms.territory = 0;
            sms.use_fm = 0;
            system_init();
            sms.territory = 0;
            sms.use_fm = 0;
            FILE *fd;
            system_poweron();
            started = true;
        }
        return res;
    }

    /** 设置调色板（GG 不需要） */
    void setPalette(int idx, int value) {
    }

    /** 重置音频缓冲区，并标记图形副本为脏 */
    void resetSfx() {
        sfxLock.Lock();
        sfxBufPos[0] = 0;
        sfxBufPos[1] = 0;
        curSfx = 0;
        sfxLock.Unlock();
        gfxLock.Lock();
        workingCopyDirty = true;
        gfxLock.Unlock();
    }

    /** 设置基础目录（GG 不需要） */
    bool setBaseDir(const char *path) {
        return true;
    }

    /** 获取音频缓冲区大小 */
    int getSfxBufferSize() {
        return SFX_BUF_SIZE;
    }

    /** 获取图形缓冲区大小（RGB 三通道分离） */
    int getGfxBufferSize() {
        return WIDTH * (HEIGHT + PADDING) * 3;
    }

};


/** 全局模拟器实例 */
SmsEmulator emulator;
/** 全局桥接实例 */
Bridge bridge(&emulator);


/**
 * SRAM 管理回调函数。
 * 由 sms_plus 引擎调用，根据模式执行保存或加载操作。
 * @param sram SRAM 数据指针
 * @param slot 存档槽位
 * @param mode 操作模式（SRAM_SAVE 或 SRAM_LOAD）
 */
void system_manage_sram(uint8 *sram, int slot, int mode) {
    if (mode == SRAM_SAVE) {
        emulator.saveBattery(sram);

    } else {
        emulator.loadBattery(sram);
    }
}

}


