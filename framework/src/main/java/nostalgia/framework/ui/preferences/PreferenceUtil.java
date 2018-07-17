package nostalgia.framework.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import nostalgia.framework.Emulator;
import nostalgia.framework.GfxProfile;
import nostalgia.framework.base.EmulatorHolder;
import nostalgia.framework.base.ViewPort;
import nostalgia.framework.ui.gamegallery.GameDescription;

public class PreferenceUtil {

    public static final int EXPORT = 1;
    public static final int IMPORT = 2;
    public static final String GAME_PREF_SUFFIX = ".gamepref";
    private static String escapedI = "{escapedI:-)}";
    private static String escapedN = "{escapedN:-)}";
    private static String escapedNull = "{escapedNULL:-)}";

    public static boolean isBatterySaveBugFixed(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("bs_bug_fixed", false);
    }

    public static void setBatterySaveBugFixed(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putBoolean("bs_bug_fixed", true);
        editor.apply();
    }

    public static boolean isQuickSaveEnabled(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_quicksave", false);
    }

    public static int getFastForwardFrameCount(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int speed = pref.getInt("general_pref_ff_speed", 4);
        return (speed + 1) * 2;
    }

    public static void migratePreferences(int type, SharedPreferences pref, File file) {
        migratePreferences(type, pref, file, NotFoundHandling.FAIL);
    }

    public static void migratePreferences(int type, SharedPreferences pref, File file, NotFoundHandling handling) {
        if (type == EXPORT) {
            exportPreferences(pref, file);
        } else if (type == IMPORT) {
            importPreferences(pref, file, handling);
        } else
            throw new IllegalArgumentException();
    }

    public static void exportPreferences(SharedPreferences pref, File file) {
        Map<String, ?> prefs = pref.getAll();
        if (prefs.size() == 0) {
            return;
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            for (Entry<String, ?> entry : prefs.entrySet()) {
                Object o = entry.getValue();
                String type = null;
                Object value = entry.getValue();
                if (o.getClass() == Integer.class) {
                    type = "I";
                }
                if (o.getClass() == Long.class) {
                    type = "L";
                }
                if (o.getClass() == String.class) {
                    type = "S";
                    String val = (String) value;
                    val = val.replace("|", escapedI);
                    val = val.replace("\n", escapedN);
                    value = val;
                }
                if (o.getClass() == Float.class) {
                    type = "F";
                }
                if (o.getClass() == Boolean.class) {
                    type = "B";
                }
                if (type == null) {
                    throw new RuntimeException("unknown type");
                }
                if (value == null) {
                    value = escapedNull;
                }
                String name = entry.getKey();
                name = name.replace("|", escapedI);
                writer.write(type + "|" + name + "|" + value + "\n");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static void importPreferences(SharedPreferences pref, File file, NotFoundHandling handling) {
        if (handling == NotFoundHandling.IGNORE && !file.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            FileReader r = new FileReader(file);
            reader = new BufferedReader(r);
            String line;
            Editor editor = pref.edit();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                String type = parts[0];
                String name = parts[1];
                name = name.replace(escapedI, "|");
                String value = parts[2];
                if (value.equals(escapedNull)) {
                    value = null;
                }
                if (type.equals("I")) {
                    editor.putInt(name, value != null ? Integer.parseInt(value) : null);
                }
                if (type.equals("B")) {
                    editor.putBoolean(name, value != null ? Boolean.parseBoolean(value) : null);
                }
                if (type.equals("F")) {
                    editor.putFloat(name, value != null ? Float.parseFloat(value) : null);
                }
                if (type.equals("L")) {
                    editor.putLong(name, value != null ? Long.parseLong(value) : null);
                }
                if (type.equals("S")) {
                    if (value != null) {
                        value = value.replace(escapedI, "|");
                        value = value.replace(escapedN, "\n");
                    }
                    editor.putString(name, value);
                }
            }
            editor.apply();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void setViewPort(Context context, ViewPort vp,
                                   int physicalScreenWidth, int physicalScreenHeight) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = pref.edit();
        String tmp = physicalScreenWidth + "x" + physicalScreenHeight;
        String x = "vp-x-" + tmp;
        String y = "vp-y-" + tmp;
        String width = "vp-width-" + tmp;
        String height = "vp-height-" + tmp;
        edit.putInt(x, vp.x);
        edit.putInt(y, vp.y);
        edit.putInt(width, vp.width);
        edit.putInt(height, vp.height);
        edit.apply();
    }


    public static void removeViewPortSave(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = pref.edit();
        for (String key : pref.getAll().keySet()) {
            if (key.startsWith("vp-")) {
                edit.remove(key);
            }
        }
        edit.apply();
    }


    public static ViewPort getViewPort(Context context, int physicalScreenWidth, int physicalScreenHeight) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String tmp = physicalScreenWidth + "x" + physicalScreenHeight;
        String x = "vp-x-" + tmp;
        String y = "vp-y-" + tmp;
        String width = "vp-width-" + tmp;
        String height = "vp-height-" + tmp;
        ViewPort vp = new ViewPort();
        vp.x = pref.getInt(x, -1);
        vp.y = pref.getInt(y, -1);
        vp.width = pref.getInt(width, -1);
        vp.height = pref.getInt(height, -1);
        if (vp.x == -1 || vp.y == -1 || vp.width == -1 || vp.height == -1) {
            return null;
        }
        return vp;
    }

    public static int getFragmentShader(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getInt("fragment_shader", -1);
    }

    public static void setFragmentShader(Context context, int shader) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putInt("fragment_shader", shader);
        editor.apply();
    }

    public static boolean isSoundEnabled(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean muted = pref.getBoolean("general_pref_mute", false);
        return !muted;
    }

    public static boolean isLoadSavFiles(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_load_sav_files", true);
    }

    public static boolean isSaveSavFiles(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_save_sav_files", true);
    }

    public static boolean isBenchmarked(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("is_benchmarked", false);
    }

    public static void setBenchmarked(Context context, boolean value) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = pref.edit();
        edit.putBoolean("is_benchmarked", value);
        edit.apply();
    }

    public static int getVibrationDuration(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return getVibrationDuration(context, pref);
    }

    public static void setEmulationQuality(Context context, int quality) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = pref.edit();
        edit.putString("general_pref_quality", quality + "");
        edit.apply();
    }

    public static int getEmulationQuality(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(pref.getString("general_pref_quality", "1"));
    }

    public static boolean isTurboEnabled(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_turbo", false);
    }

    public static boolean isABButtonEnabled(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_ab_button", false);
    }

    public static boolean isFullScreenEnabled(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_fullscreen", false);
    }

    public static int getControlsOpacity(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return (int) ((pref.getInt("general_pref_ui_opacity", 100) / 100f) * 255);
    }

    public static boolean isAutoHideControls(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_ui_autohide", true);
    }


    private static int getVibrationDuration(Context context, SharedPreferences pref) {
        return pref.getInt("game_pref_ui_strong_vibration", 0) * 10;
    }

    public static GfxProfile getVideoProfile(Context context, Emulator emulator, GameDescription game) {
        String gfxProfileName = getVideoMode(context, emulator, game.checksum);
        GfxProfile gfx = null;
        if (gfxProfileName != null) {
            for (GfxProfile profile : EmulatorHolder.getInfo().getAvailableGfxProfiles()) {
                if (profile.name.toLowerCase(Locale.ENGLISH).equals(
                        gfxProfileName.toLowerCase(Locale.ENGLISH))) {
                    gfx = profile;
                    break;
                }
            }
        }
        if (gfx == null && emulator != null) {
            gfx = emulator.autoDetectGfx(game);
        }
        return gfx;
    }

    public static GfxProfile getLastGfxProfile(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String name = pref.getString("_lastGfx", null);
        try {
            List<GfxProfile> profiles = EmulatorHolder.getInfo().getAvailableGfxProfiles();
            for (GfxProfile profile : profiles) {
                if (profile.name.equals(name)) {
                    return profile;
                }
            }
        } catch (Exception ignored) {
        }

        return EmulatorHolder.getInfo().getDefaultGfxProfile();
    }

    public static void setLastGfxProfile(Context context, GfxProfile profile) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = pref.edit();
        edit.putString("_lastGfx", profile.name);
        edit.apply();
    }

    public static String getVideoMode(Context context, Emulator emulator,
                                      String gameHash) {
        if (gameHash == null) {
            return null;

        } else {
            SharedPreferences pref = context.getSharedPreferences(gameHash
                    + GAME_PREF_SUFFIX, Context.MODE_PRIVATE);
            return getVideoMode(context, emulator, pref);
        }
    }

    private static String getVideoMode(Context context, Emulator emulator, SharedPreferences pref) {
        return pref.getString("game_pref_ui_pal_ntsc_switch", null);
    }

    public static boolean isZapperEnabled(Context context, String gameHash) {
        SharedPreferences pref = context.getSharedPreferences(gameHash + GAME_PREF_SUFFIX, Context.MODE_PRIVATE);
        return isZapperEnable(context, pref);
    }

    private static boolean isZapperEnable(Context context, SharedPreferences pref) {
        return pref.getBoolean("game_pref_zapper", false);
    }

    public static ROTATION getDisplayRotation(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return getDisplayRotation(context, pref);
    }

    public static int getLastGalleryTab(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getInt("LastGalleryTab", 0);
    }

    public static void saveLastGalleryTab(Context context, int tabIdx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putInt("LastGalleryTab", tabIdx);
        editor.apply();
    }

    private static ROTATION getDisplayRotation(Context context, SharedPreferences pref) {
        int i = Integer.parseInt(pref.getString("general_pref_rotation", "0"));
        return ROTATION.values()[i];
    }

    public static boolean isTimeshiftEnabled(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return isTimeshiftEnable(context, pref);
    }

    private static boolean isTimeshiftEnable(Context context, SharedPreferences pref) {
        return pref.getBoolean("game_pref_ui_timeshift", false);
    }

    public static boolean isWifiServerEnable(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return isWifiServerEnable(context, pref);
    }

    private static boolean isWifiServerEnable(Context context, SharedPreferences pref) {
        return pref.getBoolean("general_pref_wifi_server_enable", false);
    }

    public static void setWifiServerEnable(Context context, boolean enable) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        setWifiServerEnable(context, pref, enable);
    }

    private static void setWifiServerEnable(Context context, SharedPreferences pref, boolean enable) {
        Editor edit = pref.edit();
        edit.putBoolean("general_pref_wifi_server_enable", enable);
        edit.apply();
    }

    public static boolean isOpenGLEnable(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return isOpenGLEnable(context, pref);
    }

    private static boolean isOpenGLEnable(Context context, SharedPreferences pref) {
        return pref.getBoolean("general_pref_opengl", true);
    }

    public static boolean isDynamicDPADEnable(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_ddpad", false);
    }

    public static void setDynamicDPADEnable(Context context, boolean enable) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putBoolean("general_pref_ddpad", enable);
        editor.apply();
    }

    public static void setDynamicDPADUsed(Context context, boolean used) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putBoolean("general_pref_ddpad_used", used);
        editor.apply();
    }

    public static boolean isDynamicDPADUsed(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_ddpad_used", false);
    }

    public static boolean isFastForwardUsed(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_fastforward_used", false);
    }

    public static void setFastForwardUsed(Context context, boolean used) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putBoolean("general_pref_fastforward_used", used);
        editor.apply();
    }

    public static boolean isScreenLayoutUsed(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_screen_layout_used", false);
    }

    public static void setScreenLayoutUsed(Context context, boolean used) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putBoolean("general_pref_screen_layout_used", used);
        editor.apply();
    }

    public static boolean isFastForwardEnabled(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_fastforward", false);
    }

    public static boolean isScreenSettingsSaved(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        for (String key : pref.getAll().keySet()) {
            if (key.startsWith("vp-"))
                return true;
        }
        return false;
    }

    public static boolean isFastForwardToggleable(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("general_pref_fastforward_toggle", true);
    }

    public static void setFastForwardEnable(Context context, boolean enable) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = pref.edit();
        editor.putBoolean("general_pref_fastforward", enable);
        editor.apply();
    }

    public enum NotFoundHandling {
        IGNORE, FAIL,
    }

    public enum ROTATION {
        AUTO, PORT, LAND
    }


}
