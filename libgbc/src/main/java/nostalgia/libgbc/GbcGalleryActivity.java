package nostalgia.libgbc;

import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.GalleryActivity;

public class GbcGalleryActivity extends GalleryActivity {

    @Override
    public Class<? extends EmulatorActivity> getEmulatorActivityClass() {
        return GbcEmulatorActivity.class;
    }

    @Override
    protected Set<String> getRomExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("gb");
        set.add("gbc");
        return set;
    }

    @Override
    public Emulator getEmulatorInstance() {
        return GbcEmulator.getInstance();
    }

}
