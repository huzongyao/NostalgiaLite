package nostalgia.framework.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.Display;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import nostalgia.framework.base.EmulatorUtils;
import nostalgia.framework.base.SlotUtils;
import nostalgia.framework.ui.gamegallery.GameDescription;

/**
 * 模拟器通用工具类。
 * <p>提供文件哈希计算、ZIP 解压、OpenGL ES 2.0 支持检测、
 * 设备类型判断、网络状态查询、屏幕截图生成等通用工具方法。</p>
 *
 * @author NostalgiaLite
 */
public class EmuUtils {

    private static final String TAG = "utils.EmuUtils";
    /** MD5 计算时的最大读取字节数 */
    private static final int MD5_BYTES_COUNT = 10240;
    /** 复用的屏幕尺寸测量点 */
    private static Point size = new Point();

    /** 私有构造，禁止实例化 */
    private EmuUtils() {
    }

    /**
     * 去除文件扩展名。
     *
     * @param str 文件名
     * @return 不含扩展名的文件名
     */
    public static String stripExtension(String str) {
        if (str == null)
            return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1)
            return str;
        return str.substring(0, pos);
    }

    /**
     * 计算文件的 MD5 校验和。
     *
     * @param file 要计算的文件
     * @return MD5 十六进制字符串，失败返回空字符串
     */
    public static String getMD5Checksum(File file) {
        try (InputStream fis = new FileInputStream(file)) {
            return countMD5(fis);
        } catch (IOException e) {
            NLog.e(TAG, "", e);
        }
        return "";
    }

    /**
     * 计算输入流的 MD5 校验和。
     *
     * @param zis 输入流
     * @return MD5 十六进制字符串
     * @throws IOException 读取失败时抛出
     */
    public static String getMD5Checksum(InputStream zis) throws IOException {
        return countMD5(zis);
    }

    /**
     * 计算 Uri 指向内容的 MD5 校验和。
     *
     * @param context 上下文
     * @param uri     内容 Uri
     * @return MD5 十六进制字符串，失败返回空字符串
     */
    public static String getMD5Checksum(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            return countMD5(is);
        } catch (IOException e) {
            NLog.e(TAG, "", e);
        }
        return "";
    }
    
    /**
     * 将 Uri 指向的内容复制到目标文件。
     *
     * @param context     上下文
     * @param uri         源内容 Uri
     * @param destination 目标文件
     * @return 复制成功返回目标文件，失败返回 null
     */
    public static File copyUriToFile(Context context, Uri uri, File destination) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            
            try (FileOutputStream os = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return destination;
        } catch (IOException e) {
            NLog.e(TAG, "Failed to copy file", e);
            if (destination.exists()) {
                destination.delete();
            }
            return null;
        }
    }

    /**
     * 计算输入流的 MD5 校验和（内部实现）。
     * <p>仅读取前 {@value #MD5_BYTES_COUNT} 字节进行计算，
     * 若文件小于该阈值则返回 "small file"。</p>
     *
     * @param is 输入流
     * @return MD5 十六进制字符串
     */
    private static String countMD5(InputStream is) {
        if (is == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[MD5_BYTES_COUNT];
            int readCount;
            int totalCount = 0;
            int updateCount = 0;
            while ((readCount = is.read(buffer)) != -1) {
                updateCount = readCount;
                if ((totalCount + readCount) > MD5_BYTES_COUNT) {
                    updateCount = MD5_BYTES_COUNT - totalCount;
                }
                md.update(buffer, 0, updateCount);
                totalCount += updateCount;
                if (totalCount >= MD5_BYTES_COUNT)
                    break;
            }
            if (totalCount >= MD5_BYTES_COUNT) {
                byte[] digest = md.digest();
                StringBuilder result = new StringBuilder(digest.length * 2);
                for (byte aDigest : digest) {
                    result.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
                }
                return result.toString();
            } else {
                return "small file";
            }
        } catch (NoSuchAlgorithmException e) {
            NLog.e(TAG, "", e);
        } catch (IOException e) {
            NLog.e(TAG, "", e);
        }

        return "";
    }

    /**
     * 获取 ZIP 文件中指定条目的 CRC32 校验值。
     *
     * @param dir   ZIP 文件路径
     * @param entry 条目名称
     * @return CRC32 值，失败返回 -1
     */
    public static long getCrc(String dir, String entry) {
        try (ZipFile zf = new ZipFile(dir)) {
            ZipEntry ze = zf.getEntry(entry);
            return ze.getCrc();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 检查设备是否支持 OpenGL ES 2.0。
     *
     * @param context 上下文
     * @return 支持返回 true
     */
    public static boolean checkGL20Support(Context context) {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        egl.eglInitialize(display, version);
        int EGL_OPENGL_ES2_BIT = 4;
        int[] configAttribs = {
                EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[10];
        int[] num_config = new int[1];
        egl.eglChooseConfig(display, configAttribs, configs, 10, num_config);
        egl.eglTerminate(display);
        return num_config[0] > 0;
    }

    /**
     * 从 ZIP 文件中解压指定条目到目标文件。
     *
     * @param zipFile    源 ZIP 文件
     * @param entryName  条目名称
     * @param outputFile 输出文件
     * @throws IOException 解压失败时抛出
     */
    public static void extractFile(File zipFile, String entryName, File outputFile)
            throws IOException {
        NLog.i(TAG, "extract " + entryName + " from " + zipFile.getAbsolutePath() + " to "
                + outputFile.getAbsolutePath());
        try (ZipFile zipFile2 = new ZipFile(zipFile)) {
            ZipEntry ze = zipFile2.getEntry(entryName);
            if (ze == null) {
                throw new IOException("ZIP entry not found: " + entryName);
            }

            try (InputStream zis = zipFile2.getInputStream(ze);
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[20480];
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
            }
        }
    }

    /** 去除文件扩展名（与 {@link #stripExtension} 功能相同） */
    public static String removeExt(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            return fileName.substring(0, idx);
        } else {
            return fileName;
        }
    }

    /** 获取文件扩展名（不含点号） */
    public static String getExt(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            return fileName.substring(idx + 1);
        } else {
            return "";
        }
    }

    /** 根据设备特性判断设备类型（手机/平板/电视） */
    public static ServerType getDeviceType(Context context) {
        if (context.getPackageManager().hasSystemFeature("android.hardware.telephony")) {
            return ServerType.mobile;
        } else if (context.getPackageManager().hasSystemFeature("android.hardware.touchscreen")) {
            return ServerType.tablet;
        } else {
            return ServerType.tv;
        }
    }

    /** 获取屏幕显示宽度（像素） */
    public static int getDisplayWidth(Display display) {
        display.getSize(size);
        return size.x;
    }

    /** 获取屏幕显示高度（像素） */
    public static int getDisplayHeight(Display display) {
        display.getSize(size);
        return size.y;
    }

    /** 检查应用是否为可调试模式 */
    public static boolean isDebuggable(Context ctx) {
        boolean debuggable = false;
        PackageManager pm = ctx.getPackageManager();
        try {
            ApplicationInfo appinfo = pm.getApplicationInfo(ctx.getPackageName(), 0);
            debuggable = (0 != (appinfo.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (NameNotFoundException ignored) {
        }
        return debuggable;
    }

    /** 检查网络是否可用 */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /** 检查 WiFi 是否已连接 */
    public static boolean isWifiAvailable(Context context) {
        WifiManager manager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        return (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) & (wifiInfo.getIpAddress() != 0);
    }

    /** 获取局域网广播地址 */
    public static InetAddress getBroadcastAddress(Context context) {
        String ip = getNetPrefix(context) + ".255";
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** 获取本机 IP 信息（内部实现） */
    private static IpInfo getIP() {
        try {
            IpInfo result = new IpInfo();
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        int prefixLen = Integer.MAX_VALUE;
                        for (InterfaceAddress address : intf.getInterfaceAddresses()) {
                            if (address.getNetworkPrefixLength() < prefixLen) {
                                prefixLen = address.getNetworkPrefixLength();
                            }
                        }
                        String sAddr = addr.getHostAddress().toUpperCase();
                        byte[] ip = addr.getAddress();
                        int iAddr = ((int) ip[0] << (24)) & 0xFF000000
                                | ((int) ip[1] << (16)) & 0x00FF0000
                                | ((int) ip[2] << (8)) & 0x0000FF00
                                | ((int) ip[3] << (0)) & 0x000000FF;
                        boolean isIPv4 = addr instanceof Inet4Address;
                        if (isIPv4) {
                            result.sAddress = sAddr;
                            result.address = iAddr;
                            result.setPrefixLen(prefixLen);
                            return result;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new IpInfo();
    }

    /** 获取本机所在网段前缀（如 "192.168.1"） */
    public static String getNetPrefix(Context context) {
        IpInfo info = getIP();
        int prefix = info.address & info.netmask;
        return ((prefix >> 24) & 0xff) + "." + ((prefix >> 16) & 0xff) + "." + ((prefix >> 8) & 0xff);
    }

    /** 获取本机 IP 地址字符串 */
    public static String getIpAddr(Context context) {
        return getIP().sAddress;
    }

    /**
     * 为游戏创建截图缩略图 Bitmap（2 倍放大，无抗锯齿）。
     *
     * @param context 上下文
     * @param game    游戏描述
     * @return 放大后的截图 Bitmap
     */
    public static Bitmap createScreenshotBitmap(Context context, GameDescription game) {
        String path = SlotUtils.getScreenshotPath(EmulatorUtils.getBaseDir(context), game.checksum, 0);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap == null) {
            return null;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int newW = w * 2;
        int newH = h * 2;
        Rect from = new Rect(0, 0, w, h);
        Rect to = new Rect(0, 0, newW, newH);
        Bitmap largeBitmap = Bitmap.createBitmap(bitmap.getWidth() * 2,
                bitmap.getHeight() * 2, Config.ARGB_8888);
        Canvas c = new Canvas(largeBitmap);
        Paint p = new Paint();
        p.setDither(false);
        p.setFilterBitmap(false);
        c.drawBitmap(bitmap, from, to, p);
        bitmap.recycle();
        return largeBitmap;
    }

    /** 检查指定 Action 的 Intent 是否有应用可处理 */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /** 设备类型枚举：手机、平板、电视 */
    public enum ServerType {
        mobile, tablet, tv
    }

    /** IP 地址信息封装类 */
    public static class IpInfo {
        public String sAddress;
        public int address;
        public int netmask;

        public void setPrefixLen(int len) {
            netmask = 0;
            int n = 31;
            for (int i = 0; i < len; i++) {
                netmask |= 1 << (n);
                n--;
            }
            NLog.e("netmask", len + "");
            NLog.e("netmask", netmask + "");
        }
    }
}
