package org.zmreborn;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import org.zmreborn.LauncherSettings;

/**
 * Data model for a Live Folder querying content provider items dynamically.
 */
class LiveFolderInfo extends FolderInfo {
    Intent baseIntent;
    int displayMode;
    boolean filtered;
    Drawable icon;
    Intent.ShortcutIconResource iconResource;
    Uri uri;

    LiveFolderInfo() {
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER;
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        String titleStr = this.title != null ? this.title.toString() : "";
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, titleStr);
        if (this.uri != null) {
            values.put("uri", this.uri.toString());
        }
        if (this.baseIntent != null) {
            values.put(LauncherSettings.BaseLauncherColumns.INTENT, this.baseIntent.toUri(0));
        }
        values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, LauncherSettings.Favorites.ICON_TYPE_RESOURCE);
        values.put("displayMode", this.displayMode);
        if (this.iconResource != null) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, this.iconResource.packageName);
            values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, this.iconResource.resourceName);
        }
    }

    @Override
    public String toString() {
        return this.title != null ? this.title.toString() : "";
    }
}
