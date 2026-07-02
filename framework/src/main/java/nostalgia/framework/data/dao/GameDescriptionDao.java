package nostalgia.framework.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import nostalgia.framework.data.entity.GameDescriptionEntity;

/**
 * 游戏描述数据访问对象（DAO）。
 * <p>
 * 提供对 GameDescription 表的增删改查操作，
 * 支持按名称、最近游玩、游玩次数、插入时间等多种排序和模糊搜索。
 * </p>
 */
@Dao
public interface GameDescriptionDao {

    /** 插入单条游戏记录，返回生成的行 ID */
    @Insert
    long insert(GameDescriptionEntity game);

    /** 批量插入游戏记录 */
    @Insert
    void insertAll(List<GameDescriptionEntity> games);

    /** 更新游戏记录 */
    @Update
    void update(GameDescriptionEntity game);

    /** 删除游戏记录 */
    @Delete
    void delete(GameDescriptionEntity game);

    /** 根据 ID 删除游戏 */
    @Query("DELETE FROM GameDescription WHERE _id = :id")
    void deleteById(long id);

    /** 删除所有游戏记录 */
    @Query("DELETE FROM GameDescription")
    void deleteAll();

    /** 获取所有游戏记录 */
    @Query("SELECT * FROM GameDescription")
    List<GameDescriptionEntity> getAll();

    /** 根据 ID 查询游戏 */
    @Query("SELECT * FROM GameDescription WHERE _id = :id")
    GameDescriptionEntity getById(long id);

    /** 根据校验和查询游戏（唯一匹配） */
    @Query("SELECT * FROM GameDescription WHERE checksum = :checksum LIMIT 1")
    GameDescriptionEntity getByChecksum(String checksum);

    /** 按名称忽略大小写升序获取所有游戏 */
    @Query("SELECT * FROM GameDescription ORDER BY name COLLATE NOCASE ASC")
    List<GameDescriptionEntity> getAllSortedByName();

    /** 按最近游玩时间降序获取所有游戏 */
    @Query("SELECT * FROM GameDescription ORDER BY lastGameTime DESC")
    List<GameDescriptionEntity> getAllSortedByLastPlayed();

    /** 按游玩次数降序获取所有游戏 */
    @Query("SELECT * FROM GameDescription ORDER BY runCount DESC")
    List<GameDescriptionEntity> getAllSortedByMostPlayed();

    /** 按插入时间降序获取所有游戏 */
    @Query("SELECT * FROM GameDescription ORDER BY inserTime DESC")
    List<GameDescriptionEntity> getAllSortedByInsertTime();

    /** 获取游戏总数 */
    @Query("SELECT COUNT(*) FROM GameDescription")
    int getCount();

    /** 按名称模糊搜索游戏（忽略大小写） */
    @Query("SELECT * FROM GameDescription WHERE name LIKE :filter ORDER BY name COLLATE NOCASE ASC")
    List<GameDescriptionEntity> searchByName(String filter);

    /** 根据 ZIP 文件 ID 获取该压缩包内的所有游戏 */
    @Query("SELECT * FROM GameDescription WHERE zipfile_id = :zipfileId")
    List<GameDescriptionEntity> getByZipFileId(long zipfileId);

    /** 根据 ZIP 文件 ID 删除关联的所有游戏 */
    @Query("DELETE FROM GameDescription WHERE zipfile_id = :zipfileId")
    void deleteByZipFileId(long zipfileId);
}
