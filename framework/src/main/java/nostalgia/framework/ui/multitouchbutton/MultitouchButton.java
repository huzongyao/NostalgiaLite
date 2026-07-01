package nostalgia.framework.ui.multitouchbutton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatButton;

/**
 * 支持多点触控的按钮控件。
 * <p>
 * 继承自 AppCompatButton，实现 MultitouchBtnInterface 接口，
 * 支持触摸进入/退出事件和重绘状态管理。
 * </p>
 */
public class MultitouchButton extends AppCompatButton
        implements MultitouchBtnInterface {

    protected boolean repaint = true;
    OnMultitouchEventListener listener;

    public MultitouchButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MultitouchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onTouchEnter(MotionEvent event) {
        setPressed(true);

        if (listener != null)
            listener.onMultitouchEnter(this);
    }

    public void onTouchExit(MotionEvent event) {
        setPressed(false);

        if (listener != null)
            listener.onMultitouchExit(this);
    }

    public void setOnMultitouchEventlistener(OnMultitouchEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void requestRepaint() {
        repaint = true;
    }

    @Override
    public void removeRequestRepaint() {
        repaint = false;
    }

    @Override
    public boolean isRepaintState() {
        return repaint;
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

}
