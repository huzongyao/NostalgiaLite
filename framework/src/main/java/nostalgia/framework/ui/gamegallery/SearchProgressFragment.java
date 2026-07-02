package nostalgia.framework.ui.gamegallery;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.NumberFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import nostalgia.framework.R;

/**
 * 搜索进度对话框片段。
 * <p>替代已废弃的 {@link android.app.ProgressDialog}，
 * 使用 {@link DialogFragment} + {@link ProgressBar} 实现进度条和取消功能。</p>
 */
public class SearchProgressFragment extends DialogFragment {

    public static final String TAG = "SearchProgressFragment";

    private static final String KEY_MESSAGE = "message";
    private static final String KEY_MAX = "max";

    private ProgressBar progressBar;
    private TextView messageView;
    private OnCancelListener cancelListener;

    /** 取消按钮点击回调接口 */
    public interface OnCancelListener {
        void onCancel();
    }

    /**
     * 创建新实例。
     *
     * @param message 显示的消息文本
     * @param max     进度条最大值，0 表示不确定进度
     * @return 新的对话框片段实例
     */
    public static SearchProgressFragment newInstance(String message, int max) {
        SearchProgressFragment fragment = new SearchProgressFragment();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE, message);
        args.putInt(KEY_MAX, max);
        fragment.setArguments(args);
        fragment.setCancelable(false);
        return fragment;
    }

    /** 设置取消监听器 */
    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }

    /** 更新消息文本 */
    public void setMessage(String message) {
        if (messageView != null) {
            messageView.setText(message);
        } else if (getArguments() != null) {
            getArguments().putString(KEY_MESSAGE, message);
        }
    }

    /** 设置当前进度值 */
    public void setProgress(int progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    /** 获取当前进度值 */
    public int getProgress() {
        return progressBar != null ? progressBar.getProgress() : 0;
    }

    /** 设置进度条最大值 */
    public void setMax(int max) {
        if (progressBar != null) {
            progressBar.setMax(max);
            progressBar.setIndeterminate(false);
        }
    }

    /** 设置是否为不确定进度模式 */
    public void setIndeterminate(boolean indeterminate) {
        if (progressBar != null) {
            progressBar.setIndeterminate(indeterminate);
        }
    }

    /** 设置进度数字格式（兼容 ProgressDialog API） */
    public void setProgressNumberFormat(String format) {
        // DialogFragment 不直接支持此格式
    }

    /** 设置进度百分比格式（兼容 ProgressDialog API） */
    public void setProgressPercentFormat(NumberFormat format) {
        // DialogFragment 不直接支持此格式
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String message = getArguments() != null ? getArguments().getString(KEY_MESSAGE) : "";
        int max = getArguments() != null ? getArguments().getInt(KEY_MAX, 100) : 100;

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_search_progress, null);
        progressBar = view.findViewById(R.id.progress_bar);
        messageView = view.findViewById(R.id.progress_message);

        progressBar.setMax(max);
        progressBar.setIndeterminate(max <= 0);
        messageView.setText(message);

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (cancelListener != null) {
                        cancelListener.onCancel();
                    }
                })
                .create();
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        // 先移除可能残留的同名 Fragment（dismiss 事务尚未提交时会出现这种情况）
        DialogFragment existing = (DialogFragment) manager.findFragmentByTag(TAG);
        if (existing != null) {
            existing.dismissAllowingStateLoss();
            manager.executePendingTransactions();
        }
        super.show(manager, tag);
    }

    /**
     * 安全关闭对话框，同时确保 FragmentManager 中不再残留此 Fragment。
     * 解决 dismiss() 异步提交导致 findFragmentByTag 仍能找到的问题。
     */
    public void safeDismiss() {
        try {
            dismissAllowingStateLoss();
        } catch (Exception ignored) {
            // Fragment 已分离或 Activity 已销毁
        }
        FragmentManager fm = getFragmentManager();
        if (fm != null) {
            DialogFragment existing = (DialogFragment) fm.findFragmentByTag(TAG);
            if (existing != null) {
                fm.beginTransaction().remove(existing).commitAllowingStateLoss();
            }
            fm.executePendingTransactions();
        }
    }
}
