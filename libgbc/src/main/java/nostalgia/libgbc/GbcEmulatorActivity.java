package nostalgia.libgbc;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;

public class GbcEmulatorActivity extends EmulatorActivity {

    private static String shader = "precision mediump float;"
            + "varying vec2 v_texCoord;                     "
            + "uniform sampler2D s_texture;                 "


            + "void main()                                  "
            + "{                                             "
            + "	vec2 p1 = v_texCoord;                        "
            + "	vec2 p2 = vec2(p1.s, p1.t + 0.28125 + 0.05078125);        "
            + "	vec2 p3 = vec2(p2.s, p2.t + 0.28125 + 0.05078125);        "
            + "	float r = texture2D(s_texture, p1).a;        "
            + "	float g = texture2D(s_texture, p2).a;        "
            + "	float b = texture2D(s_texture, p3).a;        "
            + "	vec4 res = vec4(r, g, b, 1.0);             "
            + "	gl_FragColor.rgba = res;                   "
            + "}                                           ";

    @Override
    public Emulator getEmulatorInstance() {
        return GbcEmulator.getInstance();
    }

    @Override
    public String getFragmentShader() {
        return shader;
    }

    @Override
    public int getGLTextureSize() {
        return 512;
    }

    @Override
    public boolean hasGLPalette() {
        return false;
    }

}
