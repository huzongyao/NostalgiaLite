package nostalgia.appgg;

import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.GalleryActivity;

/**
 * Game Gear 游戏画廊 Activity。
 * <p>继承自 {@link GalleryActivity}，负责扫描并展示 .gg 格式的游戏 ROM 文件。</p>
 */
public class GGGalleryActivity extends GalleryActivity {

    /**
     * 获取 GG 模拟器实例。
     * @return GGEmulator 单例
     */
    @Override
    public Emulator getEmulatorInstance() {
        return GGEmulator.getInstance();
    }

    /**
     * 获取游戏运行界面的 Activity 类。
     * @return GGEmulatorActivity 类
     */
    @Override
    public Class<? extends EmulatorActivity> getEmulatorActivityClass() {
        return GGEmulatorActivity.class;
    }

    /**
     * 获取支持的 ROM 文件扩展名集合。
     * @return 包含 "gg" 的扩展名集合
     */
    @Override
    protected Set<String> getRomExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("gg");
        return set;
    }
}
