package org.zmreborn;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ApplicationsAdapterContractTest {
    @Test
    public void stableIdsAreDeterministicAndProfileSensitive() {
        long first = ApplicationsAdapter.stableId("profile:1|component:org.example/.Main");
        long repeated = ApplicationsAdapter.stableId("profile:1|component:org.example/.Main");
        long anotherProfile = ApplicationsAdapter.stableId(
                "profile:2|component:org.example/.Main");

        assertEquals(first, repeated);
        assertNotEquals(first, anotherProfile);
    }

    @Test
    public void componentAndFolderKeysRemainDistinct() {
        ApplicationItemInfo application = new ApplicationItemInfo();
        application.componentName = "org.example/.Main";
        AppListFolderInfo folder = new AppListFolderInfo(
                7L, "Folder", java.util.Collections.<ApplicationItemInfo>emptyList());

        assertEquals("component:org.example/.Main", application.getStableKey());
        assertEquals("folder:7", folder.getStableKey());
    }
}
