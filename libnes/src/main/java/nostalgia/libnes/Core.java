package nostalgia.libnes;

import nostalgia.framework.base.JniBridge;

public class Core extends JniBridge {
    private static Core instance = new Core();

    static {
        System.loadLibrary("nostalgia");
    }

    private Core() {
    }

    public static Core getInstance() {
        return instance;
    }

}
