package nostalgia.libgg;

import nostalgia.framework.EmulatorApplication;
import nostalgia.framework.base.EmulatorHolder;

public abstract class GGApplication extends EmulatorApplication {

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
