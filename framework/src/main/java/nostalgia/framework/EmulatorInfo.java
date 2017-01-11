package nostalgia.framework;

import java.util.List;
import java.util.Map;

public interface EmulatorInfo {

    String getName();

    boolean hasZapper();

    boolean supportsRawCheats();

    String getCheatInvalidCharsRegex();

    GfxProfile getDefaultGfxProfile();

    SfxProfile getDefaultSfxProfile();

    KeyboardProfile getDefaultKeyboardProfile();

    List<GfxProfile> getAvailableGfxProfiles();

    List<SfxProfile> getAvailableSfxProfiles();

    Map<Integer, Integer> getKeyMapping();

    int getNumQualityLevels();

    int[] getDeviceKeyboardCodes();

    String[] getDeviceKeyboardNames();

    String[] getDeviceKeyboardDescriptions();

    boolean isMultiPlayerSupported();

}
