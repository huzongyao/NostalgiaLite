/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package nostalgia.framework.ui.widget;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * 基于对话框的 SeekBar 偏好设置项。
 * <p>点击后弹出包含 SeekBar 滑块的对话框，
 * 用户可拖动滑块调整数值，结果自动持久化到 SharedPreferences。
 * 支持自定义最大值、默认值、后缀文本和对话框消息。</p>
 * <p>原始作者：Matthew Wiggins，基于 Apache 2.0 许可证发布。</p>
 *
 * @author Matthew Wiggins
 */
public class SeekBarPreference extends DialogPreference
        implements SeekBar.OnSeekBarChangeListener {

    /** Android 命名空间 URI，用于读取 XML 属性 */
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    /** 特殊 hack 模式标志（值偏移 +1 显示） */
    boolean hack = false;
    /** SeekBar 滑块控件 */
    private SeekBar mSeekBar;
    /** 对话框顶部提示文本 */
    private TextView mSplashText, mValueText;
    /** 上下文 */
    private Context mContext;
    /** 对话框提示消息和数值后缀文本 */
    private String mDialogMessage, mSuffix;
    /** 默认值、最大值和当前值 */
    private int mDefault, mMax, mValue = 0;
    /** 是否显示提示文本 */
    private boolean mShowText = true;

    /**
     * 构造 SeekBar 偏好设置项。
     * <p>从 XML 属性中读取对话框消息、后缀文本、默认值和最大值。</p>
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mDialogMessage = attrs.getAttributeValue(ANDROID_NS, "dialogMessage");
        mSuffix = attrs.getAttributeValue(ANDROID_NS, "text");

        if (mSuffix.equals("[hack]")) {
            hack = true;
            mSuffix = "";
        }
        mDefault = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(ANDROID_NS, "max", 100);

    }

    /** 创建对话框视图：包含提示文本、当前值显示和 SeekBar 滑块 */
    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        mSplashText = new TextView(mContext);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(32);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
        return layout;
    }

    /** 绑定对话框视图时初始化 SeekBar 的最大值和当前进度 */
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }

    /** 设置初始值：恢复时从持久化存储读取，否则使用默认值 */
    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        mValue = restore ? (shouldPersist() ? getPersistedInt(mDefault) : 0) : (Integer) defaultValue;
    }

    /** SeekBar 进度变化时回调，更新显示文本并持久化新值 */
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {

        String t = String.valueOf(value + (hack ? 1 : 0));
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));

        if (shouldPersist())
            persistInt(value);
        callChangeListener(value);
    }

    /** 用户开始拖动 SeekBar 时的回调（无需额外处理） */
    public void onStartTrackingTouch(SeekBar seek) {
    }

    /** 用户停止拖动 SeekBar 时的回调（无需额外处理） */
    public void onStopTrackingTouch(SeekBar seek) {
    }

    /** 获取 SeekBar 最大值 */
    public int getMax() {
        return mMax;
    }

    /** 设置 SeekBar 最大值 */
    public void setMax(int max) {
        mMax = max;
    }

    /** 获取当前进度值 */
    public int getProgress() {
        return mValue;
    }

    /** 设置当前进度值，并同步更新 SeekBar 显示 */
    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }
}