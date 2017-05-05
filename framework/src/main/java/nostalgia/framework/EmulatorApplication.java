package nostalgia.framework;


import android.app.Application;

import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.Utils;

abstract public class EmulatorApplication extends Application {

    private static final String TAG = EmulatorApplication.class.getName();

    public void onCreate() {
        boolean debug = Utils.isDebuggable(this);
        NLog.setDebugMode(debug);
        super.onCreate();
    }

    public abstract boolean hasGameMenu();
}
