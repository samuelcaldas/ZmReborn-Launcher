package org.zmreborn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;
import java.lang.reflect.Array;

/**
 * BroadcastReceiver processing broadcast requests to install shortcuts on the desktop.
 */
public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    private final int[] coordinates = new int[2];

    @Override
    public void onReceive(Context context, Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }
        int screen = Launcher.getScreen();
        if (installShortcut(context, data, screen)) {
            return;
        }
        int count = Launcher.getScreenCount(context);
        for (int i = 0; i < count; i++) {
            if (i != screen && installShortcut(context, data, i)) {
                return;
            }
        }
    }

    private boolean installShortcut(Context context, Intent data, int screen) {
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        if (findEmptyCell(context, this.coordinates, screen)) {
            CellLayout.CellInfo cell = new CellLayout.CellInfo();
            cell.cellX = this.coordinates[0];
            cell.cellY = this.coordinates[1];
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
                Toast.makeText(context, context.getString(R.string.shortcut_installed, name), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getString(R.string.shortcut_duplicate, name), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        Toast.makeText(context, context.getString(R.string.out_of_space), Toast.LENGTH_SHORT).show();
        return false;
    }

    private static boolean findEmptyCell(Context context, int[] xy, int screen) {
        int xCount = PreferencesUtil.getContentGridRows(context);
        int yCount = PreferencesUtil.getContentGridColumns(context);
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, xCount, yCount);
        Cursor cursor = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI,
                new String[]{"cellX", "cellY", "spanX", "spanY"}, "screen=?",
                new String[]{String.valueOf(screen)}, null);
        if (cursor == null) {
            return false;
        }
        try {
            int cellXIndex = cursor.getColumnIndexOrThrow("cellX");
            int cellYIndex = cursor.getColumnIndexOrThrow("cellY");
            int spanXIndex = cursor.getColumnIndexOrThrow("spanX");
            int spanYIndex = cursor.getColumnIndexOrThrow("spanY");
            while (cursor.moveToNext()) {
                int cellX = cursor.getInt(cellXIndex);
                int cellY = cursor.getInt(cellYIndex);
                int spanX = cursor.getInt(spanXIndex);
                int spanY = cursor.getInt(spanYIndex);
                for (int x = cellX; x < cellX + spanX && x < xCount; x++) {
                    for (int y = cellY; y < cellY + spanY && y < yCount; y++) {
                        occupied[x][y] = true;
                    }
                }
            }
        } finally {
            cursor.close();
        }
        return CellLayout.findVacantCell(xy, 1, 1, xCount, yCount, occupied);
    }
}
