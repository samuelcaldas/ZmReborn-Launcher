package org.zmreborn;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.zmreborn.LauncherSettings;

/**
 * Item model representing an installed launcher activity, application shortcut, or custom shortcut tile.
 */
class ApplicationItemInfo extends ItemInfo {
    private static final Collator COLLATOR = Collator.getInstance();

    boolean customIcon;
    boolean filtered;
    String componentName;
    Drawable icon;
    Bitmap iconBitmap;
    Intent.ShortcutIconResource iconResource;
    Intent intent;
    CharSequence title;
    Bitmap titleBitmap;

    ApplicationItemInfo() {
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
    }

    ApplicationItemInfo(ApplicationItemInfo info) {
        super(info);
        if (info == null) {
            return;
        }
        this.title = info.title != null ? info.title.toString() : null;
        this.intent = info.intent != null ? new Intent(info.intent) : null;
        if (info.iconResource != null) {
            this.iconResource = new Intent.ShortcutIconResource();
            this.iconResource.packageName = info.iconResource.packageName;
            this.iconResource.resourceName = info.iconResource.resourceName;
        }
        this.icon = info.icon;
        this.filtered = info.filtered;
        this.customIcon = info.customIcon;
        this.componentName = info.componentName;
    }

    final void setIntent(Intent intent) {
        this.intent = intent;
    }

    /**
     * Sets the launch activity ComponentName and standard launcher flags.
     */
    final void setActivity(ComponentName className, int launchFlags) {
        if (className == null) {
            return;
        }
        this.componentName = className.flattenToString();
        this.intent = new Intent(Intent.ACTION_MAIN);
        this.intent.addCategory(Intent.CATEGORY_LAUNCHER);
        this.intent.setComponent(className);
        this.intent.setFlags(launchFlags);
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
    }

    /**
     * Returns a stable unique key identifying this application item across drawer rebuilds and sorting.
     */
    String getStableKey() {
        if (this.componentName != null && this.componentName.length() > 0) {
            return "component:" + this.componentName;
        }
        if (this.intent != null && this.intent.getComponent() != null) {
            return "component:" + this.intent.getComponent().flattenToString();
        }
        if (this.intent != null) {
            return "intent:" + this.intent.toUri(0);
        }
        return "title:" + String.valueOf(this.title);
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        String titleStr = this.title != null ? this.title.toString() : null;
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, titleStr);
        String uri = this.intent != null ? this.intent.toUri(0) : null;
        values.put(LauncherSettings.BaseLauncherColumns.INTENT, uri);
        if (this.customIcon) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, LauncherSettings.Favorites.ICON_TYPE_BITMAP);
            Bitmap bitmap = extractBitmap(this.icon);
            writeBitmap(values, bitmap);
            return;
        }
        values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, LauncherSettings.Favorites.ICON_TYPE_RESOURCE);
        if (this.iconResource != null) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, this.iconResource.packageName);
            values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, this.iconResource.resourceName);
        }
    }

    private static Bitmap extractBitmap(Drawable icon) {
        if (icon instanceof BitmapDrawable) {
            return ((BitmapDrawable) icon).getBitmap();
        }
        if (icon instanceof FastBitmapDrawable) {
            return ((FastBitmapDrawable) icon).getBitmap();
        }
        return null;
    }

    /**
     * Diagnostic utility dumping an application info list to Logcat.
     */
    public static void dumpApplicationInfoList(String tag, String label, List<ApplicationItemInfo> list) {
        if (list == null) {
            Log.d(tag, label + " size=0 (null)");
            return;
        }
        Log.d(tag, label + " size=" + list.size());
        for (ApplicationItemInfo info : list) {
            Log.d(tag, "   title=\"" + info.title + "\" titleBitmap=" + info.titleBitmap + " iconBitmap=" + info.iconBitmap);
        }
    }

    @Override
    public String toString() {
        return this.title != null ? this.title.toString() : "";
    }

    /**
     * Orders application items alphabetically by their localized title using Collator.
     */
    static class TitleComparator implements Comparator<ApplicationItemInfo> {
        @Override
        public int compare(ApplicationItemInfo first, ApplicationItemInfo second) {
            String titleA = first.title != null ? first.title.toString() : "";
            String titleB = second.title != null ? second.title.toString() : "";
            return COLLATOR.compare(titleA, titleB);
        }
    }
}
