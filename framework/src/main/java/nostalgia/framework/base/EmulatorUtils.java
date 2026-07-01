package nostalgia.framework.base;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import nostalgia.framework.EmulatorException;

/**
 * 模拟器工具类，提供基础目录获取等功能。
 */
public class EmulatorUtils {

    /**
     * 获取模拟器数据文件的基础目录。
     * 优先使用外部存储，若不可用则回退到内部存储。
     *
     * @param context Android 上下文
     * @return 基础目录的绝对路径
     * @throws EmulatorException 如果无法获取工作目录
     */
    public static String getBaseDir(Context context) {
        File dir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            dir = context.getExternalFilesDir(null);
        }
        if (dir == null) {
            dir = context.getFilesDir();
        }
        if (dir == null || !dir.exists()) {
            throw new EmulatorException("No working directory");
        }
        return dir.getAbsolutePath();
    }
}
