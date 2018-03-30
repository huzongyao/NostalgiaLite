package nostalgia.appgg;

import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;

import nostalgia.framework.BasicEmulatorInfo;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.EmulatorException;
import nostalgia.framework.EmulatorInfo;
import nostalgia.framework.GfxProfile;
import nostalgia.framework.KeyboardProfile;
import nostalgia.framework.SfxProfile;
import nostalgia.framework.SfxProfile.SoundEncoding;
import nostalgia.framework.base.JniBridge;
import nostalgia.framework.base.JniEmulator;
import nostalgia.framework.controllers.KeyboardController;
import nostalgia.framework.ui.gamegallery.GameDescription;

public class GGEmulator extends JniEmulator {

    public final static String PACK_SUFFIX = "nggs";
    private static GGEmulator instance;
    private static EmulatorInfo info = new Info();

    private GGEmulator() {
    }

    public static GGEmulator getInstance() {
        if (instance == null) {
            instance = new GGEmulator();
        }
        return instance;
    }

    @Override
    public EmulatorInfo getInfo() {
        if (info == null) {
            info = new Info();
        }
        return info;
    }

    @Override
    public JniBridge getBridge() {
        return Core.getInstance();
    }

    @Override
    public void enableCheat(String gg) {
        int addrVal = -1;
        int valVal = -1;
        gg = gg.replace("-", "");

        if (gg.startsWith("00") && gg.length() == 8) {
            String addr = gg.substring(2, 5 + 1);
            String val = gg.substring(6);
            addrVal = Integer.parseInt(addr, 16);
            valVal = Integer.parseInt(val, 16);
        }

        if ((addrVal < 0 || valVal < 0)
                || !getBridge().enableRawCheat(addrVal, valVal, -1)) {
            throw new EmulatorException(R.string.act_emulator_invalid_cheat, gg);
        }
    }

    @Override
    public GfxProfile autoDetectGfx(GameDescription game) {
        return getInfo().getDefaultGfxProfile();
    }

    @Override
    public SfxProfile autoDetectSfx(GameDescription game) {
        return getInfo().getDefaultSfxProfile();
    }

    private static class Info extends BasicEmulatorInfo {

        static List<GfxProfile> profiles = new ArrayList<>();
        static List<SfxProfile> sfxProfiles = new ArrayList<>();

        static {
            GfxProfile prof = new GGGfxProfile();
            prof.fps = 60;
            prof.name = "default";
            prof.originalScreenWidth = 160;
            prof.originalScreenHeight = 144;
            profiles.add(prof);

            SfxProfile sfx = new GGSfxProfile();
            sfx.name = "low";
            sfx.isStereo = true;
            sfx.encoding = SoundEncoding.PCM16;
            sfx.bufferSize = 2048 * 8 * 2;
            sfx.quality = 0;
            sfx.rate = 22050;
            sfxProfiles.add(sfx);

            sfx = new GGSfxProfile();
            sfx.name = "medium";
            sfx.isStereo = true;
            sfx.encoding = SoundEncoding.PCM16;
            sfx.bufferSize = 2048 * 8 * 2;
            sfx.rate = 44100;
            sfx.quality = 1;
            sfxProfiles.add(sfx);

            sfx = new GGSfxProfile();
            sfx.name = "high";
            sfx.isStereo = true;
            sfx.encoding = SoundEncoding.PCM16;
            sfx.bufferSize = 2048 * 8 * 2;
            sfx.rate = 44100;
            sfx.quality = 2;
            sfxProfiles.add(sfx);
        }

        @Override
        public boolean hasZapper() {
            return false;
        }

        @Override
        public boolean isMultiPlayerSupported() {
            return false;
        }

        @Override
        public String getName() {
            return "Nostalgia.GG";
        }

        @Override
        public GfxProfile getDefaultGfxProfile() {
            return profiles.get(0);
        }

        @Override
        public SfxProfile getDefaultSfxProfile() {
            return sfxProfiles.get(0);
        }

        @Override
        public KeyboardProfile getDefaultKeyboardProfile() {
            return null;
        }

        @Override
        public List<GfxProfile> getAvailableGfxProfiles() {
            return profiles;
        }

        @Override
        public List<SfxProfile> getAvailableSfxProfiles() {
            return sfxProfiles;
        }

        @Override
        public boolean supportsRawCheats() {
            return false;
        }

        @Override
        public SparseIntArray getKeyMapping() {
            SparseIntArray mapping = new SparseIntArray();
            mapping.put(EmulatorController.KEY_UP, 0x01);
            mapping.put(EmulatorController.KEY_DOWN, 0x02);
            mapping.put(EmulatorController.KEY_LEFT, 0x04);
            mapping.put(EmulatorController.KEY_RIGHT, 0x08);
            mapping.put(EmulatorController.KEY_A, 0x10);
            mapping.put(EmulatorController.KEY_B, 0x20);
            mapping.put(EmulatorController.KEY_START, 0x80);
            mapping.put(EmulatorController.KEY_SELECT, -1);
            mapping.put(EmulatorController.KEY_A_TURBO, 0x10 + 1000);
            mapping.put(EmulatorController.KEY_B_TURBO, 0x20 + 1000);
            return mapping;
        }

        @Override
        public int[] getDeviceKeyboardCodes() {
            return new int[]{
                    EmulatorController.KEY_UP,
                    EmulatorController.KEY_DOWN,
                    EmulatorController.KEY_RIGHT,
                    EmulatorController.KEY_LEFT,
                    EmulatorController.KEY_START,

                    EmulatorController.KEY_A, EmulatorController.KEY_B,
                    EmulatorController.KEY_A_TURBO,
                    EmulatorController.KEY_B_TURBO,

                    KeyboardController.KEYS_LEFT_AND_UP,
                    KeyboardController.KEYS_RIGHT_AND_UP,

                    KeyboardController.KEYS_RIGHT_AND_DOWN,
                    KeyboardController.KEYS_LEFT_AND_DOWN,

                    KeyboardController.KEY_SAVE_SLOT_0,
                    KeyboardController.KEY_LOAD_SLOT_0,

                    KeyboardController.KEY_SAVE_SLOT_1,
                    KeyboardController.KEY_LOAD_SLOT_1,

                    KeyboardController.KEY_SAVE_SLOT_2,
                    KeyboardController.KEY_LOAD_SLOT_2,

                    KeyboardController.KEY_MENU,
                    KeyboardController.KEY_FAST_FORWARD,
                    KeyboardController.KEY_BACK
            };
        }

        @Override
        public String[] getDeviceKeyboardNames() {
            return new String[]{"UP", "DOWN", "RIGHT", "LEFT", "START", "1",
                    "2", "TURBO 1", "TURBO 2", "LEFT+UP", "RIGHT+UP",
                    "RIGHT+DOWN", "LEFT+DOWN", "SAVE STATE 1",
                    "LOAD STATE 1",
                    "SAVE STATE 2",
                    "LOAD STATE 2",
                    "SAVE STATE 3",
                    "LOAD STATE 3",
                    "MENU", "FAST FORWARD", "EXIT",
            };
        }

        @Override
        public int getNumQualityLevels() {
            return 3;
        }

        private static class GGGfxProfile extends GfxProfile {
            @Override
            public int toInt() {
                return 0;
            }
        }

        private static class GGSfxProfile extends SfxProfile {
            @Override
            public int toInt() {
                return this.rate;
            }
        }

    }

}
