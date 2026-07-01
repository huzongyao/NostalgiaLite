package nostalgia.appgg;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.base.EmulatorHolder;

/**
 * Game Gear 模拟器应用程序入口类。
 * <p>继承自 {@link BaseApplication}，在应用启动时注册 GG 模拟器，
 * 并禁用游戏内菜单功能。</p>
 */
public class GGApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册 Game Gear 模拟器类到模拟器持有器
        EmulatorHolder.setEmulatorClass(GGEmulator.class);
    }

    /**
     * 是否启用游戏内菜单。
     * @return 始终返回 false，GG 模块不支持游戏菜单
     */
    @Override
    public boolean hasGameMenu() {
        return false;
    }
}
