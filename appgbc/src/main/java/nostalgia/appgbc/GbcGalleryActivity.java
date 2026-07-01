package nostalgia.appgbc;

import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.GalleryActivity;

/**
 * GBC 游戏画廊界面。
 * <p>提供 GB/GBC ROM 文件浏览和启动功能，
 * 支持 .gb 和 .gbc 格式。</p>
 *
 * @author NostalgiaLite
 */
public class GbcGalleryActivity extends GalleryActivity {

    /** 获取 GBC 模拟器运行界面类 */
    @Override
    public Class<? extends EmulatorActivity> getEmulatorActivityClass() {
        return GbcEmulatorActivity.class;
    }

    /** 获取 GBC 支持的 ROM 文件扩展名集合（.gb, .gbc） */
    @Override
    protected Set<String> getRomExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("gb");
        set.add("gbc");
        return set;
    }

    /** 获取 GBC 模拟器实例 */
    @Override
    public Emulator getEmulatorInstance() {
        return GbcEmulator.getInstance();
    }

}
