package nostalgia.framework;

import android.view.View;

import nostalgia.framework.ui.gamegallery.GameDescription;

/**
 * 模拟器输入控制器接口。
 * <p>
 * 定义了虚拟手柄按键的键码常量以及控制器必须实现的生命周期回调，
 * 用于处理游戏状态变化。实现类包括 {@code TouchController}（屏幕触摸输入）
 * 和 {@code KeyboardController}（物理键盘/手柄输入）。
 * </p>
 */
public interface EmulatorController {

    // 标准手柄按键码
    int KEY_A = 0;
    int KEY_B = 1;
    int KEY_A_TURBO = 255;
    int KEY_B_TURBO = 256;
    int KEY_X = 2;
    int KEY_Y = 3;
    int KEY_START = 4;
    int KEY_SELECT = 5;
    int KEY_UP = 6;
    int KEY_DOWN = 7;
    int KEY_LEFT = 8;
    int KEY_RIGHT = 9;

    // 触摸/按键动作类型
    int ACTION_DOWN = 0;
    int ACTION_UP = 1;

    /** 模拟器 Activity 恢复时调用。 */
    void onResume();

    /** 模拟器 Activity 暂停时调用。 */
    void onPause();

    /**
     * 窗口焦点变化时调用。
     *
     * @param hasFocus true 表示窗口获得焦点
     */
    void onWindowFocusChanged(boolean hasFocus);

    /**
     * 游戏开始时调用。
     *
     * @param game 已启动的游戏描述
     */
    void onGameStarted(GameDescription game);

    /**
     * 游戏暂停时调用。
     *
     * @param game 已暂停的游戏描述
     */
    void onGamePaused(GameDescription game);

    /**
     * 将控制器连接到模拟器实例。
     *
     * @param port     玩家端口号
     * @param emulator 要连接的模拟器
     */
    void connectToEmulator(int port, Emulator emulator);

    /** 获取与此控制器关联的 View。 */
    View getView();

    /** 模拟器 Activity 销毁时调用，用于释放资源。 */
    void onDestroy();

}
