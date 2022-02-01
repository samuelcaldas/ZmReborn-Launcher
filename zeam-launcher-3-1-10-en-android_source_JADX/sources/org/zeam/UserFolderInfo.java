package org.zeam;

import android.content.ContentValues;
import java.util.ArrayList;
import org.zeam.LauncherSettings;

class UserFolderInfo extends FolderInfo {
    ArrayList<ApplicationItemInfo> contents = new ArrayList<>();

    UserFolderInfo() {
        this.itemType = 2;
    }

    public void add(ApplicationItemInfo item) {
        this.contents.add(item);
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, this.title.toString());
    }

    public String toString() {
        return this.title.toString();
    }
}
