#include <jni.h>
#include <android/bitmap.h>
#include "fceux/driver.h"
#include "fceux/state.h"
#include "fceux/fceu.h"
#include "Emulator.h"
#include "Bridge.h"
#include "fceux/android_log.h"

extern "C" {

using namespace std;
using namespace emudroid;

#define WIDTH 256
#define HEIGHT_PAL 240
#define HEIGHT_NTSC 224

#define SFX_BUF_SIZE 2048 * 8 * 2

class NesEmulator : public Emulator {

private:
    bool zapperEnabled;
    BUFFER_TYPE travel[40][256 * 256];
    bool stereo;

public:
    NesEmulator() {
        LOGE("NesEmulator Constructor");
        pads = 0;
        inited = false;
        gfxBuf = NULL;
        strcpy(lastPath, "");
        initBuffers();
        resetSfx();
    }


    bool setBaseDir(const char *path) {
        FCEUI_SetBaseDirectory(path);
        return true;
    }

    bool setBatterySaveDir(const char *path) {
        NOSTALGIA_SetBatterySaveDir(path);
        return true;
    }

    bool saveSavFiles;
    bool loadSavFiles;

    bool start(int gfxInit, int sfxInit, int generalInit) {
        if (inited) {
            return true;
        }

        bool success = FCEUI_Initialize();
        disableAllCheats();

        if (success) {
            saveSavFiles = generalInit >= 1000;

            if (generalInit >= 1000) {
                generalInit -= 1000;
            }

            disableBatterySaving = !saveSavFiles;
            loadSavFiles = generalInit >= 100;

            if (generalInit >= 100) {
                generalInit -= 100;
            }

            setHistoryEnabled(generalInit >= 10);

            if (generalInit >= 10) {
                generalInit -= 10;
            }

            zapperPressed = false;
            zapperEnabled = generalInit == 1;
            FCEUI_SetSoundVolume(100);
            FCEUI_SetLowPass(1);
            FCEUI_SetSoundQuality(0);

            if (sfxInit == -1) {
                soundEnabled = false;

            } else {
                soundEnabled = true;
                int quality = sfxInit / 100;
                sfxInit = sfxInit - quality * 100;
                stereo = quality > 0;
                FCEUI_Sound(sfxInit * 11025);
            }

            counter = 0;
            isPal = gfxInit == 1;
            fps = isPal ? 50 : 60;
            zapper[0] = 0;
            zapper[1] = 0;
            zapper[2] = 0;
            resetSfx();
            origWidth = WIDTH;
            origHeight = isPal ? HEIGHT_PAL : HEIGHT_NTSC;
            offsetIdx = isPal ? 0 : (8 * WIDTH);
        }

        inited = success;
        return success;
    }

    int cur;
    int fps;
    bool isPal;
    int counter;
    FCEUGI *game;
    bool inited;
    int32 *tmpSfxBuf;
    char *gfxBuf;
    int pads;
    int zapper[3];
    int zapperPressed;
    bool soundEnabled;

    bool emulate(int keys, int turbos, int numFramesToSkip) {
        bool canSaveHistory = true;

        if (numFramesToSkip == -1) {
            numFramesToSkip = 10;
            canSaveHistory = false;
        }

        if (zapperPressed > 0) {
            zapperPressed--;

            if (zapperPressed == 0) {
                zapper[2] = 0;
            }
        }

        pads = keys;
        int ssize;

        for (int i = 0; i < numFramesToSkip; i++) {
            FCEUI_Emulate((uint8 **) &gfxBuf, &tmpSfxBuf, &ssize, 1);
            appendToSfxBuffer(tmpSfxBuf, ssize);
        }

        FCEUI_Emulate((uint8 **) &gfxBuf, &tmpSfxBuf, &ssize, 0);
        memcpy((void *) gfxBufs[workingGfx], (void *) gfxBuf, 256 * 240);
        swapBuffersAfterWrite();

        if (historyEnabled && canSaveHistory) {
            counter += 1 + numFramesToSkip;

            if (counter >= fps / 4) {
                counter = 0;
                saveToHistory();
            }
        }

        appendToSfxBuffer(tmpSfxBuf, ssize);
        return true;
    }

    void appendToSfxBuffer(int32 *data, int numStereoSamples) {
        if (!soundEnabled) {
            return;
        }

        int numShorts = numStereoSamples << 1;
        int numInts = numStereoSamples;
        int numBytes = numInts << 2;
        sfxLock.Lock();
        int back = curSfx;
        int slen = sfxBufPos[back];

        if (slen + numShorts >= SFX_BUF_SIZE) {
            slen = 0;
        }

        sfxBufPos[back] = slen + numShorts;
        short *buf = sfxBufs[back];

        if (stereo) {
            int j = 0;
            int pos = sfxBufPos[back];

            for (int i = slen; i < pos; i += 2) {
                int d = data[j];
                int b0 = (d & (0x000000ff)) >> 0;
                int b1 = (d & (0x0000ff00)) >> 8;
                short s0 = (b1 << 8) | b0;
                buf[i + 0] = s0;
                buf[i + 1] = s0;
                j++;
            }

        } else {
            memcpy((void *) buf + (slen << 1), (void *) data, numBytes);
        }

        sfxLock.Unlock();
    }

    bool addCheat(const char *name, int ggAddr, int ggVal, int ggComp) {
        uint32 cAddr;
        uint8 cVal;
        int cCompare, cType;
        int i = 0;

        while (FCEUI_GetCheat(i, NULL, &cAddr, &cVal, &cCompare, NULL, &cType)) {
            if ((ggAddr == cAddr) && (ggVal == cVal) && (ggComp == cCompare)
                && (cType == 1)) {
                return true;
            }
            i++;
        }
        return FCEUI_AddCheat(name, ggAddr, ggVal, ggComp, 1);
    }

    bool enableRawCheat(int ggAddr, int ggVal, int ggComp) {
        return addCheat("whatever", ggAddr, ggVal, ggComp);
    }

    bool enableCheat(const char *cheat, int type) {
        int ggAddr;
        int ggVal;
        int ggComp;

        if (!FCEUI_DecodeGG(cheat, &ggAddr, &ggVal, &ggComp)) {
            return false;
        }

        char c = 0;
        return addCheat(cheat, ggAddr, ggVal, ggComp);
    }

    bool disableAllCheats() {
        int i = 0;
        uint32 cAddr;
        uint8 cVal;
        char *cName;
        int cCompare, cType;

        while (FCEUI_GetCheat(i, &cName, &cAddr, &cVal, &cCompare, NULL, &cType)) {
            FCEUI_DelCheat(i);
            i++;
        }

        return true;
    }


    bool renderHistory(JNIEnv *env, jobject bitmap, int pos, int w, int h) {
        this->render(env, bitmap, w, h, travel[posToIdx(pos)]);
    }


    bool doLoadHistoryState(int idx) {
        bool res = FCEUSS_LoadFP(&(ms[idx]), SSLOADPARAM_NOBACKUP);
        return res;
    }


    bool doSaveHistoryState(int idx) {
        ms[idx].truncate(0);
        bool res = FCEUSS_SaveMS(&(ms[idx]), 0);
        memcpy((void *) (travel[idx]), (void *) gfxBuf, 256 * 256);
        return true;
    }


    EMUFILE_MEMORY ms[HIS_SIZE];

    bool saveState(const char *path, int slot) {
        FCEUI_SaveState(path);
        resetSfx();
        return true;
    }

    bool doLoadState(const char *path, int slot) {
        FCEUI_LoadState(path);
        resetSfx();
        return true;
    }

    bool reset() {
        FCEUI_PowerNES();

        if (loadSavFiles) {
            doLoadGame(lastPath, 0, 0);

        } else {
            GameInterface(GI_RESETSAVE);
            resetSfx();
        }

        return true;
    }

    bool stop() {
        if (game != NULL) {
            FCEUI_CloseGame();
            game = NULL;
        }

        FCEUI_Kill();
        inited = false;
    }

    bool fireZapper(int x, int y) {
        zapper[0] = x;
        zapper[1] = y;
        zapper[2] = 0;

        if (x == -1 && y == -1) {
            zapper[2] = 2;

        } else {
            zapper[2] = 1;
        }

        zapperPressed = 7;
        return true;
    }


    bool doLoadGame(const char *path, const char *batterySaveDir, const char *strippedName) {
        if (game != NULL) {
            FCEUI_CloseGame();
        }

        if (batterySaveDir != 0) {
            NOSTALGIA_SetBatterySaveDir(batterySaveDir);
        }

        game = FCEUI_LoadGame(path, 0);
        FCEUI_SetVidSystem(isPal ? 1 : 0);

        if (game != NULL) {
            FCEUI_SetInputFourscore(true);
            FCEUI_SetInput(0, SI_GAMEPAD, &pads, 0);

            if (zapperEnabled) {
                FCEUI_SetInput(1, SI_ZAPPER, &zapper, 1);

            } else {
                FCEUI_SetInput(1, SI_GAMEPAD, &pads, 0);
            }

            resetSfx();
        }

        return game != NULL;
    }

    void setPalette(int idx, int value) {
        emuPalette[idx] = value;
    }

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

    int getSfxBufferSize() {
        return SFX_BUF_SIZE;
    }

    int getGfxBufferSize() {
        return 256 * 256;
    }

};

NesEmulator emulator;
Bridge bridge(&emulator);

}


bool turbo = 0;
int closeFinishedMovie = 0;

unsigned long FCEUD_GetTime(void) {
    return 0;
}

unsigned long FCEUD_GetTimeFreq(void) {
    return 0;
}

bool FCEUD_ShouldDrawInputAids() {
    return false;
}

unsigned int *GetKeyboard(void) {
    return 0;
}

FILE *FCEUD_UTF8fopen(const char *fn, const char *mode) {
    FILE *f = fopen(fn, mode);
    return f;
}

EMUFILE_FILE *FCEUD_UTF8_fstream(const char *n, const char *m) {
    EMUFILE_FILE *f = new EMUFILE_FILE(n, m);
    if (!f->is_open()) {
        delete f;
        return NULL;
    } else {
        return f;
    }
}

FCEUFILE *FCEUD_OpenArchiveIndex(ArchiveScanRecord &asr, std::string &fname, int innerIndex) {
    return NULL;
}

FCEUFILE *FCEUD_OpenArchive(ArchiveScanRecord &asr, std::string &fname,
                            std::string *innerFilename) {
    return NULL;
}

ArchiveScanRecord FCEUD_ScanArchive(std::string fname) {
    return ArchiveScanRecord();
}


const char *FCEUD_GetCompilerString() {
    return NULL;
}


void FCEUD_SetPalette(uint8 index, uint8 r, uint8 g, uint8 b) {
    uint32 res = ((uint32) 0xFF000000) | (b << 16) | (g << 8) | (r << 0);
    emulator.setPalette(index, res);
}

void FCEUD_GetPalette(uint8 i, uint8 *r, uint8 *g, uint8 *b) {
}


void FCEUD_PrintError(const char *s) {
    LOGE(s);
}

void FCEUD_Message(const char *s) {
    LOGI(s);
}


void FCEUD_SoundToggle(void) {
}

void FCEUD_SoundVolumeAdjust(int) {
}

void FCEUI_UseInputPreset(int preset) {
}

void FCEUD_AviRecordTo(void) {
}

void FCEUD_AviStop(void) {
}

int FCEUI_AviBegin(const char *fname) {
    return 1;
}

void FCEUI_AviEnd(void) {
}

void FCEUI_AviVideoUpdate(const unsigned char *buffer) {
}

void FCEUI_AviSoundUpdate(void *soundData, int soundLen) {
}

bool FCEUI_AviIsRecording() {
    return false;
}

bool FCEUI_AviEnableHUDrecording() {
    return false;
}

void FCEUI_SetAviEnableHUDrecording(bool enable) {
}

bool FCEUI_AviDisableMovieMessages() {
    return true;
}

void FCEUI_SetAviDisableMovieMessages(bool disable) {
}


int FCEUD_SendData(void *data, uint32 len) {
    return 1;
}

int FCEUD_RecvData(void *data, uint32 len) {
    return 1;
}


void FCEUD_NetplayText(uint8 *text) {
}


void FCEUD_NetworkClose(void) {
}

void FCEUD_SaveStateAs(void) {
}

void FCEUD_LoadStateFrom(void) {
}


void FCEUD_SetInput(bool fourscore, bool microphone, ESI port0, ESI port1, ESIFC fcexp) {
}

void FCEUD_MovieRecordTo(void) {
}

void FCEUD_MovieReplayFrom(void) {
}

void FCEUD_LuaRunFrom(void) {
}

void FCEUD_SetEmulationSpeed(int cmd) {
}

void FCEUD_TurboOn(void) {
}

void FCEUD_TurboOff(void) {
}

void FCEUD_TurboToggle(void) {
}

int FCEUD_ShowStatusIcon(void) {
    return 0;
}

void FCEUD_ToggleStatusIcon(void) {
}

void FCEUD_HideMenuToggle(void) {
}


void FCEUD_CmdOpen(void) {
}


void FCEUD_DebugBreakpoint(int bp_num) {
}


void FCEUD_TraceInstruction(uint8 *opcode, int size) {
}


void FCEUD_UpdateNTView(int scanline, bool drawall) {
}

void FCEUD_UpdatePPUView(int scanline, int drawall) {
}


bool FCEUD_PauseAfterPlayback() {
    return false;
}


void FCEUD_VideoChanged() {
}

void GetMouseData(uint32 (&md)[3]) {
}

void RefreshThrottleFPS() {
}

