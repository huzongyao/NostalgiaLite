package nostalgia.framework.ui.gamegallery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nostalgia.framework.R;
import nostalgia.framework.SlotInfo;
import nostalgia.framework.base.SlotUtils;
import nostalgia.framework.ui.widget.PopupMenu;
import nostalgia.framework.utils.NLog;

public class SlotSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_GAME = "EXTRA_GAME";
    public static final String EXTRA_BASE_DIRECTORY = "EXTRA_BASE_DIR";
    public static final String EXTRA_SLOT = "EXTRA_SLOT";
    public static final String EXTRA_DIALOG_TYPE_INT = "EXTRA_DIALOG_TYPE_INT";
    public static final int DIALOAG_TYPE_LOAD = 1;
    public static final int DIALOAG_TYPE_SAVE = 2;
    private static final String TAG = "SlotSelectionActivity";
    private final static int SEND_SLOT = 0;
    private final static int REMOVE_SLOT = 1;
    View[] slots = new View[8];
    Drawable clearIcon, sendIcon;
    GameDescription game;
    int type;
    int loadFocusIdx = 0;
    int saveFocusIdx = 0;
    private ActionBar actionBar;

    private void initSlot(final SlotInfo slotInfo, final int idx, final String labelS,
                          String messageS, final String dateS, final String timeS) {

        final View slotView = slots[idx];
        final boolean isUsed = slotInfo.isUsed;
        Bitmap screenshotBitmap = slotInfo.screenShot;
        TextView label = slotView.findViewById(R.id.row_slot_label);
        TextView message = slotView.findViewById(R.id.row_slot_message);
        TextView date = slotView.findViewById(R.id.row_slot_date);
        TextView time = slotView.findViewById(R.id.row_slot_time);
        ImageView screenshot = slotView.findViewById(R.id.row_slot_screenshot);
        label.setText(labelS);
        message.setText(messageS);
        date.setText(dateS);
        time.setText(timeS);
        slotView.setOnClickListener(v -> onSelected(game, idx + 1, isUsed));

        if (isUsed) {
            slotView.setOnLongClickListener(v -> {
                PopupMenu menu = new PopupMenu(SlotSelectionActivity.this);
                menu.setHeaderTitle(labelS);
                menu.setOnItemSelectedListener(item -> {
                    if (item.getItemId() == SEND_SLOT) {
                    }
                });
                menu.add(SEND_SLOT, R.string.act_slot_popup_menu_send).setIcon(sendIcon);
                menu.show(slotView);
                return true;
            });
        }

        if (screenshotBitmap != null) {
            screenshot.setImageBitmap(screenshotBitmap);
            message.setVisibility(View.INVISIBLE);
        }
    }

    private void onSelected(GameDescription game, int slot, boolean isUsed) {
        if (type == DIALOAG_TYPE_LOAD && (!isUsed)) {
            return;
        }

        Intent data = new Intent();
        data.putExtra(EXTRA_GAME, game);
        data.putExtra(EXTRA_SLOT, slot);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clearIcon = getResources().getDrawable(R.drawable.ic_clear_slot);
        sendIcon = getResources().getDrawable(R.drawable.ic_send_slot);
        game = (GameDescription) getIntent().getSerializableExtra(EXTRA_GAME);
        String baseDir = getIntent().getStringExtra(EXTRA_BASE_DIRECTORY);
        List<SlotInfo> slotInfos = SlotUtils.getSlots(baseDir, game.checksum);
        type = getIntent().getIntExtra(EXTRA_DIALOG_TYPE_INT, DIALOAG_TYPE_LOAD);

        setContentView(R.layout.activity_slot_selection);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(type == DIALOAG_TYPE_LOAD ?
                    R.string.game_menu_load : R.string.game_menu_save);
        }
        slots[0] = findViewById(R.id.slot_0);
        slots[1] = findViewById(R.id.slot_1);
        slots[2] = findViewById(R.id.slot_2);
        slots[3] = findViewById(R.id.slot_3);
        slots[4] = findViewById(R.id.slot_4);
        slots[5] = findViewById(R.id.slot_5);
        slots[6] = findViewById(R.id.slot_6);
        slots[7] = findViewById(R.id.slot_7);
        java.text.DateFormat dateFormat = DateFormat.getDateFormat(this);
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(this);
        Calendar dd = Calendar.getInstance();
        dd.set(1970, 10, 10);
        String emptyDate = dateFormat.format(dd.getTime());
        emptyDate = emptyDate.replace("1970", "----");
        emptyDate = emptyDate.replace('0', '-');
        emptyDate = emptyDate.replace('1', '-');
        long focusTime = 0;
        saveFocusIdx = -1;

        for (int i = 0; i < SlotUtils.NUM_SLOTS; i++) {
            String message = "EMPTY";
            SlotInfo slotInfo = slotInfos.get(i);
            if (slotInfo.isUsed) {
                message = "USED";
            }
            String label = "SLOT  " + (i + 1);
            Date time = new Date(slotInfo.lastModified);
            String dateString = slotInfo.lastModified == -1 ? emptyDate : dateFormat.format(time);
            String timeString = slotInfo.lastModified == -1 ? "--:--" : timeFormat.format(time);
            initSlot(slotInfo, i, label, message, dateString, timeString);

            if (focusTime < slotInfo.lastModified) {
                loadFocusIdx = i;
                focusTime = slotInfo.lastModified;
            }
            if (!slotInfo.isUsed && saveFocusIdx == -1) {
                saveFocusIdx = i;
            }
        }
        if (loadFocusIdx < 0)
            loadFocusIdx = 0;
        if (saveFocusIdx < 0)
            saveFocusIdx = 0;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onResume() {
        super.onResume();
        Handler h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                slots[msg.what].requestFocusFromTouch();
                NLog.i(TAG, "focus item:" + loadFocusIdx);
                SharedPreferences pref = getSharedPreferences("slot-pref", Context.MODE_PRIVATE);
                if (!pref.contains("show")) {
                    Editor editor = pref.edit();
                    editor.putBoolean("show", true);
                    editor.apply();
                }
            }
        };
        h.sendEmptyMessageDelayed(type == DIALOAG_TYPE_LOAD ? loadFocusIdx : saveFocusIdx, 500);
    }

}
