package nostalgia.framework.ui.widget;

import android.content.Intent;
import android.graphics.drawable.Drawable;

/**
 * 自定义弹出菜单的菜单项数据类。
 * <p>封装了菜单项的 ID、标题、图标和点击意图，
 * 供 {@link PopupMenu} 使用。</p>
 *
 * @author NostalgiaLite
 */
public class MenuItem {

    /** 菜单项唯一标识符 */
    private int itemId;
    /** 菜单项显示标题 */
    private String title;
    /** 菜单项图标（可为 null） */
    private Drawable icon;
    /** 菜单项点击后触发的 Intent */
    private Intent intent;

    /** 获取菜单项 ID */
    public int getItemId() {
        return itemId;
    }

    /** 设置菜单项 ID */
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    /** 获取菜单项标题 */
    public String getTitle() {
        return title;
    }

    /** 设置菜单项标题 */
    public void setTitle(String title) {
        this.title = title;
    }

    /** 获取菜单项图标 */
    public Drawable getIcon() {
        return icon;
    }

    /** 设置菜单项图标 */
    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    /** 获取菜单项关联的 Intent */
    public Intent getIntent() {
        return intent;
    }

    /** 设置菜单项关联的 Intent */
    public void setIntent(Intent intent) {
        this.intent = intent;
    }
}
