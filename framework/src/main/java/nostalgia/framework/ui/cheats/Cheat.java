package nostalgia.framework.ui.cheats;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import nostalgia.framework.utils.NLog;

public class Cheat {
    public static final String CHEAT_PREF_SUFFIX = ".cheats";
    String chars = "";
    String desc = "";
    boolean enable = false;

    public Cheat(String chars, String desc, boolean enable) {
        this.chars = chars;
        this.desc = desc;
        this.enable = enable;
    }


    public static ArrayList<Cheat> getAllCheats(Context context, String gameHash) {
        ArrayList<Cheat> result = new ArrayList<>();
        SharedPreferences pref =
                context.getSharedPreferences(gameHash + CHEAT_PREF_SUFFIX, Context.MODE_PRIVATE);
        @SuppressWarnings("unchecked")
        Map<String, String> all = (Map<String, String>) pref.getAll();
        for (Entry<String, String> item : all.entrySet()) {
            String[] pom = item.getValue().split("\\|");

            if (pom.length > 0) {
                Boolean enable = pom[0].equals("1");
                String desc = pom.length > 1 ? pom[1] : "";
                result.add(new Cheat(item.getKey(), desc, enable));
            }
        }

        return result;
    }

    public static int[] rawToValues(String raw) {
        String comp = null;
        String addr = raw.split(":")[0];
        String val = raw.split(":")[1];

        if (addr.contains("?")) {
            String[] segments = addr.split("\\?");
            addr = segments[0];
            comp = segments[1];
        }

        int iaddr = Integer.parseInt(addr, 16);
        int ival = Integer.parseInt(val, 16);
        int icomp = -1;

        if (comp != null) {
            icomp = Integer.parseInt(comp, 16);
        }

        NLog.i("cheat", "cheat " + valuesToRaw(iaddr, ival, icomp));
        return new int[]{iaddr, ival, icomp};
    }

    public static String valuesToRaw(int addr, int val, int comp) {
        String hexAddr = Integer.toHexString(addr);
        String hexVal = Integer.toHexString(val);
        hexAddr = "0000".substring(hexAddr.length()) + hexAddr;
        hexVal = "00".substring(hexVal.length()) + hexVal;
        String hexComp = null;

        if (comp != -1) {
            hexComp = Integer.toHexString(comp);
            hexComp = "00".substring(hexComp.length()) + hexComp;
        }
        return hexAddr + (hexComp != null ? "?" + hexComp + ":" : ":") + hexVal;
    }


    public static ArrayList<String> getAllEnableCheats(Context context, String gameHash) {
        ArrayList<Cheat> cheats = getAllCheats(context, gameHash);
        ArrayList<String> result = new ArrayList<>();
        for (Cheat cheat : cheats) {
            if (cheat.enable)
                result.add(cheat.chars);
        }
        return result;
    }


    public static void saveCheats(Context context, String gameHash, ArrayList<Cheat> items) {
        SharedPreferences pref =
                context.getSharedPreferences(gameHash + CHEAT_PREF_SUFFIX, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.clear();
        for (Cheat cheat : items) {
            if (!cheat.chars.equals("")) {
                editor.putString(cheat.chars, (cheat.enable ? "1" : "0") + "|" + cheat.desc + "|");
            }
        }
        editor.apply();
    }
}
