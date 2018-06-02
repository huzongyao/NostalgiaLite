package nostalgia.framework.base;

import android.app.Activity;
import android.os.Bundle;

public class OpenGLTestActivity extends Activity
        implements OpenGLTestView.Callback {

    OpenGLTestView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new OpenGLTestView(this, this);
        setContentView(view);
    }

    @Override
    public void onDetected(final int i) {
        runOnUiThread(() -> {
            setResult(i);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (view != null) {
            view.onPause();
        }
    }
}
