package org.zmreborn;

import java.util.ArrayList;
import java.util.List;

/**
 * Drawer-only folder tile model. Workspace favorites never reference this record.
 */
final class AppListFolderInfo extends ApplicationItemInfo {
    private final long folderId;
    private final ArrayList<ApplicationItemInfo> contents;

    AppListFolderInfo(long folderId, CharSequence title, List<ApplicationItemInfo> contents) {
        super();
        this.folderId = folderId;
        this.title = title;
        this.contents = contents != null ? new ArrayList<>(contents) : new ArrayList<ApplicationItemInfo>();
        this.icon = null;
        this.filtered = true;
    }

    /**
     * Returns the unique database ID for this app-list folder.
     */
    long getFolderId() {
        return this.folderId;
    }

    @Override
    String getStableKey() {
        return "folder:" + this.folderId;
    }

    /**
     * Returns a copy of the list of application items contained in this folder.
     */
    ArrayList<ApplicationItemInfo> getContents() {
        return new ArrayList<>(this.contents);
    }
}
