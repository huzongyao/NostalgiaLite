package nostalgia.framework.remote;

public interface ControllerEventSource {

    public void setControllerEventListener(OnControllerEventListener listener);

    public void onResume();

    public void onPause();

}
