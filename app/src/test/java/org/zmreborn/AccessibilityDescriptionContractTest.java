package org.zmreborn;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessibilityDescriptionContractTest {

    @Test
    public void folderContentDescriptionsIncludeItemCount() throws Exception {
        String strings = readResourceFile("values/strings.xml");
        assertTrue("Folder with items accessibility string must exist",
                strings.contains("accessibility_folder_with_items"));
        assertTrue("String must include title placeholder",
                strings.contains("%1$s"));
        assertTrue("String must include count placeholder",
                strings.contains("%2$d"));
    }

    @Test
    public void drawerToggleButtonDescriptionExistsForOpenClose() throws Exception {
        String strings = readResourceFile("values/strings.xml");
        assertTrue("Open drawer accessibility string must exist",
                strings.contains("accessibility_open_drawer"));
        assertTrue("Close drawer accessibility string must exist",
                strings.contains("accessibility_close_drawer"));
    }

    @Test
    public void screenIndicatorGeneratesPageOfYDescriptions() throws Exception {
        String screenIndicator = readProjectFile("src/main/java/org/zmreborn/ScreenIndicator.java");
        assertTrue("ScreenIndicator must call setContentDescription",
                screenIndicator.contains("setContentDescription"));
        assertTrue("ScreenIndicator must use accessibility_page_indicator string",
                screenIndicator.contains("accessibility_page_indicator"));
        assertTrue("Must format current page (mCurrent + 1)",
                screenIndicator.contains("mCurrent + 1"));
        assertTrue("Must include total items count (mItems)",
                screenIndicator.contains("mItems"));
    }

    @Test
    public void applicationsAdapterAppliesDescriptionsToItems() throws Exception {
        String adapter = readProjectFile("src/main/java/org/zmreborn/ApplicationsAdapter.java");
        assertTrue("ApplicationsAdapter must build descriptions",
                adapter.contains("buildApplicationDescription"));
        assertTrue("Must call setContentDescription on items",
                adapter.contains("setContentDescription"));
        assertTrue("Description builder must derive descriptions from titles",
                adapter.contains("info.title"));
    }

    @Test
    public void folderRenameAndCloseButtonsHaveAccessibilityDescriptions() throws Exception {
        String folder = readProjectFile("src/main/java/org/zmreborn/Folder.java");
        assertTrue("Rename button must set accessibility description",
                folder.contains("accessibility_folder_rename"));
        assertTrue("Close button must set accessibility description",
                folder.contains("accessibility_folder_close"));
    }

    @Test
    public void folderIconIncludesItemCountInDescription() throws Exception {
        String folderIcon = readProjectFile("src/main/java/org/zmreborn/FolderIcon.java");
        assertTrue("FolderIcon must set content description",
                folderIcon.contains("setContentDescription"));
        assertTrue("Must get item count from folder contents",
                folderIcon.contains("folderInfo.contents.size()"));
        assertTrue("Must include accessibility_folder_with_items string",
                folderIcon.contains("accessibility_folder_with_items"));
    }

    private static String readProjectFile(String relativePath) throws Exception {
        byte[] content = Files.readAllBytes(projectFile(relativePath).toPath());
        return new String(content, StandardCharsets.UTF_8);
    }

    private static String readResourceFile(String relativePath) throws Exception {
        byte[] content = Files.readAllBytes(resourceFile(relativePath).toPath());
        return new String(content, StandardCharsets.UTF_8);
    }

    private static File resourceFile(String relativePath) {
        return projectFile("src/main/res/" + relativePath);
    }

    private static File projectFile(String relativePath) {
        File workingDirectory = new File(System.getProperty("user.dir"));
        File moduleFile = new File(workingDirectory, relativePath);
        if (moduleFile.exists()) {
            return moduleFile;
        }
        return new File(workingDirectory, "app/" + relativePath);
    }
}
