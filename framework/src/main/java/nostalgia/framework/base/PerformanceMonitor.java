package nostalgia.framework.base;

import android.content.Context;

import nostalgia.framework.ui.preferences.PreferenceUtil;

/**
 * 性能监控器。
 * <p>管理 OpenGL 和模拟器基准测试的回调逻辑，
 * 根据测试结果自动调整模拟质量设置。</p>
 */
public class PerformanceMonitor implements Benchmark.BenchmarkCallback {

    /** OpenGL 基准测试名称 */
    public static final String OPEN_GL_BENCHMARK = "openGL";
    /** 模拟器基准测试名称 */
    public static final String EMULATION_BENCHMARK = "emulation";

    /** 帧时间阈值（毫秒），低于此值视为性能达标 */
    private static final float FRAME_TIME_THRESHOLD = 17f;
    /** 高质量等级 */
    private static final int HIGH_QUALITY = 2;

    private final Context context;
    private final EmulatorView emulatorView;
    private int numTests = 0;
    private int numOk = 0;

    /**
     * 构造性能监控器。
     *
     * @param context      Android 上下文
     * @param emulatorView 模拟器视图，用于调整质量等级
     */
    public PerformanceMonitor(Context context, EmulatorView emulatorView) {
        this.context = context;
        this.emulatorView = emulatorView;
    }

    @Override
    public void onBenchmarkReset(Benchmark benchmark) {
    }

    @Override
    public void onBenchmarkEnded(Benchmark benchmark, int steps, long totalTime) {
        float millisPerFrame = totalTime / (float) steps;
        numTests++;
        if (benchmark.getName().equals(OPEN_GL_BENCHMARK)) {
            if (millisPerFrame < FRAME_TIME_THRESHOLD) {
                numOk++;
            }
        }
        if (benchmark.getName().equals(EMULATION_BENCHMARK)) {
            if (millisPerFrame < FRAME_TIME_THRESHOLD) {
                numOk++;
            }
        }
        if (numTests == 2) {
            PreferenceUtil.setBenchmarked(context, true);
            if (numOk == 2) {
                emulatorView.setQuality(HIGH_QUALITY);
                PreferenceUtil.setEmulationQuality(context, HIGH_QUALITY);
            }
        }
    }

    /**
     * 创建 OpenGL 基准测试实例。
     *
     * @return 配置好的 OpenGL 基准测试
     */
    public Benchmark createOpenGLBenchmark() {
        return new Benchmark(OPEN_GL_BENCHMARK, 200, this);
    }

    /**
     * 创建模拟器基准测试实例。
     *
     * @return 配置好的模拟器基准测试
     */
    public Benchmark createEmulationBenchmark() {
        return new Benchmark(EMULATION_BENCHMARK, 1000, this);
    }
}
