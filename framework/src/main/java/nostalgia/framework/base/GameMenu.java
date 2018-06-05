package nostalgia.framework.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import nostalgia.framework.R;
import nostalgia.framework.utils.DialogUtils;
import nostalgia.framework.utils.EmuUtils;

public class GameMenu {

    ArrayList<GameMenuItem> items = new ArrayList<>();
    Context context;
    OnGameMenuListener listener;
    LayoutInflater inflater;
    private Dialog dialog = null;

    public GameMenu(Context context, OnGameMenuListener listener) {
        this.context = context;
        this.listener = listener;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        listener.onGameMenuCreate(this);
    }

    public GameMenuItem add(String label, int iconRID) {
        GameMenuItem item = new GameMenuItem();
        item.id = items.size();
        item.title = label;
        item.iconRID = iconRID;
        items.add(item);
        return item;
    }

    public GameMenuItem add(String label) {
        return add(label, -1);
    }

    public GameMenuItem add(int labelRID) {
        GameMenuItem item = add((String) context.getText(labelRID), -1);
        item.id = labelRID;
        return item;
    }

    public GameMenuItem add(int labelRID, int iconRID) {
        GameMenuItem item = add((String) context.getText(labelRID), iconRID);
        item.id = labelRID;
        return item;
    }

    public void dismiss() {
        if (isOpen()) {
            dialog.dismiss();
        }
    }

    public boolean isOpen() {
        return dialog != null && dialog.isShowing();
    }

    public void open() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = new Dialog(context, R.style.GameDialogTheme);
        listener.onGameMenuPrepare(this);
        RelativeLayout surroundContainer = (RelativeLayout) inflater.inflate(R.layout.game_menu_surround, null);
        surroundContainer.setOnClickListener(v -> {
            if (dialog != null)
                dialog.cancel();
        });
        LinearLayout container = surroundContainer.findViewById(R.id.game_menu_container);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) container.getLayoutParams();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = EmuUtils.getDisplayWidth(display);
        int px = width / 10;
        params.setMargins(px, 0, px, 0);
        container.setLayoutParams(params);
        int padding = context.getResources().getDimensionPixelSize(
                R.dimen.dialog_back_padding);
        container.setPadding(padding, padding, padding, padding);
        int margin = context.getResources().getDimensionPixelSize(
                R.dimen.dialog_button_margin);
        boolean landsacpe = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        for (int i = 0; i < items.size(); i++) {
            if (landsacpe) {
                LayoutParams pp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1);
                pp.gravity = Gravity.CENTER_VERTICAL;
                LinearLayout menuRow = new LinearLayout(context);
                GameMenuItem item = items.get(i);
                menuRow.addView(createButton(item, margin, dialog), pp);
                i++;

                if (i < items.size()) {
                    LinearLayout lineSeparator = new LinearLayout(context);
                    lineSeparator.setBackgroundColor(0xffffffff);
                    menuRow.addView(lineSeparator, 1, LayoutParams.MATCH_PARENT);
                    GameMenuItem item2 = items.get(i);
                    menuRow.addView(createButton(item2, margin, dialog), pp);
                }

                container.addView(menuRow);

            } else {
                GameMenuItem item = items.get(i);
                container.addView(createButton(item, margin, dialog),
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            }

            if (i < (items.size() - 1)) {
                LinearLayout linSeperator = new LinearLayout(context);
                linSeperator.setBackgroundColor(0xffffffff);
                container.addView(linSeperator, LayoutParams.MATCH_PARENT, 1);
            }
        }

        dialog.setContentView(surroundContainer);
        dialog.setOnCancelListener(d -> {
            listener.onGameMenuClosed(GameMenu.this);
            dialog = null;
        });
        dialog.setOnDismissListener(d -> {
            listener.onGameMenuClosed(GameMenu.this);
            dialog = null;
        });
        DialogUtils.show(dialog, true);
        listener.onGameMenuOpened(this);
    }

    private View createButton(final GameMenuItem item, int margin, final Dialog dialog) {
        View view = inflater.inflate(R.layout.game_menu_item, null);
        TextView label = view.findViewById(R.id.game_menu_item_label);
        label.setText(item.getTitle());
        ImageView iconView = view.findViewById(R.id.game_menu_item_icon);
        view.setOnClickListener(v -> {
            listener.onGameMenuItemSelected(GameMenu.this, item);
            dialog.dismiss();
            listener.onGameMenuClosed(GameMenu.this);
        });
        int iconRID = item.iconRID;

        if (iconRID > 0) {
            iconView.setImageResource(iconRID);
        }

        view.setFocusable(true);
        view.setEnabled(item.enable);
        label.setEnabled(item.enable);
        return view;
    }

    public GameMenuItem getItem(int id) {
        for (GameMenuItem item : items) {
            if (item.id == id) {
                return item;
            }
        }

        return null;
    }

    public interface OnGameMenuListener {

        void onGameMenuCreate(GameMenu menu);

        void onGameMenuPrepare(GameMenu menu);

        void onGameMenuOpened(GameMenu menu);

        void onGameMenuClosed(GameMenu menu);

        void onGameMenuItemSelected(GameMenu menu, GameMenuItem item);
    }

    public class GameMenuItem {
        String title = "";
        int id;
        int iconRID = -1;
        boolean enable = true;

        public String getTitle() {
            return title;
        }

        public int getId() {
            return id;
        }

        public void setEnable(boolean en) {
            enable = en;
        }

        public void set(String title, int id) {
            this.title = title;
            this.id = id;
        }
    }

}
