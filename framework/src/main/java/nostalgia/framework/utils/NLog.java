package nostalgia.framework.utils;

public class NLog {


    private static boolean WTF = true;
    private static boolean E = true;
    private static boolean W = true;
    private static boolean D = false;
    private static boolean I = false;
    private static boolean V = false;

    public static void setDebugMode(boolean debug) {
        if (debug) {
            WTF = true;
            E = true;
            W = true;
            D = true;
            I = true;
            V = true;
        }
    }

    public static void e(String tag, String msg) {
        if (E)
            android.util.Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        if (E)
            android.util.Log.e(tag, msg, e);
    }

    public static void d(String tag, String msg) {
        if (D)
            android.util.Log.d(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (W)
            android.util.Log.w(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (I)
            android.util.Log.i(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (V)
            android.util.Log.i(tag, msg);
    }

    public static void wtf(String tag, String msg) {
        if (WTF)
            android.util.Log.wtf(tag, msg);
    }

    public static void wtf(String tag, String msg, Throwable th) {
        if (WTF)
            android.util.Log.wtf(tag, msg, th);
    }
}
