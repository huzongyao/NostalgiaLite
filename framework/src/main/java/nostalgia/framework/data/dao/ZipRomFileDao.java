package nostalgia.framework.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import nostalgia.framework.data.entity.ZipRomFileEntity;

/**
 * ZIP ROM 文件数据访问对象（DAO）。
 * <p>
 * 提供对 ZipRomFile 表的增删改查操作，
 * 支持按 ID、哈希值、路径查询。
 * </p>
 */
@Dao
public interface ZipRomFileDao {

    /** 插入 ZIP ROM 文件记录，返回生成的行 ID */
    @Insert
    long insert(ZipRomFileEntity zipFile);

    /** 更新 ZIP ROM 文件记录 */
    @Update
    void update(ZipRomFileEntity zipFile);

    /** 删除 ZIP ROM 文件记录 */
    @Delete
    void delete(ZipRomFileEntity zipFile);

    /** 根据 ID 删除 ZIP ROM 文件 */
    @Query("DELETE FROM ZipRomFile WHERE _id = :id")
    void deleteById(long id);

    /** 删除所有 ZIP ROM 文件记录 */
    @Query("DELETE FROM ZipRomFile")
    void deleteAll();

    /** 获取所有 ZIP ROM 文件 */
    @Query("SELECT * FROM ZipRomFile")
    List<ZipRomFileEntity> getAll();

    /** 根据 ID 查询 ZIP ROM 文件 */
    @Query("SELECT * FROM ZipRomFile WHERE _id = :id")
    ZipRomFileEntity getById(long id);

    /** 根据哈希值查询 ZIP ROM 文件（唯一匹配） */
    @Query("SELECT * FROM ZipRomFile WHERE hash = :hash LIMIT 1")
    ZipRomFileEntity getByHash(String hash);

    /** 根据路径查询 ZIP ROM 文件（唯一匹配） */
    @Query("SELECT * FROM ZipRomFile WHERE path = :path LIMIT 1")
    ZipRomFileEntity getByPath(String path);
}
