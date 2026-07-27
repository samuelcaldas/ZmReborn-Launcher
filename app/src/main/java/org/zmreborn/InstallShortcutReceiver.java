package org.zmreborn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;
import java.lang.reflect.Array;
import org.zmreborn.CellLayout;
import org.zmreborn.LauncherSettings;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    private final int[] mCoordinates = new int[2];

    public void onReceive(Context context, Intent data) {
        if (ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            int screen = Launcher.getScreen();
            if (!installShortcut(context, data, screen)) {
                int count = Launcher.getScreenCount(context);
                int i = 0;
                while (i < count) {
                    if (i == screen || !installShortcut(context, data, i)) {
                        i++;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    private boolean installShortcut(Context context, Intent data, int screen) {
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        if (findEmptyCell(context, this.mCoordinates, screen)) {
            CellLayout.CellInfo cell = new CellLayout.CellInfo();
            cell.cellX = this.mCoordinates[0];
            cell.cellY = this.mCoordinates[1];
            cell.screen = screen;
            Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            if (intent == null) {
                return false;
            }
            if (intent.getAction() == null) {
                intent.setAction("android.intent.action.VIEW");
            }
            if (data.getBooleanExtra("duplicate", true) || !LauncherModel.shortcutExists(context, name, intent)) {
                Launcher.addShortcut(context, data, cell, true);
                Toast.makeText(context, context.getString(R.string.shortcut_installed, new Object[]{name}), 0).show();
            } else {
                Toast.makeText(context, context.getString(R.string.shortcut_duplicate, new Object[]{name}), 0).show();
            }
            return true;
        }
        Toast.makeText(context, context.getString(R.string.out_of_space), 0).show();
        return false;
    }

    private static boolean findEmptyCell(Context context, int[] xy, int screen) {
        int xCount = PreferencesUtil.getContentGridRows(context);
        int yCount = PreferencesUtil.getContentGridColumns(context);
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{xCount, yCount});
        Cursor cursor = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"cellX", "cellY", "spanX", "spanY"}, "screen=?", new String[]{String.valueOf(screen)}, (String) null);
        int cellXIndex = cursor.getColumnIndexOrThrow("cellX");
        int cellYIndex = cursor.getColumnIndexOrThrow("cellY");
        int spanXIndex = cursor.getColumnIndexOrThrow("spanX");
        int spanYIndex = cursor.getColumnIndexOrThrow("spanY");
        while (cursor.moveToNext()) {
            try {
                int cellX = cursor.getInt(cellXIndex);
                int cellY = cursor.getInt(cellYIndex);
                int spanX = cursor.getInt(spanXIndex);
                int spanY = cursor.getInt(spanYIndex);
                int x = cellX;
                while (x < cellX + spanX && x < xCount) {
                    int y = cellY;
                    while (y < cellY + spanY && y < yCount) {
                        occupied[x][y] = true;
                        y++;
                    }
                    x++;
                }
            } catch (Exception e) {
                cursor.close();
                return false;
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }
        cursor.close();
        return CellLayout.findVacantCell(xy, 1, 1, xCount, yCount, occupied);
    }
}
