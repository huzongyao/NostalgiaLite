package nostalgia.framework;


import android.app.Application;

import com.blankj.utilcode.util.Utils;

import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;

abstract public class EmulatorApplication extends Application {

    private static final String TAG = EmulatorApplication.class.getName();

    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        boolean debug = EmuUtils.isDebuggable(this);
        NLog.setDebugMode(debug);
    }

    public abstract boolean hasGameMenu();
}
