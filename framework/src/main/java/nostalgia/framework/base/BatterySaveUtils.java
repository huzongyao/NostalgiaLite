package nostalgia.framework.base;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.FileUtils;


public class BatterySaveUtils {
    private BatterySaveUtils() {
    }

    public static void createSavFileCopyIfNeeded(Context context, String gameFilePath) {
        File gameFile = new File(gameFilePath);
        File batterySavFile =
                new File(gameFile.getParent(), EmuUtils.stripExtension(gameFile.getName()) + ".sav");
        if (!batterySavFile.exists()) {
            return;
        }
        if (batterySavFile.canWrite()) {
            return;
        }
        String sourceMD5 = EmuUtils.getMD5Checksum(batterySavFile);
        if (needsRewrite(context, batterySavFile, sourceMD5)) {
            File copyFile = new File(EmulatorUtils.getBaseDir(context), batterySavFile.getName());
            try {
                FileUtils.copyFile(batterySavFile, copyFile);
                saveMD5Meta(context, batterySavFile, sourceMD5);
            } catch (Exception ignored) {
            }
        }
    }

    private static void saveMD5Meta(Context context, File batterySavFile, String md5) {
        File metaFile = getMetaFile(context, batterySavFile);
        FileWriter fw = null;
        try {
            metaFile.delete();
            metaFile.createNewFile();
            fw = new FileWriter(metaFile);
            fw.write(md5);
        } catch (Exception ignored) {
        } finally {
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (Exception ignored) {
            }
        }
    }


    private static boolean needsRewrite(Context context, File sourceBatteryFile, String sourceMD5) {
        String previousSourceMD5;
        File metaFile = getMetaFile(context, sourceBatteryFile);
        File targetFile = new File(EmulatorUtils.getBaseDir(context), sourceBatteryFile.getName());
        if (!metaFile.exists() || !targetFile.exists()) {
            return true;
        } else {
            FileReader fileReader = null;
            BufferedReader br = null;
            try {
                fileReader = new FileReader(metaFile);
                br = new BufferedReader(fileReader);
                previousSourceMD5 = br.readLine();
            } catch (Exception ignored) {
                return true;
            } finally {
                try {
                    if (fileReader != null) {
                        fileReader.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        Log.d("MD5", "source: " + sourceMD5 + " old: " + previousSourceMD5);
        return !sourceMD5.equals(previousSourceMD5);
    }

    private static File getMetaFile(Context context, File batterySavFile) {
        return new File(EmulatorUtils.getBaseDir(context), batterySavFile.getName() + ".meta");
    }

    public static String getBatterySaveDir(Context context, String gameFilePath) {
        File f = new File(gameFilePath);
        String directory = f.getParent();
        String batteryPath = directory;
        boolean isWritable = new File(batteryPath).canWrite();
        if (!isWritable || directory.equals(context.getExternalCacheDir().getAbsolutePath())) {
            batteryPath = EmulatorUtils.getBaseDir(context);
        }
        return batteryPath;
    }

}
