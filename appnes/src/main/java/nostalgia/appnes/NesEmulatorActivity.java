package nostalgia.appnes;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.preferences.PreferenceUtil;

public class NesEmulatorActivity extends EmulatorActivity {

    String shader1 = "precision mediump float;"
            + "varying vec2 v_texCoord;"
            + "uniform sampler2D s_texture;"
            + "uniform sampler2D s_palette; "
            + "void main()"
            + "{           "
            + "		 float a = texture2D(s_texture, v_texCoord).a;"
            + "	     float c = floor((a * 256.0) / 127.5);"
            + "      float x = a - c * 0.001953;"
            + "      vec2 curPt = vec2(x, 0);"
            + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;"
            + "}";

    String shader2 = "precision mediump float;"
            + "varying vec2 v_texCoord;"
            + "uniform sampler2D s_texture;"
            + "uniform sampler2D s_palette; "
            + "void main()"
            + "{"
            + "		 float a = texture2D(s_texture, v_texCoord).a;"
            + "		 float x = a;	"
            + "		 vec2 curPt = vec2(x, 0);"
            + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;"
            + "}";

    @Override
    public Emulator getEmulatorInstance() {
        return NesEmulator.getInstance();
    }

    @Override
    public String getFragmentShader() {
        int shaderIdx = PreferenceUtil.getFragmentShader(this);
        if (shaderIdx == 1) {
            return shader2;
        }
        return shader1;
    }
}
