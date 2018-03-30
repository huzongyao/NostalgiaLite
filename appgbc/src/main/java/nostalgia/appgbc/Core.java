package nostalgia.appgbc;

import nostalgia.framework.base.JniBridge;

public class Core extends JniBridge {
    private static Core instance = new Core();

    static {
        System.loadLibrary("gbc");
    }

    private Core() {
    }

    public static Core getInstance() {
        return instance;
    }
}
