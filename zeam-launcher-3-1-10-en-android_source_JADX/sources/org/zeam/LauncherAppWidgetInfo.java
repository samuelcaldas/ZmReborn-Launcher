package org.zeam;

import android.appwidget.AppWidgetHostView;
import android.content.ContentValues;

class LauncherAppWidgetInfo extends ItemInfo {
    int appWidgetId;
    AppWidgetHostView hostView = null;

    LauncherAppWidgetInfo(int appWidgetId2) {
        this.itemType = 4;
        this.appWidgetId = appWidgetId2;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put("appWidgetId", Integer.valueOf(this.appWidgetId));
    }

    public String toString() {
        return Integer.toString(this.appWidgetId);
    }
}
