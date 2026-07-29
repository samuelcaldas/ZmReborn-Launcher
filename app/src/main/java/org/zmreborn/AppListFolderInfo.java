package org.zmreborn;

import java.util.ArrayList;
import java.util.List;

/** Drawer-only folder tile. Workspace favorites never reference this record. */
final class AppListFolderInfo extends ApplicationItemInfo {
    private final long folderId;
    private final ArrayList<ApplicationItemInfo> contents;

    AppListFolderInfo(long folderId, CharSequence title, List<ApplicationItemInfo> contents) {
        super();
        this.folderId = folderId;
        this.title = title;
        this.contents = new ArrayList<>(contents);
        this.icon = null;
        this.filtered = true;
    }

    long getFolderId() {
        return folderId;
    }

    @Override
    String getStableKey() {
        return "folder:" + this.folderId;
    }

    ArrayList<ApplicationItemInfo> getContents() {
        return new ArrayList<>(contents);
    }
}
