package nostalgia.framework;

/**
 * 模拟器设置，封装了模拟过程中的各项可配置选项。
 * <p>
 * 通过 {@link #toInt()} 方法将所有设置编码为单个整数，
 * 便于传递给 JNI 本地层。
 * </p>
 */
public class EmulatorSettings {
    /** 是否启用光枪（Zapper）模式 */
    boolean zapperEnabled;
    /** 是否启用时间旅行历史回退功能 */
    boolean historyEnabled;
    /** 是否加载电池存档文件（.sav） */
    boolean loadSavFiles;
    /** 是否保存电池存档文件（.sav） */
    boolean saveSavFiles;
    /** 模拟质量等级 */
    int quality = 0;

    /**
     * 将所有设置编码为单个整数，供 JNI 层解析。
     * <p>
     * 编码规则：个位=光枪, 十位=历史, 百位=加载存档, 千位=保存存档, 万位起=质量等级
     * </p>
     */
    public int toInt() {
        int x = zapperEnabled ? 1 : 0;
        x += historyEnabled ? 10 : 0;
        x += loadSavFiles ? 100 : 0;
        x += saveSavFiles ? 1000 : 0;
        x += quality * 10000;
        return x;
    }
}
