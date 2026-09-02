package org.zmreborn;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import org.zmreborn.LauncherSettings;

/**
 * Item model representing the dock button that opens the all-applications drawer.
 */
public class ApplicationsGridItemInfo extends ItemInfo {
    CharSequence title;

    ApplicationsGridItemInfo(Context context) {
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPS_GRID;
        if (context != null) {
            this.title = context.getResources().getString(R.string.group_applications);
        }
    }

    /**
     * Resolves the drawer-open icon against {@code context}'s current resources so the icon
     * reflects the theme active at render time, rather than a color cached at construction.
     */
    Drawable resolveIcon(Context context) {
        if (context == null) {
            return null;
        }
        return context.getDrawable(R.drawable.applications_grid);
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, this.itemType);
        values.put("container", this.container);
        values.put("screen", this.screen);
        values.put("cellX", this.cellX);
        values.put("cellY", this.cellY);
    }
}
