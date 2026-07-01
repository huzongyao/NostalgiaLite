package nostalgia.framework;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import nostalgia.framework.controllers.KeyboardController;
import nostalgia.framework.utils.NLog;

/**
 * 键盘/手柄映射配置，定义物理按键到虚拟手柄按键的映射关系。
 * <p>
 * 支持多种预设配置（默认、PS3手柄、Wii遥控器）以及用户自定义配置，
 * 配置通过 SharedPreferences 持久化存储。
 * </p>
 */
public class KeyboardProfile implements Serializable {
    /** 预设配置名称 */
    public static final String[] DEFAULT_PROFILES_NAMES = {"default", "ps3", "wiimote"};

    private static final long serialVersionUID = 5817859819275903370L;
    private static final String KEYBOARD_PROFILES_SETTINGS = "keyboard_profiles_pref";
    private static final String KEYBOARD_PROFILE_POSTFIX = "_keyboard_profile";
    private static final String TAG = "KeyboardProfile";

    /** 按键名称数组（由子类或外部初始化） */
    public static String[] BUTTON_NAMES = null;
    /** 按键描述数组 */
    public static String[] BUTTON_DESCRIPTIONS = null;
    /** 按键对应的 Android KeyEvent 键码数组 */
    public static int[] BUTTON_KEY_EVENT_CODES = null;

    /** 配置名称 */
    public String name;
    /** 物理键码到虚拟手柄按键的映射表 */
    public SparseIntArray keyMap = new SparseIntArray();

    /**
     * 创建默认键盘映射配置。
     * 方向键=方向，Enter=Start，空格=Select，Q=A，W=B，A=连发A，S=连发B。
     */
    public static KeyboardProfile createDefaultProfile() {
        KeyboardProfile profile = new KeyboardProfile();
        profile.name = "default";
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_LEFT, EmulatorController.KEY_LEFT);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, EmulatorController.KEY_RIGHT);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_UP, EmulatorController.KEY_UP);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_DOWN, EmulatorController.KEY_DOWN);
        profile.keyMap.put(KeyEvent.KEYCODE_ENTER, EmulatorController.KEY_START);
        profile.keyMap.put(KeyEvent.KEYCODE_SPACE, EmulatorController.KEY_SELECT);
        profile.keyMap.put(KeyEvent.KEYCODE_Q, EmulatorController.KEY_A);
        profile.keyMap.put(KeyEvent.KEYCODE_W, EmulatorController.KEY_B);
        profile.keyMap.put(KeyEvent.KEYCODE_A, EmulatorController.KEY_A_TURBO);
        profile.keyMap.put(KeyEvent.KEYCODE_S, EmulatorController.KEY_B_TURBO);
        return profile;
    }

    /**
     * 创建 PS3 手柄映射配置。
     * 方向键=方向，Start/Select=Start/Select，X=A，Y=B，R2=菜单，L2=返回，L1=快进。
     */
    @SuppressLint("InlinedApi")
    public static KeyboardProfile createPS3Profile() {
        KeyboardProfile profile = new KeyboardProfile();
        profile.name = "ps3";
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_LEFT, EmulatorController.KEY_LEFT);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, EmulatorController.KEY_RIGHT);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_UP, EmulatorController.KEY_UP);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_DOWN, EmulatorController.KEY_DOWN);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_START, EmulatorController.KEY_START);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_SELECT, EmulatorController.KEY_SELECT);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_B, EmulatorController.KEY_A);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_Y, EmulatorController.KEY_B);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_A, EmulatorController.KEY_A_TURBO);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_X, EmulatorController.KEY_B_TURBO);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_R2, KeyboardController.KEY_MENU);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_L2, KeyboardController.KEY_BACK);
        profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_L1, KeyboardController.KEY_FAST_FORWARD);
        return profile;
    }

    /**
     * 创建 Wii 遥控器映射配置。
     * 支持双人映射：玩家1使用数字键1/2和方向键，玩家2使用字母键IJKL和OJHM。
     */
    public static KeyboardProfile createWiimoteProfile() {
        KeyboardProfile profile = new KeyboardProfile();
        profile.name = "wiimote";
        // 玩家1映射
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_LEFT, EmulatorController.KEY_LEFT);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, EmulatorController.KEY_RIGHT);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_UP, EmulatorController.KEY_UP);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_DOWN, EmulatorController.KEY_DOWN);
        profile.keyMap.put(KeyEvent.KEYCODE_P, EmulatorController.KEY_START);
        profile.keyMap.put(KeyEvent.KEYCODE_M, EmulatorController.KEY_SELECT);
        profile.keyMap.put(KeyEvent.KEYCODE_1, EmulatorController.KEY_B);
        profile.keyMap.put(KeyEvent.KEYCODE_2, EmulatorController.KEY_A);
        profile.keyMap.put(KeyEvent.KEYCODE_DPAD_CENTER, KeyboardController.KEY_MENU);
        profile.keyMap.put(KeyEvent.KEYCODE_H, KeyboardController.KEY_BACK);
        // 玩家2映射（使用偏移量区分）
        profile.keyMap.put(KeyEvent.KEYCODE_O,
                EmulatorController.KEY_LEFT + KeyboardController.PLAYER2_OFFSET);
        profile.keyMap.put(KeyEvent.KEYCODE_J,
                EmulatorController.KEY_RIGHT + KeyboardController.PLAYER2_OFFSET);
        profile.keyMap.put(KeyEvent.KEYCODE_I,
                EmulatorController.KEY_UP + KeyboardController.PLAYER2_OFFSET);
        profile.keyMap.put(KeyEvent.KEYCODE_K,
                EmulatorController.KEY_DOWN + KeyboardController.PLAYER2_OFFSET);
        profile.keyMap.put(KeyEvent.KEYCODE_PLUS,
                EmulatorController.KEY_START + KeyboardController.PLAYER2_OFFSET);
        profile.keyMap.put(KeyEvent.KEYCODE_MINUS,
                EmulatorController.KEY_SELECT + KeyboardController.PLAYER2_OFFSET);
        profile.keyMap.put(KeyEvent.KEYCODE_COMMA,
                EmulatorController.KEY_B + KeyboardController.PLAYER2_OFFSET);
        profile.keyMap.put(KeyEvent.KEYCODE_PERIOD,
                EmulatorController.KEY_A + KeyboardController.PLAYER2_OFFSET);
        return profile;
    }

    /**
     * 获取当前选中的键盘配置。
     *
     * @param gameHash 游戏的唯一标识（未使用，保留用于未来按游戏设置不同配置）
     * @param context  Android 上下文
     * @return 当前选中的键盘配置
     */
    public static KeyboardProfile getSelectedProfile(String gameHash, Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String name = pref.getString("pref_game_keyboard_profile", "default");
        return load(context, name);
    }

    /**
     * 从 SharedPreferences 加载指定名称的键盘配置。
     * 若无自定义数据则回退到对应的预设配置。
     *
     * @param context Android 上下文
     * @param name    配置名称
     * @return 加载的键盘配置
     */
    public static KeyboardProfile load(Context context, String name) {
        if (name != null) {
            SharedPreferences pref = context.getSharedPreferences(name + KEYBOARD_PROFILE_POSTFIX,
                    Context.MODE_PRIVATE);
            if (pref.getAll().size() != 0) {
                KeyboardProfile profile = new KeyboardProfile();
                profile.name = name;
                for (Entry<String, ?> entry : pref.getAll().entrySet()) {
                    String key = entry.getKey();
                    Integer value = (Integer) entry.getValue();
                    int nkey = Integer.parseInt(key);
                    int nvalue = value;
                    profile.keyMap.put(nkey, nvalue);
                }
                return profile;
            } else {
                NLog.i(TAG, "empty " + name + KEYBOARD_PROFILE_POSTFIX);
                switch (name) {
                    case "ps3":
                        return createPS3Profile();
                    case "wiimote":
                        return createWiimoteProfile();
                    default:
                        return createDefaultProfile();
                }
            }
        } else {
            return createDefaultProfile();
        }
    }

    /**
     * 获取所有可用的配置名称列表（预设 + 用户自定义）。
     *
     * @param context Android 上下文
     * @return 配置名称列表
     */
    public static ArrayList<String> getProfilesNames(Context context) {
        SharedPreferences pref =
                context.getSharedPreferences(KEYBOARD_PROFILES_SETTINGS, Context.MODE_PRIVATE);
        Set<String> prefNames = pref.getAll().keySet();
        ArrayList<String> names = new ArrayList<>();
        // 先添加预设名称
        for (String defName : DEFAULT_PROFILES_NAMES) {
            if (!prefNames.contains(defName))
                names.add(defName);
        }
        // 再添加用户自定义名称
        names.addAll(prefNames);
        return names;
    }

    /**
     * 判断指定名称是否为预设配置。
     *
     * @param name 配置名称
     * @return true 表示是预设配置
     */
    public static boolean isDefaultProfile(String name) {
        boolean defProf = false;
        for (String defName : KeyboardProfile.DEFAULT_PROFILES_NAMES) {
            if (defName.equals(name)) {
                defProf = true;
            }
        }
        return defProf;
    }

    /**
     * 恢复指定预设配置到默认状态。
     *
     * @param name    预设配置名称
     * @param context Android 上下文
     */
    public static void restoreDefaultProfile(String name, Context context) {
        KeyboardProfile prof = null;
        switch (name) {
            case "ps3":
                prof = createPS3Profile();
                break;
            case "default":
                prof = createDefaultProfile();
                break;
            case "wiimote":
                prof = createWiimoteProfile();
                break;
        }
        if (prof != null) {
            prof.save(context);
        } else {
            NLog.e(TAG, "Keyboard profile " + name + " is unknown!!");
        }
    }

    /**
     * 删除此配置。
     *
     * @param context Android 上下文
     * @return true 表示删除成功
     */
    public boolean delete(Context context) {
        NLog.i(TAG, "delete profile " + name);
        SharedPreferences pref =
                context.getSharedPreferences(name + ".keyprof", Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.clear();
        editor.apply();
        pref = context.getSharedPreferences(KEYBOARD_PROFILES_SETTINGS, Context.MODE_PRIVATE);
        editor = pref.edit();
        editor.remove(name);
        editor.apply();
        return true;
    }

    /**
     * 保存此配置到 SharedPreferences。
     *
     * @param context Android 上下文
     * @return true 表示保存成功
     */
    public boolean save(Context context) {
        SharedPreferences pref =
                context.getSharedPreferences(name + KEYBOARD_PROFILE_POSTFIX, Context.MODE_PRIVATE);
        NLog.i(TAG, "save profile " + name + " " + keyMap);
        Editor editor = pref.edit();
        editor.clear();

        // 遍历所有可映射按键，保存当前映射关系
        for (int i = 0; i < BUTTON_NAMES.length; i++) {
            int value = BUTTON_KEY_EVENT_CODES[i];
            int idx = keyMap.indexOfValue(value);
            int key = idx == -1 ? 0 : keyMap.keyAt(idx);

            if (key != 0) {
                NLog.i(TAG, "save " + BUTTON_NAMES[i] + " " + key + "->" + value);
                editor.putInt(key + "", value);
            }
        }

        editor.apply();
        // 非默认配置记录到配置注册表中
        if (!name.equals("default")) {
            pref = context.getSharedPreferences(KEYBOARD_PROFILES_SETTINGS, Context.MODE_PRIVATE);
            editor = pref.edit();
            editor.putBoolean(name, true);
            editor.remove("default");
            editor.apply();
        }

        return true;
    }

}
