package nostalgia.framework;

import android.graphics.Bitmap;

/**
 * 存档槽位信息，用于在存档选择界面展示各槽位的存档状态。
 */
public class SlotInfo {
    /** 槽位 ID */
    public int id;
    /** 该槽位是否已有存档 */
    public boolean isUsed;
    /** 存档文件路径 */
    public String path;
    /** 存档时的游戏截图 */
    public Bitmap screenShot;
    /** 存档文件的最后修改时间（毫秒时间戳） */
    public long lastModified = -1;
}
