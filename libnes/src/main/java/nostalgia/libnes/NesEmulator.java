package nostalgia.libnes;

import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nostalgia.framework.BasicEmulatorInfo;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.EmulatorInfo;
import nostalgia.framework.GfxProfile;
import nostalgia.framework.SfxProfile;
import nostalgia.framework.SfxProfile.SoundEncoding;
import nostalgia.framework.base.JniBridge;
import nostalgia.framework.base.JniEmulator;
import nostalgia.framework.ui.gamegallery.GameDescription;

public class NesEmulator extends JniEmulator {

    public static final String PACK_SUFFIX = "nness";
    private static EmulatorInfo info;
    private static NesEmulator instance;
    public String[] palExclusiveKeywords = new String[]{".beauty|beast",
            ".hammerin|harry", ".noah|ark", ".rockets|rivals",
            ".formula|sensation", ".trolls|crazyland", "asterix", "elite",
            "smurfs", "international cricket", "turrican", "valiant",
            "aladdin", "aussie rules", "banana prince", "chevaliers",
            "crackout", "devil world", "kick off", "hyper soccer", "ufouria",
            "lion king", "gimmick", "dropzone", "drop zone", "$mario bros",
            "road fighter", "rodland", "parasol stars", "parodius",
            "over horizon", "championship rally", "aussio rules",
    };

    public String[] palHashes = new String[]{
            "85ce1107c922600990884d63c75cfec4",
            "6f6d5cc27354e1527fc88ec97c8b7c27",
            "83c8b2142884965c2214196f3f71f6ec",
            "caf9d44ae71fa8ade852fb453d797798",
            "fe36a09cd6c94916d48ea61776978cc8",
            "3eb49813c3c5b6088bfed3f1d7ecaa0e",
            "b40b25a9bc54eb8f46310fae45723759",
            "d91a5f3e924916eb16bb6a3255f532bc",
    };

    private NesEmulator() {
    }

    public static JniEmulator getInstance() {
        if (instance == null) {
            instance = new NesEmulator();
        }
        return instance;
    }

    @Override
    public GfxProfile autoDetectGfx(GameDescription game) {
        String name = game.getCleanName();
        name = name.toLowerCase();
        if (name.contains("(e)") || name.contains("(europe)")
                || name.contains("(f)") || name.contains("(g)")
                || name.contains("(i)") || name.contains("(pal)")
                || name.contains("[e]") || name.contains("[f]")
                || name.contains("[g]") || name.contains("[i]")
                || name.contains("[europe]") || name.contains("[pal]")) {
            return Info.pal;
        } else {
            for (String pal : palExclusiveKeywords) {
                if (pal.startsWith("$")) {
                    pal = pal.substring(1);
                    if (name.startsWith(pal)) {
                        return Info.pal;
                    }
                } else {
                    String[] kws = new String[1];
                    kws[0] = pal;
                    if (pal.startsWith(".")) {
                        pal = pal.substring(1);
                        kws = pal.split("\\|");
                    }
                    for (String keyWord : kws) {
                        if (name.contains(keyWord)) {
                            return Info.pal;
                        }
                    }
                }
            }
        }
        if (Arrays.asList(palHashes).contains(game.checksum)) {
            return Info.pal;
        }
        return getInfo().getDefaultGfxProfile();
    }

    @Override
    public SfxProfile autoDetectSfx(GameDescription game) {
        return getInfo().getDefaultSfxProfile();
    }

    @Override
    public JniBridge getBridge() {
        return Core.getInstance();
    }

    @Override
    public EmulatorInfo getInfo() {
        if (info == null) {
            info = new Info();
        }
        return info;
    }

    private static class Info extends BasicEmulatorInfo {
        private static List<SfxProfile> sfxProfiles = new ArrayList<>();
        private static List<GfxProfile> gfxProfiles = new ArrayList<>();
        private static GfxProfile pal;
        private static GfxProfile ntsc;

        static {
            ntsc = new NesGfxProfile();
            ntsc.fps = 60;
            ntsc.name = "NTSC";
            ntsc.originalScreenWidth = 256;
            ntsc.originalScreenHeight = 224;
            gfxProfiles.add(ntsc);

            pal = new NesGfxProfile();
            pal.fps = 50;
            pal.name = "PAL";
            pal.originalScreenWidth = 256;
            pal.originalScreenHeight = 240;
            gfxProfiles.add(pal);

            SfxProfile low = new NesSfxProfile();
            low.name = "low";
            low.bufferSize = 2048 * 8 * 2;
            low.encoding = SoundEncoding.PCM16;
            low.isStereo = true;
            low.rate = 11025;
            low.quality = 0;
            sfxProfiles.add(low);

            SfxProfile medium = new NesSfxProfile();
            medium.name = "medium";
            medium.bufferSize = 2048 * 8 * 2;
            medium.encoding = SoundEncoding.PCM16;
            medium.isStereo = true;
            medium.rate = 22050;
            medium.quality = 1;
            sfxProfiles.add(medium);

            SfxProfile high = new NesSfxProfile();
            high.name = "high";
            high.bufferSize = 2048 * 8 * 2;
            high.encoding = SoundEncoding.PCM16;
            high.isStereo = true;
            high.rate = 44100;
            high.quality = 2;
            sfxProfiles.add(high);
        }

        public boolean hasZapper() {
            return true;
        }

        @Override
        public SparseIntArray getKeyMapping() {
            SparseIntArray mapping = new SparseIntArray();
            mapping.put(EmulatorController.KEY_A, 0x01);
            mapping.put(EmulatorController.KEY_B, 0x02);
            mapping.put(EmulatorController.KEY_SELECT, 0x04);
            mapping.put(EmulatorController.KEY_START, 0x08);
            mapping.put(EmulatorController.KEY_UP, 0x10);
            mapping.put(EmulatorController.KEY_DOWN, 0x20);
            mapping.put(EmulatorController.KEY_LEFT, 0x40);
            mapping.put(EmulatorController.KEY_RIGHT, 0x80);
            mapping.put(EmulatorController.KEY_A_TURBO, 0x01 + 1000);
            mapping.put(EmulatorController.KEY_B_TURBO, 0x02 + 1000);
            return mapping;
        }

        @Override
        public String getName() {
            return "Nostalgia.NES";
        }

        @Override
        public GfxProfile getDefaultGfxProfile() {
            return ntsc;
        }

        @Override
        public SfxProfile getDefaultSfxProfile() {
            return sfxProfiles.get(0);
        }

        @Override
        public List<GfxProfile> getAvailableGfxProfiles() {
            return gfxProfiles;
        }

        @Override
        public List<SfxProfile> getAvailableSfxProfiles() {
            return sfxProfiles;
        }

        public boolean supportsRawCheats() {
            return true;
        }

        @Override
        public int getNumQualityLevels() {
            return 3;
        }

        private static class NesGfxProfile extends GfxProfile {
            @Override
            public int toInt() {
                return fps == 50 ? 1 : 0;
            }
        }

        private static class NesSfxProfile extends SfxProfile {
            @Override
            public int toInt() {
                int x = rate / 11025;
                x += quality * 100;
                return x;
            }
        }
    }
}
