package nostalgia.framework.base;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import nostalgia.framework.R;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;

/**
 * 截图保存工具类。
 * <p>负责将模拟器当前画面截取为 Bitmap，并通过 MediaStore API
 * 保存到系统图库的 Pictures 目录下。</p>
 */
public final class ScreenshotHelper {

    private static final String TAG = "ScreenshotHelper";

    private ScreenshotHelper() {
    }

    /**
     * 保存游戏截图到系统图库。
     * <p>截图以 PNG 格式保存到 Pictures/{模拟器名称}/ 目录。</p>
     *
     * @param activity 当前 Activity（用于获取 ContentResolver 和显示 Toast）
     * @param game     当前游戏描述
     */
    public static void saveScreenshot(Activity activity, GameDescription game) {
        String name = game.getCleanName() + "-screenshot";
        String emulatorName = EmulatorHolder.getInfo().getName().replace(' ', '_');

        Bitmap bitmap = EmuUtils.createScreenshotBitmap(activity, game);
        if (bitmap == null) {
            Toast.makeText(activity,
                    activity.getString(R.string.act_game_screenshot_failed), Toast.LENGTH_LONG).show();
            return;
        }

        ContentResolver resolver = activity.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".png");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                "Pictures/" + emulatorName);

        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (imageUri == null) {
            bitmap.recycle();
            Toast.makeText(activity,
                    activity.getString(R.string.act_game_screenshot_failed), Toast.LENGTH_LONG).show();
            return;
        }

        try (OutputStream fos = resolver.openOutputStream(imageUri)) {
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                Toast.makeText(activity,
                        activity.getString(R.string.act_game_screenshot_saved), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            NLog.e(TAG, "保存截图失败", e);
            resolver.delete(imageUri, null, null);
            Toast.makeText(activity,
                    activity.getString(R.string.act_game_screenshot_failed), Toast.LENGTH_LONG).show();
        } finally {
            bitmap.recycle();
        }
    }
}
