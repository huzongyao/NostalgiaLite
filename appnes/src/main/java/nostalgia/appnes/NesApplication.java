package nostalgia.appnes;

import nostalgia.framework.EmulatorApplication;
import nostalgia.framework.base.EmulatorHolder;

public class NesApplication extends EmulatorApplication {

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
