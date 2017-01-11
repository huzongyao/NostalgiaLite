package nostalgia.framework;

import android.content.Context;


public class EmulatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private int stringResId = -1;
    private String formatArg;

    public EmulatorException(String msg) {
        super(msg);
    }

    public EmulatorException(int stringResId) {
        this.stringResId = stringResId;
    }

    public EmulatorException(int stringResId, String t) {
        this.stringResId = stringResId;
        this.formatArg = t;
    }

    public String getMessage(Context context) {
        if (stringResId != -1) {
            String resource = context.getResources().getString(stringResId);

            if (formatArg != null) {
                return String.format(resource, formatArg);

            } else {
                return resource;
            }
        }

        return getMessage();
    }

}
