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
import java.util.Iterator;
import org.zmreborn.LauncherSettings;

class ApplicationItemInfo extends ItemInfo {
    /* access modifiers changed from: private */
    public static final Collator sCollator = Collator.getInstance();
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
        this.itemType = 1;
    }

    public ApplicationItemInfo(ApplicationItemInfo info) {
        super(info);
        this.title = info.title.toString();
        this.intent = new Intent(info.intent);
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

    /* access modifiers changed from: package-private */
    public final void setIntent(Intent intent2) {
    }

    /* access modifiers changed from: package-private */
    public final void setActivity(ComponentName className, int launchFlags) {
        this.componentName = className.flattenToString();
        this.intent = new Intent("android.intent.action.MAIN");
        this.intent.addCategory("android.intent.category.LAUNCHER");
        this.intent.setComponent(className);
        this.intent.setFlags(launchFlags);
        this.itemType = 0;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        String titleStr;
        String uri;
        Bitmap bitmap;
        super.onAddToDatabase(values);
        if (this.title != null) {
            titleStr = this.title.toString();
        } else {
            titleStr = null;
        }
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, titleStr);
        if (this.intent != null) {
            uri = this.intent.toUri(0);
        } else {
            uri = null;
        }
        values.put(LauncherSettings.BaseLauncherColumns.INTENT, uri);
        if (this.customIcon) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, 1);
            if (this.icon instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) this.icon).getBitmap();
            } else {
                bitmap = ((FastBitmapDrawable) this.icon).getBitmap();
            }
            writeBitmap(values, bitmap);
            return;
        }
        values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, 0);
        if (this.iconResource != null) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, this.iconResource.packageName);
            values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, this.iconResource.resourceName);
        }
    }

    public static void dumpApplicationInfoList(String tag, String label, ArrayList<ApplicationItemInfo> list) {
        Log.d(tag, String.valueOf(label) + " size=" + list.size());
        Iterator<ApplicationItemInfo> it = list.iterator();
        while (it.hasNext()) {
            ApplicationItemInfo info = it.next();
            Log.d(tag, "   title=\"" + info.title + "\" titleBitmap=" + info.titleBitmap + " iconBitmap=" + info.iconBitmap);
        }
    }

    public String toString() {
        return this.title.toString();
    }

    static class TitleComparator implements Comparator<ApplicationItemInfo> {
        TitleComparator() {
        }

        public final int compare(ApplicationItemInfo a, ApplicationItemInfo b) {
            return ApplicationItemInfo.sCollator.compare(a.title.toString(), b.title.toString());
        }
    }
}
