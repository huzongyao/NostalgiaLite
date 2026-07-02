package nostalgia.framework.base;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.controllers.DynamicDPad;
import nostalgia.framework.controllers.KeyboardController;
import nostalgia.framework.controllers.QuickSaveController;
import nostalgia.framework.controllers.TouchController;
import nostalgia.framework.controllers.ZapperGun;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.NLog;

/**
 * 输入控制器管理器。
 * <p>统一管理所有输入控制器（触摸、动态方向键、键盘、光枪、快捷存档）
 * 的创建、生命周期和事件分发，减轻 EmulatorActivity 的负担。</p>
 */
public class InputControllerManager {

    private static final String TAG = "InputControllerManager";

    private final EmulatorActivity activity;
    private final List<EmulatorController> controllers;
    private final List<View> controllerViews;

    private TouchController touchController;
    private DynamicDPad dynamicDPad;

    /**
     * 构造输入控制器管理器。
     *
     * @param activity 宿主 Activity
     * @param emulator 模拟器实例
     * @param game     当前游戏描述
     */
    public InputControllerManager(EmulatorActivity activity, Emulator emulator,
                                  GameDescription game) {
        this.activity = activity;
        this.controllers = new ArrayList<>();
        this.controllerViews = new ArrayList<>();

        initControllers(activity, emulator, game);
    }

    /** 创建并注册所有输入控制器 */
    private void initControllers(EmulatorActivity activity, Emulator emulator,
                                 GameDescription game) {
        touchController = new TouchController(activity);
        touchController.connectToEmulator(0, emulator);
        addController(touchController);

        dynamicDPad = new DynamicDPad(activity,
                activity.getWindowManager().getDefaultDisplay(), touchController);
        dynamicDPad.connectToEmulator(0, emulator);
        addController(dynamicDPad);

        QuickSaveController qsc = new QuickSaveController(activity, touchController);
        addController(qsc);

        KeyboardController kc = new KeyboardController(emulator,
                activity.getApplicationContext(), game.checksum, activity);
        addController(kc);

        ZapperGun zapper = new ZapperGun(activity.getApplicationContext(), activity);
        zapper.connectToEmulator(1, emulator);
        addController(zapper);
    }

    /** 注册控制器，收集其 View */
    private void addController(EmulatorController controller) {
        controllers.add(controller);
        View controllerView = controller.getView();
        if (controllerView != null) {
            controllerViews.add(controllerView);
        }
    }

    /** 将控制器 View 添加到指定视图容器 */
    public void addControllerViewsToGroup(ViewGroup group) {
        for (View controllerView : controllerViews) {
            group.addView(controllerView);
        }
    }

    /** 分发触摸事件到所有控制器视图 */
    public void dispatchTouchEvent(MotionEvent ev) {
        if (touchController != null) {
            touchController.show();
        }
        for (View controllerView : controllerViews) {
            controllerView.dispatchTouchEvent(ev);
        }
    }

    /** 分发按键事件到所有控制器视图 */
    public void dispatchKeyEvent(KeyEvent ev) {
        for (View controllerView : controllerViews) {
            controllerView.dispatchKeyEvent(ev);
        }
    }

    /** 隐藏触摸控制器（如果启用了自动隐藏） */
    public void hideTouchController(boolean autoHide) {
        NLog.i(TAG, "hide controller");
        if (autoHide && touchController != null) {
            touchController.hide();
        }
    }

    /** 更新 DynamicDPad 的启用状态 */
    public void updateDynamicDPadState(EmulatorActivity activity) {
        if (PreferenceUtil.isDynamicDPADEnable(activity)) {
            if (!controllers.contains(dynamicDPad)) {
                controllers.add(dynamicDPad);
                controllerViews.add(dynamicDPad.getView());
            }
            PreferenceUtil.setDynamicDPADUsed(activity, true);
        } else {
            controllers.remove(dynamicDPad);
            controllerViews.remove(dynamicDPad.getView());
        }
    }

    /** 通知所有控制器 Activity 暂停 */
    public void onPause(GameDescription game) {
        for (EmulatorController controller : controllers) {
            controller.onPause();
            controller.onGamePaused(game);
        }
    }

    /** 通知所有控制器 Activity 恢复 */
    public void onResume() {
        for (EmulatorController controller : controllers) {
            controller.onResume();
        }
    }

    /** 通知所有控制器游戏已启动 */
    public void onGameStarted(GameDescription game) {
        for (EmulatorController controller : controllers) {
            controller.onGameStarted(game);
        }
    }

    /** 通知所有控制器窗口焦点变化 */
    public void onWindowFocusChanged(boolean hasFocus, GameDescription game) {
        for (EmulatorController controller : controllers) {
            controller.onGameStarted(game);
        }
    }

    /** 通知所有控制器 Activity 销毁，释放资源 */
    public void onDestroy() {
        for (EmulatorController controller : controllers) {
            controller.onDestroy();
        }
        controllers.clear();
        controllerViews.clear();
    }

    /** 获取触摸控制器实例 */
    public TouchController getTouchController() {
        return touchController;
    }

    /** 获取动态方向键实例 */
    public DynamicDPad getDynamicDPad() {
        return dynamicDPad;
    }

    /** 获取所有控制器列表（只读） */
    public List<EmulatorController> getControllers() {
        return controllers;
    }

    /** 获取所有控制器视图列表（只读） */
    public List<View> getControllerViews() {
        return controllerViews;
    }
}
