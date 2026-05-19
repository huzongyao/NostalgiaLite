package nostalgia.framework.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import nostalgia.framework.data.entity.GameDescriptionEntity;

@Dao
public interface GameDescriptionDao {
    
    @Insert
    long insert(GameDescriptionEntity game);
    
    @Insert
    void insertAll(List<GameDescriptionEntity> games);
    
    @Update
    void update(GameDescriptionEntity game);
    
    @Delete
    void delete(GameDescriptionEntity game);
    
    @Query("DELETE FROM GameDescription WHERE _id = :id")
    void deleteById(long id);
    
    @Query("DELETE FROM GameDescription")
    void deleteAll();
    
    @Query("SELECT * FROM GameDescription")
    List<GameDescriptionEntity> getAll();
    
    @Query("SELECT * FROM GameDescription WHERE _id = :id")
    GameDescriptionEntity getById(long id);
    
    @Query("SELECT * FROM GameDescription WHERE checksum = :checksum LIMIT 1")
    GameDescriptionEntity getByChecksum(String checksum);
    
    @Query("SELECT * FROM GameDescription ORDER BY name COLLATE NOCASE ASC")
    List<GameDescriptionEntity> getAllSortedByName();
    
    @Query("SELECT * FROM GameDescription ORDER BY lastGameTime DESC")
    List<GameDescriptionEntity> getAllSortedByLastPlayed();
    
    @Query("SELECT * FROM GameDescription ORDER BY runCount DESC")
    List<GameDescriptionEntity> getAllSortedByMostPlayed();
    
    @Query("SELECT * FROM GameDescription ORDER BY inserTime DESC")
    List<GameDescriptionEntity> getAllSortedByInsertTime();
    
    @Query("SELECT COUNT(*) FROM GameDescription")
    int getCount();
    
    @Query("SELECT * FROM GameDescription WHERE name LIKE :filter ORDER BY name COLLATE NOCASE ASC")
    List<GameDescriptionEntity> searchByName(String filter);
    
    @Query("SELECT * FROM GameDescription WHERE zipfile_id = :zipfileId")
    List<GameDescriptionEntity> getByZipFileId(long zipfileId);
}
