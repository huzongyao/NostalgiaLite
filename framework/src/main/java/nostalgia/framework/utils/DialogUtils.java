package nostalgia.framework.utils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.Window;

import nostalgia.framework.remote.VirtualDPad;

public class DialogUtils {

    private static DialogInterface.OnShowListener listener = new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface d) {
            Window window = ((Dialog) d).getWindow();
            VirtualDPad.getInstance().attachToWindow(window);
            VirtualDPad.getInstance().onResume(window);
        }
    };

    private DialogUtils() {
    }

    public static void show(Dialog dialog, boolean cancelable) {
        dialog.setOnShowListener(listener);
        dialog.setCanceledOnTouchOutside(cancelable);
        dialog.show();
    }

}
