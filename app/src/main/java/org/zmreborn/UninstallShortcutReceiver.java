package org.zmreborn;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;
import java.net.URISyntaxException;

/**
 * BroadcastReceiver processing broadcast requests to remove shortcuts from the launcher.
 */
public class UninstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_UNINSTALL_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    @Override
    public void onReceive(Context context, Intent data) {
        if (!ACTION_UNINSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }
        Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        boolean duplicate = data.getBooleanExtra("duplicate", true);
        if (intent == null || name == null) {
            return;
        }
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI,
                new String[]{"_id", LauncherSettings.BaseLauncherColumns.INTENT},
                "title=?", new String[]{name}, null);
        if (cursor == null) {
            return;
        }
        boolean changed = false;
        try {
            int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
            int idIndex = cursor.getColumnIndexOrThrow("_id");
            while (cursor.moveToNext()) {
                try {
                    String uriString = cursor.getString(intentIndex);
                    if (uriString != null && intent.filterEquals(Intent.parseUri(uriString, 0))) {
                        contentResolver.delete(
                                LauncherSettings.Favorites.getContentUri(cursor.getLong(idIndex), false),
                                null, null);
                        changed = true;
                        if (!duplicate) {
                            break;
                        }
                    }
                } catch (URISyntaxException ignored) {
                }
            }
        } finally {
            cursor.close();
        }
        if (changed) {
            contentResolver.notifyChange(LauncherSettings.Favorites.CONTENT_URI, null);
            Toast.makeText(context, context.getString(R.string.shortcut_uninstalled, name), Toast.LENGTH_SHORT).show();
        }
    }
}
