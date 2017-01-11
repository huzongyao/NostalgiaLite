package nostalgia.framework;

import android.view.View;

import nostalgia.framework.ui.gamegallery.GameDescription;

public interface EmulatorController {

    public static final int KEY_A = 0;
    public static final int KEY_B = 1;
    public static final int KEY_A_TURBO = 255;
    public static final int KEY_B_TURBO = 256;
    public static final int KEY_X = 2;
    public static final int KEY_Y = 3;
    public static final int KEY_START = 4;
    public static final int KEY_SELECT = 5;
    public static final int KEY_UP = 6;
    public static final int KEY_DOWN = 7;
    public static final int KEY_LEFT = 8;
    public static final int KEY_RIGHT = 9;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;

    void onResume();

    void onPause();

    void onWindowFocusChanged(boolean hasFocus);

    void onGameStarted(GameDescription game);

    void onGamePaused(GameDescription game);

    void connectToEmulator(int port, Emulator emulator);

    View getView();

    void onDestroy();

}
