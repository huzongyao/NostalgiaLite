package nostalgia.libnes;

import nostalgia.framework.EmulatorApplication;
import nostalgia.framework.base.EmulatorHolder;

public abstract class NesApplication extends EmulatorApplication {

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
