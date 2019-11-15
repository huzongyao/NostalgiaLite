package nostalgia.framework.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;

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
                startWithPermission();
            }
        }, 800L);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void startWithPermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE)
                .callback(new PermissionUtils.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        Intent intent = new Intent();
                        intent.setAction(getString(R.string.action_gallery_page));
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onDenied() {
                        finish();
                    }
                }).request();
    }
}
