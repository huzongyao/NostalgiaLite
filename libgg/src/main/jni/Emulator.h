
#ifndef EMULATOR_H_
#define EMULATOR_H_

#include "jni.h"
#include "settings.h"
#include <pthread.h>




namespace emudroid {

class CThreadLock
{
public:
    CThreadLock();
    virtual ~CThreadLock();

    void Lock();
    void Unlock();
private:
    pthread_mutex_t mutexlock;
};



class Emulator {

public:
    Emulator();
    virtual bool start(int gfx, int sfx, int general) = 0;
    bool emulateFrame(int keys, int turbos, int numFramesToSkip);
    virtual bool setBaseDir(const char *path) = 0;
    bool loadGame(const char *path, const char*batterySaveDir,const char*strippedName);
    bool loadState(const char *path, int slot);



    virtual bool saveState(const char *path, int slot) = 0;

    virtual bool doLoadGame(const char*path, const char*batterySaveDir, const char*strippedName) = 0;
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

    JavaVM *jvm;
protected:

    static const int HIS_SIZE = 40;
    virtual bool emulate(int keys, int turbos, int numFramesToSkip) = 0;
    CThreadLock sfxLock;
    CThreadLock gfxLock;
    char * lastPath;

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
