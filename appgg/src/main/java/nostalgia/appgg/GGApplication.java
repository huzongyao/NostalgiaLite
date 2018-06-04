package nostalgia.appgg;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.base.EmulatorHolder;

public class GGApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        EmulatorHolder.setEmulatorClass(GGEmulator.class);
    }

    @Override
    public boolean hasGameMenu() {
        return false;
    }
}
