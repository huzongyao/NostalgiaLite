package nostalgia.framework;

/**
 * 图像配置，定义模拟器的显示参数。
 * <p>
 * 包括原始屏幕宽高和帧率，各平台模拟器需提供具体的实现。
 * </p>
 */
public abstract class GfxProfile {

    /** 配置名称 */
    public String name;
    /** 原始屏幕宽度（像素） */
    public int originalScreenWidth;
    /** 原始屏幕高度（像素） */
    public int originalScreenHeight;
    /** 每秒帧数 */
    public int fps;

    /**
     * 将图像配置编码为整数，供 JNI 层或偏好设置使用。
     *
     * @return 编码后的整数值
     */
    public abstract int toInt();
}
