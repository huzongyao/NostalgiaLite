package nostalgia.appgbc;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;

/**
 * GBC 模拟器游戏运行界面。
 * <p>提供 GBC 专用的 OpenGL 片段着色器（RGB 三通道分离纹理渲染），
 * 使用 512x512 纹理尺寸。</p>
 *
 * @author NostalgiaLite
 */
public class GbcEmulatorActivity extends EmulatorActivity {

    /** GBC 专用片段着色器：从三行 Alpha 纹理中分别提取 R/G/B 通道 */
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

    /** 获取 GBC 模拟器实例 */
    @Override
    public Emulator getEmulatorInstance() {
        return GbcEmulator.getInstance();
    }

    /** 获取 GBC 片段着色器 */
    @Override
    public String getFragmentShader() {
        return shader;
    }

    /** 获取 OpenGL 纹理尺寸（512x512） */
    @Override
    public int getGLTextureSize() {
        return 512;
    }

    /** GBC 不使用 OpenGL 调色板 */
    @Override
    public boolean hasGLPalette() {
        return false;
    }

}
