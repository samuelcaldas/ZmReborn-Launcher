package org.zeam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AppListFolderRecord {
    private final long id;
    private final String title;
    private final int position;
    private final ArrayList<String> componentNames;

    AppListFolderRecord(long id, String title, int position, List<String> componentNames) {
        this.id = id;
        this.title = title;
        this.position = position;
        this.componentNames = new ArrayList<>(componentNames);
    }

    long getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    int getPosition() {
        return position;
    }

    List<String> getComponentNames() {
        return Collections.unmodifiableList(componentNames);
    }
}
