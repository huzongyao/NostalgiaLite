package nostalgia.framework.ui.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;

import nostalgia.framework.remote.VirtualDPad;

public class ControllableListPreference extends ListPreference {

    public ControllableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ControllableListPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        VirtualDPad.getInstance().onResume(getDialog().getWindow());
    }

}
