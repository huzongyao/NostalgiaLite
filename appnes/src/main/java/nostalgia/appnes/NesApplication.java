package nostalgia.appnes;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.base.EmulatorHolder;

/**
 * NES（FC）模拟器应用程序类。
 * <p>初始化时将模拟器实现设置为 {@link NesEmulator}，
 * 并启用游戏菜单功能。</p>
 *
 * @author NostalgiaLite
 */
public class NesApplication extends BaseApplication {

    /** 应用创建时注册 NES 模拟器实现类 */
    @Override
    public void onCreate() {
        super.onCreate();
        EmulatorHolder.setEmulatorClass(NesEmulator.class);
    }

    /** 启用游戏内菜单 */
    @Override
    public boolean hasGameMenu() {
        return true;
    }
}
