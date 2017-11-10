package nostalgia.framework.ui.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class PlayersLabelView extends View {

    private static final String TAG = "PlayersLabelView";
    Paint paint = new Paint();
    float textSize = 0;
    int[] offsets = new int[]{0, 300, 800};
    int offset = 0;

    public PlayersLabelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public PlayersLabelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayersLabelView(Context context) {
        this(context, null, 0);
    }

    private void init() {
        paint.setColor(0xffffffff);
        Resources r = getResources();
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                25, r.getDisplayMetrics());
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
    }

    public void setPlayersOffsets(int[] offsets) {
        this.offsets = offsets;
        invalidate();
    }

    public void setOffset(int offset) {
        this.offset = offset;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(0, 40);
        canvas.rotate(-90, 0, 0);
        for (int i = 0; i < offsets.length; i++) {
            String label = "PLAYER " + (i + 1);
            float width = paint.measureText(label);
            int off = (int) (offset - width - offsets[i] + 40);
            boolean active = false;
            if (i < (offsets.length - 1)) {
                active = offsets[i] <= offset && offset < offsets[i + 1];
            } else {
                active = offsets[i] <= offset && offset < offsets[i] + 20000;
            }
            if (active && (offset > (40 - width)))
                off = (int) (40 - width);
            paint.setColor(0xff000000);
            paint.setStyle(Style.FILL);
            canvas.drawRect(off - 2, 0, off + width, getMeasuredWidth(), paint);
            paint.setColor(0xffffffff);
            canvas.drawText(label, off, 40, paint);
        }
        canvas.restore();
    }

}
