package nostalgia.framework.ui.gamegallery;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件名扩展名过滤器。
 * <p>
 * 实现 FilenameFilter 接口，根据指定的扩展名集合过滤文件。
 * 支持显示/隐藏目录和隐藏文件的配置。
 * </p>
 */
public class FilenameExtFilter implements FilenameFilter {

    /** 允许的文件扩展名集合（带点前缀） */
    Set<String> exts;
    /** 是否显示目录 */
    boolean showDir = false;
    /** 是否显示隐藏文件 */
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

    /** 为扩展名添加点前缀 */
    private Set<String> addDots(Set<String> exts) {
        Set<String> temp = new HashSet<>();

        for (String ext : exts) {
            temp.add("." + ext);
        }

        return temp;
    }

    /** 根据扩展名、目录和隐藏文件配置判断是否接受文件 */
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