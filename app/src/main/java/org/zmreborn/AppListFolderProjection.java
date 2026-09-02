package org.zmreborn;

import android.content.ComponentName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Projects flat application lists and drawer folders into a combined item list where assigned applications are nested inside folder tiles.
 */
final class AppListFolderProjection {
    private AppListFolderProjection() {
    }

    /**
     * Projects folders and application items into a combined ordered list.
     *
     * @param folders list of configured drawer folders
     * @param applications list of installed applications
     * @return combined list with folder tiles preceding unassigned applications
     */
    static ArrayList<ApplicationItemInfo> project(List<AppListFolderRecord> folders,
            List<ApplicationItemInfo> applications) {
        if (folders == null || folders.isEmpty()) {
            return applications != null ? new ArrayList<>(applications) : new ArrayList<ApplicationItemInfo>();
        }
        if (applications == null || applications.isEmpty()) {
            ArrayList<ApplicationItemInfo> emptyResult = new ArrayList<>();
            for (AppListFolderRecord folder : folders) {
                emptyResult.add(new AppListFolderInfo(folder.getId(), folder.getTitle(), Collections.<ApplicationItemInfo>emptyList()));
            }
            return emptyResult;
        }

        Map<String, ApplicationItemInfo> byComponent = indexApplications(applications);
        ArrayList<AppListFolderRecord> orderedFolders = new ArrayList<>(folders);
        Collections.sort(orderedFolders, new Comparator<AppListFolderRecord>() {
            @Override
            public int compare(AppListFolderRecord left, AppListFolderRecord right) {
                int titleOrder = left.getTitle().compareToIgnoreCase(right.getTitle());
                if (titleOrder != 0) {
                    return titleOrder;
                }
                return Integer.compare(left.getPosition(), right.getPosition());
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

    private static Map<String, ApplicationItemInfo> indexApplications(
            List<ApplicationItemInfo> applications) {
        Map<String, ApplicationItemInfo> byComponent = new HashMap<>();
        for (ApplicationItemInfo application : applications) {
            byComponent.put(componentNameOf(application), application);
        }
        return byComponent;
    }

    static String componentNameOf(ApplicationItemInfo application) {
        if (application == null) {
            return "";
        }
        if (application.componentName != null) {
            return application.componentName;
        }
        ComponentName component = application.intent == null ? null : application.intent.getComponent();
        return component == null ? "" : component.flattenToString();
    }
}
