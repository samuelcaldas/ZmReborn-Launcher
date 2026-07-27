package org.zmreborn;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SystemBarResourceContractTest {
    @Test
    public void launcherUsesProjectTheme() throws Exception {
        NodeList activities = parseProjectFile("src/main/AndroidManifest.xml")
                .getElementsByTagName("activity");
        Element launcher = findByAttribute(activities, "android:name", ".Launcher");
        assertEquals("@style/LauncherTheme", launcher.getAttribute("android:theme"));
    }

    @Test
    public void baseThemePreservesFrameworkWallpaperWindowBounds() throws Exception {
        Element theme = findTheme("src/main/res/values/styles.xml", "LauncherTheme");
        assertEquals("@android:style/Theme.Wallpaper.NoTitleBar",
                theme.getAttribute("parent"));
        assertEquals(0, theme.getElementsByTagName("item").getLength());
    }

    @Test
    public void api35ThemeOptsOutOfForcedEdgeToEdge() throws Exception {
        Element theme = findTheme(
                "src/main/res/values-v35/styles.xml", "LauncherTheme");
        assertEquals("@android:style/Theme.Wallpaper.NoTitleBar",
                theme.getAttribute("parent"));
        assertEquals("true",
                itemValue(theme, "android:windowOptOutEdgeToEdgeEnforcement"));
    }

    private static Element findTheme(String relativePath, String name) throws Exception {
        NodeList styles = parseProjectFile(relativePath).getElementsByTagName("style");
        return findByAttribute(styles, "name", name);
    }

    private static Element findByAttribute(NodeList nodes, String attribute, String value) {
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            if (value.equals(element.getAttribute(attribute))) {
                return element;
            }
        }
        assertNotNull("Missing element with " + attribute + "=" + value, null);
        return null;
    }

    private static String itemValue(Element style, String name) {
        NodeList items = style.getElementsByTagName("item");
        Element item = findByAttribute(items, "name", name);
        return item.getTextContent().trim();
    }

    private static Document parseProjectFile(String relativePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(projectFile(relativePath));
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
