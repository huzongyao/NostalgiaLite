package nostalgia.framework.base;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorException;
import nostalgia.framework.EmulatorInfo;
import nostalgia.framework.EmulatorSettings;
import nostalgia.framework.GameInfo;
import nostalgia.framework.GfxProfile;
import nostalgia.framework.KeyboardProfile;
import nostalgia.framework.R;
import nostalgia.framework.SfxProfile;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.EmuUtils;

public abstract class JniEmulator implements Emulator {
    private static final String TAG = "JniEmulator";
    private static final int SIZE = 32768 * 2;
    private static Map<String, String> md5s = new HashMap<>();
    private final Object readyLock = new Object();
    private final Object loadLock = new Object();
    private final short[][] testX = new short[2][SIZE];
    private final Object sfxLock = new Object();
    private final Object viewPortLock = new Object();
    private AtomicBoolean ready = new AtomicBoolean();
    private AtomicInteger totalWritten = new AtomicInteger();
    private String baseDir;
    private boolean loadFailed = false;
    private int cur = 0;
    private int[] lenX = new int[2];
    private boolean fastForward;
    private int numFastForwardFrames;
    private int minSize;
    private boolean useOpenGL;
    private GameInfo gameInfo;
    private Bitmap bitmap;
    private SfxProfile sfx;
    private GfxProfile gfx;
    private AudioTrack track;
    private short[] sfxBuffer;
    private JniBridge jni;
    private int keys;
    private int turbos = ~0;
    private int viewPortWidth;
    private int viewPortHeight;

    public JniEmulator() {
        EmulatorInfo info = getInfo();
        KeyboardProfile.BUTTON_NAMES = info.getDeviceKeyboardNames();
        KeyboardProfile.BUTTON_KEY_EVENT_CODES = info.getDeviceKeyboardCodes();
        KeyboardProfile.BUTTON_DESCRIPTIONS = info.getDeviceKeyboardDescriptions();
        this.jni = getBridge();
    }

    public abstract JniBridge getBridge();

    @Override
    public abstract GfxProfile autoDetectGfx(GameDescription game);

    @Override
    public abstract SfxProfile autoDetectSfx(GameDescription game);

    @Override
    public int getHistoryItemCount() {
        return jni.getHistoryItemCount();
    }

    @Override
    public void setFastForwardFrameCount(int frames) {
        numFastForwardFrames = frames;
    }

    @Override
    public void loadHistoryState(int pos) {
        if (!jni.loadHistoryState(pos)) {
            throw new EmulatorException("load history state failed");
        }
    }

    @Override
    public void renderHistoryScreenshot(Bitmap bmp, int pos) {
        if (!jni.renderHistory(bmp, pos, bmp.getWidth(), bmp.getHeight())) {
            throw new EmulatorException("render history failed");
        }
    }

    @Override
    public boolean isGameLoaded() {
        synchronized (loadLock) {
            return gameInfo != null;
        }
    }

    @Override
    public GameInfo getLoadedGame() {
        synchronized (loadLock) {
            return gameInfo;
        }
    }

    @Override
    public void start(GfxProfile gfx, SfxProfile sfx, EmulatorSettings settings) {
        synchronized (readyLock) {
            ready.set(false);
            setFastForwardEnabled(false);
            if (sfx != null) {
                sfxBuffer = new short[sfx.bufferSize];
                initSound(sfx);
            }
            this.sfx = sfx;
            this.gfx = gfx;
            if (!jni.start(gfx.toInt(), sfx == null ? -1 : sfx.toInt(), settings.toInt())) {
                throw new EmulatorException("init failed");
            }
            synchronized (loadLock) {
                gameInfo = null;
            }
            ready.set(true);
        }
    }

    @Override
    public GfxProfile getActiveGfxProfile() {
        return gfx;
    }

    @Override
    public SfxProfile getActiveSfxProfile() {
        return sfx;
    }

    @Override
    public void reset() {
        synchronized (readyLock) {
            ready.set(false);
            if (track != null) {
                track.flush();
            }
            synchronized (testX) {
                lenX[0] = 0;
                lenX[1] = 0;
            }
            if (!jni.reset()) {
                throw new EmulatorException("reset failed");
            }
            ready.set(true);
        }
    }

    public void setBaseDir(String path) {
        this.baseDir = path;
        if (!jni.setBaseDir(path)) {
            throw new EmulatorException("could not set base dir");
        }
    }

    @Override
    public void saveState(int slot) {
        String fileName = SlotUtils.getSlotPath(baseDir, getMD5(null), slot);
        Bitmap screen = null;
        try {
            screen = Bitmap.createBitmap(gfx.originalScreenWidth, gfx.originalScreenHeight, Config.ARGB_8888);
        } catch (OutOfMemoryError ignored) {
        }
        if (screen != null) {
            if (!jni.renderVP(screen, gfx.originalScreenWidth, gfx.originalScreenHeight)) {
                throw new EmulatorException(R.string.act_game_screenshot_failed);
            }
        }
        if (!jni.saveState(fileName, slot)) {
            throw new EmulatorException(R.string.act_emulator_save_state_failed);
        }
        if (screen != null) {
            String pngFileName = SlotUtils.getScreenshotPath(baseDir, getMD5(null), slot);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(pngFileName);
                screen.compress(CompressFormat.PNG, 60, out);
            } catch (Exception e) {
                throw new EmulatorException(R.string.act_game_screenshot_failed);

            } finally {
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            File file = new File(pngFileName);
            NLog.i(TAG, "SCREEN: " + file.length());
            screen.recycle();
        } else {
            throw new EmulatorException(R.string.act_game_screenshot_failed);
        }
    }

    @Override
    public void loadState(int slot) {
        String fileName = SlotUtils.getSlotPath(baseDir, getMD5(null), slot);
        if (!new File(fileName).exists()) {
            return;
        }
        if (!jni.loadState(fileName, slot)) {
            throw new EmulatorException(R.string.act_emulator_load_state_failed);
        }
    }

    @Override
    public void loadGame(String fileName, String batteryDir, String batterySaveFullPath) {
        if (!jni.loadGame(fileName, batteryDir, batterySaveFullPath)) {
            synchronized (loadLock) {
                loadFailed = true;
                loadLock.notifyAll();
            }
            throw new EmulatorException(R.string.act_emulator_load_game_failed);
        }
        GameInfo gi = new GameInfo();
        gi.path = fileName;
        gi.md5 = getMD5(fileName);
        synchronized (loadLock) {
            loadFailed = false;
            gameInfo = gi;
            loadLock.notifyAll();
        }
    }

    @Override
    public void setKeyPressed(int port, int key, boolean isPressed) {
        int n = port * 8;
        if (key >= 1000) {
            key -= 1000;
            setTurboEnabled(port, key, isPressed);
        }
        if (isPressed) {
            keys |= (key << n);
        } else {
            keys &= ~(key << n);
        }
    }

    public void setTurboEnabled(int port, int key, boolean isEnabled) {
        int n = port * 8;
        int t = ~turbos;
        if (isEnabled) {
            t |= (key << n);
        } else {
            t &= ~(key << n);
        }
        turbos = ~t;
    }

    public void readPalette(int[] result) {
        synchronized (loadLock) {
            if (gameInfo == null && !loadFailed) {
                try {
                    loadLock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        if (result == null) {
            throw new IllegalArgumentException();
        }

        if (gameInfo != null) {
            if (!jni.readPalette(result)) {
                throw new EmulatorException("error reading palette");
            }
        }
    }

    @Override
    public void setViewPortSize(int w, int h) {
        if (!jni.setViewPortSize(w, h)) {
            throw new EmulatorException("set view port size failed");
        }
        synchronized (viewPortLock) {
            viewPortWidth = w;
            viewPortHeight = h;
        }
    }

    public void stop() {
        synchronized (readyLock) {
            ready.set(false);

            if (bitmap != null) {
                bitmap.recycle();
                NLog.d(TAG, "bitmap recycled");
            }
            if (track != null) {
                track.flush();
                track.stop();
                track.release();
                track = null;
            }
            jni.stop();
            gameInfo = null;
            bitmap = null;
        }
    }

    public boolean isReady() {
        return ready.get();
    }

    @Override
    public void fireZapper(float x, float y) {
        int emuX;
        int emuY;
        if (x == -1 || y == -1) {
            emuX = -1;
            emuY = -1;
        } else {
            emuX = (int) (getActiveGfxProfile().originalScreenWidth * x);
            emuY = (int) (getActiveGfxProfile().originalScreenHeight * y);
        }
        if (!jni.fireZapper(emuX, emuY)) {
            throw new EmulatorException("firezapper failed");
        }
    }

    @Override
    public void resetKeys() {
        keys = 0;
    }

    @Override
    public void emulateFrame(int numFramesToSkip) {
        if (fastForward && numFramesToSkip > -1) {
            numFramesToSkip = numFastForwardFrames;
        }
        if (!jni.emulate(keys, turbos, numFramesToSkip)) {
            throw new EmulatorException("emulateframe failed");
        }
    }

    @Override
    public void renderGfx() {
        if (!jni.render(bitmap)) {
            createBitmap(viewPortWidth, viewPortHeight);
            if (!jni.render(bitmap)) {
                throw new EmulatorException("render failed");
            }
        }
    }

    @Override
    public void renderGfxGL() {
        if (!jni.renderGL()) {
            throw new EmulatorException("render failed");
        }
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        if (useOpenGL) {
            throw new IllegalStateException();
        }
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        canvas.drawBitmap(bitmap, x, y, null);
    }

    @Override
    public void enableCheat(String gg) {
        if (!jni.enableCheat(gg, 0)) {
            throw new EmulatorException(R.string.act_emulator_invalid_cheat, gg);
        }
    }

    @Override
    public void enableRawCheat(int addr, int val, int comp) {
        if (!jni.enableRawCheat(addr, val, comp)) {
            throw new EmulatorException(R.string.act_emulator_invalid_cheat, Integer.toHexString(addr)
                    + ":" + Integer.toHexString(val));
        }
    }

    @Override
    public void readSfxData() {
        int length = jni.readSfxBuffer(sfxBuffer);
        int slen;
        int back;

        synchronized (testX) {
            back = cur;
            slen = lenX[back];
            if (length > 0) {
                if (slen + length < SIZE) {
                    System.arraycopy(sfxBuffer, 0, testX[back], 0, length);
                    lenX[back] = length;
                } else {
                    lenX[back] = 0;
                }
            }
        }
    }

    public void onEmulationResumed() {
        synchronized (sfxLock) {
            resetTrack();
        }
    }

    public void onEmulationPaused() {
    }

    @Override
    public void setFastForwardEnabled(boolean enabled) {
        fastForward = enabled;
    }

    private void resetTrack() {
        if (track != null) {
            track.flush();
            track.write(new short[minSize - 2], 0, minSize - 2);
        }
    }

    @Override
    public void renderSfx() {
        synchronized (readyLock) {
            if (track == null) {
                return;
            }
            int slen;
            int cur = this.cur;
            synchronized (testX) {
                slen = lenX[cur];

                if (slen > 0) {
                    lenX[cur] = 0;
                    this.cur = cur == 0 ? 1 : 0;
                }
            }

            if (slen > 0) {
                track.flush();
                track.write(testX[cur], 0, slen);
                totalWritten.set(slen);
            }
        }
    }

    private void createBitmap(int w, int h) {
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
    }

    private void initSound(SfxProfile sfx) {
        int format = sfx.isStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int encoding = sfx.encoding == SfxProfile.SoundEncoding.PCM8 ?
                AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT;
        minSize = AudioTrack.getMinBufferSize(sfx.rate, format, encoding);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, sfx.rate, format,
                encoding, minSize, AudioTrack.MODE_STREAM);
        try {
            track.play();
            resetTrack();

        } catch (Exception e) {
            throw new EmulatorException("sound init failed");
        }

        NLog.d(TAG, "sound init OK");
    }

    private String getMD5(String path) {
        if (path == null) {
            path = getLoadedGame().path;
        }
        if (!md5s.containsKey(path)) {
            String md5 = EmuUtils.getMD5Checksum(new File(path));
            md5s.put(path, md5);
        }
        return md5s.get(path);
    }

}
