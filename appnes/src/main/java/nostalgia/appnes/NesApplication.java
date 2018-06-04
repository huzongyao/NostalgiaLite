package nostalgia.appnes;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.base.EmulatorHolder;

public class NesApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        EmulatorHolder.setEmulatorClass(NesEmulator.class);
    }

    @Override
    public boolean hasGameMenu() {
        return true;
    }
}
