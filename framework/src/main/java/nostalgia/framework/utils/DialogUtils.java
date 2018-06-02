package nostalgia.framework.utils;

import android.app.Dialog;


public class DialogUtils {

    private DialogUtils() {
    }

    public static void show(Dialog dialog, boolean cancelable) {
        dialog.setCanceledOnTouchOutside(cancelable);
        dialog.show();
    }

}
