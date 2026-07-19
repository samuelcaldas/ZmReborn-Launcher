package org.zeam;

import android.content.ComponentName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AppListFolderProjection {
    private AppListFolderProjection() {
    }

    static ArrayList<ApplicationItemInfo> project(List<AppListFolderRecord> folders,
            List<ApplicationItemInfo> applications) {
        HashMap<String, ApplicationItemInfo> byComponent = indexApplications(applications);
        ArrayList<AppListFolderRecord> orderedFolders = new ArrayList<>(folders);
        Collections.sort(orderedFolders, new Comparator<AppListFolderRecord>() {
            public int compare(AppListFolderRecord left, AppListFolderRecord right) {
                int titleOrder = left.getTitle().compareToIgnoreCase(right.getTitle());
                if (titleOrder != 0) {
                    return titleOrder;
                }
                return left.getPosition() - right.getPosition();
            }
        });
        Set<String> assignedComponents = new HashSet<>();
        ArrayList<ApplicationItemInfo> result = new ArrayList<>();
        for (AppListFolderRecord folder : orderedFolders) {
            ArrayList<ApplicationItemInfo> contents = new ArrayList<>();
            for (String componentName : folder.getComponentNames()) {
                ApplicationItemInfo application = byComponent.get(componentName);
                if (application != null) {
                    contents.add(application);
                    assignedComponents.add(componentName);
                }
            }
            result.add(new AppListFolderInfo(folder.getId(), folder.getTitle(), contents));
        }
        for (ApplicationItemInfo application : applications) {
            String componentName = componentNameOf(application);
            if (!assignedComponents.contains(componentName)) {
                result.add(application);
            }
        }
        return result;
    }

    private static HashMap<String, ApplicationItemInfo> indexApplications(
            List<ApplicationItemInfo> applications) {
        HashMap<String, ApplicationItemInfo> byComponent = new HashMap<>();
        for (ApplicationItemInfo application : applications) {
            byComponent.put(componentNameOf(application), application);
        }
        return byComponent;
    }

    static String componentNameOf(ApplicationItemInfo application) {
        if (application.componentName != null) {
            return application.componentName;
        }
        ComponentName component = application.intent == null ? null : application.intent.getComponent();
        return component == null ? "" : component.flattenToString();
    }
}
