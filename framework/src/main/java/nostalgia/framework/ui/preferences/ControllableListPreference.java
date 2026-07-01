package nostalgia.framework.ui.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;


/**
 * 可控列表偏好设置。
 * <p>
 * 继承自 ListPreference，提供对话框显示的控制能力。
 * </p>
 */
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
    }

}
