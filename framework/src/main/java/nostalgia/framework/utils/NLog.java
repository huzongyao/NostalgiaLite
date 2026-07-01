package nostalgia.framework.utils;

import android.util.Log;

/**
 * 日志工具类。
 * <p>封装 Android {@link Log}，提供按级别开关的日志输出方法。
 * 支持通过 {@link #setDebugMode(boolean)} 一次性开启所有级别的日志。</p>
 *
 * @author NostalgiaLite
 */
public class NLog {

    /** 各日志级别开关标志（默认仅开启 Error/Warning） */
    private static boolean WTF = true;
    private static boolean E = true;
    private static boolean W = true;
    private static boolean D = false;
    private static boolean I = false;
    private static boolean V = false;

    /**
     * 设置调试模式，开启所有日志级别输出。
     *
     * @param debug true 开启调试模式，false 无效（仅支持开启）
     */
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

    /** 输出 Error 级别日志 */
    public static void e(String tag, String msg) {
        if (E)
            Log.e(tag, msg);
    }

    /** 输出 Error 级别日志（含异常信息） */
    public static void e(String tag, String msg, Throwable e) {
        if (E)
            Log.e(tag, msg, e);
    }

    /** 输出 Debug 级别日志 */
    public static void d(String tag, String msg) {
        if (D)
            Log.d(tag, msg);
    }

    /** 输出 Warning 级别日志 */
    public static void w(String tag, String msg) {
        if (W)
            Log.w(tag, msg);
    }

    /** 输出 Info 级别日志 */
    public static void i(String tag, String msg) {
        if (I)
            Log.i(tag, msg);
    }

    /** 输出 Verbose 级别日志 */
    public static void v(String tag, String msg) {
        if (V)
            Log.i(tag, msg);
    }

    /** 输出 WTF（What a Terrible Failure）级别日志 */
    public static void wtf(String tag, String msg) {
        if (WTF)
            Log.wtf(tag, msg);
    }

    /** 输出 WTF 级别日志（含异常信息） */
    public static void wtf(String tag, String msg, Throwable th) {
        if (WTF)
            Log.wtf(tag, msg, th);
    }
}
