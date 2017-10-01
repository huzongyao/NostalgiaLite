package nostalgia.framework.controllers;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.View;

import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.KeyboardProfile;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.utils.NLog;

public class KeyboardController implements EmulatorController {

    public static final int PLAYER2_OFFSET = 100000;
    public static final int KEY_XPERIA_CIRCLE = 2068987562;
    public static final int KEY_MENU = 902;
    public static final int KEY_BACK = 900;
    public static final int KEY_RESET = 901;
    public static final int KEY_FAST_FORWARD = 903;
    public static final int KEY_SAVE_SLOT_0 = 904;
    public static final int KEY_LOAD_SLOT_0 = 905;
    public static final int KEY_SAVE_SLOT_1 = 906;
    public static final int KEY_LOAD_SLOT_1 = 907;
    public static final int KEY_SAVE_SLOT_2 = 908;
    public static final int KEY_LOAD_SLOT_2 = 909;

    private static final String TAG = "controller.KeyboardController";
    public static int KEYS_RIGHT_AND_UP = keysToMultiCode(EmulatorController.KEY_RIGHT, EmulatorController.KEY_UP);
    public static int KEYS_RIGHT_AND_DOWN = keysToMultiCode(EmulatorController.KEY_RIGHT, EmulatorController.KEY_DOWN);
    public static int KEYS_LEFT_AND_DOWN = keysToMultiCode(EmulatorController.KEY_LEFT, EmulatorController.KEY_DOWN);
    public static int KEYS_LEFT_AND_UP = keysToMultiCode(EmulatorController.KEY_LEFT, EmulatorController.KEY_UP);

    String gameHash;
    private int[] tmpKeys = new int[2];
    private boolean[] loadingOrSaving = new boolean[4];
    private EmulatorActivity emulatorActivity;
    private KeyboardProfile profile;
    private Emulator emulator;
    private SparseIntArray keyMapping;
    private Context context;

    public KeyboardController(Emulator emulator, Context context, String gameHash, EmulatorActivity activity) {
        this.context = context;
        this.gameHash = gameHash;
        this.emulatorActivity = activity;
        this.emulator = emulator;
        this.keyMapping = emulator.getInfo().getKeyMapping();
    }

    private static int keysToMultiCode(int key1, int key2) {
        return key1 * 1000 + key2 + 10000;
    }

    private static boolean isMulti(int mapValue) {
        return mapValue >= 10000;
    }

    @Override
    public void onResume() {
        profile = KeyboardProfile.getSelectedProfile(gameHash, context);
        emulator.resetKeys();
        for (int i = 0; i < loadingOrSaving.length; i++) {
            loadingOrSaving[i] = false;
        }
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    @Override
    public void onGameStarted(GameDescription game) {
    }

    @Override
    public void onGamePaused(GameDescription game) {
    }

    @Override
    public void connectToEmulator(int port, Emulator emulator) {
        throw new UnsupportedOperationException();
    }

    private void multiToKeys(int mapValue, int[] keys) {
        mapValue -= 10000;
        int key1 = mapValue / 1000;
        mapValue -= (key1 * 1000);
        int key2 = mapValue;
        keys[0] = key1;
        keys[1] = key2;
    }

    @Override
    public View getView() {
        return new View(context) {
            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                int mapValue;
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed()) {
                        keyCode = KEY_XPERIA_CIRCLE;
                    }
                }
                if (profile != null && (mapValue = profile.keyMap.get(keyCode, -1)) != -1) {
                    processKey(mapValue, true);
                    return true;
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            }

            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                int mapValue;

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed()) {
                        keyCode = KEY_XPERIA_CIRCLE;
                    }
                }
                if (profile != null && (mapValue = profile.keyMap.get(keyCode, -1)) != -1) {
                    processKey(mapValue, false);
                    emulatorActivity.hideTouchController();
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            }
        };
    }

    public void processKey(int mapValue, boolean pressed) {
        NLog.i(TAG, "process key " + mapValue);
        int port = 0;

        if (mapValue >= PLAYER2_OFFSET) {
            mapValue -= PLAYER2_OFFSET;
            port = 1;
        }

        if (mapValue == KEY_BACK) {
            if (pressed) {
                emulatorActivity.finish();
            }
        } else if (mapValue == KEY_SAVE_SLOT_0) {
            save(1, pressed);

        } else if (mapValue == KEY_SAVE_SLOT_1) {
            save(2, pressed);

        } else if (mapValue == KEY_SAVE_SLOT_2) {
            save(3, pressed);

        } else if (mapValue == KEY_LOAD_SLOT_0) {
            load(1, pressed);

        } else if (mapValue == KEY_LOAD_SLOT_1) {
            load(2, pressed);

        } else if (mapValue == KEY_LOAD_SLOT_2) {
            load(3, pressed);
        } else if (mapValue == KEY_FAST_FORWARD) {
            if (pressed) {
                emulatorActivity.onFastForwardDown();
            } else {
                emulatorActivity.onFastForwardUp();
            }
        } else if (mapValue == KEY_MENU) {
            if (pressed) {
                emulatorActivity.openGameMenu();
            }

        } else if (isMulti(mapValue)) {
            multiToKeys(mapValue, tmpKeys);
            emulator.setKeyPressed(port, keyMapping.get(tmpKeys[0]), pressed);
            emulator.setKeyPressed(port, keyMapping.get(tmpKeys[1]), pressed);

        } else {
            NLog.i(TAG, "process key " + mapValue + " " + keyMapping);
            int value = keyMapping.get(mapValue);
            emulator.setKeyPressed(port, value, pressed);
        }
    }

    private void save(int slot, boolean isKeyPressed) {
        if (isKeyPressed && !loadingOrSaving[slot]) {
            loadingOrSaving[slot] = true;
            emulatorActivity.getManager().saveState(slot);
        }
        if (!isKeyPressed) {
            loadingOrSaving[slot] = false;
        }
    }

    private void load(int slot, boolean isKeyPressed) {
        if (isKeyPressed && !loadingOrSaving[slot]) {
            loadingOrSaving[slot] = true;
            emulatorActivity.getManager().loadState(slot);
        }
        if (!isKeyPressed) {
            loadingOrSaving[slot] = false;
        }
    }

    @Override
    public void onDestroy() {
        context = null;
        emulatorActivity = null;
    }

}
