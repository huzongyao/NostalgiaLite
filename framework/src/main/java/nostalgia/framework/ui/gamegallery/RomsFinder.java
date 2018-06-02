package nostalgia.framework.ui.gamegallery;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import nostalgia.framework.utils.DatabaseHelper;
import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.SDCardUtil;

public class RomsFinder extends Thread {
    private static final String TAG = "RomsFinder";
    private FilenameExtFilter filenameExtFilter;
    private FilenameExtFilter inZipFileNameExtFilter;
    private String androidAppDataFolder = "";
    private HashMap<String, GameDescription> oldGames = new HashMap<>();
    private DatabaseHelper dbHelper;
    private ArrayList<GameDescription> games = new ArrayList<>();
    private OnRomsFinderListener listener;
    private BaseGameGalleryActivity activity;
    private boolean searchNew = true;
    private File selectedFolder;
    private AtomicBoolean running = new AtomicBoolean(false);

    public RomsFinder(Set<String> exts, Set<String> inZipExts, BaseGameGalleryActivity activity,
                      OnRomsFinderListener listener, boolean searchNew, File selectedFolder) {
        this.listener = listener;
        this.activity = activity;
        this.searchNew = searchNew;
        this.selectedFolder = selectedFolder;
        filenameExtFilter = new FilenameExtFilter(exts, true, false);
        inZipFileNameExtFilter = new FilenameExtFilter(inZipExts, true, false);
        androidAppDataFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android";
        dbHelper = new DatabaseHelper(activity);
    }

    public static ArrayList<GameDescription> getAllGames(DatabaseHelper helper) {
        return helper.selectObjsFromDb(GameDescription.class, false, "GROUP BY checksum", null);
    }

    private void getRomAndPackedFiles(File root, List<File> result, HashSet<String> usedPaths) {
        String dirPath = null;
        Stack<DirInfo> dirStack = new Stack<>();
        dirStack.removeAllElements();
        dirStack.add(new DirInfo(root, 0));
        final int MAX_LEVEL = 12;

        while (running.get() && !dirStack.empty()) {
            DirInfo dir = dirStack.remove(0);
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
                                    dirStack.add(new DirInfo(file, dir.level + 1));
                                }
                            } else {
                                NLog.i(TAG, "cesta " + canonicalPath + " jiz byla prohledana");
                            }
                        } else {
                            result.add(file);
                        }
                    }
                }
            } else {
                NLog.i(TAG, "cesta " + dirPath + " jiz byla prohledana");
            }
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        running.set(true);
        NLog.i(TAG, "start");
        activity.runOnUiThread(() -> listener.onRomsFinderStart(searchNew));
        ArrayList<GameDescription> oldRoms = getAllGames(dbHelper);
        oldRoms = removeNonExistRoms(oldRoms);
        final ArrayList<GameDescription> roms = oldRoms;
        NLog.i(TAG, "old games " + oldRoms.size());
        activity.runOnUiThread(() -> listener.onRomsFinderFoundGamesInCache(roms));

        if (searchNew) {
            for (GameDescription desc : oldRoms) {
                oldGames.put(desc.path, desc);
            }
            startFileSystemMode(oldRoms);
        } else {
            activity.runOnUiThread(() -> listener.onRomsFinderEnd(false));
        }
    }

    private void checkZip(File zipFile) {
        File externalCache = activity.getExternalCacheDir();

        if (externalCache != null) {
            String cacheDir = externalCache.getAbsolutePath();
            NLog.i(TAG, "check zip" + zipFile.getAbsolutePath());
            String hash = ZipRomFile.computeZipHash(zipFile);
            ZipRomFile zipRomFile = dbHelper.selectObjFromDb(ZipRomFile.class,
                    "WHERE hash=\"" + hash + "\"");
            ZipFile zip = null;

            if (zipRomFile == null) {
                zipRomFile = new ZipRomFile();
                zipRomFile.path = zipFile.getAbsolutePath();
                zipRomFile.hash = hash;

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
                                InputStream is = zip.getInputStream(ze);
                                String checksum = EmuUtils.getMD5Checksum(is);
                                try {
                                    if (is != null) {
                                        is.close();
                                    }
                                } catch (Exception ignored) {
                                }
                                GameDescription game = new GameDescription(ze.getName(), "", checksum);
                                game.inserTime = System.currentTimeMillis();
                                zipRomFile.games.add(game);
                                games.add(game);
                            }
                        }

                        if (counterEntry > 20 && counterRoms == 0) {
                            listener.onRomsFinderFoundZipEntry(zipFile.getName() + "\n" + ze.getName(),
                                    max - 20 - 1);
                            NLog.i(TAG, "Predcasne ukonceni prohledavani zipu. V prvnich 20 zaznamech v zipu neni ani jeden rom");
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
                    if (running.get()) {
                        dbHelper.insertObjToDb(zipRomFile);
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
            } else {
                games.addAll(zipRomFile.games);
                listener.onRomsFinderFoundZipEntry(zipFile.getName(), zipRomFile.games.size());
                NLog.i(TAG, "found zip in cache " + zipRomFile.games.size());
            }
        } else {
            NLog.e(TAG, "external cache dir is null");
            activity.showSDCardFailed();
        }
    }

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
                    try {
                        ZipFile zzFile = new ZipFile(file);
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
                    game.inserTime = System.currentTimeMillis();
                    dbHelper.insertObjToDb(game);
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

    public void stopSearch() {
        if (running.get()) {
            listener.onRomsFinderCancel(true);
        }

        running.set(false);
        NLog.i(TAG, "cancel search");
    }

    private ArrayList<GameDescription> removeNonExistRoms(ArrayList<GameDescription> roms) {
        HashSet<String> hashs = new HashSet<>();
        ArrayList<GameDescription> newRoms = new ArrayList<>(roms.size());
        Map<Long, ZipRomFile> zipsMap = new HashMap<>();

        for (ZipRomFile zip : dbHelper.selectObjsFromDb(ZipRomFile.class,
                false, null, null)) {
            File zipFile = new File(zip.path);

            if (zipFile.exists()) {
                zipsMap.put(zip._id, zip);

            } else {
                dbHelper.deleteObjFromDb(zip);
                dbHelper.deleteObjsFromDb(GameDescription.class, "where zipfile_id=" + zip._id);
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
                    dbHelper.deleteObjFromDb(game);
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

    private class DirInfo {
        public File file;
        public int level;

        public DirInfo(File f, int level) {
            this.level = level;
            this.file = f;
        }
    }

}
