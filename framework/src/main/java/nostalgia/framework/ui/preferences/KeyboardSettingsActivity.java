package nostalgia.framework.ui.preferences;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nostalgia.framework.KeyboardProfile;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorHolder;
import nostalgia.framework.controllers.KeyboardController;
import nostalgia.framework.utils.NLog;

public class KeyboardSettingsActivity extends AppCompatActivity
        implements OnItemClickListener {

    public static final String EXTRA_PROFILE_NAME = "EXTRA_PROFILE_NAME";
    public static final String EXTRA_NEW_BOOL = "EXTRA_NEW_BOOL";
    public static final int RESULT_NAME_CANCEL = 645943;
    private static final String TAG = "KeyboardSettingsActivity";
    private static SparseArray<String> NON_PRINTABLE_KEY_LABELS = new SparseArray<>();

    static {
        initNonPrintMap();
    }

    private ListView list = null;
    private KeyboardProfile profile;
    private SparseIntArray inverseMap = new SparseIntArray();
    private ArrayList<String> profilesNames;
    private Adapter adapter;
    private boolean newProfile = false;
    private boolean deleted = false;

    @SuppressLint("InlinedApi")
    private static void initNonPrintMap() {
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_ENTER, "Enter");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_SPACE, "Space");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_LEFT, "Left");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_RIGHT, "Right");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_UP, "Up");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_DOWN, "Down");
        NON_PRINTABLE_KEY_LABELS.put(KeyboardController.KEY_XPERIA_CIRCLE, "Circle");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_A, "A");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_B, "B");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_C, "C");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_X, "X");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_Y, "Y");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_Z, "Z");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_SELECT, "Select");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_START, "Start");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_MODE, "MODE");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_THUMBL, "THUMBL");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_THUMBR, "THUMBR");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_1, "1");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_2, "2");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_3, "3");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_4, "4");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_5, "5");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_6, "6");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_7, "7");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_8, "8");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_9, "9");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_10, "10");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_11, "11");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_12, "12");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_13, "13");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_14, "14");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_15, "15");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_16, "16");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_R1, "R1");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_R2, "R2");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_L1, "L1");
        NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_L2, "L2");
    }

    public static String getKeyLabel(int keyCode) {
        if (keyCode == 0) {
            return "";
        }
        String text = NON_PRINTABLE_KEY_LABELS.get(keyCode);
        if (text != null) {
            return text;
        } else {
            KeyEvent event = new KeyEvent(0, keyCode);
            char ch = (char) event.getUnicodeChar();
            if (ch != 0) {
                return ch + "";
            } else {
                return "key-" + keyCode;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_settings);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        profilesNames = KeyboardProfile.getProfilesNames(this);
        list = findViewById(R.id.act_keyboard_settings_list);
        profile = KeyboardProfile.load(this, getIntent().getStringExtra(EXTRA_PROFILE_NAME));
        inverseMap.clear();

        SparseIntArray keyMap = profile.keyMap;
        for (Integer code : KeyboardProfile.BUTTON_KEY_EVENT_CODES) {
            inverseMap.append(code, 0);
        }
        for (int i = 0; i < keyMap.size(); i++) {
            inverseMap.append(keyMap.valueAt(i), keyMap.keyAt(i));
        }
        if (getIntent().getBooleanExtra(EXTRA_NEW_BOOL, false)) {
            profile.name = "new profile";
            newProfile = true;
            showDialog(0);
        }

        setTitle(String.format(getText(R.string.key_profile_pref).toString(),
                profile.name));
        adapter = new Adapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        final PlayersLabelView plv = findViewById(R.id.act_keyboard_settings_plv);

        if (EmulatorHolder.getInfo().isMultiPlayerSupported()) {
            plv.setPlayersOffsets(adapter.getPlayersOffset());
            list.setOnScrollListener(new OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem,
                                     int visibleItemCount, int totalItemCount) {
                    View v = list.getChildAt(0);
                    if (v != null) {
                        int currentY = 0;
                        for (int i = 0; i < list.getFirstVisiblePosition(); i++) {
                            currentY += adapter.getRowHeight();
                        }
                        int scrollY = -list.getChildAt(0).getTop() + currentY;
                        plv.setOffset(scrollY);
                    }
                }
            });

        } else {
            plv.setVisibility(View.GONE);
        }
    }

    @Override
    @Deprecated
    protected Dialog onCreateDialog(int id) {
        Builder alertDialogBuilder = new Builder(this);
        final EditText editText = new EditText(this);
        editText.setHint("Insert profile name");
        editText.setPadding(10, 10, 10, 10);
        alertDialogBuilder.setView(editText);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id1) -> {
                    profile.name = editText.getText().toString();
                    setTitle(String.format(
                            getText(R.string.key_profile_pref).toString(),
                            profile.name));
                    Intent data = new Intent();
                    data.putExtra(EXTRA_PROFILE_NAME, profile.name);
                    setResult(RESULT_OK, data);
                })
                .setNegativeButton("Cancel",
                        (dialog, id12) -> {
                            dialog.cancel();
                            setResult(RESULT_NAME_CANCEL);
                            finish();
                        });
        final AlertDialog alertDialog = alertDialogBuilder.create();
        final Pattern pattern = Pattern.compile("[a-zA-Z0-9]");
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Button ok = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                String txt = s.toString();
                Matcher m = pattern.matcher(txt);

                if (!profilesNames.contains(txt) && !txt.equals("")
                        && m.replaceAll("").length() == 0) {
                    ok.setEnabled(true);

                } else {
                    ok.setEnabled(false);
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
        alertDialog.setOnShowListener(dialog -> {
            Button ok = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            ok.setEnabled(false);
        });
        return alertDialog;
    }

    @Override
    protected void onResume() {
        super.onResume();
        deleted = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
        if (position == KeyboardProfile.BUTTON_NAMES.length) {
            if (KeyboardProfile.isDefaultProfile(profile.name)) {
                KeyboardProfile.restoreDefaultProfile(profile.name, this);
            } else {
                profile.delete(this);
            }
            deleted = true;
            finish();
        } else {
            Builder builder = new Builder(this);
            builder.setTitle(String.format(getResources().getString(R.string.press_key),
                    KeyboardProfile.BUTTON_NAMES[position]));
            builder.setNegativeButton("Cancel", null);
            EditText view = new EditText(KeyboardSettingsActivity.this);
            builder.setView(view);
            final Dialog d = builder.create();
            view.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    char ch = s.charAt(0);
                    proccessKeyEvent(ch + "", d, (int) ch, position);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            d.setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed()) {
                        keyCode = KeyboardController.KEY_XPERIA_CIRCLE;
                    }
                }

                String txt = getKeyLabel(keyCode);
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    return proccessKeyEvent(txt, dialog, keyCode, position);
                } else {
                    return false;
                }
            });
            d.show();
        }
    }

    private boolean proccessKeyEvent(String txt, DialogInterface dialog, int keyCode, int position) {
        NLog.i(TAG, "txt:" + txt);
        if (!txt.equals("") && keyCode != KeyEvent.KEYCODE_BACK) {
            int idx = inverseMap.indexOfValue(keyCode);
            if (idx >= 0) {
                inverseMap.put(inverseMap.keyAt(idx), 0);
            }
            inverseMap.append(KeyboardProfile.BUTTON_KEY_EVENT_CODES[position], keyCode);
            NLog.i(TAG, "isert " + KeyboardProfile.BUTTON_NAMES[position] + " :" + keyCode);
            adapter.notifyDataSetInvalidated();
            dialog.dismiss();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!deleted) {
            profile.keyMap.clear();
            for (int i = 0; i < inverseMap.size(); i++) {
                profile.keyMap.append(inverseMap.valueAt(i), inverseMap.keyAt(i));
            }
            profile.save(this);
        }
    }

    private class Adapter extends BaseAdapter {

        LayoutInflater inflater;
        private int heightCache = -1;

        public Adapter() {
            inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_keyboard_settings, null);
            }
            TextView name = convertView.findViewById(R.id.row_keyboard_name);
            TextView desc = convertView.findViewById(R.id.row_keyboard_desc);
            TextView keyName = convertView.findViewById(R.id.row_keyboard_key_name);
            convertView.setEnabled(true);
            if (position < KeyboardProfile.BUTTON_NAMES.length) {
                name.setText(KeyboardProfile.BUTTON_NAMES[position]);
                int keyCode = inverseMap.get(KeyboardProfile.BUTTON_KEY_EVENT_CODES[position]);
                String label = getKeyLabel(keyCode);
                keyName.setText(label);
                keyName.setVisibility(View.VISIBLE);
            } else {
                name.setText(KeyboardProfile.isDefaultProfile(profile.name) ?
                        getText(R.string.pref_keyboard_settings_restore_def)
                        : getText(R.string.pref_keyboard_settings_delete_prof));
                desc.setVisibility(View.GONE);
                keyName.setVisibility(View.GONE);
            }
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public int getCount() {
            return KeyboardProfile.BUTTON_NAMES.length + (newProfile ? 0 : 1);
        }

        public int getRowHeight() {
            if (heightCache < 0) {
                View convertView = inflater.inflate(R.layout.row_keyboard_settings, null);
                convertView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                heightCache = convertView.getMeasuredHeight();
            }
            return heightCache;
        }

        public int[] getPlayersOffset() {
            ArrayList<Integer> result = new ArrayList<>();
            String lastDesc = "";
            int h = getRowHeight();
            for (int i = 0; i < KeyboardProfile.BUTTON_NAMES.length; i++) {
                String desc = KeyboardProfile.BUTTON_DESCRIPTIONS[i];
                if (!lastDesc.equals(desc)) {
                    result.add(i * h);
                    lastDesc = desc;
                }
            }
            int[] res = new int[result.size()];
            for (int i = 0; i < result.size(); i++)
                res[i] = result.get(i);
            return res;
        }

    }

}
