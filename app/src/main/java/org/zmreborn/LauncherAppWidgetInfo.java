package org.zmreborn;

import android.appwidget.AppWidgetHostView;
import android.content.ContentValues;
import org.zmreborn.LauncherSettings;

/**
 * Item model representing an Android AppWidget placed on the workspace desktop.
 */
class LauncherAppWidgetInfo extends ItemInfo {
    int appWidgetId;
    AppWidgetHostView hostView = null;

    LauncherAppWidgetInfo(int appWidgetId) {
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;
        this.appWidgetId = appWidgetId;
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put("appWidgetId", this.appWidgetId);
    }

    @Override
    public String toString() {
        return Integer.toString(this.appWidgetId);
    }
}
