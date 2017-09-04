package nostalgia.framework;

import android.util.SparseIntArray;

import java.util.List;

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

    SparseIntArray getKeyMapping();

    int getNumQualityLevels();

    int[] getDeviceKeyboardCodes();

    String[] getDeviceKeyboardNames();

    String[] getDeviceKeyboardDescriptions();

    boolean isMultiPlayerSupported();

}
