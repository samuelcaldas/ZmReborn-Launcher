package org.zeam;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsResourceContractTest {
    @Test
    public void preferencesExposeAllExistingControlsWithoutHiddenDependencies() throws Exception {
        Element root = parse("xml/preferences.xml").getDocumentElement();
        assertEquals(36, countPreferences(root));
        assertFalse("Preference dependencies must remain explicit in Java", containsDependency(root));
    }

    @Test
    public void preferenceRowKeepsFrameworkBindingViews() throws Exception {
        String layout = read("layout/settings_preference.xml");
        assertTrue(layout.contains("@android:id/title"));
        assertTrue(layout.contains("@android:id/summary"));
        assertTrue(layout.contains("@android:id/widget_frame"));
        assertTrue(layout.contains("48dp"));
        String checkbox = read("layout/settings_checkbox_widget.xml");
        assertTrue(checkbox.contains("@android:id/checkbox"));
    }

    private static int countPreferences(Element element) {
        int count = element.hasAttribute("android:key") ? 1 : 0;
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element) {
                count += countPreferences((Element) child);
            }
        }
        return count;
    }

    private static boolean containsDependency(Element element) {
        if (element.hasAttribute("android:dependency")) {
            return true;
        }
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element && containsDependency((Element) child)) {
                return true;
            }
        }
        return false;
    }

    private static org.w3c.dom.Document parse(String relativePath) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resourceFile(relativePath));
    }

    private static String read(String relativePath) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(resourceFile(relativePath)));
        StringBuilder content = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return content.toString();
    }

    private static File resourceFile(String relativePath) {
        File workingDirectory = new File(System.getProperty("user.dir"));
        File moduleResource = new File(workingDirectory, "src/main/res/" + relativePath);
        if (moduleResource.isFile()) {
            return moduleResource;
        }
        return new File(workingDirectory, "app/src/main/res/" + relativePath);
    }
}
