package nostalgia.framework;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import nostalgia.framework.ui.gamegallery.GameDescription;

public interface Emulator {

    EmulatorInfo getInfo();

    void start(GfxProfile cfg, SfxProfile sfx, EmulatorSettings settings);

    GfxProfile getActiveGfxProfile();

    SfxProfile getActiveSfxProfile();

    void reset();

    void saveState(int slot);

    void loadState(int slot);

    void loadHistoryState(int pos);

    int getHistoryItemCount();

    void renderHistoryScreenshot(Bitmap bmp, int pos);

    void setBaseDir(String baseDir);

    void loadGame(String fileName, String batterySaveDir, String batterySaveFullPath);

    void onEmulationResumed();

    void onEmulationPaused();

    void enableCheat(String gg);

    void enableRawCheat(int addr, int val, int comp);

    boolean isGameLoaded();

    GameInfo getLoadedGame();

    void setKeyPressed(int port, int key, boolean isPressed);

    void setTurboEnabled(int port, int key, boolean isEnabled);

    void setViewPortSize(int w, int h);

    void resetKeys();

    void fireZapper(float x, float y);

    void setFastForwardEnabled(boolean enabled);

    void setFastForwardFrameCount(int frames);

    void emulateFrame(int numFramesToSkip);

    void readSfxData();

    void renderSfx();

    void readPalette(int[] palette);

    void renderGfx();

    void renderGfxGL();

    void draw(Canvas canvas, int x, int y);

    void stop();

    boolean isReady();

    GfxProfile autoDetectGfx(GameDescription game);

    SfxProfile autoDetectSfx(GameDescription game);

}
