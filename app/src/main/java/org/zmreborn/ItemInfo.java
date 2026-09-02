package org.zmreborn;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import org.zmreborn.LauncherSettings;

/**
 * Base data model representing any launchable item, shortcut, widget, or folder placed on the workspace or dock.
 */
class ItemInfo {
    static final int NO_ID = -1;

    long id = NO_ID;
    int cellX = NO_ID;
    int cellY = NO_ID;
    long container = NO_ID;
    boolean isGesture = false;
    int itemType;
    int screen = NO_ID;
    int spanX = 1;
    int spanY = 1;

    ItemInfo() {
    }

    ItemInfo(ItemInfo info) {
        if (info == null) {
            return;
        }
        this.id = info.id;
        this.cellX = info.cellX;
        this.cellY = info.cellY;
        this.spanX = info.spanX;
        this.spanY = info.spanY;
        this.screen = info.screen;
        this.itemType = info.itemType;
        this.container = info.container;
    }

    /**
     * Serializes core item position and type coordinates into a database ContentValues record.
     */
    void onAddToDatabase(ContentValues values) {
        values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, this.itemType);
        if (!this.isGesture) {
            values.put("container", this.container);
            values.put("screen", this.screen);
            values.put("cellX", this.cellX);
            values.put("cellY", this.cellY);
            values.put("spanX", this.spanX);
            values.put("spanY", this.spanY);
        }
    }

    /**
     * Compresses the given bitmap to PNG byte array and places it into the provided ContentValues.
     */
    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap == null || values == null) {
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(bitmap.getWidth() * bitmap.getHeight() * 4);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            values.put(LauncherSettings.BaseLauncherColumns.ICON, out.toByteArray());
        } catch (IOException e) {
            Log.w("Favorite", "Could not write icon", e);
        }
    }

    /**
     * Returns a comparator ordering items horizontally by cellX coordinate.
     */
    static Comparator<ItemInfo> createCellXComparator() {
        return new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo first, ItemInfo second) {
                return Integer.compare(first.cellX, second.cellX);
            }
        };
    }
}
