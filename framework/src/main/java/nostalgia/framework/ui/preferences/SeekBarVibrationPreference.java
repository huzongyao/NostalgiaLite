package nostalgia.framework.ui.preferences;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.widget.SeekBar;

import nostalgia.framework.ui.widget.SeekBarPreference;

public class SeekBarVibrationPreference extends SeekBarPreference {

    private Vibrator vibrator;

    public SeekBarVibrationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        setNegativeButtonText("");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seek) {
        super.onStopTrackingTouch(seek);
        vibrator.vibrate(seek.getProgress() * 10);
    }


    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
    }

}
