package nostalgia.framework.data;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nostalgia.framework.data.dao.GameDescriptionDao;
import nostalgia.framework.data.dao.ZipRomFileDao;
import nostalgia.framework.data.entity.GameDescriptionEntity;
import nostalgia.framework.data.entity.ZipRomFileEntity;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.gamegallery.ZipRomFile;
import nostalgia.framework.utils.EmuUtils;

/**
 * 游戏数据仓库，封装对 Room 数据库的 CRUD 操作。
 * <p>
 * 管理游戏描述（GameDescription）和 ZIP ROM 文件（ZipRomFile）两类数据，
 * 提供实体与领域对象之间的转换，支持多种排序和搜索方式。
 * 采用双重检查锁定单例模式。
 * </p>
 */
public class GameRepository {

    private static volatile GameRepository INSTANCE;
    private final GameDescriptionDao gameDescriptionDao;
    private final ZipRomFileDao zipRomFileDao;

    private GameRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        gameDescriptionDao = database.gameDescriptionDao();
        zipRomFileDao = database.zipRomFileDao();
    }
    
    public static GameRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (GameRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GameRepository(context);
                }
            }
        }
        return INSTANCE;
    }
    
    /** 获取所有游戏（默认顺序） */
    public ArrayList<GameDescription> getAllGames() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAll();
        return convertToGameDescriptions(entities);
    }
    
    /** 按名称排序获取所有游戏 */
    public ArrayList<GameDescription> getAllGamesSortedByName() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByName();
        return convertToGameDescriptions(entities);
    }
    
    /** 按最近游玩时间排序获取所有游戏 */
    public ArrayList<GameDescription> getAllGamesSortedByLastPlayed() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByLastPlayed();
        return convertToGameDescriptions(entities);
    }
    
    /** 按游玩次数排序获取所有游戏 */
    public ArrayList<GameDescription> getAllGamesSortedByMostPlayed() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByMostPlayed();
        return convertToGameDescriptions(entities);
    }
    
    /** 按插入时间排序获取所有游戏 */
    public ArrayList<GameDescription> getAllGamesSortedByInsertTime() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByInsertTime();
        return convertToGameDescriptions(entities);
    }
    
    /** 根据 ID 获取游戏 */
    public GameDescription getGameById(long id) {
        GameDescriptionEntity entity = gameDescriptionDao.getById(id);
        return entity != null ? convertToGameDescription(entity) : null;
    }
    
    /** 根据校验和获取游戏 */
    public GameDescription getGameByChecksum(String checksum) {
        GameDescriptionEntity entity = gameDescriptionDao.getByChecksum(checksum);
        return entity != null ? convertToGameDescription(entity) : null;
    }
    
    /** 插入单个游戏，插入后会将生成的 ID 回写到 game._id */
    public void insertGame(GameDescription game) {
        GameDescriptionEntity entity = convertToEntity(game);
        game._id = gameDescriptionDao.insert(entity);
    }
    
    /** 批量插入游戏列表 */
    public void insertGames(List<GameDescription> games) {
        List<GameDescriptionEntity> entities = new ArrayList<>();
        for (GameDescription game : games) {
            entities.add(convertToEntity(game));
        }
        gameDescriptionDao.insertAll(entities);
    }
    
    /** 更新游戏信息 */
    public void updateGame(GameDescription game) {
        GameDescriptionEntity entity = convertToEntity(game);
        gameDescriptionDao.update(entity);
    }
    
    /** 更新游戏指定字段 */
    public void updateGameFields(GameDescription game, String[] fields) {
        GameDescriptionEntity entity = convertToEntity(game);
        gameDescriptionDao.update(entity);
    }
    
    /** 删除游戏 */
    public void deleteGame(GameDescription game) {
        GameDescriptionEntity entity = convertToEntity(game);
        gameDescriptionDao.delete(entity);
    }
    
    /** 根据 ID 删除游戏 */
    public void deleteGameById(long id) {
        gameDescriptionDao.deleteById(id);
    }
    
    /** 获取游戏总数 */
    public int getGameCount() {
        return gameDescriptionDao.getCount();
    }
    
    /** 按名称模糊搜索游戏 */
    public ArrayList<GameDescription> searchGames(String filter) {
        List<GameDescriptionEntity> entities = gameDescriptionDao.searchByName("%" + filter + "%");
        return convertToGameDescriptions(entities);
    }
    
    // ========== ZIP ROM 文件操作 ==========

    /** 插入 ZIP ROM 文件，插入后会将生成的 ID 回写到 zipFile._id */
    public void insertZipFile(ZipRomFile zipFile) {
        ZipRomFileEntity entity = convertToEntity(zipFile);
        zipFile._id = zipRomFileDao.insert(entity);
    }
    
    /** 根据 ID 获取 ZIP ROM 文件 */
    public ZipRomFile getZipFileById(long id) {
        ZipRomFileEntity entity = zipRomFileDao.getById(id);
        return entity != null ? convertToZipRomFile(entity) : null;
    }
    
    /** 根据哈希值获取 ZIP ROM 文件 */
    public ZipRomFile getZipFileByHash(String hash) {
        ZipRomFileEntity entity = zipRomFileDao.getByHash(hash);
        return entity != null ? convertToZipRomFile(entity) : null;
    }
    
    /** 删除 ZIP ROM 文件 */
    public void deleteZipFile(ZipRomFile zipFile) {
        ZipRomFileEntity entity = convertToEntity(zipFile);
        zipRomFileDao.delete(entity);
    }
    
    /** 获取所有 ZIP ROM 文件，并为每个文件加载其包含的游戏列表 */
    public ArrayList<ZipRomFile> getAllZipFiles() {
        List<ZipRomFileEntity> entities = zipRomFileDao.getAll();
        ArrayList<ZipRomFile> zipFiles = new ArrayList<>();
        if (entities != null) {
            for (ZipRomFileEntity entity : entities) {
                ZipRomFile zipFile = convertToZipRomFile(entity);
                // 加载该 ZIP 文件包含的游戏
                zipFile.games = getGamesByZipFileId(zipFile._id);
                zipFiles.add(zipFile);
            }
        }
        return zipFiles;
    }
    
    /** 根据 ZIP 文件 ID 获取其包含的所有游戏 */
    private ArrayList<GameDescription> getGamesByZipFileId(long zipfileId) {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getByZipFileId(zipfileId);
        return convertToGameDescriptions(entities);
    }
    
    // ========== 实体转换方法 ==========

    /** 将实体列表转换为领域对象列表 */
    private ArrayList<GameDescription> convertToGameDescriptions(List<GameDescriptionEntity> entities) {
        ArrayList<GameDescription> games = new ArrayList<>();
        if (entities != null) {
            for (GameDescriptionEntity entity : entities) {
                games.add(convertToGameDescription(entity));
            }
        }
        return games;
    }
    
    /** 将游戏描述实体转换为领域对象 */
    private GameDescription convertToGameDescription(GameDescriptionEntity entity) {
        if (entity == null) return null;
        
        GameDescription game = new GameDescription();
        game._id = entity._id;
        game.name = entity.name;
        game.path = entity.path;
        game.checksum = entity.checksum;
        game.zipfile_id = entity.zipfile_id;
        game.inserTime = entity.inserTime;
        game.lastGameTime = entity.lastGameTime;
        game.runCount = entity.runCount;
        return game;
    }
    
    /** 将领域对象转换为游戏描述实体 */
    private GameDescriptionEntity convertToEntity(GameDescription game) {
        if (game == null) return null;
        
        GameDescriptionEntity entity = new GameDescriptionEntity();
        entity._id = game._id;
        entity.name = game.name;
        entity.path = game.path;
        entity.checksum = game.checksum;
        entity.zipfile_id = game.zipfile_id;
        entity.inserTime = game.inserTime;
        entity.lastGameTime = game.lastGameTime;
        entity.runCount = game.runCount;
        return entity;
    }
    
    /** 将 ZIP ROM 文件实体转换为领域对象 */
    private ZipRomFile convertToZipRomFile(ZipRomFileEntity entity) {
        if (entity == null) return null;
        
        ZipRomFile zipFile = new ZipRomFile();
        zipFile._id = entity._id;
        zipFile.hash = entity.hash;
        zipFile.path = entity.path;
        return zipFile;
    }
    
    /** 将领域对象转换为 ZIP ROM 文件实体 */
    private ZipRomFileEntity convertToEntity(ZipRomFile zipFile) {
        if (zipFile == null) return null;
        
        ZipRomFileEntity entity = new ZipRomFileEntity();
        entity._id = zipFile._id;
        entity.hash = zipFile.hash;
        entity.path = zipFile.path;
        return entity;
    }
}
