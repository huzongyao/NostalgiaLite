package nostalgia.framework.ui.preferences;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

import java.util.List;

import nostalgia.framework.GfxProfile;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorHolder;
import nostalgia.framework.ui.gamegallery.GameDescription;

/**
 * 游戏偏好设置 Activity。
 * <p>
 * 针对单个游戏的设置页面，包括视频模式、光枪等游戏特定配置。
 * 每个游戏使用独立的 SharedPreferences 文件存储设置。
 * </p>
 */
public class GamePreferenceActivity extends AppCompatPreferenceActivity {

    public static final String EXTRA_GAME = "EXTRA_GAME";
    private GameDescription game;

    /** 初始化光枪设置（仅当模拟器支持时显示） */
    static void initZapper(Preference zapper, PreferenceCategory zapperCategory) {
        if (!EmulatorHolder.getInfo().hasZapper()) {
            zapperCategory.removePreference(zapper);
        }
    }

    /** 初始化视频模式偏好设置（根据可用的图像配置动态生成选项） */
    static void initVideoPreference(ListPreference preference,
                                    PreferenceCategory category, PreferenceScreen screen) {
        List<GfxProfile> profiles = EmulatorHolder.getInfo().getAvailableGfxProfiles();

        if (profiles.size() > 1) {
            CharSequence[] res =
                    new CharSequence[EmulatorHolder.getInfo().getAvailableGfxProfiles().size() + 1];
            res[0] = "Auto";
            int i = 1;
            for (GfxProfile gfx : profiles) {
                res[i] = gfx.name;
                i++;
            }
            preference.setEntries(res);
            preference.setEntryValues(res);
            if (preference.getValue() == null) {
                preference.setValue("Auto");
            }
        } else {
            category.removePreference(preference);
            screen.removePreference(category);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        game = (GameDescription) getIntent().getSerializableExtra(EXTRA_GAME);
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.game_preferences_header, target);
    }

}
