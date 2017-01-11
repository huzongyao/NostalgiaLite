package nostalgia.framework.base;

import android.content.Context;

public interface Migrator {

    void doExport(Context context, String targetDir);

    void doImport(Context context, String sourceDir);
}
