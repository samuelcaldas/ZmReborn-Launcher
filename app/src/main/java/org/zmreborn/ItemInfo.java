package org.zmreborn;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import org.zmreborn.LauncherSettings;

class ItemInfo {
    static final int NO_ID = -1;
    int cellX = NO_ID;
    int cellY = NO_ID;
    long container = -1;

    /* renamed from: id */
    long f3id = -1;
    boolean isGesture = false;
    int itemType;
    int screen = NO_ID;
    int spanX = 1;
    int spanY = 1;

    ItemInfo() {
    }

    ItemInfo(ItemInfo info) {
        this.f3id = info.f3id;
        this.cellX = info.cellX;
        this.cellY = info.cellY;
        this.spanX = info.spanX;
        this.spanY = info.spanY;
        this.screen = info.screen;
        this.itemType = info.itemType;
        this.container = info.container;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(this.itemType));
        if (!this.isGesture) {
            values.put("container", Long.valueOf(this.container));
            values.put("screen", Integer.valueOf(this.screen));
            values.put("cellX", Integer.valueOf(this.cellX));
            values.put("cellY", Integer.valueOf(this.cellY));
            values.put("spanX", Integer.valueOf(this.spanX));
            values.put("spanY", Integer.valueOf(this.spanY));
        }
    }

    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(bitmap.getWidth() * bitmap.getHeight() * 4);
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
                values.put(LauncherSettings.BaseLauncherColumns.ICON, out.toByteArray());
            } catch (IOException e) {
                Log.w("Favorite", "Could not write icon");
            }
        }
    }

    static Comparator<ItemInfo> createCellXComparator() {
        return new Comparator<ItemInfo>() {
            public int compare(ItemInfo itemInfo1, ItemInfo itemInfo2) {
                int cellX1 = itemInfo1.cellX;
                int cellX2 = itemInfo2.cellX;
                if (cellX1 > cellX2) {
                    return 1;
                }
                if (cellX1 < cellX2) {
                    return ItemInfo.NO_ID;
                }
                return 0;
            }
        };
    }
}
