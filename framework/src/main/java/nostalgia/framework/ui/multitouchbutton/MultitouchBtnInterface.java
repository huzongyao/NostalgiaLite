package nostalgia.framework.ui.multitouchbutton;

import android.view.MotionEvent;

/**
 * 多点触控按钮接口。
 * <p>
 * 定义触控按钮的基本行为，包括触摸进入/退出事件、
 * 重绘请求、按下状态查询等。
 * </p>
 */
public interface MultitouchBtnInterface {
    void onTouchEnter(MotionEvent event);

    void onTouchExit(MotionEvent event);

    void setOnMultitouchEventlistener(OnMultitouchEventListener listener);

    int getId();

    boolean isPressed();

    void requestRepaint();

    void removeRequestRepaint();

    boolean isRepaintState();

    int getVisibility();

}
