package nostalgia.framework.ui.remotecontroller;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;

import nostalgia.framework.EmulatorController;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorHolder;
import nostalgia.framework.controllers.RemoteController;
import nostalgia.framework.remote.wifi.WifiControllerClient;
import nostalgia.framework.remote.wifi.WifiServerInfoReceiver;
import nostalgia.framework.remote.wifi.WifiServerInfoReceiver.BroadcastReceiverListener;
import nostalgia.framework.remote.wifi.WifiServerInfoReceiver.DetectionResult;
import nostalgia.framework.ui.gamegallery.GalleryActivity;
import nostalgia.framework.ui.multitouchbutton.MultitouchBtnInterface;
import nostalgia.framework.ui.multitouchbutton.MultitouchImageButton;
import nostalgia.framework.ui.multitouchbutton.MultitouchLayer;
import nostalgia.framework.ui.multitouchbutton.OnMultitouchEventListener;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.ActivitySwitcher;
import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.Utils;

public class RemoteControllerActivity extends Activity {

    private static final String TAG = "RemoteControllerActivity";

    SparseIntArray resToKeyCode = new SparseIntArray();
    TextView portIndicator;
    EditText searchBox;
    boolean searchMode = false;
    String ip = "";
    int port = 0;
    WifiServerInfoReceiver broadcastReceiverService = new WifiServerInfoReceiver();
    private WifiControllerClient client;
    OnMultitouchEventListener emulatorKeysListener = new OnMultitouchEventListener() {
        @Override
        public void onMultitouchEnter(MultitouchBtnInterface btn) {
            if (client != null) {
                client.sendControllerEmulatorKeyEvent(1,
                        resToKeyCode.get(btn.getId()));
            }
        }

        @Override
        public void onMultitouchExit(MultitouchBtnInterface btn) {
            if (client != null)
                client.sendControllerEmulatorKeyEvent(0,
                        resToKeyCode.get(btn.getId()));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_remote_controller);
        resToKeyCode.put(R.id.button_left, EmulatorController.KEY_LEFT);
        resToKeyCode.put(R.id.button_right, EmulatorController.KEY_RIGHT);
        resToKeyCode.put(R.id.button_up, EmulatorController.KEY_UP);
        resToKeyCode.put(R.id.button_down, EmulatorController.KEY_DOWN);
        resToKeyCode.put(R.id.button_select, EmulatorController.KEY_SELECT);
        resToKeyCode.put(R.id.button_start, EmulatorController.KEY_START);
        resToKeyCode.put(R.id.button_a, EmulatorController.KEY_A);
        resToKeyCode.put(R.id.button_b, EmulatorController.KEY_B);
        resToKeyCode.put(R.id.button_back, RemoteController.KEY_BACK);
        resToKeyCode.put(R.id.button_menu, RemoteController.KEY_MENU);
        MultitouchLayer mtl = (MultitouchLayer) findViewById(R.id.act_remote_mtl);
        mtl.disableLoadSettings();
        mtl.setVibrationDuration(PreferenceUtil.getVibrationDuration(this));
        MultitouchImageButton up = (MultitouchImageButton) findViewById(R.id.button_up);
        up.setOnMultitouchEventlistener(emulatorKeysListener);
        MultitouchImageButton down = (MultitouchImageButton) findViewById(R.id.button_down);
        down.setOnMultitouchEventlistener(emulatorKeysListener);
        MultitouchImageButton left = (MultitouchImageButton) findViewById(R.id.button_left);
        left.setOnMultitouchEventlistener(emulatorKeysListener);
        MultitouchImageButton right = (MultitouchImageButton) findViewById(R.id.button_right);
        right.setOnMultitouchEventlistener(emulatorKeysListener);
        MultitouchImageButton a = (MultitouchImageButton) findViewById(R.id.button_a);
        a.setOnMultitouchEventlistener(emulatorKeysListener);
        MultitouchImageButton b = (MultitouchImageButton) findViewById(R.id.button_b);
        b.setOnMultitouchEventlistener(emulatorKeysListener);
        MultitouchImageButton select = (MultitouchImageButton) findViewById(R.id.button_select);

        if (select != null) {
            select.setOnMultitouchEventlistener(emulatorKeysListener);
        }

        MultitouchImageButton start = (MultitouchImageButton) findViewById(R.id.button_start);
        start.setOnMultitouchEventlistener(emulatorKeysListener);
        MultitouchImageButton back = (MultitouchImageButton) findViewById(R.id.button_back);
        back.setOnMultitouchEventlistener(new OnMultitouchEventListener() {
            @Override
            public void onMultitouchEnter(MultitouchBtnInterface btn) {
                if (client != null) {
                    client.sendControllerAndroidKeyEvent(new KeyEvent(
                            KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                }
            }

            @Override
            public void onMultitouchExit(MultitouchBtnInterface btn) {
                if (client != null)
                    client.sendControllerAndroidKeyEvent(new KeyEvent(
                            KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            }
        });
        MultitouchImageButton menu = (MultitouchImageButton) findViewById(R.id.button_menu);
        menu.setOnMultitouchEventlistener(new OnMultitouchEventListener() {
            @Override
            public void onMultitouchEnter(MultitouchBtnInterface btn) {
                if (client != null) {
                    client.sendControllerAndroidKeyEvent(new KeyEvent(
                            KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU));
                }
            }

            @Override
            public void onMultitouchExit(MultitouchBtnInterface btn) {
                if (client != null)
                    client.sendControllerAndroidKeyEvent(new KeyEvent(
                            KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU));
            }
        });
        MultitouchImageButton search = (MultitouchImageButton) findViewById(R.id.button_search);
        search.setOnMultitouchEventlistener(new OnMultitouchEventListener() {
            @Override
            public void onMultitouchExit(MultitouchBtnInterface btn) {
            }

            @Override
            public void onMultitouchEnter(MultitouchBtnInterface btn) {
                ((View) searchBox.getParent()).setVisibility(View.VISIBLE);
                searchBox.requestFocus();
            }
        });
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        ip = prefs.getString("IP", "10.0.0.5");
        setPort(prefs.getInt("port", 0));
        ActivitySwitcher.animationIn(findViewById(R.id.root),
                getWindowManager());
        MultitouchImageButton connect = (MultitouchImageButton) findViewById(R.id.button_connect);
        connect.setOnMultitouchEventlistener(new OnMultitouchEventListener() {
            @Override
            public void onMultitouchExit(MultitouchBtnInterface btn) {
                openSelectServerDialog();
            }

            @Override
            public void onMultitouchEnter(MultitouchBtnInterface btn) {
            }
        });
        portIndicator = (TextView) findViewById(R.id.port_indicator);
        searchBox = (EditText) findViewById(R.id.search_editbox);
        searchBox.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, final boolean hasFocus) {
                searchBox.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) RemoteControllerActivity.this
                                .getSystemService(Context.INPUT_METHOD_SERVICE);

                        if (hasFocus) {
                            imm.showSoftInput(searchBox,
                                    InputMethodManager.SHOW_FORCED);

                        } else {
                            imm.hideSoftInputFromWindow(
                                    searchBox.getWindowToken(), 0);
                        }
                    }
                });

                if (!hasFocus) {
                    ((View) searchBox.getParent())
                            .setVisibility(View.INVISIBLE);
                }

                searchMode = hasFocus;

                if (client != null) {
                    client.sendControllerCommandEvent(
                            GalleryActivity.COMMAND_SEARCHMODE, hasFocus ? 1
                                    : 0, 0);
                }
            }
        });
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (client != null)
                    client.sendControllerTextEvent(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        openSelectServerDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (client != null) {
            client.onPause();
        }

        broadcastReceiverService.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (client != null) {
            client.onStop();
        }

        broadcastReceiverService.stop();
    }

    private void openSelectServerDialog() {
        final Dialog dialog = new Dialog(this, R.style.DialogTheme);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.dialog_select_server, null);
        final ListView listView = (ListView) content
                .findViewById(R.id.dialog_select_server_list);
        final ArrayList<DetectionResult> values = new ArrayList<DetectionResult>();
        final ServerSelectAdapter listAdapter = new ServerSelectAdapter(this,
                values);
        listView.setAdapter(listAdapter);
        dialog.setContentView(content);
        TextView title = (TextView) content
                .findViewById(R.id.dialog_select_server_title);
        Button cancel = (Button) content
                .findViewById(R.id.dialog_select_server_btn_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                broadcastReceiverService.stop();
                finish();
            }
        });
        Button manually = (Button) content
                .findViewById(R.id.dialog_select_server_btn_manually);
        manually.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openSelectIpDialog();
            }
        });
        dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                broadcastReceiverService.stop();
                finish();
            }
        });
        dialog.show();
        broadcastReceiverService.startExploring(RemoteControllerActivity.this,
                new BroadcastReceiverListener() {
                    @Override
                    public void onServerDetect(DetectionResult result) {
                        int pos = values.indexOf(result);

                        if (pos != -1) {
                            values.set(pos, result);
                            listAdapter.notifyDataSetInvalidated();

                        } else {
                            values.add(result);
                        }
                    }
                });
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                DetectionResult result = values.get(arg2);
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(RemoteControllerActivity.this);
                Editor editor = prefs.edit();
                NLog.i(TAG, result.address.getHostAddress());
                ip = result.address.getHostAddress();
                editor.putString("IP", result.address.getHostAddress());
                editor.apply();
                broadcastReceiverService.stop();
                dialog.dismiss();
                openPortDialog();
            }
        });
    }

    private void openSelectIpDialog() {
        final Dialog dialog = new Dialog(this, R.style.DialogTheme);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.dialog_manually_set_server,
                null);
        dialog.setContentView(content);
        final TextView prefix = (TextView) content
                .findViewById(R.id.dialog_manually_server_set_ip_prefix);
        final EditText input = (EditText) content
                .findViewById(R.id.dialog_manually_server_set);
        final Button okBtn = (Button) content
                .findViewById(R.id.dialog_select_server_btn_ok);
        final Button cancel = (Button) content
                .findViewById(R.id.dialog_select_server_btn_cancel);
        TextView title = (TextView) content
                .findViewById(R.id.dialog_select_server_title);
        String prefixS = Utils.getNetPrefix(this) + ".";
        prefix.setText(prefixS);
        String iptxt = ip;

        if (iptxt.startsWith(prefixS)) {
            iptxt = iptxt.replace(prefixS, "");

        } else {
            iptxt = "1";
        }

        input.setText(iptxt);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                try {
                    int num = Integer.parseInt(s.toString());

                    if (num > 0 && num < 256) {
                        input.setTextColor(0xffffffff);
                        okBtn.setEnabled(true);

                    } else {
                        input.setTextColor(0xffff0000);
                        okBtn.setEnabled(false);
                    }

                } catch (NumberFormatException e) {
                    input.setTextColor(0xffff0000);
                    okBtn.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                broadcastReceiverService.stop();
                finish();
            }
        });
        okBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(RemoteControllerActivity.this);
                Editor editor = prefs.edit();
                ip = prefix.getText().toString() + input.getText().toString();
                editor.putString("IP", ip);
                editor.apply();
                openPortDialog();
            }
        });
        dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                broadcastReceiverService.stop();
                finish();
            }
        });
        dialog.show();
    }

    private void openPortDialog() {
        if (EmulatorHolder.getInfo().isMultiPlayerSupported()) {
            final Dialog dialog = new Dialog(this, R.style.DialogTheme);
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View content = inflater.inflate(R.layout.dialog_select_port, null);
            dialog.setContentView(content);
            TextView title = (TextView) content
                    .findViewById(R.id.dialog_select_server_title);
            dialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    broadcastReceiverService.stop();
                    finish();
                }
            });
            dialog.show();
            int[] ids = new int[]{R.id.dialog_select_port_1,
                    R.id.dialog_select_port_2, R.id.dialog_select_port_3,
                    R.id.dialog_select_port_4
            };

            for (int i = 0; i < ids.length; i++) {
                Button b = (Button) content.findViewById(ids[i]);
                final int portIdx = i;
                b.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        start(portIdx);
                    }
                });
            }

        } else {
            start(0);
        }
    }

    private void start(int portIdx) {
        setPort(portIdx);
        portIndicator.setText((port + 1) + "");

        try {
            if (client != null) {
                client.onStop();
            }

            client = new WifiControllerClient(InetAddress.getByName(ip), port);
            client.onResume();

        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }
    }

    private void setPort(int port) {
        this.port = port;

        if (client != null) {
        }
    }

    @Override
    public void finish() {
        ActivitySwitcher.animationOut(findViewById(R.id.root),
                getWindowManager(),
                new ActivitySwitcher.AnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished() {
                        RemoteControllerActivity.super.finish();
                        overridePendingTransition(0, 0);
                    }
                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && searchMode) {
            ((View) searchBox.getParent()).setVisibility(View.INVISIBLE);
            return true;

        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

}
