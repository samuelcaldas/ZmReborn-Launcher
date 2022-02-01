package org.zeam;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import org.zeam.LauncherSettings;

class LiveFolderInfo extends FolderInfo {
    Intent baseIntent;
    int displayMode;
    boolean filtered;
    Drawable icon;
    Intent.ShortcutIconResource iconResource;
    Uri uri;

    LiveFolderInfo() {
        this.itemType = 3;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, this.title.toString());
        values.put("uri", this.uri.toString());
        if (this.baseIntent != null) {
            values.put(LauncherSettings.BaseLauncherColumns.INTENT, this.baseIntent.toUri(0));
        }
        values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, 0);
        values.put("displayMode", Integer.valueOf(this.displayMode));
        if (this.iconResource != null) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, this.iconResource.packageName);
            values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, this.iconResource.resourceName);
        }
    }

    public String toString() {
        return this.title.toString();
    }
}
