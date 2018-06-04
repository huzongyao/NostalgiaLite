package nostalgia.appgbc;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.base.EmulatorHolder;


public class GbcApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        EmulatorHolder.setEmulatorClass(GbcEmulator.class);
    }

    @Override
    public boolean hasGameMenu() {
        return false;
    }

}