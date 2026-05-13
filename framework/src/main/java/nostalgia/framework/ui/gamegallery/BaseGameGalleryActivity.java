package nostalgia.framework.ui.gamegallery;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.Emulator;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.RomsFinder.OnRomsFinderListener;
import nostalgia.framework.utils.DatabaseHelper;
import nostalgia.framework.utils.DialogUtils;
import nostalgia.framework.utils.FileUtils;
import nostalgia.framework.utils.NLog;

abstract public class BaseGameGalleryActivity extends AppCompatActivity
        implements OnRomsFinderListener {

    private static final String TAG = "BaseGameGalleryActivity";
    private static final int REQUEST_SELECT_FILE = 1002;

    protected Set<String> exts;
    protected Set<String> inZipExts;
    protected boolean reloadGames = true;
    protected boolean reloading = false;
    protected boolean importingRom = false;
    private RomsFinder romsFinder = null;
    private DatabaseHelper dbHelper = null;
    private Intent pendingShareIntent = null;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);
        SharedPreferences pref = getSharedPreferences("android50comp", Context.MODE_PRIVATE);
        String androidVersion = Build.VERSION.RELEASE;

        if (!pref.getString("androidVersion", "").equals(androidVersion)) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            dbHelper.onUpgrade(db, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
            db.close();
            Editor editor = pref.edit();
            editor.putString("androidVersion", androidVersion);
            editor.apply();
            NLog.i(TAG, "Reinit DB " + androidVersion);
        }
        reloadGames = true;
        
        // 暂存分享Intent，等子类初始化完exts和inZipExts后再处理
        pendingShareIntent = getIntent();
    }
    
    protected void onExtensionsInitialized() {
        // 等子类设置完exts和inZipExts后再处理分享Intent
        if (pendingShareIntent != null) {
            handleShareIntent(pendingShareIntent);
            pendingShareIntent = null;
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (exts != null && inZipExts != null) {
            handleShareIntent(intent);
        } else {
            pendingShareIntent = intent;
        }
    }
    
    private void handleShareIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Uri uri = null;
            if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                uri = intent.getData();
            }
            
            if (uri != null) {
                importRom(uri);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!FileUtils.isSDCardRWMounted()) {
            showSDCardFailed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (romsFinder != null) {
            romsFinder.stopSearch();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void reloadGames(boolean searchNew, File selectedFolder) {
        if (romsFinder == null) {
            reloadGames = false;
            reloading = searchNew;
            romsFinder = new RomsFinder(exts, inZipExts, this, this, searchNew, selectedFolder);
            romsFinder.start();
        }
    }
    
    protected void importRom(File file) {
        if (romsFinder == null) {
            importingRom = true;
            reloading = true;
            romsFinder = new RomsFinder(exts, inZipExts, this, this, file);
            romsFinder.start();
        }
    }
    
    protected void importRom(Uri uri) {
        if (romsFinder == null) {
            importingRom = true;
            reloading = true;
            romsFinder = new RomsFinder(exts, inZipExts, this, this, uri);
            romsFinder.start();
        }
    }
    
    protected void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        
        // Create a mime type filter for common ROM formats
        String[] mimeTypes = {
            "application/octet-stream",
            "application/zip",
            "application/x-zip-compressed"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        Intent chooser = Intent.createChooser(intent, getString(R.string.gallery_select_rom_title));
        startActivityForResult(chooser, REQUEST_SELECT_FILE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importRom(uri);
            }
        }
    }
    


    @Override
    public void onRomsFinderFoundGamesInCache(ArrayList<GameDescription> oldRoms) {
        setLastGames(oldRoms);
    }

    @Override
    public void onRomsFinderNewGames(ArrayList<GameDescription> roms) {
        setNewGames(roms);
    }

    @Override
    public void onRomsFinderEnd(boolean searchNew) {
        romsFinder = null;
        reloading = false;
        importingRom = false;
    }

    @Override
    public void onRomsFinderCancel(boolean searchNew) {
        romsFinder = null;
        reloading = false;
        importingRom = false;
    }

    protected void stopRomsFinding() {
        if (romsFinder != null) {
            romsFinder.stopSearch();
        }
    }

    public void showSDCardFailed() {
        runOnUiThread(() -> {
            AlertDialog dialog = new AlertDialog.Builder(BaseGameGalleryActivity.this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.gallery_sd_card_not_mounted)
                    .setOnCancelListener(dialog1 -> finish())
                    .setPositiveButton(R.string.exit, (dialog1, which) -> finish())
                    .create();
            DialogUtils.show(dialog, true);
        });
    }

    public abstract Class<? extends EmulatorActivity> getEmulatorActivityClass();

    abstract public void setLastGames(ArrayList<GameDescription> games);

    abstract public void setNewGames(ArrayList<GameDescription> games);

    abstract protected Set<String> getRomExtensions();

    public abstract Emulator getEmulatorInstance();

    protected Set<String> getArchiveExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("zip");
        return set;
    }

}
