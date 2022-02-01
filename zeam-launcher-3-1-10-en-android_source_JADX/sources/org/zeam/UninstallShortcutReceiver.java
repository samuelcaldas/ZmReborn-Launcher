package org.zeam;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.widget.Toast;
import java.net.URISyntaxException;
import org.zeam.LauncherSettings;

public class UninstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_UNINSTALL_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    public void onReceive(Context context, Intent data) {
        if (ACTION_UNINSTALL_SHORTCUT.equals(data.getAction())) {
            Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
            boolean duplicate = data.getBooleanExtra("duplicate", true);
            if (intent != null && name != null) {
                ContentResolver contentResolver = context.getContentResolver();
                Cursor cursor = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"_id", LauncherSettings.BaseLauncherColumns.INTENT}, "title=?", new String[]{name}, (String) null);
                int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
                int idIndex = cursor.getColumnIndexOrThrow("_id");
                boolean changed = false;
                while (cursor.moveToNext()) {
                    try {
                        try {
                            if (intent.filterEquals(Intent.parseUri(cursor.getString(intentIndex), 0))) {
                                contentResolver.delete(LauncherSettings.Favorites.getContentUri(cursor.getLong(idIndex), false), (String) null, (String[]) null);
                                changed = true;
                                if (!duplicate) {
                                    break;
                                }
                            } else {
                                continue;
                            }
                        } catch (URISyntaxException e) {
                        }
                    } finally {
                        cursor.close();
                    }
                }
                if (changed) {
                    contentResolver.notifyChange(LauncherSettings.Favorites.CONTENT_URI, (ContentObserver) null);
                    Toast.makeText(context, context.getString(C0041R.string.shortcut_uninstalled, new Object[]{name}), 0).show();
                }
            }
        }
    }
}
