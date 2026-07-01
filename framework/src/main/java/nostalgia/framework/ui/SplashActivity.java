package nostalgia.framework.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import nostalgia.framework.R;

/**
 * 启动画面 Activity。
 * <p>
 * 显示启动 Logo 800ms 后自动跳转到游戏画廊页面。
 * </p>
 */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                startActivity();
            }
        }, 800L);
    }

    /** 延迟 800ms 后启动游戏画廊 */
    private void startActivity() {
        Intent intent = new Intent();
        intent.setAction(getString(R.string.action_gallery_page));
        startActivity(intent);
        finish();
    }
}
