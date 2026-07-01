package nostalgia.framework.ui.timetravel;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.Locale;

import nostalgia.framework.EmulatorException;
import nostalgia.framework.R;
import nostalgia.framework.base.Manager;
import nostalgia.framework.ui.gamegallery.GameDescription;

/**
 * 时间回溯对话框。
 * <p>允许玩家通过拖动进度条浏览模拟器历史状态截图，
 * 选择某个历史时间点后恢复该状态，实现"时间回溯"功能。</p>
 *
 * @author NostalgiaLite
 */
public class TimeTravelDialog extends Dialog implements OnSeekBarChangeListener {

    /** 历史截图预览 ImageView */
    private ImageView img;
    /** 显示回溯时间偏移的文本标签（如 "-0.50s"） */
    private TextView label;
    /** 模拟器管理器，用于加载历史状态和截图 */
    private Manager manager;
    /** 用于显示历史截图的 Bitmap 对象 */
    private Bitmap bitmap;
    /** 当前游戏描述 */
    private GameDescription game;
    /** 进度条最大值（历史帧总数减一） */
    private int max = 0;

    /**
     * 构造时间回溯对话框。
     * <p>初始化界面布局、进度条监听器、确认/取消按钮，
     * 并暂停模拟器以渲染当前历史截图。</p>
     *
     * @param context  上下文
     * @param manager  模拟器管理器
     * @param game     当前游戏描述
     */
    public TimeTravelDialog(final Context context, Manager manager,
                            GameDescription game) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        this.manager = manager;
        this.game = game;
        bitmap = Bitmap.createBitmap(256, 256, Config.ARGB_8888);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.dialog_time_travel, null);
        setContentView(content);
        final SeekBar seekBar = content.findViewById(R.id.dialog_time_seek);
        seekBar.setOnSeekBarChangeListener(this);
        Button cancel = content.findViewById(R.id.dialog_time_btn_cancel);
        cancel.setOnClickListener(v -> cancel());
        cancel.setFocusable(true);
        img = content.findViewById(R.id.dialog_time_img);
        label = content.findViewById(R.id.dialog_time_label);
        max = manager.getHistoryItemCount() - 1;
        seekBar.setMax(max);
        seekBar.setProgress(max);
        Button ok = content.findViewById(R.id.dialog_time_wheel_btn_ok);
        ok.setOnClickListener(v -> {
            TimeTravelDialog.this.manager
                    .startGame(TimeTravelDialog.this.game);
            TimeTravelDialog.this.manager.loadHistoryState(max
                    - seekBar.getProgress());

            try {
                TimeTravelDialog.this.manager.enableCheats(context,
                        TimeTravelDialog.this.game);

            } catch (EmulatorException ignored) {
            }
            dismiss();
        });
        ok.setFocusable(true);
        manager.pauseEmulation();
        manager.renderHistoryScreenshot(bitmap, 0);
        img.setImageBitmap(bitmap);
    }

    /**
     * 进度条值变化时回调，更新回溯时间标签并渲染对应历史截图。
     *
     * @param seekBar  进度条
     * @param progress 当前进度值
     * @param fromUser 是否由用户触摸触发
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        label.setText(String.format(Locale.getDefault(),
                "-%02.2fs", (max - progress) / 4f));
        manager.renderHistoryScreenshot(bitmap, max - progress);
        img.setImageBitmap(bitmap);
        img.invalidate();
    }

    /** 用户开始拖动进度条时的回调（无需额外处理） */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /** 用户停止拖动进度条时的回调（无需额外处理） */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

}
