package nostalgia.framework.base;

import android.content.Context;

import java.io.File;

import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorException;
import nostalgia.framework.EmulatorRunner;
import nostalgia.framework.R;
import nostalgia.framework.ui.cheats.Cheat;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.utils.FileUtils;
import nostalgia.framework.utils.NLog;

public class Manager extends EmulatorRunner {

    public Manager(Emulator emulator, Context context) {
        super(emulator, context);
    }

    public void setFastForwardEnabled(boolean enabled) {
        emulator.setFastForwardEnabled(enabled);
    }

    public void setFastForwardFrameCount(int frames) {
        emulator.setFastForwardFrameCount(frames);
    }

    public void copyAutoSave(int slot) {
        if (!emulator.isGameLoaded()) {
            throw new EmulatorException("game not loaded");
        }

        String md5 = emulator.getLoadedGame().md5;
        String base = EmulatorUtils.getBaseDir(context);
        String source = SlotUtils.getSlotPath(base, md5, 0);
        String target = SlotUtils.getSlotPath(base, md5, slot);
        String sourcePng = SlotUtils.getScreenshotPath(base, md5, 0);
        String targetPng = SlotUtils.getScreenshotPath(base, md5, slot);

        try {
            FileUtils.copyFile(new File(source), new File(target));
            FileUtils.copyFile(new File(sourcePng), new File(targetPng));

        } catch (Exception e) {
            throw new EmulatorException(R.string.act_emulator_save_state_failed);
        }
    }

    public int enableCheats(Context ctx, GameDescription game) {
        int numCheats = 0;

        for (String cheatChars : Cheat.getAllEnableCheats(ctx, game.checksum)) {
            if (cheatChars.contains(":")) {
                if (EmulatorHolder.getInfo().supportsRawCheats()) {
                    int[] rawValues = null;

                    try {
                        rawValues = Cheat.rawToValues(cheatChars);

                    } catch (Exception e) {
                        throw new EmulatorException(
                                R.string.act_emulator_invalid_cheat, cheatChars);
                    }
                    enableRawCheat(rawValues[0], rawValues[1], rawValues[2]);
                } else {
                    throw new EmulatorException(R.string.act_emulator_invalid_cheat, cheatChars);
                }

            } else {
                enableCheat(cheatChars.toUpperCase());
            }

            numCheats++;
        }

        return numCheats;
    }

    public void benchMark() {
        emulator.reset();
        long t1 = System.currentTimeMillis();

        for (int i = 0; i < 3000; i++) {
            emulator.emulateFrame(0);

            try {
                Thread.sleep(2);

            } catch (Exception ignored) {
            }
        }

        long t2 = System.currentTimeMillis();
        NLog.e("benchmark", "bechmark: " + (t2 - t1) / 1000f);
    }
}
