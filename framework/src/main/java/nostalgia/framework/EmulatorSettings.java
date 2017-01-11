package nostalgia.framework;

public class EmulatorSettings {
    boolean zapperEnabled;
    boolean historyEnabled;
    boolean loadSavFiles;
    boolean saveSavFiles;
    int quality = 0;

    public int toInt() {
        int x = zapperEnabled ? 1 : 0;
        x += historyEnabled ? 10 : 0;
        x += loadSavFiles ? 100 : 0;
        x += saveSavFiles ? 1000 : 0;
        x += quality * 10000;
        return x;
    }
}
