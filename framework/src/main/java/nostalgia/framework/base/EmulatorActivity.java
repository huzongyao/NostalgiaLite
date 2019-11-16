package nostalgia.framework.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Environment;
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

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.EmulatorException;
import nostalgia.framework.EmulatorRunner;
import nostalgia.framework.R;
import nostalgia.framework.base.Benchmark.BenchmarkCallback;
import nostalgia.framework.base.GameMenu.GameMenuItem;
import nostalgia.framework.base.GameMenu.OnGameMenuListener;
import nostalgia.framework.controllers.DynamicDPad;
import nostalgia.framework.controllers.KeyboardController;
import nostalgia.framework.controllers.QuickSaveController;
import nostalgia.framework.controllers.TouchController;
import nostalgia.framework.controllers.ZapperGun;
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


public abstract class EmulatorActivity extends Activity
        implements OnGameMenuListener, EmulatorRunner.OnNotRespondingListener {

    public static final String EXTRA_GAME = "game";
    public static final String EXTRA_SLOT = "slot";
    public static final String EXTRA_FROM_GALLERY = "fromGallery";
    private static final String TAG = "EmulatorActivity";
    private static final String OPEN_GL_BENCHMARK = "openGL";
    private static final String EMULATION_BENCHMARK = "emulation";
    private static final int REQUEST_SAVE = 1;
    private static final int REQUEST_LOAD = 2;
    public static PackageManager pm;
    public static String pn;
    public static String sd;
    private static int oldConfig;
    private final int maxPRC = 10;
    boolean isRestarting;
    boolean canRestart;
    boolean runTimeMachine = false;
    TimeTravelDialog dialog;
    private GameMenu gameMenu = null;
    private GameDescription game = null;
    private DynamicDPad dynamic;
    private TouchController touchController = null;
    private boolean autoHide;
    private Boolean warningShowing = false;
    private boolean isFF = false;
    private boolean isToggleFF = false;
    private boolean isFFPressed = false;
    private boolean exceptionOccurred;
    private Integer slotToRun;
    private Integer slotToSave;
    private List<EmulatorController> controllers;
    private Manager manager;
    private EmulatorView emulatorView;

    private BenchmarkCallback benchmarkCallback = new BenchmarkCallback() {
        private int numTests = 0;
        private int numOk = 0;

        @Override
        public void onBenchmarkReset(Benchmark benchmark) {
        }

        @Override
        public void onBenchmarkEnded(Benchmark benchmark, int steps, long totalTime) {
            float millisPerFrame = totalTime / (float) steps;
            numTests++;
            if (benchmark.getName().equals(OPEN_GL_BENCHMARK)) {
                if (millisPerFrame < 17) {
                    numOk++;
                }
            }
            if (benchmark.getName().equals(EMULATION_BENCHMARK)) {
                if (millisPerFrame < 17) {
                    numOk++;
                }
            }
            if (numTests == 2) {
                PreferenceUtil.setBenchmarked(EmulatorActivity.this, true);
                if (numOk == 2) {
                    emulatorView.setQuality(2);
                    PreferenceUtil.setEmulationQuality(EmulatorActivity.this, 2);
                } else {
                }
            }
        }
    };
    private List<View> controllerViews;
    private ViewGroup group;
    private String baseDir;

    public abstract Emulator getEmulatorInstance();

    public abstract String getFragmentShader();

    public int[] getTextureBounds(Emulator emulator) {
        return null;
    }

    public int getGLTextureSize() {
        return 256;
    }

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
            if (needsBenchmark) {
                openGLView.setBenchmark(new Benchmark(OPEN_GL_BENCHMARK, 200, benchmarkCallback));
            }
        }

        emulatorView = openGLView != null ? openGLView :
                new UnacceleratedView(this, emulator, paddingLeft, paddingTop);
        controllers = new ArrayList<>();
        touchController = new TouchController(this);
        controllers.add(touchController);
        touchController.connectToEmulator(0, emulator);
        dynamic = new DynamicDPad(this, getWindowManager().getDefaultDisplay(), touchController);
        controllers.add(dynamic);
        dynamic.connectToEmulator(0, emulator);
        QuickSaveController qsc = new QuickSaveController(this, touchController);
        controllers.add(qsc);
        KeyboardController kc = new KeyboardController(emulator, getApplicationContext(), game.checksum, this);
        ZapperGun zapper = new ZapperGun(getApplicationContext(), this);
        zapper.connectToEmulator(1, emulator);
        controllers.add(zapper);
        controllers.add(kc);
        group = new FrameLayout(this);
        Display display = getWindowManager().getDefaultDisplay();
        int w = EmuUtils.getDisplayWidth(display);
        int h = EmuUtils.getDisplayHeight(display);
        LayoutParams params = new LayoutParams(w, h);
        group.setLayoutParams(params);
        group.addView(emulatorView.asView());
        controllerViews = new ArrayList<>();
        for (EmulatorController controller : controllers) {
            View controllerView = controller.getView();
            if (controllerView != null) {
                controllerViews.add(controllerView);
                group.addView(controllerView);
            }
        }

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
            manager.setBenchmark(new Benchmark(EMULATION_BENCHMARK, 1000, benchmarkCallback));
        }
    }

    public void hideTouchController() {
        NLog.i(TAG, "hide controler");
        if (autoHide) {
            if (touchController != null) {
                touchController.hide();
            }
        }
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
        controllerViews.clear();
        try {
            manager.destroy();
        } catch (EmulatorException ignored) {
        }
        for (EmulatorController controller : controllers) {
            controller.onDestroy();
        }
        controllers.clear();
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
        if (touchController != null) {
            touchController.show();
        }
        for (View controllerView : controllerViews) {
            controllerView.dispatchTouchEvent(ev);
        }
        return res;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        boolean res = super.dispatchKeyEvent(ev);
        for (View controllerView : controllerViews) {
            controllerView.dispatchKeyEvent(ev);
        }
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
        pm = null;
        if (gameMenu != null && gameMenu.isOpen()) {
            gameMenu.dismiss();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        for (EmulatorController controller : controllers) {
            controller.onPause();
            controller.onGamePaused(game);
        }
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

    private void restartProcess(Class<?> activityToStartClass) {
        isRestarting = true;
        Intent intent = new Intent(this, RestarterActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra(RestarterActivity.EXTRA_PID, android.os.Process.myPid());
        String className = activityToStartClass.getName();
        intent.putExtra(RestarterActivity.EXTRA_CLASS, className);
        startActivity(intent);
    }

    private int decreaseResumesToRestart() {
        int prc = PreferenceManager.getDefaultSharedPreferences(this).getInt("PRC", maxPRC);
        if (prc > 0) {
            prc--;
        }
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt("PRC", prc);
        editor.apply();
        return prc;
    }

    private void resetProcessResetCounter() {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt("PRC", maxPRC);
        editor.apply();
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
        boolean shouldRestart = decreaseResumesToRestart() == 0;
        if (!isAfterProcessRestart && shouldRestart && canRestart) {
            resetProcessResetCounter();
            restartProcess(this.getClass());
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
        if (PreferenceUtil.isDynamicDPADEnable(this)) {
            if (!controllers.contains(dynamic)) {
                controllers.add(dynamic);
                controllerViews.add(dynamic.getView());
            }
            PreferenceUtil.setDynamicDPADUsed(this, true);
        } else {
            controllers.remove(dynamic);
            controllerViews.remove(dynamic.getView());
        }
        if (PreferenceUtil.isFastForwardEnabled(this)) {
            PreferenceUtil.setFastForwardUsed(this, true);
        }
        if (PreferenceUtil.isScreenSettingsSaved(this)) {
            PreferenceUtil.setScreenLayoutUsed(this, true);
        }
        pm = getPackageManager();
        pn = getPackageName();
        for (EmulatorController controller : controllers) {
            controller.onResume();
        }
        try {
            manager.startGame(game);
            for (EmulatorController controller : controllers) {
                controller.onGameStarted(game);
            }
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
        for (EmulatorController controller : controllers) {
            controller.onGameStarted(game);
        }
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
                    for (EmulatorController controller : controllers) {
                        controller.onGameStarted(game);
                        controller.onResume();
                    }
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
                for (EmulatorController controller : controllers) {
                    controller.onGamePaused(game);
                    controller.onPause();
                }
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
            if (item.id == R.string.game_menu_back_to_past) {
                onGameBackToPast();
            } else if (item.id == R.string.game_menu_reset) {
                manager.resetEmulator();
                enableCheats();
            } else if (item.id == R.string.game_menu_save) {
                Intent i = new Intent(this, SlotSelectionActivity.class);
                i.putExtra(SlotSelectionActivity.EXTRA_GAME, game);
                i.putExtra(SlotSelectionActivity.EXTRA_BASE_DIRECTORY, baseDir);
                i.putExtra(SlotSelectionActivity.EXTRA_DIALOG_TYPE_INT,
                        SlotSelectionActivity.DIALOAG_TYPE_SAVE);
                freeStartActivityForResult(this, i, REQUEST_SAVE);
            } else if (item.id == R.string.game_menu_load) {
                Intent i = new Intent(this, SlotSelectionActivity.class);
                i.putExtra(SlotSelectionActivity.EXTRA_GAME, game);
                i.putExtra(SlotSelectionActivity.EXTRA_BASE_DIRECTORY, baseDir);
                i.putExtra(SlotSelectionActivity.EXTRA_DIALOG_TYPE_INT,
                        SlotSelectionActivity.DIALOAG_TYPE_LOAD);
                freeStartActivityForResult(this, i, REQUEST_LOAD);
            } else if (item.id == R.string.game_menu_cheats) {
                Intent i = new Intent(this, CheatsActivity.class);
                i.putExtra(CheatsActivity.EXTRA_IN_GAME_HASH, game.checksum);
                freeStartActivity(this, i);
            } else if (item.id == R.string.game_menu_settings) {
                Intent i = new Intent(this, GamePreferenceActivity.class);
                i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                        GamePreferenceFragment.class.getName());
                i.putExtra(GamePreferenceActivity.EXTRA_GAME, game);
                startActivity(i);
            } else if (item.id == R.string.gallery_menu_pref) {
                Intent i = new Intent(this, GeneralPreferenceActivity.class);
                i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                        GeneralPreferenceFragment.class.getName());
                startActivity(i);
            } else if (item.id == R.string.game_menu_screenshot) {
                saveScreenshotWithPermission();
            }
        } catch (EmulatorException e) {
            handleException(e);
        }
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

    private void saveScreenshotWithPermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE)
                .callback(new PermissionUtils.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        saveGameScreenshot();
                    }

                    @Override
                    public void onDenied() {

                    }
                }).request();
    }

    private void saveGameScreenshot() {
        String name = game.getCleanName() + "-screenshot";
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                EmulatorHolder.getInfo().getName().replace(' ', '_'));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File to = dir;
        int counter = 0;
        while (to.exists()) {
            String nn = name + (counter == 0 ? "" : "(" + counter + ")") + ".png";
            to = new File(dir, nn);
            counter++;
        }
        try {
            FileOutputStream fos = new FileOutputStream(to);
            EmuUtils.createScreenshotBitmap(EmulatorActivity.this, game)
                    .compress(CompressFormat.PNG, 90, fos);
            fos.close();
            Toast.makeText(EmulatorActivity.this,
                    getString(R.string.act_game_screenshot_saved,
                            to.getAbsolutePath()), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            NLog.e(TAG, "", e);
            throw new EmulatorException(getString(R.string.act_game_screenshot_failed));
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
