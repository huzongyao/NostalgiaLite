package nostalgia.framework.base;

/**
 * 性能基准测试工具，用于测量模拟器和 OpenGL 渲染的帧率性能。
 * <p>
 * 在指定数量的帧后触发回调，根据测量结果自动调整模拟质量。
 * </p>
 */
public class Benchmark {
    private boolean isRunning = true;
    private BenchmarkCallback callback;
    private long startTime = -1;
    private long totalTime = 0;
    private int steps = 0;
    private int numSteps = 0;
    private String name;

    public Benchmark(String name, int numSteps, BenchmarkCallback callback) {
        this.name = name;
        this.callback = callback;
        this.numSteps = numSteps;
    }

    public void reset() {
        startTime = -1;
        steps = 0;
        totalTime = 0;
        callback.onBenchmarkReset(this);
    }

    public void notifyFrameStart() {
        if (!isRunning) {
            return;
        }

        startTime = System.currentTimeMillis();
        steps++;
    }

    public void notifyFrameEnd() {
        if (!isRunning) {
            return;
        }
        if (startTime != -1) {
            totalTime += System.currentTimeMillis() - startTime;
        }
        if (steps == numSteps) {
            callback.onBenchmarkEnded(this, steps, totalTime);
            stop();
        }
    }

    public void stop() {
        isRunning = false;
    }

    public String getName() {
        return name;
    }

    /** 基准测试完成时的回调接口。 */
    public interface BenchmarkCallback {
        void onBenchmarkReset(Benchmark benchmark);

        void onBenchmarkEnded(Benchmark benchmark, int steps, long totalTime);
    }
}
