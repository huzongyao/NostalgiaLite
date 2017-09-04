package nostalgia.framework.base;

import android.content.Context;

import java.io.File;

import nostalgia.framework.EmulatorException;
import nostalgia.framework.R;

public class EmulatorUtils {

    public static String getBaseDir(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            throw new EmulatorException(R.string.gallery_sd_card_not_mounted);
        }
        File baseDir = new File(dir.getAbsolutePath());
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new EmulatorException("could not create working directory");
            }
        }
        return baseDir.getAbsolutePath();
    }

}
