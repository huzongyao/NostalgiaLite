/**
 * GBC 模拟器 C++ 实现。
 * <p>继承 Emulator 基类，封装 libgambatte 核心引擎。
 * 实现 GameBoy Color 平台特有的帧模拟、音频重采样、
 * 金手指、存档和历史状态管理功能。</p>
 */
#include "gambatte.h"
#include "resamplerinfo.h"
#include <GLES/gl.h>
#include <jni.h>
#include <list>
#include <fstream>
#include <sstream>
#include <cstring>

#include "Emulator.h"
#include "Bridge.h"

extern "C" {

using namespace std;
using namespace gambatte;
using namespace emudroid;

class Input : public InputGetter {
public:
    unsigned bits;

    unsigned operator()() {
        return bits;
    }
};

/** 屏幕宽度（像素） */
#define WIDTH 160
/** 屏幕高度（像素） */
#define HEIGHT 144
/** 音频缓冲区大小 */
#define SFX_BUF_SIZE 2048 * 8
/** 图形缓冲填充行数（用于着色器对齐） */
#define PADDING 26

/**
 * GameBoy Color 模拟器实现类。
 * <p>封装 libgambatte 的 GB 核心，管理图形/音频缓冲、
 * 金手指、存档和历史状态。</p>
 */
class GameBoyEmulator : public Emulator {
public:

    unsigned int *gfxBuf;
    short *sfxBuf;
    std::stringstream historyItems[40];

    /** 构造 GBC 模拟器，初始化 gambatte 引擎和缓冲区 */
    GameBoyEmulator() {
        ok = false;
        initBuffers();
        origWidth = WIDTH;
        origHeight = HEIGHT;
        historyEnabled = false;
        counter = 0;
        gfxBuf = new unsigned int[WIDTH * HEIGHT];
        resampler = ResamplerInfo::get(0).create(2097152, 22050, 35112 + 2064);
        sfxBuf = new short[SFX_BUF_SIZE];
        gb.setInputGetter(&input);
    }

    /** 获取音频缓冲区大小 */
    int getSfxBufferSize() {
        return SFX_BUF_SIZE;
    }

    /** 获取图形缓冲区大小 */
    int getGfxBufferSize() {
        return WIDTH * (HEIGHT + PADDING) * 3;
    }


    bool ok;

    /**
     * 启动模拟器。
     * <p>解析通用参数，设置电池存档和历史状态开关。</p>
     */
    bool start(int gfxInit, int sfxInit, int generalInit) {
        soundEnabled = sfxInit != -1;
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

        gb.loadSavFiles = loadSavFiles;
        gb.saveSavFiles = saveSavFiles;
        return true;
    }

    bool soundEnabled;
    bool loadSavFiles;
    bool saveSavFiles;
    int counter;

    /**
     * 执行一帧模拟。
     * <p>先执行跳帧，再渲染一帧。将 RGB 分离存储到三行 Alpha 纹理，
     * 定期保存历史状态，重采样音频并写入缓冲。</p>
     */
    bool emulate(int keys, int turbos, int numFramesToSkip) {
        input.bits = keys;
        unsigned int samples = 35112;

        for (int i = 0; i < numFramesToSkip; i++) {
            gb.runFor(NULL, WIDTH, (unsigned int *) tmpSfxBuf, samples);
        }

        gb.runFor(gfxBuf, WIDTH, (unsigned int *) tmpSfxBuf, samples);
        int wc = workingGfx;
        int C1 = 160 * (144 + PADDING);
        int C2 = 160 * (144 + PADDING) * 2;

        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            int pixel = gfxBuf[i];
            unsigned char r = (pixel & 0x00ff0000) >> 16;
            unsigned char g = (pixel & 0x0000ff00) >> 8;
            unsigned char b = (pixel & 0x000000ff) >> 0;
            gfxBufs[wc][i] = r;
            gfxBufs[wc][i + C1] = g;
            gfxBufs[wc][i + C2] = b;
        }

        if (historyEnabled) {
            counter += 1 + numFramesToSkip;

            if (counter >= 15) {
                counter = 0;
                saveToHistory();
            }
        }

        swapBuffersAfterWrite();

        if (soundEnabled) {
            int ssize = resampler->resample(resampledSfxBuf, (const short *) tmpSfxBuf, samples);
            ssize *= 2;
            sfxLock.Lock();
            int back = curSfx;
            int pos = sfxBufPos[back];

            for (int i = 0; i < ssize && i + pos < SFX_BUF_SIZE; i++) {
                sfxBufs[back][i + pos] = resampledSfxBuf[i];
            }

            int curPos = sfxBufPos[back] + ssize;

            if (curPos >= SFX_BUF_SIZE) {
                curPos = 0;
            }

            sfxBufPos[back] = curPos;
            sfxLock.Unlock();
        }

        return true;
    }

    std::string cheats;

    /** 加载游戏 ROM */
    bool doLoadGame(const char *path, const char *batterySaveDir, const char *strippedName) {
        int size = 0;
        cheats.clear();
        std::string s = std::string(path, strlen(path));
        gb.setSaveDir(batterySaveDir);
        bool failed = gb.load(s, 0);
        bool success = !failed;
        return success;
    }

    bool setBaseDir(const char *path) {
        return true;
    }

    /** 启用金手指（支持 GameGenie 和 GameShark） */
    bool enableCheat(const char *cheat, int type) {
        bool first = cheats.size() == 0;
        cheats += (first ? "" : ";") + string(cheat);

        if (type == 0) {
            gb.setGameGenie(cheats);
        }

        if (type == 1) {
            gb.setGameShark(cheats);
        }

        return true;
    }

    /** 保存状态到文件 */
    bool saveState(const char *state, int slot) {
        std::string filename = std::string(state, strlen(state));
        std::ofstream stream(filename.c_str(), std::ios_base::binary);
        gb.saveState(NULL, 0, stream);
        return true;
    }

    /** 渲染历史状态帧 */
    bool renderHistory(JNIEnv *env, jobject bitmap, int pos, int w, int h) {
        this->render(env, bitmap, w, h, travel[posToIdx(pos)]);
    }

    /** 从内存流加载历史状态 */
    bool doLoadHistoryState(int idx) {
        std::string str = historyItems[idx].str();
        std::stringstream stream;
        stream.str(str);
        stream.seekg(0);
        stream.clear();
        gb.loadState(stream);
        return true;
    }

    /** 从文件加载状态 */
    bool doLoadState(const char *state, int slot) {
        std::string filename = std::string(state, strlen(state));
        std::ifstream stream(filename.c_str(), std::ios_base::binary);
        gb.loadState(stream);
        gfxLock.Lock();
        workingCopyDirty = true;
        gfxLock.Unlock();
        return true;
    }

    /** 重置模拟器 */
    bool reset() {
        gb.reset();
        return true;
    }


    BUFFER_TYPE travel[40][160 * (144 + PADDING) * 3];

    /** 保存历史状态到内存流和旅行缓冲 */
    bool doSaveHistoryState(int idx) {
        std::stringstream *stream = &historyItems[idx];
        stream->clear();
        stream->str("");
        stream->seekp(0);
        gb.saveState(NULL, 0, stream[0]);
        memcpy((void *) (travel[idx]), (void *) gfxBufs[workingGfx], (144 + PADDING) * 160 * 3);
        return true;
    }

    /** 停止模拟器 */
    bool stop() {
        return true;
    }

    /** OpenGL 纹理渲染 */
    bool renderGL() {
        int stable = swapBuffersBeforeRead();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 160, (144 + PADDING) * 3, GL_ALPHA,
                        GL_UNSIGNED_BYTE, gfxBufs[stable]);
        return true;
    }

    /** 像素格式转换：GBC 直接组合 RGB 通道分离存储的三平面数据 */
    unsigned int getPixel(const BUFFER_TYPE *buf, int idx) const {
        return 0xff000000 | (buf[idx + 23040 + 3200] << 8) | (buf[idx + 46080 + 6400] << 16) | (buf[idx] << 0);
    }

private:
    Resampler *resampler;
    GB gb;
    Input input;
    gambatte::uint_least32_t tmpSfxBuf[35112 + 2064];
    short resampledSfxBuf[SFX_BUF_SIZE];
};

/** 全局 GBC 模拟器实例 */
GameBoyEmulator emulator;
/** 全局桥接对象 */
Bridge bridge(&emulator);

}

