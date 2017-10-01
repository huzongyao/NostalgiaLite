package nostalgia.framework.remote;

import android.view.KeyEvent;

public interface OnControllerEventListener {
    void onControllerEmulatorKeyEvent(ControllerKeyEvent event);

    void onControllerAndroidKeyEvent(KeyEvent event);

    void onControllerTextEvent(String text);

    void onControllerCommandEvent(int command, int param0, int param1);

}
