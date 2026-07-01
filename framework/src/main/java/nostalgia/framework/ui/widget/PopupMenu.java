package nostalgia.framework.ui.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nostalgia.framework.R;

/**
 * 自定义弹出菜单组件。
 * <p>基于 {@link PopupWindow} 实现，支持显示带图标和标题的菜单项列表，
 * 可锚定到指定 View 或居中显示。支持设置头部标题和条目选中回调。</p>
 *
 * @author NostalgiaLite
 */
public class PopupMenu {

    /** 自定义字体 */
    Typeface font;
    /** 上下文 */
    private Context mContext;
    /** 布局填充器 */
    private LayoutInflater mInflater;
    /** 窗口管理器，用于获取屏幕尺寸 */
    private WindowManager mWindowManager;
    /** 弹出窗口实例 */
    private PopupWindow mPopupWindow;
    /** 弹出窗口内容视图 */
    private View mContentView;
    /** 菜单项列表视图 */
    private ListView mItemsView;
    /** 头部标题 TextView */
    private TextView mHeaderTitleView;
    /** 菜单项选中监听器 */
    private OnItemSelectedListener mListener;
    /** 菜单项数据集合 */
    private List<MenuItem> mItems;
    /** 弹出菜单默认宽度（单位：sp） */
    private int mWidth = 240;
    /** 屏幕密度缩放因子 */
    private float mScale;

    /**
     * 构造弹出菜单。
     * <p>初始化窗口管理器、布局填充器、弹出窗口，
     * 并设置点击外部区域自动关闭的触摸拦截器。</p>
     *
     * @param context 上下文
     */
    @SuppressLint("ClickableViewAccessibility")
    public PopupMenu(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScale = metrics.scaledDensity;

        mItems = new ArrayList<>();

        mPopupWindow = new PopupWindow(context);
        mPopupWindow.setTouchInterceptor((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                mPopupWindow.dismiss();
                return true;
            }
            return false;
        });
        setContentView(mInflater.inflate(R.layout.popup_menu, null));
    }

    /**
     * 设置弹出窗口的内容视图，并初始化菜单列表和标题视图。
     *
     * @param contentView 内容视图
     */
    private void setContentView(View contentView) {
        mContentView = contentView;
        mItemsView = contentView.findViewById(R.id.items);
        mHeaderTitleView = contentView.findViewById(R.id.header_title);
        mPopupWindow.setContentView(contentView);
    }

    /**
     * 添加一个菜单项。
     *
     * @param itemId   菜单项 ID
     * @param titleRes 标题字符串资源 ID
     * @return 创建的菜单项
     */
    public MenuItem add(int itemId, int titleRes) {
        MenuItem item = new MenuItem();
        item.setItemId(itemId);
        item.setTitle(mContext.getString(titleRes));
        mItems.add(item);

        return item;
    }

    /** 显示弹出菜单（居中显示，无锚点） */
    public void show() {
        show(null);
    }

    /**
     * 显示弹出菜单。
     * <p>如果指定锚点 View，则在锚点附近弹出；
     * 否则在屏幕中央显示。根据锚点上下可用空间自动选择弹出方向。</p>
     *
     * @param anchor 锚点 View（可为 null）
     */
    public void show(View anchor) {

        if (mItems.size() == 0) {
            throw new IllegalStateException(
                    "PopupMenu#add was not called with a menu item to display.");
        }

        preShow();

        MenuItemAdapter adapter = new MenuItemAdapter(mContext, mItems);
        mItemsView.setAdapter(adapter);
        mItemsView.setOnItemClickListener((parent, view, position, id) -> {
            if (mListener != null) {
                mListener.onItemSelected(mItems.get(position));
            }
            mPopupWindow.dismiss();
        });

        if (anchor == null) {
            View parent = ((Activity) mContext).getWindow().getDecorView();
            mPopupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
            return;
        }

        int xPos, yPos;
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0]
                + anchor.getWidth(), location[1] + anchor.getHeight());

        mContentView.setLayoutParams(new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mContentView.measure(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        int rootHeight = mContentView.getMeasuredHeight();
        int screenHeight = mWindowManager.getDefaultDisplay().getHeight();

        // Set x-coordinate to display the popup menu
        xPos = anchorRect.centerX() - mPopupWindow.getWidth() / 2;

        int dyTop = anchorRect.top;
        int dyBottom = screenHeight + rootHeight;
        boolean onTop = dyTop > dyBottom;

        // Set y-coordinate to display the popup menu
        if (onTop) {
            yPos = anchorRect.top - rootHeight;
        } else {
            if (anchorRect.bottom > dyTop) {
                yPos = anchorRect.bottom - 20;
            } else {
                yPos = anchorRect.top - anchorRect.bottom + 20;
            }
        }
        View parent = ((Activity) mContext).getWindow().getDecorView();

        mPopupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, xPos, yPos);
    }

    /** 弹出前的准备工作：设置宽高、可触摸、可聚焦、背景等属性 */
    private void preShow() {
        int width = (int) (mWidth * mScale);
        mPopupWindow.setWidth(width);
        mPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        mPopupWindow.setBackgroundDrawable(mContext.getResources().getDrawable(
                R.drawable.panel_background));
    }

    /** 关闭弹出菜单 */
    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    /**
     * 设置弹出菜单的头部标题。
     *
     * @param title 标题文本
     */
    public void setHeaderTitle(CharSequence title) {
        mHeaderTitleView.setText(title);
        mHeaderTitleView.setVisibility(View.VISIBLE);
        mHeaderTitleView.requestFocus();
        mHeaderTitleView.setTypeface(font);
    }

    /**
     * 设置弹出菜单宽度。
     *
     * @param width 宽度（单位：sp）
     */
    public void setWidth(int width) {
        mWidth = width;
    }

    /**
     * 设置菜单项选中监听器。
     *
     * @param listener 选中监听器
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mListener = listener;
    }

    /** 菜单项选中监听器接口 */
    public interface OnItemSelectedListener {
        void onItemSelected(MenuItem item);
    }

    /** 列表项视图持有器，缓存图标和标题引用 */
    static class ViewHolder {
        ImageView icon;
        TextView title;
    }

    /** 菜单项适配器，负责将 {@link MenuItem} 数据绑定到列表项视图 */
    private class MenuItemAdapter extends ArrayAdapter<MenuItem> {

        public MenuItemAdapter(Context context, List<MenuItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.menu_list_item, null);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.icon);
                holder.title = convertView.findViewById(R.id.title);
                holder.title.setTypeface(font);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            MenuItem item = getItem(position);
            if (item.getIcon() != null) {
                holder.icon.setImageDrawable(item.getIcon());
                holder.icon.setVisibility(View.VISIBLE);
            } else {
                holder.icon.setVisibility(View.GONE);
            }
            holder.title.setText(item.getTitle());

            return convertView;
        }
    }
}
