package nostalgia.framework;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import nostalgia.framework.ui.gamegallery.GameDescription;

/**
 * 模拟器核心接口，定义所有游戏平台模拟器的统一契约。
 * <p>
 * 每个平台特定的模拟器（NES、GBC、GG）都实现此接口，
 * 提供统一的模拟器生命周期管理，包括游戏加载、帧渲染、
 * 音视频输出、存档读档以及输入处理。
 * </p>
 */
public interface Emulator {

    /**
     * 获取模拟器的元数据信息（名称、支持的配置等）。
     */
    EmulatorInfo getInfo();

    /**
     * 使用指定的图像、声音配置和设置初始化并启动模拟器。
     *
     * @param cfg      图像配置，定义分辨率和帧率
     * @param sfx      声音配置，定义音频格式；传 null 禁用音频
     * @param settings 模拟器特定设置（光枪、历史回退、电池存档等）
     */
    void start(GfxProfile cfg, SfxProfile sfx, EmulatorSettings settings);

    /** 获取当前激活的图像配置。 */
    GfxProfile getActiveGfxProfile();

    /** 获取当前激活的声音配置。 */
    SfxProfile getActiveSfxProfile();

    /** 重置模拟器到初始状态。 */
    void reset();

    /**
     * 将当前模拟器状态保存到指定存档槽位。
     *
     * @param slot 存档槽位索引（0 保留给自动存档）
     */
    void saveState(int slot);

    /**
     * 从指定槽位加载已保存的状态。
     *
     * @param slot 存档槽位索引
     */
    void loadState(int slot);

    /**
     * 从时间旅行历史缓冲区加载状态。
     *
     * @param pos 历史位置索引
     */
    void loadHistoryState(int pos);

    /** 获取时间旅行可用的历史项目数量。 */
    int getHistoryItemCount();

    /**
     * 将历史截图渲染到指定的 Bitmap 中。
     *
     * @param bmp 目标位图
     * @param pos 历史位置索引
     */
    void renderHistoryScreenshot(Bitmap bmp, int pos);

    /**
     * 设置模拟器数据文件的基础目录。
     *
     * @param baseDir 基础目录的绝对路径
     */
    void setBaseDir(String baseDir);

    /**
     * 加载游戏 ROM 以及可选的电池存档文件。
     *
     * @param fileName            ROM 文件的绝对路径
     * @param batterySaveDir      电池存档文件目录
     * @param batterySaveFullPath 电池存档文件（.sav）的完整路径
     */
    void loadGame(String fileName, String batterySaveDir, String batterySaveFullPath);

    /** 模拟器从暂停状态恢复时调用。 */
    void onEmulationResumed();

    /** 模拟器暂停时调用。 */
    void onEmulationPaused();

    /**
     * 启用 Game Genie 金手指密码。
     *
     * @param gg 金手指密码字符串
     */
    void enableCheat(String gg);

    /**
     * 启用原始内存金手指。
     *
     * @param addr 内存地址
     * @param val  要写入的值
     * @param comp 比较值（用于条件金手指）
     */
    void enableRawCheat(int addr, int val, int comp);

    /** 判断当前是否有游戏 ROM 已加载。 */
    boolean isGameLoaded();

    /** 获取当前已加载游戏的信息。 */
    GameInfo getLoadedGame();

    /**
     * 设置控制器按键的按下/释放状态。
     *
     * @param port      玩家端口（0 或 1）
     * @param key       按键码（参见 {@link EmulatorController}）
     * @param isPressed true 表示按下，false 表示释放
     */
    void setKeyPressed(int port, int key, boolean isPressed);

    /**
     * 启用或禁用控制器按键的连发模式。
     *
     * @param port      玩家端口
     * @param key       按键码
     * @param isEnabled true 启用连发，false 禁用
     */
    void setTurboEnabled(int port, int key, boolean isEnabled);

    /**
     * 设置渲染的视口大小。
     *
     * @param w 视口宽度（像素）
     * @param h 视口高度（像素）
     */
    void setViewPortSize(int w, int h);

    /** 将所有控制器按键重置为释放状态。 */
    void resetKeys();

    /**
     * 在指定屏幕坐标处发射光枪。
     *
     * @param x 横坐标
     * @param y 纵坐标
     */
    void fireZapper(float x, float y);

    /**
     * 启用或禁用快进模式。
     *
     * @param enabled true 启用快进
     */
    void setFastForwardEnabled(boolean enabled);

    /**
     * 设置快进周期内跳过的帧数。
     *
     * @param frames 跳过的帧数
     */
    void setFastForwardFrameCount(int frames);

    /**
     * 模拟一帧或多帧并渲染结果。
     *
     * @param numFramesToSkip 跳过渲染的帧数（用于快进）；
     *                        传 -1 停止当前帧处理
     */
    void emulateFrame(int numFramesToSkip);

    /** 从模拟器读取声音数据到音频缓冲区。 */
    void readSfxData();

    /** 将当前音频缓冲区渲染输出。 */
    void renderSfx();

    /**
     * 读取当前调色板。
     *
     * @param palette 用于填充调色板颜色值的数组
     */
    void readPalette(int[] palette);

    /** 将当前视频帧渲染到内部帧缓冲区。 */
    void renderGfx();

    /** 使用 OpenGL 渲染当前视频帧。 */
    void renderGfxGL();

    /**
     * 将当前帧绘制到 Canvas 上。
     *
     * @param canvas 目标画布
     * @param x      横坐标偏移
     * @param y      纵坐标偏移
     */
    void draw(Canvas canvas, int x, int y);

    /** 停止模拟器并释放本地资源。 */
    void stop();

    /** 判断模拟器是否已初始化并准备好模拟帧。 */
    boolean isReady();

    /**
     * 根据游戏自动检测最佳图像配置。
     *
     * @param game 游戏描述
     * @return 推荐的图像配置
     */
    GfxProfile autoDetectGfx(GameDescription game);

    /**
     * 根据游戏自动检测最佳声音配置。
     *
     * @param game 游戏描述
     * @return 推荐的声音配置
     */
    SfxProfile autoDetectSfx(GameDescription game);

}
