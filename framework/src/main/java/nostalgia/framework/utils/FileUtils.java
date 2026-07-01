package nostalgia.framework.utils;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 文件操作工具类。
 * <p>提供文件读写、复制、目录清理、Assets 读取等常用文件操作方法。</p>
 *
 * @author NostalgiaLite
 */
public class FileUtils {

    /**
     * 读取 Assets 目录下的文件内容。
     *
     * @param context 上下文
     * @param asset   Assets 中的文件路径
     * @return 文件内容字符串
     * @throws IOException 读取失败时抛出
     */
    public static String readAsset(Context context, String asset) throws IOException {
        InputStream is = context.getAssets().open(asset);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * 获取不含扩展名的文件名。
     *
     * @param file 文件对象
     * @return 不含扩展名的文件名
     */
    public static String getFileNameWithoutExt(File file) {
        String name = file.getName();
        int lastIdx = name.lastIndexOf(".");
        if (lastIdx == -1) {
            return name;
        }
        return name.substring(0, lastIdx);
    }

    /**
     * 复制文件（基于文件路径）。
     *
     * @param from 源文件
     * @param to   目标文件
     * @throws IOException 复制失败时抛出
     */
    public static void copyFile(File from, File to) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(from);
            copyFile(fis, to);
        } finally {
            if (fis != null)
                fis.close();
        }
    }

    /**
     * 复制文件（基于输入流）。
     *
     * @param is   输入流
     * @param to   目标文件
     * @throws IOException 复制失败时抛出
     */
    public static void copyFile(InputStream is, File to) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(to);
            int count;
            byte[] buffer = new byte[1024];
            while ((count = is.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    /**
     * 递归清空目录下所有文件和子目录。
     *
     * @param directory 要清空的目录
     * @throws IOException 操作失败时抛出
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (directory != null) {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    cleanDirectory(file);
                }
                file.delete();
            }
        }
    }

    /**
     * 将字符串写入文件。
     *
     * @param text 要写入的字符串
     * @param file 目标文件
     * @throws IOException 写入失败时抛出
     */
    public static void saveStringToFile(String text, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(text.getBytes());
        fos.close();
    }

    /**
     * 将文件内容读取为字符串。
     *
     * @param file 要读取的文件
     * @return 文件内容字符串，失败时返回空字符串
     */
    public static String loadFileToString(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    /** 检查 SD 卡是否已挂载并可读写 */
    public static boolean isSDCardRWMounted() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}
