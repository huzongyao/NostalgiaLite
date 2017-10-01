package nostalgia.framework.remote;

public interface ControllerEventSource {

    void setControllerEventListener(OnControllerEventListener listener);

    void onResume();

    void onPause();

}
