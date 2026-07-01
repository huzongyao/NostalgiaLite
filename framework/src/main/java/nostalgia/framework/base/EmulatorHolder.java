package nostalgia.framework.base;

import java.lang.reflect.Method;

import nostalgia.framework.EmulatorInfo;

/**
 * 模拟器全局持有器，通过反射懒加载模拟器实例并缓存模拟器信息。
 * <p>
 * 各平台应用在 Application 初始化时设置模拟器类，
 * 其他模块通过此类获取模拟器信息而无需直接依赖具体实现。
 * </p>
 */
public class EmulatorHolder {

    private static Class<? extends JniEmulator> emulatorClass;
    private static EmulatorInfo info;

    /** 设置模拟器实现类。 */
    public static void setEmulatorClass(Class<? extends JniEmulator> emulatorClass) {
        EmulatorHolder.emulatorClass = emulatorClass;
    }

    /** 获取模拟器信息，首次调用时通过反射创建实例。 */
    public static EmulatorInfo getInfo() {
        if (info == null) {
            try {
                Method getInstance = emulatorClass.getMethod("getInstance");
                JniEmulator emulator = (JniEmulator) getInstance.invoke(null);
                info = emulator.getInfo();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return info;
    }
}
