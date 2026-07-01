package nostalgia.appgg;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;

/**
 * Game Gear 游戏运行界面 Activity。
 * <p>继承自 {@link EmulatorActivity}，负责 GG 游戏的实时渲染。
 * 使用 OpenGL ES 片段着色器将 RGB 三通道分离纹理合成显示。</p>
 */
public class GGEmulatorActivity extends EmulatorActivity {

    /**
     * GLSL 片段着色器。
     * <p>将纹理中的 Alpha 通道分离为 R、G、B 三个通道：
     * 第一行读取红色通道，第二行读取绿色通道，第三行读取蓝色通道。</p>
     */
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
        return GGEmulator.getInstance();
    }

    @Override
    public String getFragmentShader() {
        return shader;
    }

    /**
     * 获取 OpenGL 纹理尺寸。
     * @return 纹理尺寸 512
     */
    @Override
    public int getGLTextureSize() {
        return 512;
    }

    /**
     * 是否使用调色板渲染。
     * @return 始终返回 false，GG 使用 RGB 分离渲染而非调色板
     */
    public boolean hasGLPalette() {
        return false;
    }
}
