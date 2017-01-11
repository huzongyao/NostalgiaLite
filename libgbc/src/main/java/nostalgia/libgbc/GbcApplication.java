package nostalgia.libgbc;

import nostalgia.framework.EmulatorApplication;
import nostalgia.framework.base.EmulatorHolder;


public class GbcApplication extends EmulatorApplication {

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