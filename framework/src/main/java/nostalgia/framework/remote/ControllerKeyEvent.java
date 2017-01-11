package nostalgia.framework.remote;

public class ControllerKeyEvent {
    public int keyCode;
    public int action;
    public int port;

    @Override
    public String toString() {
        return "keycode:" + keyCode + " action:" + action + " port:" + port;
    }
}
