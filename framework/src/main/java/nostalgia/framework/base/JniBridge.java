package nostalgia.framework.base;

import android.graphics.Bitmap;

/**
 * JNI 桥接类，声明所有与本地 C/C++ 模拟器内核交互的本地方法。
 * <p>
 * 每个平台（NES、GBC、GG）提供对应的本地库实现，
 * 通过 {@link JniEmulator} 间接调用这些方法。
 * </p>
 */
public class JniBridge {

    /** 设置模拟器数据文件的基础目录。 */
    public native boolean setBaseDir(String path);

    /** 初始化模拟器，传入图像、声音和通用设置的编码值。 */
    public native boolean start(int gfx, int sfx, int general);

    /** 重置模拟器。 */
    public native boolean reset();

    /** 加载游戏 ROM。 */
    public native boolean loadGame(String fileName, String batteryDir, String strippedName);

    /** 从指定槽位加载存档状态。 */
    public native boolean loadState(String fileName, int slot);

    /** 将当前状态保存到指定槽位。 */
    public native boolean saveState(String fileName, int slot);

    /** 读取声音缓冲区数据，返回读取的长度。 */
    public native int readSfxBuffer(short[] data);

    /** 启用金手指密码。 */
    public native boolean enableCheat(String gg, int type);

    /** 启用原始内存金手指。 */
    public native boolean enableRawCheat(int addr, int val, int comp);

    /** 发射光枪，传入模拟器坐标。 */
    public native boolean fireZapper(int x, int y);

    /** 将当前帧渲染到 Bitmap。 */
    public native boolean render(Bitmap bitmap);

    /** 将当前帧渲染到指定视口大小的 Bitmap。 */
    public native boolean renderVP(Bitmap bitmap, int vw, int vh);

    /** 渲染历史截图到 Bitmap。 */
    public native boolean renderHistory(Bitmap bitmap, int item, int vw, int vh);

    /** 使用 OpenGL 渲染当前帧。 */
    public native boolean renderGL();

    /** 模拟一帧，传入按键状态、连发状态和跳帧数。 */
    public native boolean emulate(int keys, int turbos, int numFramesToSkip);

    /** 读取当前调色板数据。 */
    public native boolean readPalette(int[] result);

    /** 设置视口大小。 */
    public native boolean setViewPortSize(int w, int h);

    /** 停止模拟器并释放本地资源。 */
    public native boolean stop();

    /** 获取时间旅行历史项目数量。 */
    public native int getHistoryItemCount();

    /** 加载指定位置的历史状态。 */
    public native boolean loadHistoryState(int pos);

}
