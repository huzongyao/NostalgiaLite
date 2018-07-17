package nostalgia.framework.base;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class RestarterActivity extends Activity {

    public static final String EXTRA_PID = "pid";
    public static final String EXTRA_CLASS = "class";
    public static final String EXTRA_AFTER_RESTART = "isAfterRestart";
    RestarterThread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Loading...");
        setContentView(tv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int pid = getIntent().getExtras().getInt(EXTRA_PID);
        String className = getIntent().getExtras().getString(EXTRA_CLASS);
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (Exception ignored) {
        }
        Intent restartIntent = null;
        if (clazz != null) {
            restartIntent = new Intent(this, clazz);
            restartIntent.putExtras(getIntent());
        }
        thread = new RestarterThread(pid, restartIntent);
        thread.start();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (thread != null) {
            thread.cancel();
        }
        finish();
    }

    private class RestarterThread extends Thread {

        Intent intent;
        int pid;
        private AtomicBoolean cancelled = new AtomicBoolean(false);

        public RestarterThread(int pid, Intent intent) {
            this.intent = intent;
            this.pid = pid;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(500);
            } catch (Exception ignored) {
            }
            android.os.Process.killProcess(pid);
            try {
                Thread.sleep(300);
            } catch (Exception ignored) {
            }
            ActivityManager activityManager = (ActivityManager) getApplicationContext()
                    .getSystemService(ACTIVITY_SERVICE);
            boolean killed = false;
            while (!killed) {
                List<RunningAppProcessInfo> appProcesses;
                if (activityManager != null) {
                    appProcesses = activityManager.getRunningAppProcesses();
                    killed = true;
                    for (RunningAppProcessInfo info : appProcesses) {
                        if (info.pid == pid) {
                            killed = false;
                            break;
                        }
                    }
                    if (!killed) {
                        try {
                            Thread.sleep(30);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            if (!cancelled.get()) {
                if (intent != null) {
                    intent.putExtra(EXTRA_AFTER_RESTART, true);
                    startActivity(intent);
                } else {
                    finish();
                }
            }
        }

        public void cancel() {
            cancelled.set(true);
        }
    }

}
