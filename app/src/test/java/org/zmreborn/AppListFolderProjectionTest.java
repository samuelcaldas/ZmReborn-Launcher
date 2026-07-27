package org.zmreborn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppListFolderProjectionTest {
    @Test
    public void projectsFoldersBeforeUnassignedApplications() {
        ApplicationItemInfo camera = application("Camera", "pkg.camera", "CameraActivity");
        ApplicationItemInfo mail = application("Mail", "pkg.mail", "MailActivity");
        ApplicationItemInfo maps = application("Maps", "pkg.maps", "MapsActivity");
        AppListFolderRecord folder = new AppListFolderRecord(4, "Tools", 0,
                Arrays.asList(AppListFolderProjection.componentNameOf(maps), "missing/Activity"));

        ArrayList<ApplicationItemInfo> result = AppListFolderProjection.project(
                Arrays.asList(folder), Arrays.asList(camera, mail, maps));

        assertTrue(result.get(0) instanceof AppListFolderInfo);
        assertEquals("Tools", result.get(0).title);
        assertEquals(1, ((AppListFolderInfo) result.get(0)).getContents().size());
        assertEquals("Camera", result.get(1).title);
        assertEquals("Mail", result.get(2).title);
    }

    @Test
    public void emptyFolderRemainsVisible() {
        AppListFolderRecord folder = new AppListFolderRecord(7, "Empty", 0,
                Arrays.asList("missing/Activity"));

        ArrayList<ApplicationItemInfo> result = AppListFolderProjection.project(
                Arrays.asList(folder), new ArrayList<ApplicationItemInfo>());

        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof AppListFolderInfo);
        assertEquals(0, ((AppListFolderInfo) result.get(0)).getContents().size());
    }

    private static ApplicationItemInfo application(String title, String packageName,
            String className) {
        ApplicationItemInfo application = new ApplicationItemInfo();
        application.title = title;
        application.componentName = packageName + "/" + className;
        return application;
    }
}
