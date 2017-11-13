package nostalgia.framework.base;

import android.content.Context;

import java.util.HashMap;

import nostalgia.framework.Emulator;
import nostalgia.framework.GfxProfile;
import nostalgia.framework.ui.preferences.PreferenceUtil;

public class ViewUtils {
    private ViewUtils() {
    }

    public static ViewPort computeViewPort(Emulator emulator, int screenWidth,
                                           int screenHeight, int paddingLeft, int paddingTop) {
        GfxProfile gfx = null;

        if (emulator != null) {
            gfx = emulator.getActiveGfxProfile();

        } else {
            gfx = EmulatorHolder.getInfo().getDefaultGfxProfile();
        }

        return computeViewPort(gfx, screenWidth, screenHeight, paddingLeft,
                paddingTop);
    }

    public static ViewPort computeInitViewPort(Context context, int w, int h,
                                               int paddingLeft, int paddingTop) {
        GfxProfile gfx = EmulatorHolder.getInfo().getDefaultGfxProfile();
        return ViewUtils.computeViewPort(gfx, w, h, paddingLeft, paddingTop);
    }

    public static HashMap<String, ViewPort> computeAllInitViewPorts(
            Context context, int w, int h, int paddingLeft, int paddingTop) {
        HashMap<String, ViewPort> res = new HashMap<>();

        for (GfxProfile profile : EmulatorHolder.getInfo()
                .getAvailableGfxProfiles()) {
            ViewPort vp = computeViewPort(profile, w, h, paddingLeft,
                    paddingTop);
            res.put(profile.name, vp);
        }

        return res;
    }

    public static HashMap<String, ViewPort> loadOrComputeAllViewPorts(
            Context context, int w, int h, int paddingLeft, int paddingTop) {
        HashMap<String, ViewPort> res = computeAllInitViewPorts(context, w, h,
                paddingLeft, paddingTop);

        for (GfxProfile profile : EmulatorHolder.getInfo()
                .getAvailableGfxProfiles()) {
            ViewPort vp = loadViewPort(context, w, h, profile);

            if (vp != null) {
                res.put(profile.name, vp);
            }
        }

        return res;
    }

    public static ViewPort loadOrComputeViewPort(Context context, Emulator emulator,
                                                 int w, int h, int paddingLeft, int paddingTop,
                                                 boolean ignoreFullscreenSettings) {
        ViewPort vp = null;
        GfxProfile profile = null;

        if (emulator != null) {
            profile = emulator.getActiveGfxProfile();

        } else {
            profile = EmulatorHolder.getInfo().getDefaultGfxProfile();
        }

        if (!ignoreFullscreenSettings
                && PreferenceUtil.isFullScreenEnabled(context)) {
            vp = new ViewPort();
            vp.height = h;
            vp.width = w;
            vp.x = 0;
            vp.y = 0;

        } else if (loadViewPort(context, w, h, profile) != null) {
            vp = loadViewPort(context, w, h, profile);

        } else {
            vp = ViewUtils.computeViewPort(profile, w, h, paddingLeft,
                    paddingTop);
        }

        return vp;
    }

    private static ViewPort loadViewPort(Context context, int w, int h,
                                         GfxProfile profile) {
        ViewPort vp = PreferenceUtil.getViewPort(context, w, h);
        GfxProfile defaultProfile = EmulatorHolder.getInfo()
                .getDefaultGfxProfile();

        if (vp != null && profile != defaultProfile) {
            int vpw = vp.width;
            int vph = vp.height;
            int ow = vpw;
            int oh = vph;
            float ratio = (float) profile.originalScreenHeight
                    / profile.originalScreenWidth;

            if (w < h) {
                vpw = vp.width;
                vph = (int) (vpw * ratio);

            } else {
                vph = vp.height;
                vpw = (int) (vph / ratio);
                vp.x += (ow - vpw) / 2;
            }

            vp.width = vpw;
            vp.height = vph;
        }

        return vp;
    }

    public static ViewPort computeViewPort(GfxProfile gfx, int screenWidth,
                                           int screenHeight, int paddingLeft, int paddingTop) {
        if (gfx == null) {
            gfx = EmulatorHolder.getInfo().getDefaultGfxProfile();
        }

        int w = screenWidth - paddingLeft;
        int h = screenHeight - paddingTop;
        int vpw;
        int vph;
        float ratio = (float) gfx.originalScreenHeight
                / gfx.originalScreenWidth;

        if (w < h) {
            vpw = w;
            vph = (int) (vpw * ratio);

        } else {
            vph = h;
            vpw = (int) (vph / ratio);
        }

        ViewPort result = new ViewPort();
        result.x = (w - vpw) / 2 + paddingLeft;
        result.y = paddingTop;
        result.height = vph;
        result.width = vpw;
        return result;
    }

}
