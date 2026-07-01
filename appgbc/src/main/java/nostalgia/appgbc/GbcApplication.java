package nostalgia.appgbc;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.base.EmulatorHolder;


/**
 * GBC 模拟器应用程序类。
 * <p>初始化时将模拟器实现设置为 {@link GbcEmulator}。</p>
 *
 * @author NostalgiaLite
 */
public class GbcApplication extends BaseApplication {

    /** 应用创建时注册 GBC 模拟器实现类 */
    @Override
    public void onCreate() {
        super.onCreate();
        EmulatorHolder.setEmulatorClass(GbcEmulator.class);
    }

    /** GBC 不启用游戏内菜单 */
    @Override
    public boolean hasGameMenu() {
        return false;
    }

}