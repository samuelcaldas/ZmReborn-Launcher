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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FolderLayoutMetricsTest {
    @Test
    public void boundsPanelInsideShortNarrowViewport() {
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(220, 260, 12,
                480, 600, 58, 4, 16, 48);

        assertTrue(metrics.getPanelWidth() <= 196);
        assertTrue(metrics.getPanelHeight() <= 236);
        assertTrue(metrics.getCellWidth() > 0);
        assertTrue(metrics.getCellHeight() > 0);
        assertTrue(metrics.isScrollable());
    }

    @Test
    public void emptyFolderKeepsOneVisibleRowWithoutScrolling() {
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(480, 800, 16,
                440, 640, 58, 5, 0, 48);

        assertEquals(1, metrics.getTotalRows());
        assertEquals(1, metrics.getVisibleRows());
        assertFalse(metrics.isScrollable());
    }

    @Test
    public void invalidInputsRemainPositive() {
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(0, 0, -1,
                0, 0, -1, 0, -1, 0);

        assertTrue(metrics.getPanelWidth() > 0);
        assertTrue(metrics.getPanelHeight() > 0);
        assertTrue(metrics.getCellWidth() > 0);
        assertTrue(metrics.getCellHeight() > 0);
    }

    @Test
    public void twoLineLabelHeightFitsWithinComputedMetrics() {
        int labelMinHeight = 44;
        int cellHeight = 100;
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(480, 800, 16,
                440, 640, 58, 5, 12, labelMinHeight);

        assertTrue("Cell height must accommodate label with margin",
                metrics.getCellHeight() >= labelMinHeight);
    }

    @Test
    public void densitySafeLabelMinHeightResourceExists() throws Exception {
        Map<String, String> dimensions = readDimensions("values/dimens.xml");
        assertNotNull("label_min_height dimension must exist",
                dimensions.get("label_min_height"));
        assertTrue("label_min_height must have dp unit",
                dimensions.get("label_min_height").endsWith("dp"));
    }

    @Test
    public void folderMetricsNeverClipsLabelContent() {
        int labelMinHeight = 44;
        int cellHeight = 64;
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(220, 260, 12,
                480, 600, 58, 4, 16, labelMinHeight);

        assertTrue("Computed cell height must fit minimum label height",
                metrics.getCellHeight() >= labelMinHeight);
    }

    private static Map<String, String> readDimensions(String relativePath) throws Exception {
        NodeList nodes = parse(relativePath).getElementsByTagName("dimen");
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
