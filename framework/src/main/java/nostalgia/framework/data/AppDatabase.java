package nostalgia.framework.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import nostalgia.framework.data.dao.GameDescriptionDao;
import nostalgia.framework.data.dao.ZipRomFileDao;
import nostalgia.framework.data.entity.GameDescriptionEntity;
import nostalgia.framework.data.entity.ZipRomFileEntity;

/**
 * 应用 Room 数据库。
 * <p>
 * 采用双重检查锁定单例模式，确保全局只有一个数据库实例。
 * 包含游戏描述和 ZIP ROM 文件两张表。
 * 数据库版本升级时使用破坏性迁移（删除重建）。
 * </p>
 */
@Database(entities = {GameDescriptionEntity.class, ZipRomFileEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** 数据库文件名 */
    private static final String DATABASE_NAME = "nostalgia_db";
    /** 数据库单例实例（volatile 保证双重检查锁定的可见性） */
    private static volatile AppDatabase INSTANCE;

    /** 获取游戏描述 DAO */
    public abstract GameDescriptionDao gameDescriptionDao();

    /** 获取 ZIP ROM 文件 DAO */
    public abstract ZipRomFileDao zipRomFileDao();

    /**
     * 获取数据库单例实例。
     *
     * @param context 应用上下文
     * @return 数据库实例
     */
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
