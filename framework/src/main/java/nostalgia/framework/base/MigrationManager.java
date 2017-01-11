package nostalgia.framework.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import nostalgia.framework.KeyboardProfile;
import nostalgia.framework.ui.cheats.Cheat;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.gamegallery.RomsFinder;
import nostalgia.framework.ui.multitouchbutton.MultitouchLayer;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.DatabaseHelper;
import nostalgia.framework.utils.FileUtils;

public class MigrationManager {

    public static void doExport(Context context, String baseDir) {
        for (Migrator migrator : getMigrators()) {
            migrator.doExport(context, baseDir);
        }
    }

    public static void doImport(Context context, String baseDir) {
        for (Migrator migrator : getMigrators()) {
            migrator.doImport(context, baseDir);
        }
    }

    public static Migrator[] getMigrators() {
        return new Migrator[]{new SaveStatesMigrator(),
                new GeneralPrefMigrator(), new GamePrefMigrator(),
                new KeyboardProfile.PreferenceMigrator(),
                new MultitouchLayer.PreferenceMigrator(),
        };
    }

    private static class SaveStatesMigrator implements Migrator {

        @Override
        public void doExport(Context context, String targetDir) {
            String sSource = EmulatorUtils.getBaseDir(context);
            File source = new File(sSource);
            File[] files = source.listFiles();

            try {
                for (File file : files) {
                    File newFile = new File(targetDir, file.getName());
                    FileUtils.copyFile(file, newFile);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void doImport(Context context, String sourceDir) {
            File source = new File(sourceDir);
            File[] files = source.listFiles();
            String targetDir = EmulatorUtils.getBaseDir(context);

            try {
                for (File file : files) {
                    String name = file.getName();

                    if (name.endsWith(".state") || name.endsWith(".png")) {
                        File newFile = new File(targetDir, name);
                        FileUtils.copyFile(file, newFile);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static class GamePrefMigrator implements Migrator {

        @Override
        public void doExport(Context context, String baseDir) {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            ArrayList<GameDescription> games = RomsFinder.getAllGames(dbHelper);

            for (GameDescription game : games) {
                PreferenceUtil.exportPreferences(context.getSharedPreferences(
                        game.checksum + Cheat.CHEAT_PREF_SUFFIX,
                        Context.MODE_PRIVATE), new File(baseDir, game.checksum
                        + Cheat.CHEAT_PREF_SUFFIX));
                PreferenceUtil.exportPreferences(context.getSharedPreferences(
                        game.checksum + PreferenceUtil.GAME_PREF_SUFFIX,
                        Context.MODE_PRIVATE), new File(baseDir, game.checksum
                        + PreferenceUtil.GAME_PREF_SUFFIX));
            }
        }

        @Override
        public void doImport(Context context, String baseDir) {
            doImport(baseDir, context, Cheat.CHEAT_PREF_SUFFIX);
            doImport(baseDir, context, PreferenceUtil.GAME_PREF_SUFFIX);
        }

        private void doImport(String baseDir, Context context,
                              final String importSuffix) {
            File dir = new File(baseDir);
            String[] files = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(importSuffix);
                }
            });

            for (String file : files) {
                String prefName = file;
                SharedPreferences pref = context.getSharedPreferences(prefName,
                        Context.MODE_PRIVATE);
                PreferenceUtil.importPreferences(pref, new File(baseDir, file),
                        PreferenceUtil.NotFoundHandling.FAIL);
            }
        }

    }

    public static class GeneralPrefMigrator implements Migrator {

        private final String EXPORT_FILE = "general__preferences";

        @Override
        public void doExport(Context context, String baseDir) {
            PreferenceUtil.exportPreferences(
                    PreferenceManager.getDefaultSharedPreferences(context),
                    new File(baseDir, EXPORT_FILE));
        }

        @Override
        public void doImport(Context context, String baseDir) {
            PreferenceUtil.importPreferences(
                    PreferenceManager.getDefaultSharedPreferences(context),
                    new File(baseDir, EXPORT_FILE),
                    PreferenceUtil.NotFoundHandling.IGNORE);
        }

    }

}
