package nostalgia.framework.ui.multitouchbutton;

import android.view.MotionEvent;

public interface MultitouchBtnInterface {
    public void onTouchEnter(MotionEvent event);

    public void onTouchExit(MotionEvent event);

    public void setOnMultitouchEventlistener(OnMultitouchEventListener listener);

    public int getId();

    public boolean isPressed();

    public void requestRepaint();

    public void removeRequestRepaint();

    public boolean isRepaintState();

    public int getVisibility();

}
