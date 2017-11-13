package nostalgia.framework;

import android.view.View;

import nostalgia.framework.ui.gamegallery.GameDescription;

public interface EmulatorController {

    int KEY_A = 0;
    int KEY_B = 1;
    int KEY_A_TURBO = 255;
    int KEY_B_TURBO = 256;
    int KEY_X = 2;
    int KEY_Y = 3;
    int KEY_START = 4;
    int KEY_SELECT = 5;
    int KEY_UP = 6;
    int KEY_DOWN = 7;
    int KEY_LEFT = 8;
    int KEY_RIGHT = 9;
    int ACTION_DOWN = 0;
    int ACTION_UP = 1;

    void onResume();

    void onPause();

    void onWindowFocusChanged(boolean hasFocus);

    void onGameStarted(GameDescription game);

    void onGamePaused(GameDescription game);

    void connectToEmulator(int port, Emulator emulator);

    View getView();

    void onDestroy();

}
