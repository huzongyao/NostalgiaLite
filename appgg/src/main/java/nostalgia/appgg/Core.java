package nostalgia.appgg;

import nostalgia.framework.base.JniBridge;

/**
 * Game Gear 模拟器 JNI 桥接核心类。
 * <p>加载本地库 "gg"，提供 Java 层与 C++ 模拟器引擎之间的 JNI 通信通道。</p>
 * 采用单例模式，全局仅有一个桥接实例。
 */
public class Core extends JniBridge {
    /** 单例实例 */
    private static Core instance = new Core();

    static {
        System.loadLibrary("gg");
    }

    /** 私有构造方法，防止外部实例化 */
    private Core() {
    }

    /**
     * 获取 Core 单例实例。
     * @return Core 实例
     */
    public static Core getInstance() {
        return instance;
    }

}
