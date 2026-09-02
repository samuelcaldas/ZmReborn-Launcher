package org.zmreborn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable database record describing a drawer folder and its assigned application components.
 */
final class AppListFolderRecord {
    private final long id;
    private final String title;
    private final int position;
    private final ArrayList<String> componentNames;

    AppListFolderRecord(long id, String title, int position, List<String> componentNames) {
        this.id = id;
        this.title = title != null ? title : "";
        this.position = position;
        this.componentNames = componentNames != null ? new ArrayList<>(componentNames) : new ArrayList<String>();
    }

    long getId() {
        return this.id;
    }

    String getTitle() {
        return this.title;
    }

    int getPosition() {
        return this.position;
    }

    List<String> getComponentNames() {
        return Collections.unmodifiableList(this.componentNames);
    }
}
