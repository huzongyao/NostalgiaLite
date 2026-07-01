package nostalgia.framework.utils;

import android.app.Dialog;


/**
 * 对话框工具类。
 * <p>提供对话框显示相关的便捷方法。</p>
 *
 * @author NostalgiaLite
 */
public class DialogUtils {

    /** 私有构造，禁止实例化 */
    private DialogUtils() {
    }

    /**
     * 显示对话框。
     *
     * @param dialog     要显示的对话框
     * @param cancelable 点击对话框外部区域是否可关闭
     */
    public static void show(Dialog dialog, boolean cancelable) {
        dialog.setCanceledOnTouchOutside(cancelable);
        dialog.show();
    }

}
