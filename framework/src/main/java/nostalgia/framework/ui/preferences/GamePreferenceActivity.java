package nostalgia.framework.ui.preferences;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import java.util.List;

import nostalgia.framework.GfxProfile;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorHolder;
import nostalgia.framework.remote.VirtualDPad;
import nostalgia.framework.ui.gamegallery.GameDescription;

public class GamePreferenceActivity extends PreferenceActivity {

    public static final String EXTRA_GAME = "EXTRA_GAME";
    private GameDescription game;

    static void initZapper(Preference zapper, PreferenceCategory zapperCategory) {
        if (!EmulatorHolder.getInfo().hasZapper()) {
            zapperCategory.removePreference(zapper);
        }
    }

    static void initVideoPreference(ListPreference preference,
                                    PreferenceCategory category, PreferenceScreen screen) {
        List<GfxProfile> profiles = EmulatorHolder.getInfo()
                .getAvailableGfxProfiles();

        if (profiles.size() > 1) {
            CharSequence[] res = new CharSequence[EmulatorHolder.getInfo()
                    .getAvailableGfxProfiles().size() + 1];
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

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = (GameDescription) getIntent().getSerializableExtra(EXTRA_GAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VirtualDPad.getInstance().onResume(getWindow());
    }

    @Override
    protected void onPause() {
        super.onPause();
        VirtualDPad.getInstance().onPause();
    }


    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.game_preferences_header, target);
    }

}
