package nostalgia.framework.ui.preferences;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import nostalgia.framework.R;


public class GeneralPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general_preferences);
        SeekBarVibrationPreference vibration = (SeekBarVibrationPreference) findPreference("game_pref_ui_strong_vibration");
        String vs = Context.VIBRATOR_SERVICE;
        Vibrator mVibrator = (Vibrator) getActivity().getSystemService(vs);
        vibration.setEnabled(mVibrator.hasVibrator());
        if (!mVibrator.hasVibrator()) {
            vibration.setSummary(R.string.game_pref_ui_vibration_no_vibrator);
        }
        PreferenceCategory cat = (PreferenceCategory) findPreference("pref_general_settings_cat");
        Preference quality = findPreference("general_pref_quality");
        ListPreference selectProfile = (ListPreference) findPreference("pref_game_keyboard_profile");
        Preference editProfile = findPreference("pref_game_keyboard_edit_profile");
        GeneralPreferenceActivity.initQuality(cat, quality);
        GeneralPreferenceActivity.initProfiles(getActivity(), selectProfile, editProfile);
        GeneralPreferenceActivity.setNewProfile(selectProfile, editProfile, selectProfile.getValue());
        Preference quicksave = findPreference("general_pref_quicksave");
        GeneralPreferenceActivity.initProPreference(quicksave, getActivity());
        Preference autoHide = findPreference("general_pref_ui_autohide");
        GeneralPreferenceActivity.initProPreference(autoHide, getActivity());
        Preference opacity = findPreference("general_pref_ui_opacity");
        GeneralPreferenceActivity.initProPreference(opacity, getActivity());
        CheckBoxPreference ddpad = (CheckBoxPreference) findPreference("general_pref_ddpad");
        GeneralPreferenceActivity.initDDPAD(ddpad, getActivity());
        Preference screen = findPreference("general_pref_screen_layout");
        GeneralPreferenceActivity.initScreenSettings(screen, getActivity());
        CheckBoxPreference ff = (CheckBoxPreference) findPreference("general_pref_fastforward");
        GeneralPreferenceActivity.initFastForward(ff, getActivity());
        PreferenceCategory keyCat = (PreferenceCategory) findPreference("pref_keyboard_cat");
        Preference inputMethod = keyCat.findPreference("pref_game_keyboard_select_input_method");
        GeneralPreferenceActivity.initInputMethodPreference(inputMethod, getActivity());
        PreferenceCategory otherCat = (PreferenceCategory) findPreference("pref_others_cat");
        Preference aboutPreference = otherCat.findPreference("pref_game_others_about_game");
        GeneralPreferenceActivity.initAboutGamePreference(aboutPreference, getActivity());
    }
}
