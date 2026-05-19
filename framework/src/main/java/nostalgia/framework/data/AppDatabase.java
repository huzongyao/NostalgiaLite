package nostalgia.framework.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import nostalgia.framework.data.dao.GameDescriptionDao;
import nostalgia.framework.data.dao.ZipRomFileDao;
import nostalgia.framework.data.entity.GameDescriptionEntity;
import nostalgia.framework.data.entity.ZipRomFileEntity;

@Database(entities = {GameDescriptionEntity.class, ZipRomFileEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "nostalgia_db";
    private static volatile AppDatabase INSTANCE;
    
    public abstract GameDescriptionDao gameDescriptionDao();
    public abstract ZipRomFileDao zipRomFileDao();
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
