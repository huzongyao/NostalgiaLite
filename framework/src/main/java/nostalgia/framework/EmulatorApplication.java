package nostalgia.framework;


import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.Utils;

abstract public class EmulatorApplication extends Application {

    public static final int FIRST_INSTALLATION = -1;
    private static final String TAG = ".AppVersionChangeHandler";
    private static final String CURRENT_APP_VERSION = "app_version";
    private static final String PREVIOUS_APP_VERSION = "previous_app_version";
    private static final String PREF_NAME = "AppVersionChangeHandler.pref";
    protected String githash;
    private int previousVersion = -1;
    private int currentVersion = -1;
    private boolean isFirstRunAfterUpdate = false;
    private boolean isFirstRunEver = false;

    private void initVersionCodes() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, 0);
        int currentVersionPref = pref.getInt(CURRENT_APP_VERSION,
                FIRST_INSTALLATION);
        previousVersion = pref.getInt(PREVIOUS_APP_VERSION, FIRST_INSTALLATION);
        SharedPreferences.Editor editor = pref.edit();

        try {
            currentVersion = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionCode;

            if (currentVersionPref != currentVersion) {
                if (currentVersionPref == FIRST_INSTALLATION) {
                    isFirstRunEver = true;

                } else {
                    isFirstRunAfterUpdate = true;
                }

                previousVersion = currentVersionPref;
            }

        } catch (NameNotFoundException e) {
            NLog.e(TAG, "Very weird fail", e);
        }

        editor.putInt(CURRENT_APP_VERSION, currentVersion);
        editor.putInt(PREVIOUS_APP_VERSION, previousVersion);
        editor.apply();
    }

    public int getPreviousVersionCode() {
        return previousVersion;
    }

    public int getCurrentVersionCode() {
        return currentVersion;
    }

    public void onCreate() {
        boolean debug = Utils.isDebuggable(this);
        NLog.setDebugMode(debug);
        initVersionCodes();

        ApplicationInfo ai;

        try {
            ai = getPackageManager().getApplicationInfo(this.getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;

            if (bundle != null) {
                githash = bundle.getString("svnversion");
            }

        } catch (NameNotFoundException e) {
        }

        super.onCreate();
    }

    public abstract boolean hasGameMenu();

}
