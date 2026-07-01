package nostalgia.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import nostalgia.framework.base.BatterySaveUtils;
import nostalgia.framework.base.Benchmark;
import nostalgia.framework.base.EmulatorUtils;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.FileUtils;
import nostalgia.framework.utils.NLog;

/**
 * 模拟器执行生命周期管理器，负责游戏加载、帧模拟、音频播放、
 * 存档读档以及暂停/恢复控制。
 * <p>
 * 此类协调两个内部线程：
 * <ul>
 *   <li>{@link EmulatorThread} - 以目标帧率运行主模拟循环</li>
 *   <li>{@link AudioPlayer} - 与模拟同步读取并渲染音频数据</li>
 * </ul>
 * 所有模拟器操作通过共享锁同步，确保线程安全。
 * </p>
 */
public class EmulatorRunner {

    private static final String TAG = "EmulatorRunner";
    /** 自动存档使用的保留槽位号，在暂停/停止时自动保存状态。 */
    private static final int AUTO_SAVE_SLOT = 0;
    protected final Object lock = new Object();
    protected Emulator emulator;
    protected Context context;
    private Object pauseLock = new Object();
    private boolean audioEnabled;
    private Benchmark benchmark;
    private Object sfxReadyLock = new Object();
    private boolean sfxReady = false;
    private AudioPlayer audioPlayer;
    private AtomicBoolean isPaused = new AtomicBoolean();
    private EmulatorThread updater;
    private OnNotRespondingListener notRespondingListener;

    /**
     * 创建模拟器运行器。
     *
     * @param emulator 要管理的模拟器实例
     * @param context  Android 上下文
     */
    public EmulatorRunner(Emulator emulator, Context context) {
        this.emulator = emulator;
        emulator.setBaseDir(EmulatorUtils.getBaseDir(context));
        this.context = context;
        fixBatterySaveBug();
    }

    /**
     * 将电池存档文件从外部缓存目录迁移到基础目录。
     * 修复历史版本中存档保存在错误位置的缺陷。
     */
    private void fixBatterySaveBug() {
        if (PreferenceUtil.isBatterySaveBugFixed(context)) {
            return;
        }

        File dir = context.getExternalCacheDir();

        if (dir == null) {
            return;
        }

        FilenameFilter filter = (dir1, filename) ->
                filename.toLowerCase().endsWith(".sav");
        String cacheDir = context.getExternalCacheDir().getAbsolutePath();
        String baseDir = EmulatorUtils.getBaseDir(context);
        String[] fileNames = dir.list(filter);

        for (String filename : fileNames) {
            File source = new File(cacheDir, filename);
            File dest = new File(baseDir, filename);

            try {
                FileUtils.copyFile(source, dest);
                source.delete();
                NLog.d("SAV", "copying: " + source + " " + dest);

            } catch (Exception ignored) {
            }
        }

        PreferenceUtil.setBatterySaveBugFixed(context);
    }

    /** 释放所有资源，包括音频播放器和模拟线程。 */
    public void destroy() {
        if (audioPlayer != null) {
            audioPlayer.destroy();
        }

        if (updater != null) {
            updater.destroy();
        }
    }

    public void setOnNotRespondingListener(OnNotRespondingListener listener) {
        notRespondingListener = listener;
    }

    /** 暂停模拟，自动保存当前状态，并挂起模拟线程。 */
    public void pauseEmulation() {
        synchronized (pauseLock) {
            if (!isPaused.get()) {
                NLog.i(TAG, "--PAUSE EMULATION--");
                isPaused.set(true);
                emulator.onEmulationPaused();
                updater.pause();
                saveAutoState();
            }
        }
    }

    /** 从暂停状态恢复模拟。 */
    public void resumeEmulation() {
        synchronized (pauseLock) {
            if (isPaused.get()) {
                NLog.i(TAG, "--UNPAUSE EMULATION--");
                emulator.onEmulationResumed();
                updater.unpause();
                isPaused.set(false);
            }
        }
    }

    /** 停止当前游戏，保存状态并释放模拟器。 */
    public void stopGame() {
        if (audioPlayer != null) {
            audioPlayer.destroy();
        }

        if (updater != null) {
            updater.destroy();
        }

        saveAutoState();

        synchronized (lock) {
            emulator.stop();
        }
    }

    /** 重置模拟器到开机初始状态。 */
    public void resetEmulator() {
        synchronized (lock) {
            emulator.reset();
        }
    }

    /**
     * 开始新游戏。从偏好设置中初始化图像/声音配置，
     * 加载 ROM 和电池存档，然后启动模拟线程和音频线程。
     *
     * @param game 包含 ROM 路径的游戏描述
     */
    public void startGame(GameDescription game) {
        isPaused.set(false);

        if (updater != null) {
            updater.destroy();
        }

        if (audioPlayer != null) {
            audioPlayer.destroy();
        }

        synchronized (lock) {
            GfxProfile gfx = PreferenceUtil.getVideoProfile(context, emulator, game);
            PreferenceUtil.setLastGfxProfile(context, gfx);
            EmulatorSettings settings = new EmulatorSettings();
            settings.zapperEnabled = PreferenceUtil.isZapperEnabled(context, game.checksum);
            settings.historyEnabled = PreferenceUtil.isTimeshiftEnabled(context);
            settings.loadSavFiles = PreferenceUtil.isLoadSavFiles(context);
            settings.saveSavFiles = PreferenceUtil.isSaveSavFiles(context);
            List<SfxProfile> profiles = emulator.getInfo().getAvailableSfxProfiles();
            SfxProfile sfx;
            int desiredQuality = PreferenceUtil.getEmulationQuality(context);
            settings.quality = desiredQuality;
            desiredQuality = Math.min(profiles.size() - 1, desiredQuality);
            sfx = profiles.get(desiredQuality);

            if (!PreferenceUtil.isSoundEnabled(context)) {
                sfx = null;
            }

            audioEnabled = sfx != null;
            emulator.start(gfx, sfx, settings);
            String battery = context.getExternalCacheDir().getAbsolutePath();
            NLog.e("bat", battery);
            BatterySaveUtils.createSavFileCopyIfNeeded(context, game.path);
            String batteryDir = BatterySaveUtils.getBatterySaveDir(context, game.path);
            String possibleBatteryFileFullPath = batteryDir + "/"
                    + FileUtils.getFileNameWithoutExt(new File(game.path))
                    + ".sav";
            emulator.loadGame(game.path, batteryDir,
                    possibleBatteryFileFullPath);
            emulator.emulateFrame(0);
        }

        updater = new EmulatorThread();
        updater.setFps(emulator.getActiveGfxProfile().fps);
        updater.start();

        if (audioEnabled) {
            audioPlayer = new AudioPlayer();
            audioPlayer.start();
        }
    }

    public void enableCheat(String gg) {
        checkGameLoaded();

        synchronized (lock) {
            emulator.enableCheat(gg);
        }
    }

    public void enableRawCheat(int addr, int val, int comp) {
        checkGameLoaded();

        synchronized (lock) {
            emulator.enableRawCheat(addr, val, comp);
        }
    }

    public void saveState(int slot) {
        if (emulator.isGameLoaded()) {
            synchronized (lock) {
                emulator.saveState(slot);
            }
        }
    }

    public int getHistoryItemCount() {
        synchronized (lock) {
            return emulator.getHistoryItemCount();
        }
    }

    public void loadHistoryState(int pos) {
        synchronized (lock) {
            emulator.emulateFrame(-1);
            emulator.loadHistoryState(pos);
        }
    }

    public void renderHistoryScreenshot(Bitmap bmp, int pos) {
        synchronized (lock) {
            emulator.renderHistoryScreenshot(bmp, pos);
        }
    }

    public void loadState(int slot) {
        checkGameLoaded();

        synchronized (lock) {
            emulator.emulateFrame(-1);
            emulator.loadState(slot);
        }
    }

    private void checkGameLoaded() {
        if (!emulator.isGameLoaded()) {
            throw new EmulatorException("unexpected");
        }
    }

    private void saveAutoState() {
        saveState(AUTO_SAVE_SLOT);
    }

    public void setBenchmark(Benchmark benchmark) {
        this.benchmark = benchmark;
    }

    /** 模拟器无响应（ANR检测）时的监听器回调接口。 */
    public interface OnNotRespondingListener {
        void onNotResponding();
    }

    /**
     * 内部模拟线程，运行主模拟循环。
     * <p>
     * 根据模拟器的目标帧率计算帧间隔时间，当模拟落后时处理跳帧，
     * 并在固定间隔触发音频数据读取。
     * </p>
     */
    private class EmulatorThread extends Thread {

        private int totalSkipped;
        private long expectedTimeE1;
        private int exactDelayPerFrameE1;
        private long currentFrame;
        private long startTime;
        private boolean isPaused = true;
        private AtomicBoolean isRunning = new AtomicBoolean(true);
        private Object pauseLock = new Object();
        private int delayPerFrame;

        public void setFps(int fps) {
            // 使用10倍精度计算每帧的精确延迟（微秒级）
            exactDelayPerFrameE1 = (int) ((1000 / (float) fps) * 10);
            delayPerFrame = (int) (exactDelayPerFrameE1 / 10f + 0.5);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            setName("emudroid:gameLoop #" + (int) (Math.random() * 1000));
            NLog.i(TAG, getName() + " started");
            long skippedTime = 0;
            totalSkipped = 0;
            unpause();
            expectedTimeE1 = 0;
            int cnt = 0;
            int afterSkip = 0;

            while (isRunning.get()) {
                if (benchmark != null) {
                    benchmark.notifyFrameEnd();
                }

                long time1 = System.currentTimeMillis();

                // 暂停等待循环
                synchronized (pauseLock) {
                    while (isPaused) {
                        try {
                            pauseLock.wait();

                        } catch (InterruptedException ignored) {
                        }

                        if (benchmark != null) {
                            benchmark.reset();
                        }

                        time1 = System.currentTimeMillis();
                    }
                }

                int numFramesToSkip = 0;
                long realTime = (time1 - startTime);
                long diff = ((expectedTimeE1 / 10) - realTime);
                long delay = +diff;

                // 帧率同步：如果超前则等待，落后则最小等待
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);

                    } catch (Exception ignored) {
                    }
                } else {
                    try {
                        Thread.sleep(1);

                    } catch (Exception ignored) {
                    }
                }

                skippedTime = -diff;

                if (afterSkip > 0) {
                    afterSkip--;
                }

                // 跳帧逻辑：当落后超过3帧时计算需要跳过的帧数（最多8帧）
                if (skippedTime >= delayPerFrame * 3 && afterSkip == 0) {
                    numFramesToSkip = (int) (skippedTime / delayPerFrame) - 1;
                    int originalSkipped = numFramesToSkip;
                    numFramesToSkip = Math.min(originalSkipped, 8);
                    expectedTimeE1 += (numFramesToSkip * exactDelayPerFrameE1);
                    totalSkipped += numFramesToSkip;
                }

                if (benchmark != null) {
                    benchmark.notifyFrameStart();
                }

                synchronized (lock) {
                    if (emulator.isReady()) {
                        emulator.emulateFrame(numFramesToSkip);
                        cnt += 1 + numFramesToSkip;

                        // 每3帧读取一次音频数据
                        if (audioEnabled && cnt >= 3) {
                            emulator.readSfxData();

                            synchronized (sfxReadyLock) {
                                sfxReady = true;
                                sfxReadyLock.notifyAll();
                            }

                            cnt = 0;
                        }
                    }
                }

                currentFrame += 1 + numFramesToSkip;
                expectedTimeE1 += exactDelayPerFrameE1;
            }

            NLog.i(TAG, getName() + " finished");
        }

        public void unpause() {
            synchronized (pauseLock) {
                startTime = System.currentTimeMillis();
                currentFrame = 0;
                expectedTimeE1 = 0;
                isPaused = false;
                pauseLock.notifyAll();
            }
        }

        public void pause() {
            synchronized (pauseLock) {
                isPaused = true;
            }
        }

        public void destroy() {
            isRunning.set(false);
            unpause();
        }

    }

    /**
     * 内部音频线程，负责音频渲染。
     * <p>
     * 等待模拟线程发出音频数据就绪信号后，
     * 调用 {@link Emulator#renderSfx()} 输出声音。
     * </p>
     */
    private class AudioPlayer extends Thread {

        protected AtomicBoolean isRunning = new AtomicBoolean();

        @Override
        public void run() {
            isRunning.set(true);
            setName("emudroid:audioReader");

            while (isRunning.get()) {
                // 等待音频数据就绪信号
                synchronized (sfxReadyLock) {
                    while (!sfxReady) {
                        try {
                            sfxReadyLock.wait();

                        } catch (Exception e) {
                            return;
                        }
                    }

                    sfxReady = false;
                }

                if (emulator.isReady()) {
                    emulator.renderSfx();
                }
            }
        }

        public void destroy() {
            isRunning.set(false);
            this.interrupt();
        }


    }


}
