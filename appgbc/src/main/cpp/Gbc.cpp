#include "gambatte.h"
#include "resamplerinfo.h"
#include <GLES/gl.h>
#include <jni.h>
#include <list>
#include <fstream>
#include <sstream>

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

#define WIDTH 160
#define HEIGHT 144
#define SFX_BUF_SIZE 2048 * 8
#define PADDING 26

class GameBoyEmulator : public Emulator {
public:

    unsigned int *gfxBuf;
    short *sfxBuf;
    std::stringstream historyItems[40];

    GameBoyEmulator() {
        lastPath = (char *) malloc(1);
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

    int getSfxBufferSize() {
        return SFX_BUF_SIZE;
    }

    int getGfxBufferSize() {
        return WIDTH * (HEIGHT + PADDING) * 3;
    }


    bool ok;

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

    bool saveState(const char *state, int slot) {
        std::string filename = std::string(state, strlen(state));
        std::ofstream stream(filename.c_str(), std::ios_base::binary);
        gb.saveState(NULL, 0, stream);
        return true;
    }

    bool renderHistory(JNIEnv *env, jobject bitmap, int pos, int w, int h) {
        this->render(env, bitmap, w, h, travel[posToIdx(pos)]);
    }

    bool doLoadHistoryState(int idx) {
        std::string str = historyItems[idx].str();
        std::stringstream stream;
        stream.str(str);
        stream.seekg(0);
        stream.clear();
        gb.loadState(stream);
        return true;
    }

    bool doLoadState(const char *state, int slot) {
        std::string filename = std::string(state, strlen(state));
        std::ifstream stream(filename.c_str(), std::ios_base::binary);
        gb.loadState(stream);
        gfxLock.Lock();
        workingCopyDirty = true;
        gfxLock.Unlock();
        return true;
    }

    bool reset() {
        gb.reset();
        return true;
    }


    BUFFER_TYPE travel[40][160 * (144 + PADDING) * 3];

    bool doSaveHistoryState(int idx) {
        std::stringstream *stream = &historyItems[idx];
        stream->clear();
        stream->str("");
        stream->seekp(0);
        gb.saveState(NULL, 0, stream[0]);
        memcpy((void *) (travel[idx]), (void *) gfxBufs[workingGfx], (144 + PADDING) * 160 * 3);
        return true;
    }

    bool stop() {
        return true;
    }

    bool renderGL() {
        int stable = swapBuffersBeforeRead();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 160, (144 + PADDING) * 3, GL_ALPHA,
                        GL_UNSIGNED_BYTE, gfxBufs[stable]);
        return true;
    }

private:
    Resampler *resampler;
    GB gb;
    Input input;
    gambatte::uint_least32_t tmpSfxBuf[35112 + 2064];
    short resampledSfxBuf[SFX_BUF_SIZE];
};

GameBoyEmulator emulator;
Bridge bridge(&emulator);

}

