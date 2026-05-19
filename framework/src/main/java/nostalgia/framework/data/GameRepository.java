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
    
    // GameDescription operations
    public ArrayList<GameDescription> getAllGames() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAll();
        return convertToGameDescriptions(entities);
    }
    
    public ArrayList<GameDescription> getAllGamesSortedByName() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByName();
        return convertToGameDescriptions(entities);
    }
    
    public ArrayList<GameDescription> getAllGamesSortedByLastPlayed() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByLastPlayed();
        return convertToGameDescriptions(entities);
    }
    
    public ArrayList<GameDescription> getAllGamesSortedByMostPlayed() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByMostPlayed();
        return convertToGameDescriptions(entities);
    }
    
    public ArrayList<GameDescription> getAllGamesSortedByInsertTime() {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getAllSortedByInsertTime();
        return convertToGameDescriptions(entities);
    }
    
    public GameDescription getGameById(long id) {
        GameDescriptionEntity entity = gameDescriptionDao.getById(id);
        return entity != null ? convertToGameDescription(entity) : null;
    }
    
    public GameDescription getGameByChecksum(String checksum) {
        GameDescriptionEntity entity = gameDescriptionDao.getByChecksum(checksum);
        return entity != null ? convertToGameDescription(entity) : null;
    }
    
    public void insertGame(GameDescription game) {
        GameDescriptionEntity entity = convertToEntity(game);
        game._id = gameDescriptionDao.insert(entity);
    }
    
    public void insertGames(List<GameDescription> games) {
        List<GameDescriptionEntity> entities = new ArrayList<>();
        for (GameDescription game : games) {
            entities.add(convertToEntity(game));
        }
        gameDescriptionDao.insertAll(entities);
    }
    
    public void updateGame(GameDescription game) {
        GameDescriptionEntity entity = convertToEntity(game);
        gameDescriptionDao.update(entity);
    }
    
    public void updateGameFields(GameDescription game, String[] fields) {
        GameDescriptionEntity entity = convertToEntity(game);
        gameDescriptionDao.update(entity);
    }
    
    public void deleteGame(GameDescription game) {
        GameDescriptionEntity entity = convertToEntity(game);
        gameDescriptionDao.delete(entity);
    }
    
    public void deleteGameById(long id) {
        gameDescriptionDao.deleteById(id);
    }
    
    public int getGameCount() {
        return gameDescriptionDao.getCount();
    }
    
    public ArrayList<GameDescription> searchGames(String filter) {
        List<GameDescriptionEntity> entities = gameDescriptionDao.searchByName("%" + filter + "%");
        return convertToGameDescriptions(entities);
    }
    
    // ZipRomFile operations
    public void insertZipFile(ZipRomFile zipFile) {
        ZipRomFileEntity entity = convertToEntity(zipFile);
        zipFile._id = zipRomFileDao.insert(entity);
    }
    
    public ZipRomFile getZipFileById(long id) {
        ZipRomFileEntity entity = zipRomFileDao.getById(id);
        return entity != null ? convertToZipRomFile(entity) : null;
    }
    
    public ZipRomFile getZipFileByHash(String hash) {
        ZipRomFileEntity entity = zipRomFileDao.getByHash(hash);
        return entity != null ? convertToZipRomFile(entity) : null;
    }
    
    public void deleteZipFile(ZipRomFile zipFile) {
        ZipRomFileEntity entity = convertToEntity(zipFile);
        zipRomFileDao.delete(entity);
    }
    
    public ArrayList<ZipRomFile> getAllZipFiles() {
        List<ZipRomFileEntity> entities = zipRomFileDao.getAll();
        ArrayList<ZipRomFile> zipFiles = new ArrayList<>();
        if (entities != null) {
            for (ZipRomFileEntity entity : entities) {
                ZipRomFile zipFile = convertToZipRomFile(entity);
                // Load games for this zip file
                zipFile.games = getGamesByZipFileId(zipFile._id);
                zipFiles.add(zipFile);
            }
        }
        return zipFiles;
    }
    
    private ArrayList<GameDescription> getGamesByZipFileId(long zipfileId) {
        List<GameDescriptionEntity> entities = gameDescriptionDao.getByZipFileId(zipfileId);
        return convertToGameDescriptions(entities);
    }
    
    // Conversion methods
    private ArrayList<GameDescription> convertToGameDescriptions(List<GameDescriptionEntity> entities) {
        ArrayList<GameDescription> games = new ArrayList<>();
        if (entities != null) {
            for (GameDescriptionEntity entity : entities) {
                games.add(convertToGameDescription(entity));
            }
        }
        return games;
    }
    
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
    
    private ZipRomFile convertToZipRomFile(ZipRomFileEntity entity) {
        if (entity == null) return null;
        
        ZipRomFile zipFile = new ZipRomFile();
        zipFile._id = entity._id;
        zipFile.hash = entity.hash;
        zipFile.path = entity.path;
        return zipFile;
    }
    
    private ZipRomFileEntity convertToEntity(ZipRomFile zipFile) {
        if (zipFile == null) return null;
        
        ZipRomFileEntity entity = new ZipRomFileEntity();
        entity._id = zipFile._id;
        entity.hash = zipFile.hash;
        entity.path = zipFile.path;
        return entity;
    }
}
