package nostalgia.framework.ui.gamegallery;

import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import nostalgia.framework.data.GameRepository;
import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.SDCardUtil;

/**
 * ROM 文件搜索器。
 * <p>
 * 在后台线程中搜索文件系统、ZIP 压缩包或 Uri 中的 ROM 文件，
 * 支持单文件导入、多文件导入、文件系统扫描等模式。
 * 通过回调接口通知搜索进度和结果。
 * </p>
 */
public class RomsFinder extends Thread {
    private static final String TAG = "RomsFinder";
    private FilenameExtFilter filenameExtFilter;
    private FilenameExtFilter inZipFileNameExtFilter;
    private String androidAppDataFolder = "";
    private HashMap<String, GameDescription> oldGames = new HashMap<>();
    private GameRepository gameRepository;
    private ArrayList<GameDescription> games = new ArrayList<>();
    private OnRomsFinderListener listener;
    private BaseGameGalleryActivity activity;
    private boolean searchNew = true;
    private File selectedFolder;
    private File singleFileToImport;
    private Uri singleUriToImport;
    private ArrayList<Uri> multipleUrisToImport;
    private boolean importSingleFile = false;
    private boolean importSingleUri = false;
    private boolean importMultipleUris = false;
    private AtomicBoolean running = new AtomicBoolean(false);

    public RomsFinder(Set<String> exts, Set<String> inZipExts, BaseGameGalleryActivity activity,
            OnRomsFinderListener listener, boolean searchNew, File selectedFolder) {
        this.listener = listener;
        this.activity = activity;
        this.searchNew = searchNew;
        this.selectedFolder = selectedFolder;
        this.gameRepository = activity.getGameRepository();
        filenameExtFilter = new FilenameExtFilter(exts, true, false);
        inZipFileNameExtFilter = new FilenameExtFilter(inZipExts, true, false);
        androidAppDataFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android";
    }
    
    /** 导入本地单个 ROM 文件或 ZIP 压缩包。 */
    public RomsFinder(Set<String> exts, Set<String> inZipExts, BaseGameGalleryActivity activity,
            OnRomsFinderListener listener, File singleFile) {
        this(exts, inZipExts, activity, listener, true, null);
        this.singleFileToImport = singleFile;
        this.importSingleFile = true;
    }
    
    /** 导入系统文件选择器返回的单个 Uri。 */
    public RomsFinder(Set<String> exts, Set<String> inZipExts, BaseGameGalleryActivity activity,
            OnRomsFinderListener listener, Uri singleUri) {
        this(exts, inZipExts, activity, listener, true, null);
        this.singleUriToImport = singleUri;
        this.importSingleUri = true;
    }
    
    /** 批量导入系统文件选择器返回的多个 Uri。 */
    public RomsFinder(Set<String> exts, Set<String> inZipExts, BaseGameGalleryActivity activity,
            OnRomsFinderListener listener, ArrayList<Uri> uris) {
        this(exts, inZipExts, activity, listener, true, null);
        this.multipleUrisToImport = uris;
        this.importMultipleUris = true;
    }

    private ArrayList<GameDescription> getAllGames() {
        return gameRepository.getAllGamesSortedByName();
    }

    /** 搜索目录中的 ROM 和压缩包文件，最大深度 12 层，避免跟随循环路径重复扫描。 */
    private void getRomAndPackedFiles(File root, List<File> result, HashSet<String> usedPaths) {
        String dirPath = null;
        Deque<DirInfo> dirStack = new ArrayDeque<>();
        dirStack.addLast(new DirInfo(root, 0));
        final int MAX_LEVEL = 12;

        while (running.get() && !dirStack.isEmpty()) {
            DirInfo dir = dirStack.removeFirst();
            try {
                dirPath = dir.file.getCanonicalPath();
            } catch (IOException e1) {
                NLog.e(TAG, "search error", e1);
            }

            if (dirPath != null && !usedPaths.contains(dirPath) && dir.level <= MAX_LEVEL) {
                usedPaths.add(dirPath);
                File[] files = dir.file.listFiles(filenameExtFilter);
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            String canonicalPath = null;
                            try {
                                canonicalPath = file.getCanonicalPath();
                            } catch (IOException e) {
                                NLog.e(TAG, "search error", e);
                            }
                            if (canonicalPath != null
                                    && (!usedPaths.contains(canonicalPath))) {
                                if (canonicalPath.equals(androidAppDataFolder)) {
                                    NLog.i(TAG, "ignore " + androidAppDataFolder);
                                } else {
                                    dirStack.addLast(new DirInfo(file, dir.level + 1));
                                }
                            } else {
                                NLog.i(TAG, "skip already scanned path: " + canonicalPath);
                            }
                        } else {
                            result.add(file);
                        }
                    }
                }
            } else {
                NLog.i(TAG, "skip already scanned path: " + dirPath);
            }
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        running.set(true);
        NLog.i(TAG, "start");
        activity.runOnUiThread(() -> listener.onRomsFinderStart(searchNew));
        ArrayList<GameDescription> oldRoms = getAllGames();
        oldRoms = removeNonExistRoms(oldRoms);
        final ArrayList<GameDescription> roms = oldRoms;
        NLog.i(TAG, "old games " + oldRoms.size());
        activity.runOnUiThread(() -> listener.onRomsFinderFoundGamesInCache(roms));

        if (searchNew) {
            for (GameDescription desc : oldRoms) {
                oldGames.put(desc.path, desc);
            }
            if (importMultipleUris && multipleUrisToImport != null && !multipleUrisToImport.isEmpty()) {
                importMultipleUris();
            } else if (importSingleUri && singleUriToImport != null) {
                importSingleUri(singleUriToImport);
            } else if (importSingleFile && singleFileToImport != null) {
                importSingleFile(singleFileToImport);
            } else {
                startFileSystemMode(oldRoms);
            }
        } else {
            activity.runOnUiThread(() -> listener.onRomsFinderEnd(false));
        }
    }
    
    /** 导入单个文件 */
    private void importSingleFile(File file) {
        if (!running.get()) return;
        
        try {
            String path = file.getCanonicalPath();
            NLog.i(TAG, "Importing file: " + path);
            
            // 路径已在数据库中时直接复用，避免重复插入同一 ROM。
            if (oldGames.containsKey(path)) {
                NLog.i(TAG, "File already exists in library");
                games.add(oldGames.get(path));
                activity.runOnUiThread(() -> {
                    listener.onRomsFinderNewGames(games);
                    listener.onRomsFinderEnd(true);
                });
                return;
            }
            
            String ext = EmuUtils.getExt(path).toLowerCase();
            if (ext.equals("zip")) {
                checkZip(file);
            } else if (filenameExtFilter.accept(null, file.getName())) {
                GameDescription game = new GameDescription(file);
                game.insertTime = System.currentTimeMillis();
                gameRepository.insertGame(game);
                games.add(game);
                activity.runOnUiThread(() -> listener.onRomsFinderFoundFile(game.name));
            } else {
                NLog.e(TAG, "Unsupported file type: " + ext);
            }
            
            if (running.get()) {
                NLog.i(TAG, "Imported " + games.size() + " games from file");
                activity.runOnUiThread(() -> {
                    listener.onRomsFinderNewGames(games);
                    listener.onRomsFinderEnd(true);
                });
            }
        } catch (IOException e) {
            NLog.e(TAG, "Error importing file", e);
            activity.runOnUiThread(() -> listener.onRomsFinderEnd(true));
        }
    }
    
    /** 从 Uri 导入单个文件（先复制到本地） */
    private void importSingleUri(Uri uri) {
        if (!running.get()) return;
        
        try {
            NLog.i(TAG, "Importing file from Uri: " + uri.toString());
            
            String filename = getFilenameFromUri(uri);
            if (filename == null) {
                filename = "rom_" + System.currentTimeMillis();
            }
            
            File destDir = activity.getExternalFilesDir(null);
            if (destDir == null) {
                destDir = activity.getFilesDir();
            }
            
            File destFile = new File(destDir, filename);
            File copiedFile = EmuUtils.copyUriToFile(activity, uri, destFile);
            
            if (copiedFile == null) {
                NLog.e(TAG, "Failed to copy file");
            } else {
                importCopiedFile(copiedFile, filename);
            }
        } catch (Exception e) {
            NLog.e(TAG, "Error importing Uri", e);
        }
        
        if (running.get()) {
            NLog.i(TAG, "Imported " + games.size() + " games from uri");
            activity.runOnUiThread(() -> {
                listener.onRomsFinderNewGames(games);
                listener.onRomsFinderEnd(true);
            });
        }
    }
    
    /** 批量导入多个 Uri */
    private void importMultipleUris() {
        if (!running.get()) return;
        
        NLog.i(TAG, "Importing multiple files: " + multipleUrisToImport.size());
        
        for (Uri uri : multipleUrisToImport) {
            if (!running.get()) break;
            
            try {
                NLog.i(TAG, "Importing file from Uri: " + uri.toString());
                
                String filename = getFilenameFromUri(uri);
                if (filename == null) {
                    filename = "rom_" + System.currentTimeMillis();
                }
                
                File destDir = activity.getExternalFilesDir(null);
                if (destDir == null) {
                    destDir = activity.getFilesDir();
                }
                
                File destFile = new File(destDir, filename);
                File copiedFile = EmuUtils.copyUriToFile(activity, uri, destFile);
                
                if (copiedFile != null) {
                    importCopiedFile(copiedFile, filename);
                } else {
                    NLog.e(TAG, "Failed to copy file: " + filename);
                }
            } catch (Exception e) {
                NLog.e(TAG, "Error importing Uri", e);
            }
        }
        
        if (running.get()) {
            NLog.i(TAG, "Imported " + games.size() + " games from multiple Uris");
            activity.runOnUiThread(() -> {
                listener.onRomsFinderNewGames(games);
                listener.onRomsFinderEnd(true);
            });
        }
    }
    
    /** 导入已复制到本地的文件 */
    private void importCopiedFile(File copiedFile, String filename) {
        String path = copiedFile.getAbsolutePath();
        
        String checksum = EmuUtils.getMD5Checksum(copiedFile);
        
        GameDescription existingGame = findGameByChecksum(checksum);
        if (existingGame != null) {
            NLog.i(TAG, "Game already exists in library");
            copiedFile.delete();
            games.add(existingGame);
            return;
        }
        
        String ext = EmuUtils.getExt(filename).toLowerCase();
        if (ext.equals("zip")) {
            checkZip(copiedFile);
        } else if (filenameExtFilter.accept(null, filename)) {
            GameDescription game = new GameDescription(copiedFile, checksum);
            game.insertTime = System.currentTimeMillis();
            gameRepository.insertGame(game);
            games.add(game);
            activity.runOnUiThread(() -> listener.onRomsFinderFoundFile(game.name));
        } else {
            NLog.e(TAG, "Unsupported file type: " + ext);
            copiedFile.delete();
        }
    }
    
    /** 从 Uri 提取文件名 */
    private String getFilenameFromUri(Uri uri) {
        String result = null;
        try {
            // ContentProvider 不一定暴露真实文件名，这里先取最后一个路径片段作为兜底。
            String lastSegment = uri.getLastPathSegment();
            if (lastSegment != null) {
                int lastSlash = lastSegment.lastIndexOf('/');
                if (lastSlash >= 0) {
                    result = lastSegment.substring(lastSlash + 1);
                } else {
                    result = lastSegment;
                }
            }
        } catch (Exception e) {
            NLog.e(TAG, "Failed to get filename from Uri", e);
        }
        return result;
    }
    
    /** 根据校验和查找已存在的游戏 */
    private GameDescription findGameByChecksum(String checksum) {
        for (GameDescription game : oldGames.values()) {
            if (game.checksum.equals(checksum)) {
                return game;
            }
        }
        return null;
    }

    /** 检查 ZIP 压缩包内的 ROM 文件 */
    private void checkZip(File zipFile) {
        File externalCache = activity.getExternalCacheDir();

        if (externalCache != null) {
            String cacheDir = externalCache.getAbsolutePath();
            NLog.i(TAG, "check zip" + zipFile.getAbsolutePath());
            String hash = ZipRomFile.computeZipHash(zipFile);
            ZipRomFile zipRomFile = gameRepository.getZipFileByHash(hash);
            ZipFile zip = null;

            // 缓存命中时，从数据库加载游戏列表
            if (zipRomFile != null) {
                zipRomFile.games = gameRepository.getGamesByZipFileId(zipRomFile._id);
                if (zipRomFile.games.isEmpty()) {
                    // 旧数据损坏（游戏未入库或 zipfile_id 错误），删除后重新导入
                    NLog.i(TAG, "zip in cache has no games, re-importing");
                    gameRepository.deleteZipFile(zipRomFile);
                    zipRomFile = null;
                }
            }

            if (zipRomFile != null) {
                // 缓存命中且数据完整，直接使用
                games.addAll(zipRomFile.games);
                listener.onRomsFinderFoundZipEntry(zipFile.getName(), zipRomFile.games.size());
                NLog.i(TAG, "found zip in cache " + zipRomFile.games.size());
            } else {
                // 首次导入或旧数据损坏，重新处理 ZIP 文件
                zipRomFile = new ZipRomFile();
                zipRomFile.path = zipFile.getAbsolutePath();
                zipRomFile.hash = hash;
                // 先插入数据库获取真实 ID，后续游戏才能正确关联
                gameRepository.insertZipFile(zipRomFile);

                try {
                    ZipEntry ze;
                    File dir = new File(cacheDir);
                    int counterRoms = 0;
                    int counterEntry = 0;
                    zip = new ZipFile(zipFile);
                    int max = zip.size();
                    Enumeration<? extends ZipEntry> entries = zip.entries();

                    while (entries.hasMoreElements()) {
                        ze = entries.nextElement();
                        counterEntry++;

                        if (running.get() && (!ze.isDirectory())) {
                            String filename = ze.getName();
                            if (inZipFileNameExtFilter.accept(dir, filename)) {
                                counterRoms++;
                                String checksum;
                                try (InputStream is = zip.getInputStream(ze)) {
                                    checksum = EmuUtils.getMD5Checksum(is);
                                }
                                GameDescription game = new GameDescription(ze.getName(), "", checksum);
                                game.insertTime = System.currentTimeMillis();
                                game.zipfile_id = zipRomFile._id;
                                gameRepository.insertGame(game);
                                zipRomFile.games.add(game);
                                games.add(game);
                            }
                        }

                        if (counterEntry > 20 && counterRoms == 0) {
                            listener.onRomsFinderFoundZipEntry(zipFile.getName() + "\n" + ze.getName(),
                                    max - 20 - 1);
                            NLog.i(TAG, "stop scanning zip early: no ROM found in first 20 entries");
                            break;
                        } else {
                            String name = ze.getName();
                            int idx = name.lastIndexOf('/');
                            if (idx != -1) {
                                name = name.substring(idx + 1);
                            }
                            if (name.length() > 20) {
                                name = name.substring(0, 20);
                            }
                            listener.onRomsFinderFoundZipEntry(zipFile.getName() + "\n" + name, 0);
                        }
                    }
                } catch (Exception e) {
                    NLog.e(TAG, "", e);
                } finally {
                    try {
                        if (zip != null)
                            zip.close();
                    } catch (IOException e) {
                        NLog.e(TAG, "", e);
                    }
                }
            }
        } else {
            NLog.e(TAG, "external cache dir is null");
            activity.showSDCardFailed();
        }
    }

    /** 启动文件系统扫描模式 */
    private void startFileSystemMode(ArrayList<GameDescription> oldRoms) {
        HashSet<File> roots = new HashSet<>();

        if (selectedFolder == null) {
            roots = SDCardUtil.getAllStorageLocations();
        } else {
            roots.add(selectedFolder);
        }

        ArrayList<File> result = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        NLog.i(TAG, "start searching in file system");
        HashSet<String> usedPaths = new HashSet<>();

        for (File root : roots) {
            NLog.i(TAG, "exploring " + root.getAbsolutePath());
            getRomAndPackedFiles(root, result, usedPaths);
        }

        NLog.i(TAG, "found " + result.size() + " files");
        NLog.i(TAG, "compute checksum");
        int zipEntriesCount = 0;
        ArrayList<File> zips = new ArrayList<>();

        for (File file : result) {
            String path = file.getAbsolutePath();
            if (running.get()) {
                String ext = EmuUtils.getExt(path).toLowerCase();
                if (ext.equals("zip")) {
                    zips.add(file);
                    try (ZipFile zzFile = new ZipFile(file)) {
                        zipEntriesCount += zzFile.size();
                    } catch (Exception e) {
                        NLog.e(TAG, "", e);
                    }
                    continue;
                }
                GameDescription game;
                if (oldGames.containsKey(path)) {
                    game = oldGames.get(path);
                } else {
                    game = new GameDescription(file);
                    game.insertTime = System.currentTimeMillis();
                    gameRepository.insertGame(game);
                    listener.onRomsFinderFoundFile(game.name);
                }
                games.add(game);
            }
        }

        for (File zip : zips) {
            if (running.get()) {
                listener.onRomsFinderZipPartStart(zipEntriesCount);
                checkZip(zip);
            }
        }

        if (running.get()) {
            NLog.i(TAG, "found games: " + games.size());
            games = removeNonExistRoms(games);
        }

        NLog.i(TAG, "compute checksum- done");

        if (running.get()) {
            activity.runOnUiThread(() -> {
                listener.onRomsFinderNewGames(games);
                listener.onRomsFinderEnd(true);
            });
        }

        NLog.i(TAG, "time:" + ((System.currentTimeMillis() - startTime) / 1000));
    }

    /** 停止搜索 */
    public void stopSearch() {
        if (running.get()) {
            listener.onRomsFinderCancel(true);
        }

        running.set(false);
        NLog.i(TAG, "cancel search");
    }

    /** 移除不存在的 ROM 记录并去重 */
    private ArrayList<GameDescription> removeNonExistRoms(ArrayList<GameDescription> roms) {
        HashSet<String> hashs = new HashSet<>();
        ArrayList<GameDescription> newRoms = new ArrayList<>(roms.size());
        Map<Long, ZipRomFile> zipsMap = new HashMap<>();

        for (ZipRomFile zip : gameRepository.getAllZipFiles()) {
            File zipFile = new File(zip.path);

            if (zipFile.exists()) {
                zipsMap.put(zip._id, zip);
            } else {
                gameRepository.deleteZipFile(zip);
            }
        }

        for (GameDescription game : roms) {
            if (!game.isInArchive()) {
                File path = new File(game.path);

                if (path.exists()) {
                    if (!hashs.contains(game.checksum)) {
                        newRoms.add(game);
                        hashs.add(game.checksum);
                    }

                } else {
                    gameRepository.deleteGame(game);
                }
            } else {
                ZipRomFile zip = zipsMap.get(game.zipfile_id);

                if (zip != null) {
                    if (!hashs.contains(game.checksum)) {
                        newRoms.add(game);
                        hashs.add(game.checksum);
                    }
                }
            }
        }
        return newRoms;
    }

    /** ROM 搜索回调监听器接口 */
    public interface OnRomsFinderListener {

        void onRomsFinderStart(boolean searchNew);

        void onRomsFinderFoundGamesInCache(ArrayList<GameDescription> oldRoms);

        void onRomsFinderFoundFile(String name);

        void onRomsFinderZipPartStart(int countEntries);

        void onRomsFinderFoundZipEntry(String message, int skipEntries);

        void onRomsFinderNewGames(ArrayList<GameDescription> roms);

        void onRomsFinderEnd(boolean searchNew);

        void onRomsFinderCancel(boolean searchNew);
    }

    /** 目录搜索队列中的节点。 */
    private static final class DirInfo {
        final File file;
        final int level;

        DirInfo(File f, int level) {
            this.level = level;
            this.file = f;
        }
    }
}
