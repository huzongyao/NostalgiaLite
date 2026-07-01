package nostalgia.appnes;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.Emulator;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.base.OpenGLTestActivity;
import nostalgia.framework.ui.gamegallery.GalleryActivity;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;

/**
 * NES 游戏画廊界面。
 * <p>提供 NES ROM 文件浏览和启动功能，
 * 支持 .nes 和 .fds 格式。首次启动时检测 OpenGL ES 2.0 支持
 * 并引导用户选择渲染着色器。</p>
 *
 * @author NostalgiaLite
 */
public class NesGalleryActivity extends GalleryActivity {

    /** OpenGL 检测请求码 */
    private static final int REQUEST_CHECK_OPENGL = 200;

    /** 获取 NES 模拟器实例 */
    @Override
    public Emulator getEmulatorInstance() {
        return NesEmulator.getInstance();
    }

    /** 获取 NES 模拟器运行界面类 */
    @Override
    public Class<? extends EmulatorActivity> getEmulatorActivityClass() {
        return NesEmulatorActivity.class;
    }

    /** 获取 NES 支持的 ROM 文件扩展名集合（.nes, .fds） */
    @Override
    protected Set<String> getRomExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("nes");
        set.add("fds");
        return set;
    }


    /** 创建时检测 OpenGL ES 2.0 支持，必要时引导用户选择着色器 */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferenceUtil.getFragmentShader(this) == -1
                && EmuUtils.checkGL20Support(this)) {
            Intent intent = new Intent(this, OpenGLTestActivity.class);
            startActivityForResult(intent, REQUEST_CHECK_OPENGL);
        }
    }

    /** 处理 OpenGL 检测结果回调 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_OPENGL) {
            NLog.e("opengl", "opengl: " + resultCode);
            PreferenceUtil.setFragmentShader(this, resultCode);
        }
    }
}
