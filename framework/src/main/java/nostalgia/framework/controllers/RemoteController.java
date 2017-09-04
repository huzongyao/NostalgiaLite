package nostalgia.framework.controllers;

import android.os.PowerManager.WakeLock;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.remote.ControllerEventSource;
import nostalgia.framework.remote.ControllerKeyEvent;
import nostalgia.framework.remote.OnControllerEventListener;
import nostalgia.framework.remote.wifi.WifiControllerServer;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.preferences.PreferenceUtil;

public class RemoteController implements EmulatorController {


    public static final int KEY_MENU = 11;
    public static final int KEY_BACK = 10;
    private static Dispatcher eventDispatcher = new Dispatcher();
    InputConnection inputConnection;
    WakeLock wakeLock;
    OnRemoteControllerWarningListener warningListener = null;
    EmulatorActivity emulatorActivity;
    private int port;
    private ControllerEventSource server;
    private Emulator emulator;
    private SparseIntArray mapping;
    private SparseIntArray systemKeyMapping;
    private boolean zapperWarningShow = false;
    private HashSet<Integer> specialKeys = new HashSet<Integer>();

    public RemoteController(EmulatorActivity activity) {
        start();
        inputConnection = new BaseInputConnection(activity.getWindow().getDecorView(), false);
        systemKeyMapping = new SparseIntArray();
        systemKeyMapping.put(RemoteController.KEY_MENU, KeyEvent.KEYCODE_MENU);
        systemKeyMapping.put(RemoteController.KEY_BACK, KeyEvent.KEYCODE_BACK);
        emulatorActivity = activity;
    }

    @Override
    public void onResume() {
        start();
        emulator.resetKeys();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    private void start() {
        server = WifiControllerServer.getInstance(null);
        server.setControllerEventListener(eventDispatcher);
        eventDispatcher.addController(this);

        if (this == eventDispatcher.getController(0)) {
            server.onResume();
        }

        zapperWarningShow = false;
    }

    @Override
    public void onPause() {
        server.onPause();
        eventDispatcher.removeController(this);
    }

    @Override
    public void connectToEmulator(int port, Emulator emulator) {
        this.emulator = emulator;
        this.port = port;
        mapping = emulator.getInfo().getKeyMapping();
    }

    @Override
    public View getView() {
        return null;
    }

    public void setOnRemoteControllerWarningListener(
            OnRemoteControllerWarningListener list) {
        warningListener = list;
    }

    @Override
    public void onDestroy() {
        emulator = null;
        emulatorActivity = null;
    }

    private void processEmulatorKeyEvent(ControllerKeyEvent event) {
        if (event.port == this.port) {
            emulator.setKeyPressed(event.port, mapping.get(event.keyCode),
                    event.action == EmulatorController.ACTION_DOWN);
            emulatorActivity.hideTouchController();
        }

        if (event.port != 0 && warningListener != null
                && zapperWarningShow == false) {
            String hash = emulator.getLoadedGame().md5;

            if (PreferenceUtil.isZapperEnabled(emulatorActivity, hash)) {
                warningListener.onZapperCollision();
            }

            zapperWarningShow = true;
        }
    }

    private void processAndroidKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && !specialKeys.contains(event.getKeyCode())) {
            specialKeys.add(event.getKeyCode());
            inputConnection.sendKeyEvent(event);

        } else if (event.getAction() == KeyEvent.ACTION_UP
                && specialKeys.contains(event.getKeyCode())) {
            specialKeys.remove(event.getKeyCode());
            inputConnection.sendKeyEvent(event);
        }
    }

    private void processTextEvent(String text) {
    }

    private void processCommandEvent(int command, int param0, int param1) {
    }

    @Override
    public void onGameStarted(GameDescription game) {
    }

    @Override
    public void onGamePaused(GameDescription game) {
    }

    public interface OnRemoteControllerWarningListener {
        public void onZapperCollision();
    }

    private static class Dispatcher implements OnControllerEventListener {

        private List<RemoteController> controllers = new ArrayList<RemoteController>();

        @Override
        public void onControllerEmulatorKeyEvent(ControllerKeyEvent event) {
            for (RemoteController controller : controllers) {
                controller.processEmulatorKeyEvent(event);
            }
        }

        @Override
        public void onControllerAndroidKeyEvent(KeyEvent event) {
            for (RemoteController controller : controllers) {
                controller.processAndroidKeyEvent(event);
            }
        }

        @Override
        public void onControllerTextEvent(String text) {
            for (RemoteController controller : controllers) {
                controller.processTextEvent(text);
            }
        }

        @Override
        public void onControllerCommandEvent(int command, int param0, int param1) {
            for (RemoteController controller : controllers) {
                controller.processCommandEvent(command, param0, param1);
            }
        }

        public void addController(RemoteController rc) {
            if (!controllers.contains(rc)) {
                controllers.add(rc);
            }
        }

        public void removeController(RemoteController rc) {
            if (controllers.contains(rc)) {
                controllers.remove(rc);
            }
        }

        public EmulatorController getController(int i) {
            return controllers.get(i);
        }

    }

}
