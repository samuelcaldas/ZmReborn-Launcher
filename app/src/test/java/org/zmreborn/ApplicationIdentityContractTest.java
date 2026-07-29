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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationIdentityContractTest {
    private static final String APPLICATION_ID = "org.zmreborn";

    @Test
    public void buildAndManifestDefineZmRebornIdentity() throws Exception {
        String build = readProjectFile("build.gradle");
        String manifest = readProjectFile("src/main/AndroidManifest.xml");
        assertTrue(build.contains("namespace '" + APPLICATION_ID + "'"));
        assertTrue(build.contains("applicationId \"" + APPLICATION_ID + "\""));
        assertTrue(manifest.contains("${applicationId}.provider"));
        assertTrue(manifest.contains("${applicationId}.core"));
    }

    @Test
    public void localizedLabelsAndPaletteMatchBrand() throws Exception {
        Map<String, String> base = values("values/strings.xml", "string");
        Map<String, String> portuguese = values("values-pt-rBR/strings.xml", "string");
        Map<String, String> colors = values("values/colors.xml", "color");
        assertEquals("ZM Reborn", base.get("application_name"));
        assertEquals("ZM Reborn", portuguese.get("application_name"));
        assertPalette(colors);
    }

    @Test
    public void persistenceContractsRemainStable() throws Exception {
        String provider = readProjectFile("src/main/java/org/zmreborn/LauncherProvider.java");
        String launcher = readProjectFile("src/main/java/org/zmreborn/Launcher.java");
        assertTrue(provider.contains("DATABASE_NAME = \"launcher.db\""));
        assertTrue(provider.contains("DATABASE_VERSION = 6"));
        assertTrue(provider.contains("TABLE_FAVORITES = \"favorites\""));
        assertTrue(provider.contains("TABLE_APP_LIST_FOLDERS = \"appListFolders\""));
        assertTrue(launcher.contains("launcher.preferences"));
    }

    @Test
    public void aboutPreferenceHasNoExternalClickAction() throws Exception {
        String preferences = readProjectFile("src/main/java/org/zmreborn/Preferences.java");
        assertTrue(preferences.contains("applicationPreference.setSelectable(false)"));
        assertFalse(preferences.contains("android.intent.action.VIEW"));
        assertFalse(preferences.contains("Uri.parse"));
    }

    @Test
    public void seekBarPreferenceUsesAutoResourceNamespace() throws Exception {
        String preference = readProjectFile("src/main/java/org/zmreborn/DialogSeekBarPreference.java");
        assertTrue(preference.contains("ATTRIBUTE_NAMESPACE = \"http://schemas.android.com/apk/res-auto\""));
        assertTrue(preference.contains("attrs.getAttributeValue(ATTRIBUTE_NAMESPACE, \"text\")"));
        assertTrue(preference.contains("attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, \"min\", 0)"));
        assertTrue(preference.contains("attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, \"max\", 100)"));
        assertFalse(preference.contains("http://schemas.android.com/apk/res/org.zmreborn"));
    }

    private static void assertPalette(Map<String, String> colors) {
        assertEquals("@color/m3_surface", colors.get("zm_reborn_slate"));
        assertEquals("@color/m3_surface_glass", colors.get("zm_reborn_glass"));
        assertEquals("@color/m3_on_surface", colors.get("zm_reborn_fog"));
        assertEquals("@color/m3_outline", colors.get("zm_reborn_steel"));
        assertEquals("@color/m3_primary", colors.get("zm_reborn_amber"));
        assertEquals("@color/m3_error", colors.get("zm_reborn_ember"));
    }

    private static Map<String, String> values(String relativePath, String tagName) throws Exception {
        NodeList nodes = parse(relativePath).getElementsByTagName(tagName);
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            result.put(element.getAttribute("name"), element.getTextContent());
        }
        return result;
    }

    private static org.w3c.dom.Document parse(String relativePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(resourceFile(relativePath));
    }

    private static String readProjectFile(String relativePath) throws Exception {
        byte[] content = Files.readAllBytes(projectFile(relativePath).toPath());
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
