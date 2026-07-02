package nostalgia.framework.base;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 进程重启管理器。
 * <p>通过计数器控制进程定期重启，防止模拟器长时间运行导致内存泄漏。
 * 每 N 次 onResume 后自动杀进程并重启。</p>
 */
public class ProcessRestarter {

    private static final String PREF_KEY = "PRC";

    /**
     * 递减重启计数器。
     *
     * @param activity 当前 Activity
     * @param maxPRC   计数器初始值（每 N 次重启一次）
     * @return 递减后的值，为 0 时表示需要重启
     */
    public static int decreaseResumesToRestart(Activity activity, int maxPRC) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        int prc = prefs.getInt(PREF_KEY, maxPRC);
        if (prc > 0) {
            prc--;
        }
        prefs.edit().putInt(PREF_KEY, prc).apply();
        return prc;
    }

    /** 重置重启计数器 */
    public static void resetCounter(Activity activity, int maxPRC) {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit().putInt(PREF_KEY, maxPRC).apply();
    }

    /**
     * 重启进程。
     *
     * @param activity           当前 Activity
     * @param activityToRestart  重启后要启动的 Activity 类
     */
    public static void restartProcess(Activity activity, Class<?> activityToRestart) {
        Intent intent = new Intent(activity, RestarterActivity.class);
        intent.putExtras(activity.getIntent());
        intent.putExtra(RestarterActivity.EXTRA_PID, android.os.Process.myPid());
        intent.putExtra(RestarterActivity.EXTRA_CLASS, activityToRestart.getName());
        activity.startActivity(intent);
    }
}
