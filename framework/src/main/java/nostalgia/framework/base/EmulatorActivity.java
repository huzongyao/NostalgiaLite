package nostalgia.framework.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorException;
import nostalgia.framework.EmulatorRunner;
import nostalgia.framework.R;
import nostalgia.framework.base.GameMenu.GameMenuItem;
import nostalgia.framework.base.GameMenu.OnGameMenuListener;
import nostalgia.framework.ui.cheats.CheatsActivity;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.gamegallery.SlotSelectionActivity;
import nostalgia.framework.ui.preferences.GamePreferenceActivity;
import nostalgia.framework.ui.preferences.GamePreferenceFragment;
import nostalgia.framework.ui.preferences.GeneralPreferenceActivity;
import nostalgia.framework.ui.preferences.GeneralPreferenceFragment;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.ui.timetravel.TimeTravelDialog;
import nostalgia.framework.utils.DialogUtils;
import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;


/**
 * 模拟器 Activity 基类，所有平台模拟器的游戏界面均继承此类。
 * <p>
 * 负责统一管理模拟器的完整生命周期，包括：
 * <ul>
 *   <li>初始化模拟器实例、渲染视图、输入控制器</li>
 *   <li>游戏菜单（重置、存档、读档、金手指、截图、设置）</li>
 *   <li>快进、时间旅行、自动存档</li>
 *   <li>性能基准测试与质量自动调整</li>
 *   <li>进程重启机制（防止内存泄漏）</li>
 * </ul>
 * 子类需提供特定平台的模拟器实例和 OpenGL 片段着色器。
 * </p>
 */
public abstract class EmulatorActivity extends AppCompatActivity
        implements OnGameMenuListener, EmulatorRunner.OnNotRespondingListener {

    public static final String EXTRA_GAME = "game";
    public static final String EXTRA_SLOT = "slot";
    public static final String EXTRA_FROM_GALLERY = "fromGallery";
    private static final String TAG = "EmulatorActivity";
    private static final int REQUEST_SAVE = 1;
    private static final int REQUEST_LOAD = 2;
    private static int oldConfig;
    private final int maxPRC = 10;
    boolean isRestarting;
    boolean canRestart;
    boolean runTimeMachine = false;
    TimeTravelDialog dialog;
    private GameMenu gameMenu = null;
    private GameDescription game = null;
    private InputControllerManager inputManager;
    private boolean autoHide;
    private Boolean warningShowing = false;
    private boolean isFF = false;
    private boolean isToggleFF = false;
    private boolean isFFPressed = false;
    private boolean exceptionOccurred;
    private Integer slotToRun;
    private Integer slotToSave;
    private Manager manager;
    private EmulatorView emulatorView;
    private PerformanceMonitor performanceMonitor;
    private ViewGroup group;
    private String baseDir;

    /** 获取平台特定的模拟器实例。 */
    public abstract Emulator getEmulatorInstance();

    /** 获取 OpenGL 片段着色器代码。 */
    public abstract String getFragmentShader();

    /** 获取纹理边界，用于特殊平台（如 NES 的调色板纹理）。 */
    public int[] getTextureBounds(Emulator emulator) {
        return null;
    }

    /** 获取 OpenGL 纹理尺寸。 */
    public int getGLTextureSize() {
        return 256;
    }

    /** 是否使用 OpenGL 调色板纹理。 */
    public boolean hasGLPalette() {
        return true;
    }

    public Manager getManager() {
        return manager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(EXTRA_FROM_GALLERY)) {
            setShouldPauseOnResume(false);
            getIntent().removeExtra(EXTRA_FROM_GALLERY);
        }
        canRestart = true;
        try {
            baseDir = EmulatorUtils.getBaseDir(this);
        } catch (EmulatorException e) {
            handleException(e);
            exceptionOccurred = true;
            return;
        }
        NLog.d(TAG, "onCreate - BaseActivity");
        boolean hasOpenGL20 = EmuUtils.checkGL20Support(getApplicationContext());
        gameMenu = new GameMenu(this, this);
        game = (GameDescription) getIntent().getSerializableExtra(EXTRA_GAME);
        slotToRun = -1;
        WindowManager.LayoutParams wParams = getWindow().getAttributes();
        wParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        wParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        wParams.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
        getWindow().setAttributes(wParams);
        Emulator emulator = getEmulatorInstance();
        int paddingLeft = 0;
        int paddingTop = 0;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paddingTop = getResources().getDimensionPixelSize(R.dimen.top_panel_touchcontroler_height);
        }

        String shader = getFragmentShader();
        OpenGLView openGLView = null;
        int quality = PreferenceUtil.getEmulationQuality(this);
        boolean alreadyBenchmarked = PreferenceUtil.isBenchmarked(this);
        boolean needsBenchmark = quality != 2 && !alreadyBenchmarked;

        if (hasOpenGL20) {
            openGLView = new OpenGLView(this, emulator, paddingLeft, paddingTop, shader);
        }

        emulatorView = openGLView != null ? openGLView :
                new UnacceleratedView(this, emulator, paddingLeft, paddingTop);
        performanceMonitor = new PerformanceMonitor(this, emulatorView);
        if (hasOpenGL20 && needsBenchmark) {
            ((OpenGLView) emulatorView).setBenchmark(performanceMonitor.createOpenGLBenchmark());
        }
        inputManager = new InputControllerManager(this, emulator, game);
        group = new FrameLayout(this);
        Display display = getWindowManager().getDefaultDisplay();
        int w = EmuUtils.getDisplayWidth(display);
        int h = EmuUtils.getDisplayHeight(display);
        LayoutParams params = new LayoutParams(w, h);
        group.setLayoutParams(params);
        group.addView(emulatorView.asView());
        inputManager.addControllerViewsToGroup(group);

        group.addView(new View(getApplicationContext()) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return true;
            }
        });
        setContentView(group);
        manager = new Manager(emulator, getApplicationContext());
        manager.setOnNotRespondingListener(this);

        if (needsBenchmark) {
            manager.setBenchmark(performanceMonitor.createEmulationBenchmark());
        }
    }

    public void hideTouchController() {
        inputManager.hideTouchController(autoHide);
    }

    public void onNotResponding() {
        synchronized (warningShowing) {
            if (!warningShowing) {
                warningShowing = true;
            } else {
                return;
            }
        }

        runOnUiThread(() -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.too_slow)
                    .create();
            dialog.setOnDismissListener(dialog1 -> finish());
            try {
                manager.pauseEmulation();
            } catch (EmulatorException ignored) {
            }
            DialogUtils.show(dialog, true);
        });
    }

    public ViewPort getViewPort() {
        return emulatorView.getViewPort();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exceptionOccurred) {
            return;
        }
        oldConfig = getChangingConfigurations();
        group.removeAllViews();
        try {
            manager.destroy();
        } catch (EmulatorException ignored) {
        }
        inputManager.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setShouldPauseOnResume(false);
        if (resultCode == RESULT_OK) {
            canRestart = false;
            int slotIdx = data.getIntExtra(SlotSelectionActivity.EXTRA_SLOT, -1);
            switch (requestCode) {
                case REQUEST_SAVE:
                    slotToSave = slotIdx;
                    slotToRun = 0;
                    break;
                case REQUEST_LOAD:
                    slotToRun = slotIdx;
                    slotToSave = null;
                    break;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean res = super.dispatchTouchEvent(ev);
        inputManager.dispatchTouchEvent(ev);
        return res;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        boolean res = super.dispatchKeyEvent(ev);
        inputManager.dispatchKeyEvent(ev);
        return res;
    }

    public void setShouldPauseOnResume(boolean b) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("emulator_activity_pause", b)
                .apply();
    }

    public boolean shouldPause() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("emulator_activity_pause", false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRestarting) {
            finish();
            return;
        }
        if (exceptionOccurred) {
            return;
        }
        if (gameMenu != null && gameMenu.isOpen()) {
            gameMenu.dismiss();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        inputManager.onPause(game);
        try {
            manager.stopGame();

        } catch (EmulatorException e) {
            handleException(e);
        } finally {
            emulatorView.onPause();
        }
    }

    public void onFastForwardDown() {
        if (isToggleFF) {
            if (!isFFPressed) {
                isFFPressed = true;
                isFF = !isFF;
                manager.setFastForwardEnabled(isFF);
            }
        } else {
            manager.setFastForwardEnabled(true);
        }
    }

    public void onFastForwardUp() {
        if (!isToggleFF) {
            manager.setFastForwardEnabled(false);
        }
        isFFPressed = false;
    }

    private void handleException(EmulatorException e) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(e.getMessage(this)).create();
        dialog.setOnDismissListener(dialog1 -> runOnUiThread(this::finish));
        DialogUtils.show(dialog, true);
    }

    public void quickSave() {
        manager.saveState(10);
        runOnUiThread(() -> Toast.makeText(this,
                "state saved", Toast.LENGTH_SHORT).show());
    }

    public void quickLoad() {
        manager.loadState(10);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onResume() {
        super.onResume();
        isRestarting = false;
        Bundle extras = getIntent().getExtras();
        boolean isAfterProcessRestart = false;
        if (extras != null) {
            isAfterProcessRestart = extras.getBoolean(RestarterActivity.EXTRA_AFTER_RESTART);
        }
        getIntent().removeExtra(RestarterActivity.EXTRA_AFTER_RESTART);
        boolean shouldRestart = ProcessRestarter.decreaseResumesToRestart(this, maxPRC) == 0;
        if (!isAfterProcessRestart && shouldRestart && canRestart) {
            ProcessRestarter.resetCounter(this, maxPRC);
            isRestarting = true;
            ProcessRestarter.restartProcess(this, this.getClass());
            return;
        }
        canRestart = true;
        if (exceptionOccurred) {
            return;
        }
        autoHide = PreferenceUtil.isAutoHideControls(this);
        isToggleFF = PreferenceUtil.isFastForwardToggleable(this);
        isFF = false;
        isFFPressed = false;
        switch (PreferenceUtil.getDisplayRotation(this)) {
            case AUTO:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            case PORT:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case LAND:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
        manager.setFastForwardFrameCount(PreferenceUtil.getFastForwardFrameCount(this));
        inputManager.updateDynamicDPadState(this);
        if (PreferenceUtil.isFastForwardEnabled(this)) {
            PreferenceUtil.setFastForwardUsed(this, true);
        }
        if (PreferenceUtil.isScreenSettingsSaved(this)) {
            PreferenceUtil.setScreenLayoutUsed(this, true);
        }
        inputManager.onResume();
        try {
            manager.startGame(game);
            inputManager.onGameStarted(game);
            if (slotToRun != -1) {
                manager.loadState(slotToRun);
            } else {
                if (SlotUtils.autoSaveExists(baseDir, game.checksum)) {
                    manager.loadState(0);
                }
            }
            if (slotToSave != null) {
                manager.copyAutoSave(slotToSave);
            }
            boolean wasRotated = (oldConfig & ActivityInfo.CONFIG_ORIENTATION) == ActivityInfo.CONFIG_ORIENTATION;
            oldConfig = 0;

            if (shouldPause() && !wasRotated) {
                gameMenu.open();
            }

            setShouldPauseOnResume(true);

            if (gameMenu != null && gameMenu.isOpen()) {
                manager.pauseEmulation();
            }

            slotToRun = 0;
            int quality = PreferenceUtil.getEmulationQuality(this);
            emulatorView.setQuality(quality);
            emulatorView.onResume();
            enableCheats();
        } catch (EmulatorException e) {
            handleException(e);
        }
    }

    private void enableCheats() {
        int numCheats = 0;
        try {
            numCheats = manager.enableCheats(this, game);
        } catch (final EmulatorException e) {
            runOnUiThread(() -> Toast.makeText(this, e.getMessage(this),
                    Toast.LENGTH_SHORT).show());
        }
        if (numCheats > 0) {
            Toast.makeText(this, getString(R.string.toast_cheats_enabled, numCheats),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (exceptionOccurred) {
            return;
        }
        inputManager.onWindowFocusChanged(hasFocus, game);
    }

    public void openGameMenu() {
        gameMenu.open();
    }

    @Override
    public void onGameMenuCreate(GameMenu menu) {
        menu.add(R.string.game_menu_reset, R.drawable.ic_reload);
        menu.add(R.string.game_menu_save, R.drawable.ic_save);
        menu.add(R.string.game_menu_load, R.drawable.ic_load);
        menu.add(R.string.game_menu_cheats, R.drawable.ic_cheats);
        menu.add(R.string.game_menu_back_to_past, R.drawable.ic_time_machine);
        menu.add(R.string.game_menu_screenshot, R.drawable.ic_make_screenshot);
        BaseApplication ea = (BaseApplication) getApplication();
        int settingsStringRes = ea.hasGameMenu() ?
                R.string.game_menu_settings : R.string.gallery_menu_pref;
        menu.add(settingsStringRes, R.drawable.ic_game_settings);
    }

    @Override
    public void onGameMenuPrepare(GameMenu menu) {
        GameMenuItem backToPast = menu.getItem(R.string.game_menu_back_to_past);
        backToPast.enable = PreferenceUtil.isTimeshiftEnabled(this);
        NLog.i(TAG, "prepare menu");
    }

    @Override
    public void onGameMenuClosed(GameMenu menu) {
        try {
            if (!runTimeMachine) {
                if (!menu.isOpen()) {
                    manager.resumeEmulation();
                    inputManager.onGameStarted(game);
                    inputManager.onResume();
                }
            }
        } catch (EmulatorException e) {
            handleException(e);
        }
    }

    @Override
    public void onGameMenuOpened(GameMenu menu) {
        NLog.i(TAG, "on game menu open");
        try {
            if (manager != null) {
                manager.pauseEmulation();
                inputManager.onPause(game);
            }
        } catch (EmulatorException e) {
            handleException(e);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        setShouldPauseOnResume(false);
        super.startActivity(intent);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        setShouldPauseOnResume(false);
        super.startActivity(intent, options);
    }

    private void freeStartActivityForResult(Activity activity, Intent intent, int requestCode) {
        setShouldPauseOnResume(false);
        startActivityForResult(intent, requestCode);
    }

    private void freeStartActivity(Activity activity, Intent intent) {
        setShouldPauseOnResume(false);
        startActivity(intent);
    }

    @Override
    public void onGameMenuItemSelected(GameMenu menu, GameMenuItem item) {
        try {
            int id = item.id;
            if (id == R.string.game_menu_back_to_past) {
                onGameBackToPast();
            } else if (id == R.string.game_menu_reset) {
                onMenuReset();
            } else if (id == R.string.game_menu_save) {
                onMenuSave();
            } else if (id == R.string.game_menu_load) {
                onMenuLoad();
            } else if (id == R.string.game_menu_cheats) {
                onMenuCheats();
            } else if (id == R.string.game_menu_settings) {
                onMenuGameSettings();
            } else if (id == R.string.gallery_menu_pref) {
                onMenuGeneralSettings();
            } else if (id == R.string.game_menu_screenshot) {
                ScreenshotHelper.saveScreenshot(this, game);
            }
        } catch (EmulatorException e) {
            handleException(e);
        }
    }

    /** 游戏菜单 - 重置模拟器 */
    private void onMenuReset() {
        manager.resetEmulator();
        enableCheats();
    }

    /** 游戏菜单 - 存档 */
    private void onMenuSave() {
        Intent i = new Intent(this, SlotSelectionActivity.class);
        i.putExtra(SlotSelectionActivity.EXTRA_GAME, game);
        i.putExtra(SlotSelectionActivity.EXTRA_BASE_DIRECTORY, baseDir);
        i.putExtra(SlotSelectionActivity.EXTRA_DIALOG_TYPE_INT,
                SlotSelectionActivity.DIALOAG_TYPE_SAVE);
        freeStartActivityForResult(this, i, REQUEST_SAVE);
    }

    /** 游戏菜单 - 读档 */
    private void onMenuLoad() {
        Intent i = new Intent(this, SlotSelectionActivity.class);
        i.putExtra(SlotSelectionActivity.EXTRA_GAME, game);
        i.putExtra(SlotSelectionActivity.EXTRA_BASE_DIRECTORY, baseDir);
        i.putExtra(SlotSelectionActivity.EXTRA_DIALOG_TYPE_INT,
                SlotSelectionActivity.DIALOAG_TYPE_LOAD);
        freeStartActivityForResult(this, i, REQUEST_LOAD);
    }

    /** 游戏菜单 - 金手指 */
    private void onMenuCheats() {
        Intent i = new Intent(this, CheatsActivity.class);
        i.putExtra(CheatsActivity.EXTRA_IN_GAME_HASH, game.checksum);
        freeStartActivity(this, i);
    }

    /** 游戏菜单 - 游戏设置 */
    private void onMenuGameSettings() {
        Intent i = new Intent(this, GamePreferenceActivity.class);
        i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                GamePreferenceFragment.class.getName());
        i.putExtra(GamePreferenceActivity.EXTRA_GAME, game);
        startActivity(i);
    }

    /** 游戏菜单 - 通用设置 */
    private void onMenuGeneralSettings() {
        Intent i = new Intent(this, GeneralPreferenceActivity.class);
        i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                GeneralPreferenceFragment.class.getName());
        startActivity(i);
    }

    private void onGameBackToPast() {
        if (manager.getHistoryItemCount() > 1) {
            dialog = new TimeTravelDialog(this, manager, game);
            dialog.setOnDismissListener(dialog -> {
                runTimeMachine = false;
                try {
                    manager.resumeEmulation();
                } catch (EmulatorException e) {
                    handleException(e);
                }
            });
            DialogUtils.show(dialog, true);
            runTimeMachine = true;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        NLog.i(TAG, "activity key up event:" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (event.isAltPressed()) {
                    return true;
                }
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_3D_MODE:
            case KeyEvent.KEYCODE_APP_SWITCH:
                return super.onKeyUp(keyCode, event);
            default:
                return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        NLog.i(TAG, "activity key down event:" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                openGameMenu();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (event.isAltPressed()) {
                    return true;
                }
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_3D_MODE:
            case KeyEvent.KEYCODE_APP_SWITCH:
                return super.onKeyDown(keyCode, event);
            default:
                return true;
        }
    }


}
