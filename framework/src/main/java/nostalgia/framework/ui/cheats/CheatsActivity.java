package nostalgia.framework.ui.cheats;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorHolder;


public class CheatsActivity extends AppCompatActivity {

    public static final String EXTRA_IN_GAME_HASH = "EXTRA_IN_GAME_HASH";
    Button save;
    private ListView list;
    private CheatsListAdapter adapter;
    private String gameHash;
    private ArrayList<Cheat> cheats;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheats);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        gameHash = getIntent().getStringExtra(EXTRA_IN_GAME_HASH);
        list = findViewById(R.id.act_cheats_list);
        cheats = Cheat.getAllCheats(this, gameHash);
        adapter = new CheatsListAdapter(this, cheats);
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cheats_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.cheats_menu_add) {
            openCheatDetailDialog(-1);
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void openCheatDetailDialog(final int idx) {
        final Dialog dialog = new Dialog(this, R.style.DialogTheme);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.dialog_new_cheat, null);
        dialog.setContentView(content);
        final EditText chars = content.findViewById(R.id.dialog_new_cheat_chars);
        final EditText desc = content.findViewById(R.id.dialog_new_cheat_desc);
        save = content.findViewById(R.id.dialog_new_cheat_save);

        if (idx >= 0) {
            Cheat cheat = cheats.get(idx);
            chars.setText(cheat.chars);
            desc.setText(cheat.desc);
        }

        if (chars.getText().toString().equals("")) {
            save.setEnabled(false);
        }

        chars.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                String s = arg0.toString();
                Locale locale = Locale.getDefault();
                if (!s.equals(s.toUpperCase(locale))) {
                    s = s.toUpperCase(locale);
                    chars.setSelection(s.length());
                }
                String newText = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                if (!newText.equals(s)) {
                    chars.setText(newText);
                    chars.setSelection(newText.length());
                }
                s = newText;
                newText = s.replaceAll(EmulatorHolder.getInfo().getCheatInvalidCharsRegex(), "");
                if (!newText.equals(s)) {
                    chars.setText(newText);
                    chars.setSelection(newText.length());
                }
                if (newText.equals("")) {
                    save.setEnabled(false);
                } else {
                    save.setEnabled(true);
                }
            }
        });
        save.setOnClickListener(v -> {
            if (idx == -1) {
                cheats.add(new Cheat(chars.getText().toString(), desc.getText().toString(), true));
            } else {
                Cheat cheat = cheats.get(idx);
                cheat.chars = chars.getText().toString();
                cheat.desc = desc.getText().toString();
            }
            adapter.notifyDataSetChanged();
            Cheat.saveCheats(this, gameHash, cheats);
            dialog.cancel();
        });
        dialog.show();
    }

    public void removeCheat(int idx) {
        cheats.remove(idx);
        adapter.notifyDataSetChanged();
        Cheat.saveCheats(this, gameHash, cheats);
    }

    public void editCheat(int idx) {
        openCheatDetailDialog(idx);
    }

    public void saveCheats() {
        Cheat.saveCheats(this, gameHash, cheats);
    }

}
