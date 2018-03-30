package nostalgia.framework.ui.gamegallery;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;

import java.util.HashSet;

import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.EmuUtils;

public class FilePathWacher extends FileObserver {
    private static final String TAG = "ui.gamegallery.FilePathWacher";
    private static int flags = 0;

    static {
        flags = FileObserver.ALL_EVENTS;
    }

    private HashSet<String> exts = new HashSet<>();
    private OnSDCardChangeListener listener;

    public FilePathWacher(Context context, HashSet<String> exts, OnSDCardChangeListener listener) {
        super(Environment.getExternalStorageDirectory().getAbsolutePath(), flags);
        NLog.i(TAG, "create watcher " + Environment.getExternalStorageDirectory().getAbsolutePath()
                + " " + Integer.toBinaryString(flags));
        this.exts = exts;
        this.listener = listener;
    }

    @Override
    public void startWatching() {
        NLog.i(TAG, "start");
        super.startWatching();
    }

    @Override
    public void stopWatching() {
        NLog.i(TAG, "stop");
        super.stopWatching();
    }

    @Override
    public void onEvent(int event, String path) {
        NLog.i(TAG, Integer.toBinaryString(event) + " " + path);

        if (path != null) {
            String ext = EmuUtils.getExt(path);

            if (exts.contains(ext)) {
                NLog.i(TAG, "SD card filesystem change");
                listener.onSDCardChange();
            }
        }
    }

    public interface OnSDCardChangeListener {
        void onSDCardChange();
    }

}
