package nostalgia.framework;

import android.content.Context;


/**
 * 模拟器异常，用于表示模拟过程中的运行时错误。
 * <p>
 * 支持通过字符串资源 ID 构造异常信息，便于在 UI 层
 * 直接显示本地化的错误消息。
 * </p>
 */
public class EmulatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    /** 字符串资源 ID，用于本地化错误消息 */
    private int stringResId = -1;
    /** 格式化参数 */
    private String formatArg;

    public EmulatorException(String msg) {
        super(msg);
    }

    /**
     * 通过字符串资源 ID 构造异常。
     *
     * @param stringResId 错误消息的字符串资源 ID
     */
    public EmulatorException(int stringResId) {
        this.stringResId = stringResId;
    }

    /**
     * 通过字符串资源 ID 和格式化参数构造异常。
     *
     * @param stringResId 错误消息的字符串资源 ID
     * @param t           格式化参数
     */
    public EmulatorException(int stringResId, String t) {
        this.stringResId = stringResId;
        this.formatArg = t;
    }

    /**
     * 获取可在 UI 层直接显示的本地化错误消息。
     *
     * @param context Android 上下文，用于访问资源
     * @return 本地化的错误消息字符串
     */
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
