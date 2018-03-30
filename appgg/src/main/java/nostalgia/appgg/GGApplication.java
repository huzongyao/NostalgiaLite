package nostalgia.appgg;

import nostalgia.framework.EmulatorApplication;
import nostalgia.framework.base.EmulatorHolder;

public class GGApplication extends EmulatorApplication {

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
