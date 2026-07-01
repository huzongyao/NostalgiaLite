package nostalgia.framework.base;

import android.view.View;

/**
 * 模拟器视图接口，定义渲染视图的统一契约。
 * 实现类包括 {@link OpenGLView}（硬件加速）和 {@link UnacceleratedView}（软件渲染）。
 */
interface EmulatorView {
    void onPause();

    void onResume();

    void setQuality(int quality);

    ViewPort getViewPort();

    View asView();
}
