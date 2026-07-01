/**
 * Based on post in http://stackoverflow.com/questions/5694933/find-an-external-sd-card-location
 * author: http://stackoverflow.com/users/565319/richard
 */
package nostalgia.framework.utils;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

/**
 * SD 卡存储工具类。
 * <p>提供外部存储可用性检测、SD 卡路径查找、符号链接检测等功能。
 * 支持扫描 /proc/mounts 和 vold.fstab 发现所有可用的 SD 卡挂载点。</p>
 * <p>基于 StackOverflow 用户 richard 的实现修改。</p>
 *
 * @author NostalgiaLite
 */
public class SDCardUtil {

    /** 内部 SD 卡标识 */
    public static final String SD_CARD = "sdCard";
    /** 外部 SD 卡标识 */
    public static final String EXTERNAL_SD_CARD = "externalSdCard";
    private static final String TAG = "utils.SDCardUtil";

    /** 检查外部存储是否可用（已挂载或只读挂载） */
    public static boolean isAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /** 获取默认外部存储目录路径（末尾带 "/"） */
    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getPath() + "/";
    }

    /** 检查外部存储是否可写 */
    public static boolean isWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);

    }

    /**
     * 获取所有可用的存储位置。
     * <p>通过扫描 /proc/mounts 和 vold.fstab 发现所有 VFAT/exFAT/FUSE 类型的挂载点。</p>
     *
     * @return 所有可读存储目录的集合
     */
    public static HashSet<File> getAllStorageLocations() {
        HashSet<String> sdcards = new HashSet<>(3);
        sdcards.add("/mnt/sdcard");
        try {
            File mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    String lineLower = line.toLowerCase(); // neukladat lower
                    // primo do
                    // line, protoze
                    // zbytek line je
                    // case sensitive
                    if (lineLower.contains("vfat") || lineLower.contains("exfat") ||
                            lineLower.contains("fuse") || lineLower.contains("sdcardfs")) {
                        String[] lineElements = line.split(" ");
                        String path = lineElements[1];
                        sdcards.add(path);
                    }
                }
            }
        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }
        getSDcardsPath(sdcards);
        HashSet<File> result = new HashSet<>(sdcards.size());
        for (String mount : sdcards) {
            File root = new File(mount);
            if (root.exists() && root.isDirectory() && root.canRead()) {
                result.add(root);
            }
        }
        return result;
    }

    /**
     * 从 vold.fstab 解析 SD 卡路径并加入集合。
     *
     * @param set 存储路径集合
     */
    private static void getSDcardsPath(HashSet<String> set) {
        File file = new File("/system/etc/vold.fstab");
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            if (fr != null) {
                String defaultExternalStorage =
                        Environment.getExternalStorageDirectory().getAbsolutePath();
                br = new BufferedReader(fr);
                String s = br.readLine();
                while (s != null) {
                    if (s.startsWith("dev_mount")) {
                        String[] tokens = s.split("\\s");
                        String path = tokens[2]; // mount_point
                        if (!defaultExternalStorage.equals(path)) {
                            set.add(path);
                            break;
                        }
                    }
                    s = br.readLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检测文件是否为符号链接。
     *
     * @param file 要检测的文件
     * @return 如果是符号链接返回 true
     * @throws IOException 检测失败时抛出
     */
    public static boolean isSymlink(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("File must not be null");
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
}
