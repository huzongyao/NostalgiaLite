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

public class EmuUtils {

    private static final String TAG = "utils.EmuUtils";
    private static final int MD5_BYTES_COUNT = 10240;
    private static Point size = new Point();

    private EmuUtils() {
    }

    public static String stripExtension(String str) {
        if (str == null)
            return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1)
            return str;
        return str.substring(0, pos);
    }

    public static String getMD5Checksum(File file) {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return countMD5(fis);
        } catch (IOException e) {
            NLog.e(TAG, "", e);
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ignored) {
            }
        }
        return "";
    }

    public static String getMD5Checksum(InputStream zis) throws IOException {
        return countMD5(zis);
    }

    private static String countMD5(InputStream is) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[MD5_BYTES_COUNT];
            int readCount = 0;
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
                String result = "";
                for (byte aDigest : digest) {
                    result += Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1);
                }
                return result;
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

    public static long getCrc(String dir, String entry) {
        try {
            ZipFile zf = new ZipFile(dir);
            ZipEntry ze = zf.getEntry(entry);
            return ze.getCrc();
        } catch (Exception e) {
            return -1;
        }
    }

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

    public static void extractFile(File zipFile, String entryName, File outputFile)
            throws IOException {
        NLog.i(TAG, "extract " + entryName + " from " + zipFile.getAbsolutePath() + " to "
                + outputFile.getAbsolutePath());
        ZipFile zipFile2 = new ZipFile(zipFile);
        ZipEntry ze = zipFile2.getEntry(entryName);
        if (ze != null) {
            InputStream zis = zipFile2.getInputStream(ze);
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buffer = new byte[20480];
            int count;
            while ((count = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }
            zis.close();
            zipFile2.close();
            fos.close();
        }
    }

    public static String removeExt(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            return fileName.substring(0, idx);
        } else {
            return fileName;
        }
    }

    public static String getExt(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            return fileName.substring(idx + 1);
        } else {
            return "";
        }
    }

    public static ServerType getDeviceType(Context context) {
        if (context.getPackageManager().hasSystemFeature("android.hardware.telephony")) {
            return ServerType.mobile;
        } else if (context.getPackageManager().hasSystemFeature("android.hardware.touchscreen")) {
            return ServerType.tablet;
        } else {
            return ServerType.tv;
        }
    }

    public static int getDisplayWidth(Display display) {
        display.getSize(size);
        return size.x;
    }

    public static int getDisplayHeight(Display display) {
        display.getSize(size);
        return size.y;
    }

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

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isWifiAvailable(Context context) {
        WifiManager manager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        return (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) & (wifiInfo.getIpAddress() != 0);
    }

    public static InetAddress getBroadcastAddress(Context context) {
        String ip = getNetPrefix(context) + ".255";
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

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

    public static String getNetPrefix(Context context) {
        IpInfo info = getIP();
        int prefix = info.address & info.netmask;
        return ((prefix >> 24) & 0xff) + "." + ((prefix >> 16) & 0xff) + "." + ((prefix >> 8) & 0xff);
    }

    public static String getIpAddr(Context context) {
        return getIP().sAddress;
    }

    public static Bitmap createScreenshotBitmap(Context context, GameDescription game) {
        String path = SlotUtils.getScreenshotPath(EmulatorUtils.getBaseDir(context), game.checksum, 0);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
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

    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public enum ServerType {
        mobile, tablet, tv
    }

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
