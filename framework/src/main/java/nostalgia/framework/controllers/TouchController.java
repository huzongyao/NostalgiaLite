package nostalgia.framework.controllers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.util.Map;

import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.GfxProfile;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.multitouchbutton.MultitouchBtnInterface;
import nostalgia.framework.ui.multitouchbutton.MultitouchButton;
import nostalgia.framework.ui.multitouchbutton.MultitouchImageButton;
import nostalgia.framework.ui.multitouchbutton.MultitouchLayer;
import nostalgia.framework.ui.multitouchbutton.OnMultitouchEventListener;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.Utils;

public class TouchController implements EmulatorController,
        OnMultitouchEventListener {

    private static final String TAG = "controllers.TouchController";
    Emulator emulator;
    EmulatorActivity emulatorActivity;
    int port;
    Map<Integer, Integer> mapping;
    SparseIntArray resIdMapping = new SparseIntArray();
    MultitouchLayer multitouchLayer;
    ImageView remoteIc, zapperIc, palIc, ntscIc, muteIc;
    View view;
    MultitouchImageButton aTurbo, bTurbo, abButton, fastForward;
    @SuppressLint("HandlerLeak")
    Handler keyHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            emulator.setKeyPressed(port, msg.what, false);
        }
    };
    private boolean hidden = false;


    public TouchController(EmulatorActivity emulatorActivity) {
        this.emulatorActivity = emulatorActivity;
    }

    public void onResume() {
        if (multitouchLayer != null) {
            multitouchLayer.setVibrationDuration(PreferenceUtil
                    .getVibrationDuration(emulatorActivity));
        }

        emulator.resetKeys();
        multitouchLayer.reloadTouchProfile();
        multitouchLayer.setOpacity(PreferenceUtil
                .getControlsOpacity(emulatorActivity));
        multitouchLayer.setEnableStaticDPAD(!PreferenceUtil
                .isDynamicDPADEnable(emulatorActivity));
    }

    public void onPause() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    public void onDestroy() {
        multitouchLayer = null;
        emulatorActivity = null;
    }

    @Override
    public void connectToEmulator(int port, Emulator emulator) {
        this.emulator = emulator;
        this.port = port;
        mapping = emulator.getInfo().getKeyMapping();
    }

    boolean isPointerHandled(int pointerId) {
        return multitouchLayer.isPointerHandled(pointerId);
    }

    private View createView() {
        LayoutInflater inflater = (LayoutInflater) emulatorActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.controler_layout, null);
        multitouchLayer = (MultitouchLayer) layout
                .findViewById(R.id.touch_layer);
        MultitouchImageButton up = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_up);
        up.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_up, mapping.get(EmulatorController.KEY_UP));
        MultitouchImageButton down = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_down);
        down.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_down,
                mapping.get(EmulatorController.KEY_DOWN));
        MultitouchImageButton left = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_left);
        left.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_left,
                mapping.get(EmulatorController.KEY_LEFT));
        MultitouchImageButton right = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_right);
        right.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_right,
                mapping.get(EmulatorController.KEY_RIGHT));
        MultitouchImageButton a = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_a);
        a.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_a, mapping.get(EmulatorController.KEY_A));
        MultitouchImageButton b = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_b);
        b.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_b, mapping.get(EmulatorController.KEY_B));
        aTurbo = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_a_turbo);
        aTurbo.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_a_turbo,
                mapping.get(EmulatorController.KEY_A_TURBO));
        bTurbo = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_b_turbo);
        bTurbo.setOnMultitouchEventlistener(this);
        resIdMapping.put(R.id.button_b_turbo,
                mapping.get(EmulatorController.KEY_B_TURBO));
        abButton = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_ab);
        fastForward = (MultitouchImageButton) multitouchLayer
                .findViewById(R.id.button_fast_forward);
        fastForward
                .setOnMultitouchEventlistener(new OnMultitouchEventListener() {
                    @Override
                    public void onMultitouchExit(MultitouchBtnInterface btn) {
                        emulatorActivity.onFastForwardUp();
                    }

                    @Override
                    public void onMultitouchEnter(MultitouchBtnInterface btn) {
                        emulatorActivity.onFastForwardDown();
                    }
                });
        MultitouchButton select = (MultitouchButton) layout
                .findViewById(R.id.button_select);

        if (select != null) {
            select.setOnMultitouchEventlistener(new OnMultitouchEventListener() {
                @Override
                public void onMultitouchExit(MultitouchBtnInterface btn) {
                }

                @Override
                public void onMultitouchEnter(MultitouchBtnInterface btn) {
                    sendKey(EmulatorController.KEY_SELECT);
                }
            });
        }

        MultitouchButton start = (MultitouchButton) layout
                .findViewById(R.id.button_start);
        start.setOnMultitouchEventlistener(new OnMultitouchEventListener() {
            @Override
            public void onMultitouchExit(MultitouchBtnInterface btn) {
            }

            @Override
            public void onMultitouchEnter(MultitouchBtnInterface btn) {
                sendKey(EmulatorController.KEY_START);
            }
        });
        MultitouchImageButton menu = (MultitouchImageButton) layout
                .findViewById(R.id.button_menu);
        menu.setOnMultitouchEventlistener(new OnMultitouchEventListener() {
            @Override
            public void onMultitouchExit(MultitouchBtnInterface btn) {
            }

            @Override
            public void onMultitouchEnter(MultitouchBtnInterface btn) {
                emulatorActivity.openGameMenu();
            }
        });
        View center = layout.findViewById(R.id.button_center);
        View[] views = new View[]{menu, select, start, up, down, right, left,
                a, b, center
        };

        for (View view : views) {
            if (view != null) {
                view.setFocusable(false);
            }
        }

        remoteIc = (ImageView) layout.findViewById(R.id.ic_game_remote);
        zapperIc = (ImageView) layout.findViewById(R.id.ic_game_zapper);
        palIc = (ImageView) layout.findViewById(R.id.ic_game_pal);
        ntscIc = (ImageView) layout.findViewById(R.id.ic_game_ntsc);
        muteIc = (ImageView) layout.findViewById(R.id.ic_game_muted);
        return layout;
    }

    @Override
    public View getView() {
        if (view == null) {
            view = createView();
        }

        return view;
    }

    public void setStaticDPadEnabled(boolean enabled) {
        if (multitouchLayer != null) {
            multitouchLayer.setEnableStaticDPAD(enabled);
        }
    }

    private void sendKey(int code) {
        int cc = mapping.get(code);
        emulator.setKeyPressed(port, cc, true);
        keyHandler.sendEmptyMessageDelayed(cc, 200);
    }

    @Override
    public void onMultitouchEnter(MultitouchBtnInterface btn) {
        emulator.setKeyPressed(port, resIdMapping.get(btn.getId()), true);
    }

    @Override
    public void onMultitouchExit(MultitouchBtnInterface btn) {
        emulator.setKeyPressed(port, resIdMapping.get(btn.getId()), false);
    }

    @Override
    public void onGameStarted(GameDescription game) {
        GfxProfile gfxProfile = emulator.getActiveGfxProfile();
        zapperIc.setVisibility(PreferenceUtil.isZapperEnabled(emulatorActivity,
                game.checksum) ? View.VISIBLE : View.GONE);
        palIc.setVisibility(gfxProfile.name.equals("PAL") ? View.VISIBLE
                : View.GONE);
        ntscIc.setVisibility(gfxProfile.name.equals("NTSC") ? View.VISIBLE
                : View.GONE);
        boolean remoteVisible = PreferenceUtil
                .isWifiServerEnable(emulatorActivity)
                && Utils.isWifiAvailable(emulatorActivity);
        remoteIc.setVisibility(remoteVisible ? View.VISIBLE : View.INVISIBLE);
        muteIc.setVisibility(PreferenceUtil.isSoundEnabled(emulatorActivity) ? View.GONE
                : View.VISIBLE);

        if (PreferenceUtil.isTurboEnabled(emulatorActivity)) {
            aTurbo.setVisibility(View.VISIBLE);
            bTurbo.setVisibility(View.VISIBLE);
            aTurbo.setEnabled(true);
            bTurbo.setEnabled(true);

        } else {
            aTurbo.setVisibility(View.INVISIBLE);
            bTurbo.setVisibility(View.INVISIBLE);
            aTurbo.setEnabled(false);
            bTurbo.setEnabled(false);
        }

        if (PreferenceUtil.isFastForwardEnabled(emulatorActivity)) {
            fastForward.setVisibility(View.VISIBLE);
            fastForward.setEnabled(true);

        } else {
            fastForward.setVisibility(View.INVISIBLE);
            fastForward.setEnabled(false);
        }

        abButton.setVisibility(PreferenceUtil
                .isABButtonEnabled(emulatorActivity) ? View.VISIBLE
                : View.INVISIBLE);
        abButton.setEnabled(PreferenceUtil.isABButtonEnabled(emulatorActivity) ? true
                : false);
        multitouchLayer.invalidate();
    }

    @Override
    public void onGamePaused(GameDescription game) {
    }

    public void hide() {
        if (!hidden) {
            emulatorActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(View.GONE);
                }
            });
            hidden = true;
        }
    }

    public void show() {
        if (hidden) {
            emulatorActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(View.VISIBLE);
                }
            });
            hidden = false;
        }
    }
}
