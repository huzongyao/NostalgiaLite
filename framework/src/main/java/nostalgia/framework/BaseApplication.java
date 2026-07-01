package nostalgia.framework;


import android.app.Application;

import com.blankj.utilcode.util.Utils;

import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;

/**
 * 应用基类，所有模拟器应用（NES、GBC、GG）的 Application 均继承此类。
 * <p>
 * 负责初始化全局工具库（Utils）、根据构建类型设置日志模式，
 * 以及声明是否有游戏内菜单的抽象方法。
 * </p>
 */
abstract public class BaseApplication extends Application {

    private static final String TAG = BaseApplication.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化工具库
        Utils.init(this);
        // 根据是否为调试包设置日志开关
        boolean debug = EmuUtils.isDebuggable(this);
        NLog.setDebugMode(debug);
    }

    /**
     * 判断此应用是否支持游戏内菜单。
     *
     * @return true 表示支持游戏内菜单
     */
    public abstract boolean hasGameMenu();
}
