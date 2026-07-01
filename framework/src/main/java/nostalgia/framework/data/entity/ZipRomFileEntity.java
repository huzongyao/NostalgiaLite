package nostalgia.framework.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * ZIP ROM 文件实体，对应数据库 ZipRomFile 表。
 * <p>
 * 存储 ZIP 压缩包的基本信息，包括哈希值和文件路径。
 * 一个 ZIP 文件可包含多个游戏（通过 GameDescriptionEntity.zipfile_id 关联）。
 * </p>
 */
@Entity(tableName = "ZipRomFile")
public class ZipRomFileEntity {

    /** 主键，自增 */
    @PrimaryKey(autoGenerate = true)
    public long _id;

    /** ZIP 文件的哈希值，用于唯一标识 */
    public String hash = "";

    /** ZIP 文件路径 */
    public String path = "";
}
