package nostalgia.framework.base;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.FileUtils;


/**
 * 电池存档工具类，处理游戏电池存档文件（.sav）的复制和路径管理。
 * <p>
 * 当 ROM 所在目录不可写时，自动将存档复制到应用基础目录，
 * 并通过 MD5 元数据文件避免重复复制。
 * </p>
 */
public class BatterySaveUtils {
    private BatterySaveUtils() {
    }

    /**
     * 如果 ROM 所在目录不可写，则将 .sav 文件复制到应用基础目录。
     * 通过 MD5 校验避免重复复制。
     */
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
        try (FileWriter fw = new FileWriter(metaFile)) {
            metaFile.delete();
            metaFile.createNewFile();
            fw.write(md5);
        } catch (Exception ignored) {
        }
    }


    /** 判断源文件是否已变更，需要重新复制。 */
    private static boolean needsRewrite(Context context, File sourceBatteryFile, String sourceMD5) {
        File metaFile = getMetaFile(context, sourceBatteryFile);
        File targetFile = new File(EmulatorUtils.getBaseDir(context), sourceBatteryFile.getName());
        if (!metaFile.exists() || !targetFile.exists()) {
            return true;
        }
        String previousSourceMD5;
        try (BufferedReader br = new BufferedReader(new FileReader(metaFile))) {
            previousSourceMD5 = br.readLine();
        } catch (Exception ignored) {
            return true;
        }
        Log.d("MD5", "source: " + sourceMD5 + " old: " + previousSourceMD5);
        return !sourceMD5.equals(previousSourceMD5);
    }

    private static File getMetaFile(Context context, File batterySavFile) {
        return new File(EmulatorUtils.getBaseDir(context), batterySavFile.getName() + ".meta");
    }

    /**
     * 获取电池存档的保存目录。
     * 如果 ROM 所在目录不可写或位于缓存目录，则返回应用基础目录。
     */
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
