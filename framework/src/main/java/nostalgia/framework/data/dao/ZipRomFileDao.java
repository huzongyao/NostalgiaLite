package nostalgia.framework.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import nostalgia.framework.data.entity.ZipRomFileEntity;

@Dao
public interface ZipRomFileDao {
    
    @Insert
    long insert(ZipRomFileEntity zipFile);
    
    @Update
    void update(ZipRomFileEntity zipFile);
    
    @Delete
    void delete(ZipRomFileEntity zipFile);
    
    @Query("DELETE FROM ZipRomFile WHERE _id = :id")
    void deleteById(long id);
    
    @Query("DELETE FROM ZipRomFile")
    void deleteAll();
    
    @Query("SELECT * FROM ZipRomFile")
    List<ZipRomFileEntity> getAll();
    
    @Query("SELECT * FROM ZipRomFile WHERE _id = :id")
    ZipRomFileEntity getById(long id);
    
    @Query("SELECT * FROM ZipRomFile WHERE hash = :hash LIMIT 1")
    ZipRomFileEntity getByHash(String hash);
    
    @Query("SELECT * FROM ZipRomFile WHERE path = :path LIMIT 1")
    ZipRomFileEntity getByPath(String path);
}
