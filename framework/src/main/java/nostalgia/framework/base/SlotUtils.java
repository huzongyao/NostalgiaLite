package nostalgia.framework.base;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nostalgia.framework.SlotInfo;
import nostalgia.framework.utils.NLog;

public class SlotUtils {
    public static final int NUM_SLOTS = 8;
    private static final String SLOT_SUFFIX = ".state";
    private static final String SCREENSHOT_SUFFIX = ".png";
    private static final String TAG = "base.SlotUtils";

    private SlotUtils() {
    }

    public static boolean autoSaveExists(String baseDir, String md5) {
        String path = getSlotPath(baseDir, md5, 0);
        return new File(path).exists();
    }

    public static SlotInfo getAutoSaveSlot(String baseDir, String md5) {
        return getSlot(baseDir, md5, 0);
    }

    public static List<SlotInfo> getSlots(String baseDir, String md5) {
        ArrayList<SlotInfo> result = new ArrayList<>();

        for (int i = 1; i < (NUM_SLOTS + 1); i++) {
            SlotInfo slot = getSlot(baseDir, md5, i);
            result.add(slot);
        }

        return result;
    }

    public static String getSlotPath(String baseDir, String md5, int slot) {
        return getGameDataFilePrefix(baseDir, md5) + slot + SLOT_SUFFIX;
    }

    public static String getScreenshotPath(String baseDir, String md5, int slot) {
        return getGameDataFilePrefix(baseDir, md5) + slot + SCREENSHOT_SUFFIX;
    }

    public static String getGameDataFilePrefix(String baseDir, String md5) {
        return baseDir + "/" + md5 + ".";
    }

    public static SlotInfo getSlot(String baseDir, String md5, int idx) {
        SlotInfo slot = new SlotInfo();
        String prefix = baseDir + "/" + md5 + ".";
        File file = new File(prefix + idx + SLOT_SUFFIX);
        slot.isUsed = file.exists();
        slot.lastModified = slot.isUsed ? file.lastModified() : -1;
        slot.path = file.getAbsolutePath();
        slot.id = idx;

        if (slot.isUsed) {
            File screenShot = new File(prefix + idx + SCREENSHOT_SUFFIX);

            if (screenShot.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(screenShot
                            .getAbsolutePath());

                    if (bitmap != null) {
                        Bitmap newScreenshot = Bitmap.createBitmap(
                                bitmap.getWidth(), bitmap.getHeight(),
                                bitmap.getConfig());
                        Canvas c = new Canvas(newScreenshot);
                        float[] matrix = new float[]{
                                0.299f, 0.587f, 0.114f, 0, 0,
                                0.299f, 0.587f, 0.114f, 0, 0,
                                0.299f, 0.587f, 0.114f, 0, 0,
                                0, 0, 0, 0.5f, 0
                        };
                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        paint.setFilterBitmap(true);
                        paint.setColorFilter(new ColorMatrixColorFilter(
                                new ColorMatrix(matrix)));
                        c.drawBitmap(bitmap, 0, 0, paint);
                        bitmap.recycle();
                        slot.screenShot = newScreenshot;
                    }

                } catch (OutOfMemoryError e) {
                    NLog.e(TAG, "", e);
                }
            }
        }

        return slot;
    }
}