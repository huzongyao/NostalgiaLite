package nostalgia.framework.remote;

import android.view.KeyEvent;

public interface OnControllerEventListener {
    public void onControllerEmulatorKeyEvent(ControllerKeyEvent event);

    public void onControllerAndroidKeyEvent(KeyEvent event);

    public void onControllerTextEvent(String text);

    public void onControllerCommandEvent(int command, int param0, int param1);

}
