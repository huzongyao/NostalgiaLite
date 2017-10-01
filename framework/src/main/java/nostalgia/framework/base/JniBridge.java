package nostalgia.framework.base;

import android.graphics.Bitmap;

public class JniBridge {

    public native boolean setBaseDir(String path);

    public native boolean start(int gfx, int sfx, int general);

    public native boolean reset();

    public native boolean loadGame(String fileName, String batteryDir, String strippedName);

    public native boolean loadState(String fileName, int slot);

    public native boolean saveState(String fileName, int slot);

    public native int readSfxBuffer(short[] data);

    public native boolean enableCheat(String gg, int type);

    public native boolean enableRawCheat(int addr, int val, int comp);

    public native boolean fireZapper(int x, int y);

    public native boolean render(Bitmap bitmap);

    public native boolean renderVP(Bitmap bitmap, int vw, int vh);

    public native boolean renderHistory(Bitmap bitmap, int item, int vw, int vh);

    public native boolean renderGL();

    public native boolean emulate(int keys, int turbos, int numFramesToSkip);

    public native boolean readPalette(int[] result);

    public native boolean setViewPortSize(int w, int h);

    public native boolean stop();

    public native int getHistoryItemCount();

    public native boolean loadHistoryState(int pos);

}
