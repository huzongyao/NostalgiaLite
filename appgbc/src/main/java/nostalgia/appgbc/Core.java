package nostalgia.appgbc;

import nostalgia.framework.base.JniBridge;

/**
 * GBC 模拟器 JNI 桥接核心类。
 * <p>加载 "gbc" 本地库并提供 Java 与 C++ libgambatte 核心之间的方法桥接。
 * 采用单例模式。</p>
 *
 * @author NostalgiaLite
 */
public class Core extends JniBridge {
    /** 单例实例 */
    private static Core instance = new Core();

    /** 加载 GBC 本地库 */
    static {
        System.loadLibrary("gbc");
    }

    /** 私有构造，禁止外部实例化 */
    private Core() {
    }

    /** 获取 Core 单例实例 */
    public static Core getInstance() {
        return instance;
    }
}
