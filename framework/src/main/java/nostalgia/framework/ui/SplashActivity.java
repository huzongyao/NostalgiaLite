package nostalgia.framework.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import nostalgia.framework.R;

/**
 * Created by huzongyao on 2018/6/4.
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

    private void startActivity() {
        Intent intent = new Intent();
        intent.setAction(getString(R.string.action_gallery_page));
        startActivity(intent);
        finish();
    }
}
