package org.zeam;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import org.zeam.LauncherSettings;

public class ApplicationsGridItemInfo extends ItemInfo {
    Drawable icon;
    CharSequence title;

    ApplicationsGridItemInfo(Context context) {
        Resources resources = context.getResources();
        this.itemType = 6;
        this.icon = resources.getDrawable(R.drawable.applications_grid);
        this.title = resources.getString(R.string.group_applications);
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(this.itemType));
        values.put("container", Long.valueOf(this.container));
        values.put("screen", Integer.valueOf(this.screen));
        values.put("cellX", Integer.valueOf(this.cellX));
        values.put("cellY", Integer.valueOf(this.cellY));
    }
}
