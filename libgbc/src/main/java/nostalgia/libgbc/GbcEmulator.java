package nostalgia.libgbc;

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
import nostalgia.framework.ui.gamegallery.GameDescription;

public class GbcEmulator extends JniEmulator {

    public final static String PACK_SUFFIX = "ngbcs";
    private static GbcEmulator instance;
    private static EmulatorInfo info = new Info();

    private GbcEmulator() {
    }

    public static GbcEmulator getInstance() {
        if (instance == null) {
            instance = new GbcEmulator();
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
        int type = -1;
        gg = gg.replace("-", "");

        if (gg.length() == 9 || gg.length() == 6) {
            type = 0;
            String part1 = gg.substring(0, 3);
            String part2 = gg.substring(3, 6);
            gg = part1.concat("-").concat(part2);

            if (gg.length() == 6) {
                String part3 = gg.substring(6, 9);
                gg = gg.concat("-").concat(part3);
            }

        } else if (gg.startsWith("01") && gg.length() == 8) {
            type = 1;
        }

        if (type == -1 || !getBridge().enableCheat(gg, type)) {
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

        static List<GfxProfile> profiles = new ArrayList<GfxProfile>();
        static List<SfxProfile> sfxProfiles = new ArrayList<SfxProfile>();

        static {
            GfxProfile prof = new GbcGfxProfile();
            prof.fps = 60;
            prof.name = "default";
            prof.originalScreenWidth = 160;
            prof.originalScreenHeight = 144;
            profiles.add(prof);
            SfxProfile sfx = new GbcSfxProfile();
            sfx.name = "default";
            sfx.isStereo = true;
            sfx.encoding = SoundEncoding.PCM16;
            sfx.bufferSize = 2048 * 8;
            sfx.rate = 22050;
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
            return "Nostalgia.GBC";
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
            mapping.put(EmulatorController.KEY_A, 0x01);
            mapping.put(EmulatorController.KEY_B, 0x02);
            mapping.put(EmulatorController.KEY_SELECT, 0x04);
            mapping.put(EmulatorController.KEY_START, 0x08);
            mapping.put(EmulatorController.KEY_UP, 0x40);
            mapping.put(EmulatorController.KEY_DOWN, 0x80);
            mapping.put(EmulatorController.KEY_LEFT, 0x20);
            mapping.put(EmulatorController.KEY_RIGHT, 0x10);
            mapping.put(EmulatorController.KEY_A_TURBO, 0x01 + 1000);
            mapping.put(EmulatorController.KEY_B_TURBO, 0x02 + 1000);
            return mapping;
        }

        @Override
        public int getNumQualityLevels() {
            return 2;
        }

        private static class GbcGfxProfile extends GfxProfile {
            @Override
            public int toInt() {
                return 0;
            }
        }

        private static class GbcSfxProfile extends SfxProfile {
            @Override
            public int toInt() {
                return 0;
            }
        }

    }

}
