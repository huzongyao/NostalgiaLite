package nostalgia.framework.ui.preferences;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import nostalgia.framework.R;
import nostalgia.framework.ui.gamegallery.GameDescription;


public class GamePreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GameDescription game = (GameDescription) getActivity().getIntent()
                .getSerializableExtra(GamePreferenceActivity.EXTRA_GAME);
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(game.checksum + ".gamepref");
        addPreferencesFromResource(R.xml.game_preferences);
        final ListPreference videoProfile = (ListPreference) findPreference("game_pref_ui_pal_ntsc_switch");
        final PreferenceCategory videoProfileCategory = (PreferenceCategory) findPreference("game_pref_ui_pal_ntsc_switch_category");
        final PreferenceCategory zapperCategory = (PreferenceCategory) findPreference("game_pref_other_category");
        final Preference zapper = findPreference("game_pref_zapper");
        GamePreferenceActivity.initZapper(zapper, zapperCategory);
        GamePreferenceActivity.initVideoPreference(videoProfile, videoProfileCategory, getPreferenceScreen());
    }
}
