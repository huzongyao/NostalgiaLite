package nostalgia.framework;

import android.util.SparseIntArray;

import java.util.List;

/**
 * 模拟器信息接口，定义模拟器平台的元数据和能力描述。
 * <p>
 * 包括模拟器名称、可用的图像/声音配置列表、按键映射、
 * 金手指支持、光枪支持等信息。
 * </p>
 */
public interface EmulatorInfo {

    /** 获取模拟器名称。 */
    String getName();

    /** 是否支持光枪（Zapper）。 */
    boolean hasZapper();

    /** 是否支持原始内存金手指。 */
    boolean supportsRawCheats();

    /** 获取金手指密码中无效字符的正则表达式。 */
    String getCheatInvalidCharsRegex();

    /** 获取默认的图像配置。 */
    GfxProfile getDefaultGfxProfile();

    /** 获取默认的声音配置。 */
    SfxProfile getDefaultSfxProfile();

    /** 获取默认的键盘映射配置。 */
    KeyboardProfile getDefaultKeyboardProfile();

    /** 获取所有可用的图像配置列表。 */
    List<GfxProfile> getAvailableGfxProfiles();

    /** 获取所有可用的声音配置列表。 */
    List<SfxProfile> getAvailableSfxProfiles();

    /** 获取按键映射表。 */
    SparseIntArray getKeyMapping();

    /** 获取可用的模拟质量等级数量。 */
    int getNumQualityLevels();

    /** 获取设备键盘的按键码数组。 */
    int[] getDeviceKeyboardCodes();

    /** 获取设备键盘的名称数组。 */
    String[] getDeviceKeyboardNames();

    /** 获取设备键盘的描述数组。 */
    String[] getDeviceKeyboardDescriptions();

    /** 是否支持多人游戏（双玩家）。 */
    boolean isMultiPlayerSupported();

}
