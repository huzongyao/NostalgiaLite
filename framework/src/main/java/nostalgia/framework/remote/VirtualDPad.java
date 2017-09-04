package nostalgia.framework.remote;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.Window;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import nostalgia.framework.EmulatorController;
import nostalgia.framework.remote.wifi.WifiControllerServer;
import nostalgia.framework.utils.NLog;

public class VirtualDPad implements OnControllerEventListener {
    private static final String TAG = "remote.VirtualDPad";
    private static VirtualDPad instance = new VirtualDPad();
    public long downTime;
    private Timer timer;
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, TimerTask> longPresHandlers = new HashMap<>();
    private InputConnection connection;
    private ControllerEventSource server;
    private HashSet<OnVirtualDPEventsListener> textListeners = new HashSet<>();

    private VirtualDPad() {
    }

    public static VirtualDPad getInstance() {
        return instance;
    }

    public void attachToWindow(Window window) {
        server = WifiControllerServer.getInstance(null);
        server.setControllerEventListener(this);
        setInputConnection(new BaseInputConnection(window.getDecorView(), false));
    }

    public void detachFromWindow() {
        if (timer != null) {
            timer.cancel();
        }
        setInputConnection(null);
    }

    public void onResume(Window window) {
        attachToWindow(window);
        onResume();
    }

    public void onResume() {
        server.onResume();
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
    }

    public void onPause() {
        server.onPause();
        timer.cancel();
        longPresHandlers.clear();
    }

    @Override
    public void onControllerEmulatorKeyEvent(final ControllerKeyEvent event) {
        if (connection == null) {
            return;
        }

        boolean pressJustOnce = false;
        int kc = -1;

        switch (event.keyCode) {
            case EmulatorController.KEY_LEFT:
                kc = KeyEvent.KEYCODE_DPAD_LEFT;
                break;
            case EmulatorController.KEY_RIGHT:
                kc = KeyEvent.KEYCODE_DPAD_RIGHT;
                break;
            case EmulatorController.KEY_UP:
                kc = KeyEvent.KEYCODE_DPAD_UP;
                break;
            case EmulatorController.KEY_DOWN:
                kc = KeyEvent.KEYCODE_DPAD_DOWN;
                break;
            case EmulatorController.KEY_START:
                kc = KeyEvent.KEYCODE_DPAD_CENTER;
                break;
            case EmulatorController.KEY_A:
                kc = KeyEvent.KEYCODE_DPAD_CENTER;
                break;
            case EmulatorController.KEY_B:
                kc = KeyEvent.KEYCODE_DPAD_CENTER;
                break;
            default:
                return;
        }

        int action = event.action == EmulatorController.ACTION_DOWN ?
                KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
        final KeyEvent keyEvent = new KeyEvent(action, kc);

        if (action == EmulatorController.ACTION_DOWN) {
            if (!pressJustOnce) {
                TimerTask task = longPresHandlers.get(event.keyCode);
                if (task == null) {
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            sendKeyEvent(keyEvent);
                        }
                    };
                    longPresHandlers.put(event.keyCode, task);
                    timer.schedule(task, 800, 100);
                }
            }
            sendKeyEvent(keyEvent);
        } else {
            if (!pressJustOnce) {
                longPresHandlers.get(event.keyCode).cancel();
                longPresHandlers.put(event.keyCode, null);
            }
            sendKeyEvent(keyEvent);
        }
    }

    private void sendKeyEvent(KeyEvent event) {
        if (connection != null) {
            connection.sendKeyEvent(event);
        }
    }

    @Override
    public void onControllerAndroidKeyEvent(KeyEvent event) {
        NLog.i(TAG, "android event:" + event);
        sendKeyEvent(event);
    }

    @Override
    public void onControllerTextEvent(String text) {
        for (OnVirtualDPEventsListener listener : textListeners) {
            listener.onVirtualDPadTextEvent(text);
        }
    }

    @Override
    public void onControllerCommandEvent(int command, int param0, int param1) {
        for (OnVirtualDPEventsListener listener : textListeners) {
            NLog.i(TAG, "command " + command + " " + param0 + " " + param1);
            listener.onVirtualDPadCommandEvent(command, param0, param1);
        }
    }

    private void setInputConnection(InputConnection connection) {
        this.connection = connection;
    }

    public void addOnTextChangeListener(OnVirtualDPEventsListener listener) {
        textListeners.add(listener);
    }

    public void removeOnTextChangeListener(OnVirtualDPEventsListener listener) {
        textListeners.remove(listener);
    }

    public interface OnVirtualDPEventsListener {
        void onVirtualDPadTextEvent(String text);

        void onVirtualDPadCommandEvent(int command, int param0, int param1);
    }

}
