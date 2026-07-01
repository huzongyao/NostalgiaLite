package nostalgia.framework.ui.gamegallery;


import androidx.annotation.NonNull;

import java.io.File;
import java.io.Serializable;

import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.annotations.Column;
import nostalgia.framework.utils.annotations.Table;

/**
 * 游戏描述领域对象。
 * <p>
 * 表示一个 ROM 游戏的基本信息，包括名称、路径、校验和、
 * 所属 ZIP 文件、插入时间、最近游玩时间和游玩次数。
 * 实现 Serializable 以便通过 Intent 传递，实现 Comparable 以支持排序。
 * </p>
 */
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
        checksum = EmuUtils.getMD5Checksum(file);
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

    /** 判断游戏是否在压缩包中 */
    public boolean isInArchive() {
        return zipfile_id != -1;
    }

    /** 获取去除扩展名和路径后的干净游戏名 */
    public String getCleanName() {
        if (cleanNameCache == null) {
            String name = EmuUtils.removeExt(this.name);
            int idx = name.lastIndexOf('/');
            if (idx != -1) {
                cleanNameCache = name.substring(idx + 1);
            } else {
                cleanNameCache = name;
            }
        }
        return cleanNameCache;
    }

    /** 获取用于排序的小写名称 */
    public String getSortName() {
        if (sortNameCache == null) {
            sortNameCache = getCleanName().toLowerCase();
        }
        return sortNameCache;
    }
    
    /** 获取文件大小（字节） */
    public long getFileSize() {
        File file = new File(path);
        return file.exists() ? file.length() : 0;
    }
    
    /** 获取格式化的文件大小字符串 */
    public String getFileSizeFormatted() {
        long size = getFileSize();
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }
    }

    @Override
    public int compareTo(@NonNull GameDescription another) {
        return checksum.compareTo(another.checksum);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof GameDescription)) {
            return false;
        } else {
            GameDescription gd = (GameDescription) o;
            return gd.checksum != null && checksum.equals(gd.checksum);
        }
    }

}
