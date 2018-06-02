package nostalgia.framework.ui.multitouchbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import nostalgia.framework.R;

public class MultitouchTwoButtonArea extends MultitouchImageButton {

    protected int firstButtonRID = -1;
    protected int secondButtonRID = -1;
    private ViewHolder holder = new ViewHolder();

    public MultitouchTwoButtonArea(Context context, AttributeSet attrs,
                                   int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public MultitouchTwoButtonArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (!isInEditMode()) {
            setVisibility(View.INVISIBLE);
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.MultitouchTwoButtonArea, 0, 0);

        try {
            firstButtonRID = a.getResourceId(
                    R.styleable.MultitouchTwoButtonArea_first_button, -1);
            secondButtonRID = a.getResourceId(
                    R.styleable.MultitouchTwoButtonArea_second_button, -1);

        } finally {
            a.recycle();
        }
    }

    @Override
    public void onTouchEnter(MotionEvent event) {
        if (holder.firstButton == null) {
            initHolder();
        }

        holder.firstButton.onTouchEnter(event);
        holder.secondButton.onTouchEnter(event);
    }

    @Override
    public void onTouchExit(MotionEvent event) {
        if (holder.firstButton == null) {
            initHolder();
        }

        holder.firstButton.onTouchExit(event);
        holder.secondButton.onTouchExit(event);
    }

    private void initHolder() {
        holder.firstButton = getRootView().findViewById(firstButtonRID);
        holder.secondButton = getRootView().findViewById(secondButtonRID);
    }

    public int getFirstBtnRID() {
        return firstButtonRID;
    }

    public int getSecondBtnRID() {
        return secondButtonRID;
    }

    @Override
    public void requestRepaint() {
        super.requestRepaint();
        holder.firstButton.requestRepaint();
        holder.secondButton.requestRepaint();
    }

    private static class ViewHolder {
        public MultitouchBtnInterface firstButton;
        public MultitouchBtnInterface secondButton;
    }
}
