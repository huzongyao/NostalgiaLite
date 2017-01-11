package nostalgia.framework.base;

import android.view.View;

interface EmulatorView {
    void onPause();

    void onResume();

    void setQuality(int quality);

    ViewPort getViewPort();

    View asView();
}
