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

public class PopupMenu {

    Typeface font;
    private Context mContext;
    private LayoutInflater mInflater;
    private WindowManager mWindowManager;
    private PopupWindow mPopupWindow;
    private View mContentView;
    private ListView mItemsView;
    private TextView mHeaderTitleView;
    private OnItemSelectedListener mListener;
    private List<MenuItem> mItems;
    private int mWidth = 240;
    private float mScale;

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
     * Sets the popup's content.
     *
     * @param contentView
     */
    private void setContentView(View contentView) {
        mContentView = contentView;
        mItemsView = contentView.findViewById(R.id.items);
        mHeaderTitleView = contentView.findViewById(R.id.header_title);
        mPopupWindow.setContentView(contentView);
    }

    public MenuItem add(int itemId, int titleRes) {
        MenuItem item = new MenuItem();
        item.setItemId(itemId);
        item.setTitle(mContext.getString(titleRes));
        mItems.add(item);

        return item;
    }

    /**
     * Show popup menu.
     */
    public void show() {
        show(null);
    }

    /**
     * Show popup menu.
     *
     * @param anchor
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

    /**
     * Dismiss the popup menu.
     */
    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    /**
     * Sets the popup menu header's title.
     *
     * @param title
     */
    public void setHeaderTitle(CharSequence title) {
        mHeaderTitleView.setText(title);
        mHeaderTitleView.setVisibility(View.VISIBLE);
        mHeaderTitleView.requestFocus();
        mHeaderTitleView.setTypeface(font);
    }

    /**
     * Change the popup's width.
     *
     * @param width
     */
    public void setWidth(int width) {
        mWidth = width;
    }

    /**
     * Register a callback to be invoked when an item in this PopupMenu has been
     * selected.
     *
     * @param listener
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when an item in this
     * PopupMenu has been selected.
     */
    public interface OnItemSelectedListener {
        void onItemSelected(MenuItem item);
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
    }

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
