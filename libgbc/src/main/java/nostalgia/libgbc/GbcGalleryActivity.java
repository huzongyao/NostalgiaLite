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
        HashSet<String> exts = new HashSet<String>();
        exts.add("gb");
        exts.add("gbc");
        return exts;
    }

    @Override
    public Emulator getEmulatorInstance() {
        return GbcEmulator.getInstance();
    }

}
