package nostalgia.framework.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 游戏描述实体，对应数据库 GameDescription 表。
 * <p>
 * 存储游戏的基本信息，包括名称、路径、校验和、所属 ZIP 文件、
 * 插入时间、最近游玩时间和游玩次数。
 * 在 checksum、insertTime、lastGameTime 字段上建立索引以加速查询。
 * </p>
 */
@Entity(tableName = "GameDescription",
        indices = {
                @Index(value = "checksum"),      // 校验和索引，用于快速查重
                @Index(value = "insertTime"),      // 插入时间索引，用于按时间排序
                @Index(value = "lastGameTime")    // 最近游玩时间索引，用于排序
        })
public class GameDescriptionEntity {

    /** 主键，自增 */
    @PrimaryKey(autoGenerate = true)
    public long _id;

    /** 游戏名称 */
    public String name = "";

    /** ROM 文件路径 */
    public String path = "";

    /** ROM 文件校验和，用于唯一标识游戏 */
    public String checksum = "";

    /** 所属 ZIP 压缩文件 ID，-1 表示不属于压缩包 */
    public long zipfile_id = -1;

    /** 游戏插入时间（时间戳） */
    public long insertTime = 0;

    /** 最近一次游玩时间（时间戳） */
    public long lastGameTime = 0;

    /** 累计游玩次数 */
    public int runCount = 0;
}
