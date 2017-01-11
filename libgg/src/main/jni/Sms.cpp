

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <string>
#include <iosfwd>
#include <GLES/gl.h>
#include <GLES/glext.h>
#include <stdio.h>
#include <android/log.h>
#include <pthread.h>


#include "Emulator.h"
#include "Bridge.h"

#include "settings.h"

extern "C" {
#include "sms_plus/shared.h"
#include "sms_plus/system.h"
#include "sms_plus/loadrom.h"


    using namespace std;
    using namespace emudroid;

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE,"NOSTALGIA.SMS", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  ,"NOSTALGIA.SMS", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   ,"NOSTALGIA.SMS", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   ,"NOSTALGIA.SMS", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  ,"NOSTALGIA.SMS", __VA_ARGS__)



#include <pthread.h>

#define WIDTH 160
#define HEIGHT 144
#define PADDING 26
#define SFX_BUF_SIZE 2048 * 8


    class SmsEmulator: public Emulator {
    private:


    public:


        uint8 *gfxBuf_LQ;


        short *sfxBuf;
        bool started;
        SmsEmulator() {
            batterySavePath = NULL;
            started =false;
            numEnabledCheats = 0;
            origHeight = HEIGHT;
            origWidth = WIDTH;
            lastPath = (char*) malloc(1);
            gfxBuf_LQ = new uint8[256 * 256];
            sfxBuf = new short[SFX_BUF_SIZE];

            for (int i = 0; i < 40; i++) {
                travel[i] = new BUFFER_TYPE[160 * (144 + PADDING) * 3];
            }

            initBuffers();
            resetSfx();
        }



        bool soundEnabled;
        int soundRate;

        bool loadSavFiles;
        bool saveSavFiles;
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

        BUFFER_TYPE *travel[40];
        static const int Emulator::HIS_SIZE = 40;
        int counter;
        bool emulate(int keys, int turbos, int numFramesToSkip) {
            input.system = 0;

            if (keys & 0x80) {
                input.system = 0x01;
                keys &= ~0x80;
            }

            input.pad[0] = keys ;

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
            uint8* color;
            int si = (24 * 256) + 48;

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    uint8 pixel = gfxBuf_LQ[si] & PIXEL_MASK;
                    color = bitmap.pal.color[pixel];
                    g[targetI] = color[0];
                    g[targetI+C1] = color[1];
                    g[targetI+C2] = color[2];
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

            CThreadLock lock = sfxLock;

            if (soundEnabled) {
                lock.Lock();
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
                    j+=2;
                }

                int curPos = sfxBufPos[back] + (samples * 2);

                if (curPos >= SFX_BUF_SIZE) {
                    curPos = 0;
                }

                sfxBufPos[back] = curPos;
                lock.Unlock();
            }

            return true;
        }


        bool renderGL() {
            int stable = swapBuffersBeforeRead();
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, (HEIGHT + PADDING) * 3, GL_ALPHA,
                            GL_UNSIGNED_BYTE, gfxBufs[stable]);
            return true;
        }


        int cheatAddrs[100];
        char cheatVals[100];
        int numEnabledCheats;
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



        bool enableCheat(const char *cheat, int type) {
            return false;
        }

        bool disableAllCheats() {
            numEnabledCheats = 0;
            return true;
        }


        bool renderHistory(JNIEnv *env, jobject bitmap, int pos, int w, int h) {
            return this->render(env, bitmap, w, h, travel[posToIdx(pos)]);
        }

        bool doLoadHistoryState(int idx) {
            system_load_state_mem(historyItems[idx]);
            memset(gfxBuf_LQ, 0, 256*256*1);
            return true;
        }


        char historyItems[40][57815];

        bool doSaveHistoryState(int idx) {
            system_save_state_mem(historyItems[idx]);
            memcpy((void*) (travel[idx]), (void*) gfxBufs[workingGfx],
                   (144 + PADDING) * 160 * 3);
            return true;
        }



        bool saveState(const char* path, int slot) {
            FILE *fd;
            fd = fopen(path, "wb");

            if(fd)
            {
                system_save_state(fd);
                fclose(fd);
                return true;
            }

            return false;
        }




        bool doLoadState(const char* path, int slot) {
            FILE *fd = fopen(path, "rb");

            if(fd)
            {
                system_load_state(fd);
                fclose(fd);
                memset(gfxBuf_LQ, 0, 256*256);
                resetSfx();
                return true;
            }

            return false;
        }

        bool reset() {
            system_poweroff();
            system_poweron();
            resetSfx();
            return true;
        }

        bool stop() {
            system_poweroff();
            system_shutdown();
            started = false;
            return true;
        }

        bool fireZapper(int x, int y) {
            return true;
        }

        void saveBattery(uint8 *sram) {
            if (!saveSavFiles) {
                return;
            }

            if(sms.save)
            {
                FILE *fd = fopen(batterySavePath, "wb");

                if(fd)
                {
                    fwrite(sram, 0x8000, 1, fd);
                    fclose(fd);
                }
            }
        }

        void loadBattery(uint8 *sram) {
            if (!loadSavFiles) {
                return;
            }

            FILE *fd = fopen(batterySavePath, "rb");

            if(fd)
            {
                sms.save = 1;
                fread(sram, 0x8000, 1, fd);
                fclose(fd);
            }

            else
            {
                memset(sram, 0x00, 0x8000);
            }
        }

        char* batterySavePath;
        bool doLoadGame(const char *path, const char*batterySaveDir, const char*batteryFullPath) {
            if (batterySavePath) {
                free(batterySavePath);
            }

            batterySavePath = (char*)malloc(strlen(batteryFullPath) + 1);
            strcpy(batterySavePath, batteryFullPath);
            bool res = load_rom(strdup(path));

            if (!started) {
                memset(&bitmap, 0, sizeof(bitmap_t));
                bitmap.width  = 256;
                bitmap.height = 256;
                bitmap.depth  = (1) ? 8 : 16;
                bitmap.granularity = bitmap.depth / 8;
                bitmap.pitch  = 256 * bitmap.granularity;
                bitmap.data   =  (unsigned char*)gfxBuf_LQ;
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

        void setPalette(int idx, int value) {
        }

        void resetSfx() {
            CThreadLock lock = sfxLock;
            lock.Lock();
            sfxBufPos[0] = 0;
            sfxBufPos[1] = 0;
            curSfx = 0;
            lock.Unlock();
            CThreadLock lock2 = gfxLock;
            lock2.Lock();
            workingCopyDirty = true;
            lock2.Unlock();
        }

        bool setBaseDir(const char *path) {
            return true;
        }

        int getSfxBufferSize() {
            return SFX_BUF_SIZE;
        }

        int getGfxBufferSize() {
            return WIDTH * (HEIGHT + PADDING) * 3;
        }

    };


    SmsEmulator emulator;
    Bridge bridge(&emulator);


    void system_manage_sram(uint8 *sram, int slot, int mode)
    {
        if (mode == SRAM_SAVE) {
            emulator.saveBattery(sram);

        } else {
            emulator.loadBattery(sram);
        }
    }





}



