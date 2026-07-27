package org.zmreborn;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persists drawer folder metadata independently from workspace favorites. */
final class AppListFolderStore {
    private static final String[] FOLDER_COLUMNS = {"_id", "title", "position"};
    private static final String[] ITEM_COLUMNS = {"folderId", "componentName", "position"};
    private final ContentResolver resolver;

    AppListFolderStore(ContentResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("ContentResolver is required");
        }
        this.resolver = resolver;
    }

    ArrayList<AppListFolderRecord> loadFolders() {
        ArrayList<AppListFolderRecord> folders = new ArrayList<>();
        Cursor folderCursor = resolver.query(LauncherSettings.AppListFolders.CONTENT_URI,
                FOLDER_COLUMNS, null, null, "position ASC, _id ASC");
        if (folderCursor == null) {
            return folders;
        }
        try {
            while (folderCursor.moveToNext()) {
                long id = folderCursor.getLong(0);
                folders.add(new AppListFolderRecord(id, folderCursor.getString(1),
                        folderCursor.getInt(2), loadComponents(id)));
            }
        } finally {
            folderCursor.close();
        }
        return folders;
    }

    long createFolder(String title, int position) {
        validateTitle(title);
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("position", Math.max(0, position));
        Uri result = resolver.insert(LauncherSettings.AppListFolders.CONTENT_URI, values);
        if (result == null) {
            throw new IllegalStateException("Unable to create app-list folder");
        }
        return ContentUris.parseId(result);
    }

    void renameFolder(long folderId, String title) {
        validateTitle(title);
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        int updated = resolver.update(LauncherSettings.AppListFolders.getContentUri(folderId), values,
                null, null);
        if (updated != 1) {
            throw new IllegalStateException("Unable to rename app-list folder " + folderId);
        }
    }

    void deleteFolder(long folderId) {
        resolver.delete(LauncherSettings.AppListFolders.ITEMS_CONTENT_URI, "folderId=?",
                new String[]{String.valueOf(folderId)});
        resolver.delete(LauncherSettings.AppListFolders.getContentUri(folderId), null, null);
    }

    void removePackage(String packageName) {
        if (packageName == null || packageName.trim().length() == 0) {
            return;
        }
        resolver.delete(LauncherSettings.AppListFolders.ITEMS_CONTENT_URI,
                "componentName LIKE ?", new String[]{packageName + "/%"});
    }

    Set<String> loadAssignedComponents() {
        HashSet<String> assigned = new HashSet<>();
        Cursor cursor = resolver.query(LauncherSettings.AppListFolders.ITEMS_CONTENT_URI,
                new String[]{"componentName"}, null, null, null);
        if (cursor == null) {
            return assigned;
        }
        try {
            while (cursor.moveToNext()) {
                assigned.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return assigned;
    }

    void replaceContents(long folderId, List<String> componentNames) {
        if (componentNames == null) {
            throw new IllegalArgumentException("componentNames is required");
        }
        ArrayList<String> uniqueNames = new ArrayList<>();
        for (String componentName : componentNames) {
            if (componentName == null || componentName.trim().length() == 0) {
                throw new IllegalArgumentException("componentName must not be empty");
            }
            if (!uniqueNames.contains(componentName)) {
                uniqueNames.add(componentName);
            }
        }
        resolver.delete(LauncherSettings.AppListFolders.ITEMS_CONTENT_URI, "folderId=?",
                new String[]{String.valueOf(folderId)});
        ContentValues[] values = new ContentValues[uniqueNames.size()];
        for (int index = 0; index < uniqueNames.size(); index++) {
            ContentValues value = new ContentValues();
            value.put("folderId", folderId);
            value.put("componentName", uniqueNames.get(index));
            value.put("position", index);
            values[index] = value;
        }
        if (values.length > 0 && resolver.bulkInsert(LauncherSettings.AppListFolders.ITEMS_CONTENT_URI,
                values) != values.length) {
            throw new IllegalStateException("Unable to replace app-list folder contents");
        }
    }

    private ArrayList<String> loadComponents(long folderId) {
        ArrayList<String> components = new ArrayList<>();
        Cursor cursor = resolver.query(LauncherSettings.AppListFolders.ITEMS_CONTENT_URI,
                ITEM_COLUMNS, "folderId=?", new String[]{String.valueOf(folderId)},
                "position ASC, _id ASC");
        if (cursor == null) {
            return components;
        }
        try {
            while (cursor.moveToNext()) {
                components.add(cursor.getString(1));
            }
        } finally {
            cursor.close();
        }
        return components;
    }

    private static void validateTitle(String title) {
        if (title == null || title.trim().length() == 0) {
            throw new IllegalArgumentException("Folder title must not be empty");
        }
    }
}
