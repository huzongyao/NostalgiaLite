package nostalgia.appnes;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.preferences.PreferenceUtil;

/**
 * NES 模拟器游戏运行界面。
 * <p>提供 NES 专用的 OpenGL 片段着色器（调色板索引渲染），
 * 并在任务栈为空时按返回键返回游戏画廊。</p>
 *
 * @author NostalgiaLite
 */
public class NesEmulatorActivity extends EmulatorActivity {
    /** 标记当前是否为任务栈最后一个 Activity */
    private boolean isLastOfStack = false;
    /** 创建时检测任务栈状态 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isLastOfStack = checkLastStack();
    }
    /** 着色器 1：处理 NTSC 调色板索引（带偏移解码） */
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

    /** 着色器 2：处理 PAL 调色板索引（直接映射） */
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

    /** 获取 NES 模拟器实例 */
    @Override
    public Emulator getEmulatorInstance() {
        return NesEmulator.getInstance();
    }

    /**
     * 获取当前选中的 OpenGL 片段着色器。
     * <p>根据用户偏好选择调色板渲染着色器。</p>
     *
     * @return GLSL 片段着色器源码
     */
    @Override
    public String getFragmentShader() {
        int shaderIdx = PreferenceUtil.getFragmentShader(this);
        if (shaderIdx == 1) {
            return shader2;
        }
        return shader1;
    }
    
    /** 按返回键时，若为任务栈末尾则返回游戏画廊 */
        @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isLastOfStack) {
            Intent intent = new Intent(this, NesGalleryActivity.class);
            startActivity(intent);
        }
    }

    /** 检查当前 Activity 是否为任务栈中唯一的活动 */
    private boolean checkLastStack() {
        ActivityManager mngr = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);

        return taskList.get(0).numActivities == 1 &&
                taskList.get(0).topActivity.getClassName().equals(this.getClass().getName());
    }
}
