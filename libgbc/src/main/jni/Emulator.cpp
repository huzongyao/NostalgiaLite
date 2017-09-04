
#include "Emulator.h"
#include <android/bitmap.h>
#include <stdio.h>
#include <GLES/gl.h>

using namespace emudroid;

JavaVM *jvm;
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

bool Emulator::enableCheat(const char *cheat, int type) {
    return false;
}


bool Emulator::enableRawCheat(int addr, int val, int comp) {
    return false;
}

bool Emulator::fireZapper(int x, int y) {
    return false;
}

bool Emulator::readPalette(JNIEnv *env, jintArray result) {
    env->SetIntArrayRegion(result, 0, 256, (const int*) emuPalette);
    return true;
}

int turboCounter = 0;
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

bool Emulator::loadState(const char*path, int slot) {
    if (slot != 0) {
        historyIndex = -1;
        historySize = 0;
    }

    return doLoadState(path, slot);
}

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

bool Emulator::setHistoryEnabled(bool enabled) {
    if (enabled && !historyEnabled) {
        historyIndex = -1;
        historySize = 0;
    }

    historyEnabled = enabled;
    return true;
}

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

int Emulator::getHistoryItemCount() {
    if (!historyEnabled) {
        return 0;
    }

    return historySize;
}

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

int Emulator::posToIdx(int delta) {
    int curPos = historyIndex;
    curPos -= delta;

    if (curPos < 0) {
        curPos = HIS_SIZE - (-curPos);
    }

    return curPos;
}

bool Emulator::renderGL() {
    int stable = swapBuffersBeforeRead();
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, origWidth, origHeight, GL_ALPHA,
                    GL_UNSIGNED_BYTE, gfxBufs[stable] + offsetIdx);
    return true;
}

CThreadLock::CThreadLock() {
    pthread_mutex_init(&mutexlock, 0);
}

CThreadLock::~CThreadLock() {
    pthread_mutex_destroy(&mutexlock);
}
void CThreadLock::Lock() {
    pthread_mutex_lock(&mutexlock);
}
void CThreadLock::Unlock() {
    pthread_mutex_unlock(&mutexlock);
}

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

Emulator::~Emulator() {
}

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

void Emulator::swapBuffersAfterWrite() {
    CThreadLock lock = gfxLock;
    lock.Lock();
    int temp = workingGfx;
    workingGfx = workingGfx_copy;
    workingGfx_copy = temp;
    workingCopyDirty = true;
    lock.Unlock();
}

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
