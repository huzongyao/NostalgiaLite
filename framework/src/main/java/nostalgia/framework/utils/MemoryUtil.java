package nostalgia.framework.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Build;
import android.os.Debug;

import java.util.List;

public class MemoryUtil {
    private static final String TAG = "utils.MemoryUtil";

    @SuppressLint("NewApi")
    public static void printMemoryInfo(Context context) {
        System.gc();
        MemoryInfo mi = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;
        NLog.i(TAG, "Memory report " + context.getClass().getSimpleName());
        NLog.i(TAG, "act available memory:" + availableMegs + "MB");
        NLog.i(TAG,
                "memory: native heap alloc: "
                        + Debug.getNativeHeapAllocatedSize());
        NLog.i(TAG, "memory: native heap free: " + Debug.getNativeHeapFreeSize());

        if (Build.VERSION.SDK_INT > 15) {
            long totalMegs = mi.totalMem / 1048576L;
            long usedMegs = totalMegs - availableMegs;
            NLog.i(TAG, "act total memory:" + totalMegs + "MB");
            NLog.i(TAG, "act used memory:" + usedMegs + "MB");
        }

        List<RunningAppProcessInfo> runningAppProcesses = activityManager
                .getRunningAppProcesses();

        for (RunningAppProcessInfo i : runningAppProcesses) {
            if (i.processName.equals(context.getPackageName())) {
                Debug.MemoryInfo[] mem = activityManager
                        .getProcessMemoryInfo(new int[]{i.pid});
                NLog.i(TAG, i.processName + " pss:"
                        + (mem[0].getTotalPss() / 1024) + "MB");
            }
        }

        try {
            Runtime info = Runtime.getRuntime();
            availableMegs = info.freeMemory() / 1048576L;
            long totalMegs = info.totalMemory() / 1048576L;
            long usedMegs = totalMegs - availableMegs;
            NLog.i(TAG, "runtime available memory:" + availableMegs + "MB");
            NLog.i(TAG, "runtime total memory:" + totalMegs + "MB");
            NLog.i(TAG, "runtime used memory:" + usedMegs + "MB");

        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }

        NLog.i(TAG, "----------------------------------------");
    }
}
