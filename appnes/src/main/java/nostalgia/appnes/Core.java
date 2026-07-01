package nostalgia.appnes;

import nostalgia.framework.base.JniBridge;

/**
 * NES 模拟器 JNI 桥接核心类。
 * <p>加载 "nes" 本地库并提供 Java 与 C++ 模拟器核心之间的方法桥接。
 * 采用单例模式，全局共享同一个 JNI 桥接实例。</p>
 *
 * @author NostalgiaLite
 */
public class Core extends JniBridge {
    /** 单例实例 */
    private static Core instance = new Core();

    /** 加载 NES 本地库 */
    static {
        System.loadLibrary("nes");
    }

    /** 私有构造，禁止外部实例化 */
    private Core() {
    }

    /**
     * 获取 Core 单例实例。
     *
     * @return Core 实例
     */
    public static Core getInstance() {
        return instance;
    }

}
