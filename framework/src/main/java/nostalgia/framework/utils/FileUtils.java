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

public class FileUtils {

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

    public static String getFileNameWithoutExt(File file) {
        String name = file.getName();
        int lastIdx = name.lastIndexOf(".");
        if (lastIdx == -1) {
            return name;
        }
        return name.substring(0, lastIdx);
    }

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

    public static void saveStringToFile(String text, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(text.getBytes());
        fos.close();
    }

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

    public static boolean isSDCardRWMounted() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}
