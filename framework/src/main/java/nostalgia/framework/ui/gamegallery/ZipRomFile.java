package nostalgia.framework.ui.gamegallery;

import java.io.File;
import java.util.ArrayList;

import nostalgia.framework.utils.annotations.Column;
import nostalgia.framework.utils.annotations.ObjectFromOtherTable;
import nostalgia.framework.utils.annotations.Table;

/**
 * ZIP ROM 文件领域对象。
 * <p>
 * 表示一个 ZIP 压缩包及其包含的游戏列表。
 * 通过文件路径和长度的哈希值唯一标识 ZIP 文件。
 * </p>
 */
@Table
public class ZipRomFile {

    @Column(isPrimaryKey = true)
    public long _id;

    @Column
    public String hash;

    @Column
    public String path;

    @ObjectFromOtherTable(columnName = "zipfile_id")
    public ArrayList<GameDescription> games = new ArrayList<>();

    public ZipRomFile() {
    }

    /** 根据文件路径和长度计算 ZIP 文件哈希值 */
    public static String computeZipHash(File zipFile) {
        return zipFile.getAbsolutePath().concat("-" + zipFile.length()).hashCode() + "";
    }
}
