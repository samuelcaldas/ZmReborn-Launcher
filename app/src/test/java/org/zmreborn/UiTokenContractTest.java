package org.zmreborn;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UiTokenContractTest {
    @Test
    public void paletteExposesRequiredTokensWithExactValues() throws Exception {
        Map<String, String> expected = expectedPalette();
        Map<String, String> actual = parseValues("colors.xml", "color", "zm_reborn_");
        assertEquals(expected, actual);
        assertRequiredNames(actual, expected.keySet().toArray(new String[expected.size()]));
    }

    @Test
    public void dimensionsArePositiveAndKeepTouchAndRailContracts() throws Exception {
        Map<String, String> dimensions = parseValues("dimens.xml", "dimen", "");
        assertPositiveDimensions(dimensions);
        assertRequiredNames(dimensions,
                "space_4", "space_8", "space_12", "space_16", "space_24",
                "minimum_touch_target", "drawer_cell_min_width", "drawer_cell_min_height",
                "dock_cell_width_portrait", "dock_cell_height_portrait",
                "dock_cell_width_landscape", "dock_cell_height_landscape",
                "folder_action_size", "settings_row_height", "panel_corner_radius",
                "selector_corner_radius", "rail_thickness", "rail_active_thickness",
                "screen_indicator_dots_height", "drawer_horizontal_padding",
                "drawer_vertical_padding", "folder_spacing", "dialog_spacing",
                "navigation_strip_size", "rail_inset", "text_size_category",
                "text_size_label", "text_size_body", "text_size_title",
                "text_size_symbol", "text_size_display");
        assertTrue("minimum_touch_target must be at least 48dp",
                parseDp(dimensions, "minimum_touch_target") >= 48f);
        assertEquals(2f, parseDp(dimensions, "rail_thickness"), 0f);
        assertEquals(3f, parseDp(dimensions, "rail_active_thickness"), 0f);
        assertTrue("rail_active_thickness must exceed rail_thickness",
                parseDp(dimensions, "rail_active_thickness") > parseDp(dimensions, "rail_thickness"));
        assertEquals(12f, parseSp(dimensions, "text_size_category"), 0f);
        assertEquals(13f, parseSp(dimensions, "text_size_label"), 0f);
        assertEquals(14f, parseSp(dimensions, "text_size_body"), 0f);
        assertEquals(16f, parseSp(dimensions, "text_size_title"), 0f);
        assertEquals(20f, parseSp(dimensions, "text_size_symbol"), 0f);
        assertEquals(30f, parseSp(dimensions, "text_size_display"), 0f);
    }

    @Test
    public void typographyStylesUseSemanticScale() throws Exception {
        Map<String, String> styles = parseValues("styles.xml", "style", "TextAppearance.ZmReborn");
        assertRequiredNames(styles, "TextAppearance.ZmReborn", "TextAppearance.ZmReborn.Category",
                "TextAppearance.ZmReborn.Label", "TextAppearance.ZmReborn.Body",
                "TextAppearance.ZmReborn.Title", "TextAppearance.ZmReborn.Symbol",
                "TextAppearance.ZmReborn.Display");
        assertStyleToken(styles, "Category", "text_size_category");
        assertStyleToken(styles, "Label", "text_size_label");
        assertStyleToken(styles, "Body", "text_size_body");
        assertStyleToken(styles, "Title", "text_size_title");
        assertStyleToken(styles, "Symbol", "text_size_symbol");
        assertStyleToken(styles, "Display", "text_size_display");
    }

    @Test
    public void timingsArePositiveAndKeepExpectedDurations() throws Exception {
        Map<String, String> expected = expectedDurations();
        Map<String, String> actual = parseValues("integers.xml", "integer", "duration_");
        assertEquals(expected, actual);
        assertRequiredNames(actual, "duration_fast", "duration_short", "duration_medium", "duration_long");
        for (Map.Entry<String, String> entry : actual.entrySet()) {
            assertTrue("Timing must be positive: " + entry.getKey(), parseTiming(entry.getValue()) > 0);
        }
    }

    private static Map<String, String> expectedPalette() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("zm_reborn_slate", "#ff121a21");
        values.put("zm_reborn_glass", "#d9121a21");
        values.put("zm_reborn_fog", "#ffeaf0f3");
        values.put("zm_reborn_steel", "#ffb8c2c8");
        values.put("zm_reborn_amber", "#fff2b64a");
        values.put("zm_reborn_ember", "#ffd95c4f");
        return values;
    }

    private static Map<String, String> expectedDurations() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("duration_fast", "120");
        values.put("duration_short", "180");
        values.put("duration_medium", "240");
        values.put("duration_long", "600");
        return values;
    }

    private static Map<String, String> parseValues(String relativePath, String tagName, String namePrefix)
            throws Exception {
        NodeList nodes = parse(relativePath).getDocumentElement().getElementsByTagName(tagName);
        Map<String, String> values = new HashMap<String, String>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            String name = element.getAttribute("name");
            if (name.startsWith(namePrefix)) {
                assertTrue("Duplicate resource token: " + name,
                        values.put(name, element.getTextContent().trim()) == null);
            }
        }
        return values;
    }

    private static void assertRequiredNames(Map<String, String> values, String... names) {
        for (String name : names) {
            assertTrue("Missing resource token: " + name, values.containsKey(name));
        }
    }

    private static void assertPositiveDimensions(Map<String, String> dimensions) {
        for (Map.Entry<String, String> entry : dimensions.entrySet()) {
            assertTrue("Dimension must be positive: " + entry.getKey(),
                    parseDimension(entry.getValue()) > 0f);
        }
    }

    private static float parseDp(Map<String, String> dimensions, String name) {
        String value = required(dimensions, name).trim();
        assertTrue(name + " must use dp", value.endsWith("dp"));
        return parseDimension(value);
    }

    private static float parseSp(Map<String, String> dimensions, String name) {
        String value = required(dimensions, name).trim();
        assertTrue(name + " must use sp", value.endsWith("sp"));
        return parseDimension(value);
    }

    private static void assertStyleToken(Map<String, String> styles, String style, String token) {
        String value = required(styles, "TextAppearance.ZmReborn." + style);
        assertTrue(style + " must use " + token, value.contains("@dimen/" + token));
    }

    private static String required(Map<String, String> values, String name) {
        assertTrue("Missing resource token: " + name, values.containsKey(name));
        return values.get(name);
    }

    private static float parseDimension(String rawValue) {
        String value = rawValue.trim();
        String[] units = {"dp", "sp", "px", "pt", "in", "mm"};
        for (String unit : units) {
            if (value.endsWith(unit)) {
                try {
                    float parsed = Float.parseFloat(value.substring(0, value.length() - unit.length()));
                    assertTrue("Dimension must be finite: " + rawValue,
                            !Float.isNaN(parsed) && !Float.isInfinite(parsed));
                    return parsed;
                } catch (NumberFormatException exception) {
                    fail("Invalid dimension: " + rawValue);
                    return 0f;
                }
            }
        }
        fail("Unsupported dimension unit: " + rawValue);
        return 0f;
    }

    private static int parseTiming(String rawValue) {
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException exception) {
            fail("Invalid timing: " + rawValue);
            return 0;
        }
    }

    private static Document parse(String relativePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(resourceFile(relativePath));
    }

    private static File resourceFile(String relativePath) {
        File workingDirectory = new File(System.getProperty("user.dir"));
        File moduleResource = new File(workingDirectory, "src/main/res/values/" + relativePath);
        if (moduleResource.isFile()) {
            return moduleResource;
        }
        return new File(workingDirectory, "app/src/main/res/values/" + relativePath);
    }
}
