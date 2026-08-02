package org.zmreborn;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import org.zmreborn.LauncherSettings;

public class ApplicationsGridItemInfo extends ItemInfo {
    CharSequence title;

    ApplicationsGridItemInfo(Context context) {
        this.itemType = 6;
        this.title = context.getResources().getString(R.string.group_applications);
    }

    /**
     * Resolves the drawer-open icon against {@code context}'s current resources so the icon
     * reflects the theme active at render time, rather than a color cached at construction.
     */
    Drawable resolveIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.applications_grid);
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
