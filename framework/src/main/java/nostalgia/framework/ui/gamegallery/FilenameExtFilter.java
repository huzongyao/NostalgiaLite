package nostalgia.framework.ui.gamegallery;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FilenameExtFilter implements FilenameFilter {

    Set<String> exts;
    boolean showDir = false;
    boolean showHiden = false;

    public FilenameExtFilter(String[] exts, boolean showDirs, boolean showHiden) {
        Set<String> tmp = new HashSet<>();

        Collections.addAll(tmp, exts);

        showDir = showDirs;
        this.showHiden = showHiden;
        this.exts = addDots(tmp);
    }

    public FilenameExtFilter(Set<String> exts, boolean showDirs,
                             boolean showHiden) {
        showDir = showDirs;
        this.showHiden = showHiden;
        this.exts = addDots(exts);
    }

    public FilenameExtFilter(String... exts) {
        this(exts, false, false);
    }

    public FilenameExtFilter(String ext) {
        this(new String[]{ext}, false, false);
    }

    private Set<String> addDots(Set<String> exts) {
        Set<String> temp = new HashSet<>();

        for (String ext : exts) {
            temp.add("." + ext);
        }

        return temp;
    }

    public boolean accept(File dir, String filename) {
        if ((!showHiden) && (filename.charAt(0) == '.'))
            return false;

        if (showDir) {
            File f = new File(dir, filename);

            if (f.isDirectory())
                return true;
        }

        String fnLower = filename.toLowerCase();

        for (String ext : exts) {
            if (fnLower.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

}