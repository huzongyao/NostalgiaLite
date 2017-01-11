package nostalgia.framework.ui.gamegallery;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.Serializable;

import nostalgia.framework.utils.Utils;
import nostalgia.framework.utils.annotations.Column;
import nostalgia.framework.utils.annotations.Table;

@Table
public class GameDescription implements Serializable, Comparable<GameDescription> {

    private static final long serialVersionUID = -4166819653487858374L;

    @Column(hasIndex = true)
    public String name = "";

    @Column
    public String path = "";

    @Column(hasIndex = true)
    public String checksum = "";

    @Column(isPrimaryKey = true)
    public long _id;

    @Column
    public long zipfile_id = -1;

    @Column(hasIndex = true)
    public long inserTime = 0;

    @Column(hasIndex = true)
    public long lastGameTime = 0;

    @Column
    public int runCount = 0;
    private String cleanNameCache = null;
    private String sortNameCache = null;

    public GameDescription() {
    }

    public GameDescription(File file) {
        this(file, "");
        checksum = Utils.getMD5Checksum(file);
    }


    public GameDescription(File file, String checksum) {
        name = file.getName();
        path = file.getAbsolutePath();
        this.checksum = checksum;
    }

    public GameDescription(String name, String path, String checksum) {
        this.name = name;
        this.path = path;
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return name + " " + checksum + " zipId:" + zipfile_id;
    }

    public boolean isInArchive() {
        return zipfile_id != -1;
    }

    public String getCleanName() {
        if (cleanNameCache == null) {
            String name = Utils.removeExt(this.name);
            int idx = name.lastIndexOf('/');

            if (idx != -1) {
                cleanNameCache = name.substring(idx + 1);

            } else {
                cleanNameCache = name;
            }
        }

        return cleanNameCache;
    }

    @SuppressLint("DefaultLocale")
    public String getSortName() {
        if (sortNameCache == null) {
            sortNameCache = getCleanName().toLowerCase();
        }

        return sortNameCache;
    }

    @Override
    public int compareTo(GameDescription another) {
        return checksum.compareTo(another.checksum);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof GameDescription)) {
            return false;

        } else {
            GameDescription gd = (GameDescription) o;
            return gd.checksum == null ? false : checksum.equals(gd.checksum);
        }
    }

}
