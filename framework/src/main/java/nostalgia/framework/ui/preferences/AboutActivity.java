package nostalgia.framework.ui.preferences;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import nostalgia.framework.R;

/**
 * Created by huzongyao on 17-11-10.
 */

public class AboutActivity extends AppCompatActivity {

    private TextView mTextVersion;
    private TextView mTextAbout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mTextVersion = findViewById(R.id.textview_version);
        mTextAbout = findViewById(R.id.textview_about);
        mTextAbout.setAutoLinkMask(Linkify.ALL);
        mTextAbout.setMovementMethod(LinkMovementMethod.getInstance());
        getPackageVersionInfo();
    }

    private void getPackageVersionInfo() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mTextVersion.setText(packageInfo.versionName);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
