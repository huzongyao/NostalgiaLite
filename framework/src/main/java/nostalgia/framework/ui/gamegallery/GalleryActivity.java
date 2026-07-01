package nostalgia.framework.ui.gamegallery;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.data.GameRepository;
import nostalgia.framework.ui.gamegallery.GalleryPagerAdapter.OnItemClickListener;
import nostalgia.framework.ui.preferences.GeneralPreferenceActivity;
import nostalgia.framework.ui.preferences.GeneralPreferenceFragment;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.DialogUtils;
import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;

/**
 * 游戏画廊 Activity。
 * <p>
 * 使用 ViewPager + TabLayout 展示游戏列表，支持按名称、最近游玩、
 * 游玩次数排序。支持多选模式、删除游戏、导入 ROM、搜索 ROM 等功能。
 * 继承自 BaseGameGalleryActivity 并实现游戏点击和长按事件。
 * </p>
 */
public abstract class GalleryActivity extends BaseGameGalleryActivity
        implements OnItemClickListener {

    public static final String EXTRA_TABS_IDX = "EXTRA_TABS_IDX";
    private static final String TAG = GalleryActivity.class.getSimpleName();

    ProgressDialog searchDialog = null;
    private ViewPager pager = null;
    private TextView noGamesTextView = null;
    private GalleryPagerAdapter adapter;
    private boolean importing = false;
    private boolean rotateAnim = false;
    private TabLayout mTabLayout;
    
    /** 后台线程池，用于数据库操作和文件处理 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /** 主线程 Handler，用于将结果回调到 UI 线程 */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        adapter = new GalleryPagerAdapter(this, this);
        adapter.onRestoreInstanceState(savedInstanceState);
        pager = findViewById(R.id.game_gallery_pager);
        pager.setAdapter(adapter);
        
        noGamesTextView = findViewById(R.id.no_games_text);

        mTabLayout = findViewById(R.id.game_gallery_tab);
        mTabLayout.setupWithViewPager(pager);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);

        if (savedInstanceState != null) {
            pager.setCurrentItem(savedInstanceState.getInt(EXTRA_TABS_IDX, 0));
        } else {
            pager.setCurrentItem(PreferenceUtil.getLastGalleryTab(this));
        }

        exts = getRomExtensions();
        exts.addAll(getArchiveExtensions());
        inZipExts = getRomExtensions();
        
        // 设置长按监听器
        adapter.setOnItemLongClickListener(this::updateActionMode);
        
        // 调用基类，通知初始化完成，可以处理分享了
        onExtensionsInitialized();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery_main_menu, menu);
        updateMenuVisibility(menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    /** 更新菜单栏可见性（多选模式 vs 普通模式） */
    private void updateMenuVisibility(Menu menu) {
        boolean isMultiSelect = adapter.isMultiSelectMode();
        if (menu != null) {
            MenuItem deleteItem = menu.findItem(R.id.gallery_menu_delete);
            MenuItem selectAllItem = menu.findItem(R.id.gallery_menu_select_all);
            MenuItem selectRomItem = menu.findItem(R.id.gallery_menu_select_rom);
            MenuItem prefItem = menu.findItem(R.id.gallery_menu_pref);
            MenuItem exitItem = menu.findItem(R.id.gallery_menu_exit);
            
            if (deleteItem != null) {
                deleteItem.setVisible(isMultiSelect);
            }
            if (selectAllItem != null) {
                selectAllItem.setVisible(isMultiSelect);
            }
            if (selectRomItem != null) {
                selectRomItem.setVisible(!isMultiSelect);
            }
            if (prefItem != null) {
                prefItem.setVisible(!isMultiSelect);
            }
            if (exitItem != null) {
                exitItem.setVisible(!isMultiSelect);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.gallery_menu_pref) {
            Intent i = new Intent(this, GeneralPreferenceActivity.class);
            i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, GeneralPreferenceFragment.class.getName());
            i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            startActivity(i);
            return true;
        } else if (itemId == R.id.gallery_menu_select_rom) {
            openFilePicker();
            return true;
        } else if (itemId == R.id.gallery_menu_exit) {
            finish();
            return true;
        } else if (itemId == R.id.gallery_menu_delete) {
            deleteSelectedGames();
            return true;
        } else if (itemId == R.id.gallery_menu_select_all) {
            adapter.selectAll();
            updateActionMode();
            return true;
        } else if (itemId == android.R.id.home) {
            exitMultiSelectMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /** 更新标题栏显示（多选模式显示选中数量） */
    private void updateActionMode() {
        boolean isMultiSelect = adapter.isMultiSelectMode();
        int selectedCount = adapter.getSelectedCount();
        
        if (isMultiSelect) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(String.format(getString(R.string.selected_games), selectedCount));
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(R.string.app_name);
            }
        }
        
        invalidateOptionsMenu();
    }
    
    /** 退出多选模式 */
    private void exitMultiSelectMode() {
        adapter.setMultiSelectMode(false);
        adapter.clearSelection();
        updateActionMode();
    }
    
    /** 删除选中的游戏（包括数据库记录和文件） */
    private void deleteSelectedGames() {
        ArrayList<GameDescription> selectedGames = adapter.getSelectedGames();
        if (selectedGames.isEmpty()) {
            exitMultiSelectMode();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle(String.format(getString(R.string.gallery_delete_confirm_title), selectedGames.size()))
            .setMessage(R.string.gallery_delete_confirm_message)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                executor.execute(() -> {
                    GameRepository repository = getGameRepository();
                    for (GameDescription game : selectedGames) {
                        repository.deleteGame(game);
                        if (game.path != null) {
                            try {
                                new java.io.File(game.path).delete();
                            } catch (Exception e) {
                                NLog.e(TAG, "Failed to delete file: " + game.path, e);
                            }
                        }
                    }
                    ArrayList<GameDescription> allGames = repository.getAllGamesSortedByName();
                    mainHandler.post(() -> {
                        exitMultiSelectMode();
                        setLastGames(allGames);
                    });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    @Override
    public void onBackPressed() {
        if (adapter.isMultiSelectMode()) {
            exitMultiSelectMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotateAnim) {
            rotateAnim = false;
        }
        adapter.notifyDataSetChanged();
        // 不再自动搜索ROM，只在首次加载已有游戏
        if (reloadGames && !importing) {
            reloadGames(false, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceUtil.saveLastGalleryTab(this, pager.getCurrentItem());
    }

    @Override
    public void onItemClick(GameDescription game) {
        File gameFile = new File(game.path);
        NLog.i(TAG, "select " + game);
        GameRepository repository = getGameRepository();

        if (game.isInArchive()) {
            final File cacheFile = new File(getExternalCacheDir(), game.checksum);
            final String cachePath = cacheFile.getAbsolutePath();
            game.path = cachePath;
            
            // 在后台线程获取ZipRomFile
            executor.execute(() -> {
                ZipRomFile zipRomFile = repository.getZipFileById(game.zipfile_id);
                if (zipRomFile != null) {
                    File zipFile = new File(zipRomFile.path);
                    if (!cacheFile.exists()) {
                        try {
                            EmuUtils.extractFile(zipFile, game.name, cacheFile);
                        } catch (IOException e) {
                            NLog.e(TAG, "", e);
                        }
                    }
                }
                
                // 返回主线程处理
                mainHandler.post(() -> handleGameSelected(game, cacheFile));
            });
        } else {
            if (gameFile.exists()) {
                game.lastGameTime = System.currentTimeMillis();
                game.runCount++;
                
                // 在后台线程更新数据库
                executor.execute(() -> {
                    repository.updateGame(game);
                    
                    // 返回主线程启动游戏
                    mainHandler.post(() -> onGameSelected(game, 0));
                });
            } else {
                NLog.w(TAG, "rom file:" + gameFile.getAbsolutePath() + " does not exist");
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.gallery_rom_not_found))
                        .setTitle(R.string.error)
                        .setPositiveButton(R.string.ok, (dialog1, which)
                                -> {
                            // 在后台线程删除不存在的ROM记录
                            executor.execute(() -> {
                                repository.deleteGame(game);
                                ArrayList<GameDescription> games = repository.getAllGamesSortedByName();
                                // 返回主线程刷新显示
                                mainHandler.post(() -> setLastGames(games));
                            });
                        })
                        .setCancelable(false)
                        .create();
                dialog.show();
            }
        }
    }
    
    /** 处理游戏选中事件（从压缩包解压后） */
    private void handleGameSelected(GameDescription game, File gameFile) {
        if (gameFile.exists()) {
            game.lastGameTime = System.currentTimeMillis();
            game.runCount++;
            
            // 在后台线程更新数据库
            executor.execute(() -> {
                getGameRepository().updateGame(game);
                
                // 返回主线程启动游戏
                mainHandler.post(() -> onGameSelected(game, 0));
            });
        } else {
            NLog.w(TAG, "rom file:" + gameFile.getAbsolutePath() + " does not exist");
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.gallery_rom_not_found))
                    .setTitle(R.string.error)
                    .setPositiveButton(R.string.ok, (dialog1, which)
                            -> {
                        // 在后台线程删除不存在的ROM记录
                        executor.execute(() -> {
                            getGameRepository().deleteGame(game);
                            ArrayList<GameDescription> games = getGameRepository().getAllGamesSortedByName();
                            // 返回主线程刷新显示
                            mainHandler.post(() -> setLastGames(games));
                        });
                    })
                    .setCancelable(false)
                    .create();
            dialog.show();
        }
    }

    /** 启动模拟器 Activity */
    public boolean onGameSelected(GameDescription game, int slot) {
        Intent intent = new Intent(this, getEmulatorActivityClass());
        intent.putExtra(EmulatorActivity.EXTRA_GAME, game);
        intent.putExtra(EmulatorActivity.EXTRA_SLOT, slot);
        intent.putExtra(EmulatorActivity.EXTRA_FROM_GALLERY, true);
        startActivity(intent);
        return true;
    }

    @Override
    public void setLastGames(ArrayList<GameDescription> games) {
        adapter.setGames(games);
        updateGameVisibility(games);
    }

    @Override
    public void setNewGames(ArrayList<GameDescription> games) {
        boolean isListEmpty = adapter.addGames(games) == 0;
        updateGameVisibility(adapter.getAllGames());
    }
    
    /** 更新游戏列表可见性（无游戏时显示提示文本） */
    private void updateGameVisibility(ArrayList<GameDescription> games) {
        if (games.isEmpty()) {
            pager.setVisibility(View.GONE);
            noGamesTextView.setVisibility(View.VISIBLE);
        } else {
            pager.setVisibility(View.VISIBLE);
            noGamesTextView.setVisibility(View.GONE);
        }
    }

    /** 显示搜索进度对话框 */
    private void showSearchProgressDialog(boolean zipMode) {
        if (searchDialog == null) {
            searchDialog = new ProgressDialog(this);
            searchDialog.setMax(100);
            searchDialog.setCancelable(false);
            searchDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            searchDialog.setIndeterminate(true);
            searchDialog.setProgressNumberFormat("");
            searchDialog.setProgressPercentFormat(null);
            searchDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                    (dialog, which) -> stopRomsFinding());
        }
        searchDialog.setMessage(getString(importingRom ?
                R.string.gallery_importing_rom
                : zipMode ?
                R.string.gallery_zip_search_label
                : R.string.gallery_sdcard_search_label));
        DialogUtils.show(searchDialog, false);

    }

    public void onSearchingEnd(final int count, final boolean showToast) {
        runOnUiThread(() -> {
            if (searchDialog != null) {
                searchDialog.dismiss();
                searchDialog = null;
            }
            if (showToast) {
                if (count > 0) {
                    Snackbar.make(pager, getString(R.string.gallery_rom_import_success),
                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                } else if (importingRom) {
                    Snackbar.make(pager, getString(R.string.gallery_rom_import_failed),
                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
    }

    @Override
    public void onRomsFinderStart(boolean searchNew) {
        if (searchNew) {
            showSearchProgressDialog(false);
        }
    }

    @Override
    public void onRomsFinderZipPartStart(final int countEntries) {
        if (searchDialog != null) {
            runOnUiThread(() -> {
                if (searchDialog != null) {
                    searchDialog.setProgressNumberFormat("%1d/%2d");
                    searchDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                    searchDialog.setMessage(getString(R.string.gallery_start_sip_search_label));
                    searchDialog.setIndeterminate(false);
                    searchDialog.setMax(countEntries);
                }
            });
        }
    }

    @Override
    public void onRomsFinderCancel(boolean searchNew) {
        super.onRomsFinderCancel(searchNew);
        onSearchingEnd(0, searchNew);
    }

    @Override
    public void onRomsFinderEnd(boolean searchNew) {
        super.onRomsFinderEnd(searchNew);
        onSearchingEnd(0, searchNew);
    }

    @Override
    public void onRomsFinderNewGames(ArrayList<GameDescription> roms) {
        super.onRomsFinderNewGames(roms);
        onSearchingEnd(roms.size(), true);
    }

    @Override
    public void onRomsFinderFoundZipEntry(final String message, final int skipEntries) {
        if (searchDialog != null) {
            runOnUiThread(() -> {
                if (searchDialog != null) {
                    searchDialog.setMessage(message);
                    searchDialog.setProgress(searchDialog.getProgress() + 1 + skipEntries);
                }
            });
        }
    }

    @Override
    public void onRomsFinderFoundFile(final String name) {
        if (searchDialog != null) {
            runOnUiThread(() -> {
                if (searchDialog != null) {
                    searchDialog.setMessage(name);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_TABS_IDX, pager.getCurrentItem());
        adapter.onSaveInstanceState(outState);
    }

}
