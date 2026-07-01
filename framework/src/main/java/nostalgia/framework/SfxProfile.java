package nostalgia.framework;

/**
 * 声音配置，定义模拟器的音频参数。
 * <p>
 * 包括采样率、缓冲区大小、声道模式和编码格式，
 * 各平台模拟器需提供具体的实现。
 * </p>
 */
public abstract class SfxProfile {

    /** 配置名称 */
    public String name;
    /** 是否为立体声 */
    public boolean isStereo;
    /** 采样率（Hz） */
    public int rate;
    /** 音频缓冲区大小 */
    public int bufferSize;
    /** 音频编码格式 */
    public SoundEncoding encoding;

    /** 音质等级 */
    public int quality;

    /**
     * 将声音配置编码为整数，供 JNI 层或偏好设置使用。
     *
     * @return 编码后的整数值
     */
    public abstract int toInt();

    /** 音频编码格式枚举 */
    public enum SoundEncoding {
        /** 8位 PCM 编码 */
        PCM8,
        /** 16位 PCM 编码 */
        PCM16,
    }

}
