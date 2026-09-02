package org.zmreborn;

import android.content.ContentValues;
import java.util.ArrayList;
import org.zmreborn.LauncherSettings;

/**
 * Data model for a user-created folder containing desktop or dock application items.
 */
class UserFolderInfo extends FolderInfo {
    final ArrayList<ApplicationItemInfo> contents = new ArrayList<>();

    UserFolderInfo() {
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER;
    }

    /**
     * Appends an application item to this folder's contents.
     */
    public void add(ApplicationItemInfo item) {
        if (item != null) {
            this.contents.add(item);
        }
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        String titleStr = this.title != null ? this.title.toString() : "";
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, titleStr);
    }

    @Override
    public String toString() {
        return this.title != null ? this.title.toString() : "";
    }
}
