/**
 * Based on http://hackskrieg.wordpress.com/2012/04/20/working-vertical-seekbar-for-android/
 */

package nostalgia.framework.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * 垂直方向的 SeekBar 控件。
 * <p>通过旋转 Canvas 和交换宽高测量实现垂直布局，
 * 支持触摸拖动、进度变化监听及外部设置进度值。</p>
 * <p>基于 hackskrieg 的 VerticalSeekBar 实现修改。</p>
 *
 * @author NostalgiaLite
 */
public class VerticalSeekBar extends AppCompatSeekBar {

    /** 进度变化监听器 */
    private SeekBar.OnSeekBarChangeListener onChangeListener;
    /** 上一次记录的进度值，用于避免重复触发回调 */
    private int lastProgress = 0;

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** 尺寸变化时交换宽高，实现垂直布局 */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    /** 测量时交换宽高测量规格，使控件呈现垂直形态 */
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec,
                                          int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    /** 绘制时先旋转 -90 度再平移，实现垂直方向的 SeekBar 渲染 */
    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    /** 设置进度变化监听器 */
    @Override
    public void setOnSeekBarChangeListener(
            OnSeekBarChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    /**
     * 处理触摸事件，将垂直方向的 Y 坐标映射为 SeekBar 进度值，
     * 并在进度实际变化时触发监听器回调。
     *
     * @param event 触摸事件
     * @return 是否已处理该事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onChangeListener.onStartTrackingTouch(this);
                setPressed(true);
                setSelected(true);
                break;
            case MotionEvent.ACTION_MOVE:
                super.onTouchEvent(event);
                int progress = getMax()
                        - (int) (getMax() * event.getY() / getHeight());

                // Ensure progress stays within boundaries
                if (progress < 0) {
                    progress = 0;
                }
                if (progress > getMax()) {
                    progress = getMax();
                }
                setProgress(progress); // Draw progress
                if (progress != lastProgress) {
                    // Only enact listener if the progress has actually changed
                    lastProgress = progress;
                    onChangeListener.onProgressChanged(this, progress, true);
                }

                onSizeChanged(getWidth(), getHeight(), 0, 0);
                setPressed(true);
                setSelected(true);
                break;
            case MotionEvent.ACTION_UP:
                onChangeListener.onStopTrackingTouch(this);
                setPressed(false);
                setSelected(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                super.onTouchEvent(event);
                setPressed(false);
                setSelected(false);
                break;
        }
        return true;
    }

    /**
     * 设置进度并更新滑块位置。
     * <p>若进度值与上次不同，同时触发进度变化回调。</p>
     *
     * @param progress 新进度值
     */
    public synchronized void setProgressAndThumb(int progress) {
        setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
        if (progress != lastProgress) {
            // Only enact listener if the progress has actually changed
            lastProgress = progress;
            onChangeListener.onProgressChanged(this, progress, true);
        }
    }

    /** 获取最大值（等同于 {@link #getMax()}） */
    public synchronized int getMaximum() {
        return getMax();
    }

    /** 设置最大值（等同于 {@link #setMax(int)}） */
    public synchronized void setMaximum(int maximum) {
        setMax(maximum);
    }
}
