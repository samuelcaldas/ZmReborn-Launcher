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
    public void paletteExposesCompatibilityAliases() throws Exception {
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
                "drawer_cell_compact_width", "drawer_cell_preferred_width",
                "drawer_cell_comfortable_width", "drawer_cell_expanded_width",
                "drawer_grid_spacing",
                "dock_cell_width_portrait", "dock_cell_height_portrait",
                "dock_cell_width_landscape", "dock_cell_height_landscape",
                "folder_action_size", "settings_row_height", "panel_corner_radius",
                "selector_corner_radius", "rail_thickness", "rail_active_thickness",
                "screen_indicator_dots_height", "drawer_horizontal_padding",
                "drawer_vertical_padding", "folder_spacing", "dialog_spacing",
                "navigation_strip_size", "rail_inset", "text_size_category",
                "text_size_label", "text_size_body", "text_size_title",
                "text_size_headline", "text_size_symbol", "text_size_display",
                "shape_corner_extra_small", "shape_corner_small", "shape_corner_medium",
                "shape_corner_large", "shape_corner_extra_large",
                "elevation_surface", "elevation_dock", "elevation_folder", "elevation_drawer_header");
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
        assertEquals(20f, parseSp(dimensions, "text_size_headline"), 0f);
        assertEquals(20f, parseSp(dimensions, "text_size_symbol"), 0f);
        assertEquals(30f, parseSp(dimensions, "text_size_display"), 0f);
        assertTrue("shape tier: extra_small < small",
                parseDp(dimensions, "shape_corner_extra_small") < parseDp(dimensions, "shape_corner_small"));
        assertTrue("shape tier: small < medium",
                parseDp(dimensions, "shape_corner_small") < parseDp(dimensions, "shape_corner_medium"));
        assertTrue("shape tier: medium < large",
                parseDp(dimensions, "shape_corner_medium") < parseDp(dimensions, "shape_corner_large"));
        assertTrue("shape tier: large < extra_large",
                parseDp(dimensions, "shape_corner_large") < parseDp(dimensions, "shape_corner_extra_large"));
        assertTrue("elevation_dock must exceed elevation_surface",
                parseDp(dimensions, "elevation_dock") > parseDp(dimensions, "elevation_surface"));
        assertTrue("elevation_folder must exceed elevation_dock",
                parseDp(dimensions, "elevation_folder") > parseDp(dimensions, "elevation_dock"));
    }

    @Test
    public void typographyStylesUseSemanticScale() throws Exception {
        Map<String, String> styles = parseValues("styles.xml", "style", "TextAppearance.ZmReborn");
        assertRequiredNames(styles, "TextAppearance.ZmReborn", "TextAppearance.ZmReborn.Category",
                "TextAppearance.ZmReborn.Label", "TextAppearance.ZmReborn.Body",
                "TextAppearance.ZmReborn.Title", "TextAppearance.ZmReborn.Symbol",
                "TextAppearance.ZmReborn.Display", "TextAppearance.ZmReborn.Headline");
        assertStyleToken(styles, "Category", "text_size_category");
        assertStyleToken(styles, "Label", "text_size_label");
        assertStyleToken(styles, "Body", "text_size_body");
        assertStyleToken(styles, "Title", "text_size_title");
        assertStyleToken(styles, "Headline", "text_size_headline");
        assertStyleToken(styles, "Symbol", "text_size_symbol");
        assertStyleToken(styles, "Display", "text_size_display");
    }

    @Test
    public void m3ColorRoleTokensKeepFallbackValuesAndAliasSemantics() throws Exception {
        Map<String, String> lightRoles = parseValues("colors.xml", "color", "m3_");
        assertEquals(expectedBaseM3RoleColors(), lightRoles);

        Map<String, String> darkRoles = parseValues("values-night", "colors.xml", "color", "m3_");
        assertEquals(expectedDarkM3RoleColors(), darkRoles);

        Map<String, String> dynamicLightRoles = parseValues("values-v31", "colors.xml", "color", "m3_");
        assertEquals(expectedDynamicLightM3RoleColors(), dynamicLightRoles);

        Map<String, String> dynamicDarkRoles = parseValues("values-night-v31", "colors.xml", "color", "m3_");
        assertEquals(expectedDynamicDarkM3RoleColors(), dynamicDarkRoles);

        Map<String, String> colors = parseValues("colors.xml", "color", "");
        for (Map.Entry<String, String> alias : expectedSemanticColorAliases().entrySet()) {
            assertEquals("Unexpected alias: " + alias.getKey(),
                    alias.getValue(), required(colors, alias.getKey()));
        }

        assertColorStateRole("m3_ripple_primary", "@color/m3_primary", "0.20");
        assertColorStateRole("m3_surface_glass", "@color/m3_surface", "0.85");
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
        values.put("zm_reborn_slate", "@color/m3_surface");
        values.put("zm_reborn_glass", "@color/m3_surface_glass");
        values.put("zm_reborn_fog", "@color/m3_on_surface");
        values.put("zm_reborn_steel", "@color/m3_outline");
        values.put("zm_reborn_amber", "@color/m3_primary");
        values.put("zm_reborn_ember", "@color/m3_error");
        return values;
    }

    private static Map<String, String> expectedBaseM3RoleColors() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("m3_primary", "#ff895b00");
        values.put("m3_on_primary", "#ffffffff");
        values.put("m3_primary_container", "#ffffddb0");
        values.put("m3_on_primary_container", "#ff2b1a00");
        values.put("m3_surface", "#fffff9f2");
        values.put("m3_on_surface", "#ff201b13");
        values.put("m3_surface_variant", "#fff1e1c9");
        values.put("m3_on_surface_variant", "#ff504533");
        values.put("m3_outline", "#ff827563");
        values.put("m3_outline_variant", "#ffd5c4ac");
        values.put("m3_error", "#ffba1a1a");
        values.put("m3_on_error", "#ffffffff");
        return values;
    }

    private static Map<String, String> expectedDarkM3RoleColors() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("m3_primary", "#fff2b64a");
        values.put("m3_on_primary", "#ff121a21");
        values.put("m3_primary_container", "#ff3d2e00");
        values.put("m3_on_primary_container", "#fff2b64a");
        values.put("m3_surface", "#ff121a21");
        values.put("m3_on_surface", "#ffeaf0f3");
        values.put("m3_surface_variant", "#ff1e2832");
        values.put("m3_on_surface_variant", "#ffb8c2c8");
        values.put("m3_outline", "#ffb8c2c8");
        values.put("m3_outline_variant", "#ff3a4550");
        values.put("m3_error", "#ffd95c4f");
        values.put("m3_on_error", "#ff1a0000");
        return values;
    }

    private static Map<String, String> expectedDynamicLightM3RoleColors() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("m3_primary", "@android:color/system_accent1_600");
        values.put("m3_on_primary", "@android:color/system_accent1_0");
        values.put("m3_primary_container", "@android:color/system_accent1_100");
        values.put("m3_on_primary_container", "@android:color/system_accent1_900");
        values.put("m3_surface", "@android:color/system_neutral1_10");
        values.put("m3_on_surface", "@android:color/system_neutral1_900");
        values.put("m3_surface_variant", "@android:color/system_neutral2_100");
        values.put("m3_on_surface_variant", "@android:color/system_neutral2_700");
        values.put("m3_outline", "@android:color/system_neutral2_500");
        values.put("m3_outline_variant", "@android:color/system_neutral2_300");
        return values;
    }

    private static Map<String, String> expectedDynamicDarkM3RoleColors() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("m3_primary", "@android:color/system_accent1_200");
        values.put("m3_on_primary", "@android:color/system_accent1_800");
        values.put("m3_primary_container", "@android:color/system_accent1_700");
        values.put("m3_on_primary_container", "@android:color/system_accent1_100");
        values.put("m3_surface", "@android:color/system_neutral1_900");
        values.put("m3_on_surface", "@android:color/system_neutral1_100");
        values.put("m3_surface_variant", "@android:color/system_neutral2_700");
        values.put("m3_on_surface_variant", "@android:color/system_neutral2_200");
        values.put("m3_outline", "@android:color/system_neutral2_400");
        values.put("m3_outline_variant", "@android:color/system_neutral2_600");
        return values;
    }

    private static Map<String, String> expectedSemanticColorAliases() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("window_background", "@color/m3_surface");
        values.put("grid_dark_background", "@color/m3_surface");
        values.put("bubble_dark_background", "@color/m3_surface_variant");
        values.put("delete_color_filter", "@color/m3_error");
        values.put("appwidget_error_color", "@color/m3_error");
        values.put("snag_callout_color", "@color/m3_primary");
        values.put("gesture_color", "@color/m3_primary");
        values.put("uncertain_gesture_color", "@color/m3_outline");
        values.put("preferences_default_general_selector_colour_pressed", "@color/m3_primary");
        values.put("preferences_default_general_selector_colour_focused", "@color/m3_primary");
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
        return parseValues("values", relativePath, tagName, namePrefix);
    }

    private static Map<String, String> parseValues(String resourceDirectory, String relativePath,
            String tagName, String namePrefix) throws Exception {
        NodeList nodes = parse(resourceDirectory, relativePath).getDocumentElement()
                .getElementsByTagName(tagName);
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

    private static void assertColorStateRole(String resourceName, String expectedColor,
            String expectedAlpha) throws Exception {
        Element selector = parse("color", resourceName + ".xml").getDocumentElement();
        assertEquals("selector", selector.getTagName());
        NodeList items = selector.getElementsByTagName("item");
        assertEquals("Unexpected state count: " + resourceName, 1, items.getLength());
        Element item = (Element) items.item(0);
        assertEquals("Unexpected color role: " + resourceName,
                expectedColor, item.getAttribute("android:color"));
        assertEquals("Unexpected alpha: " + resourceName,
                expectedAlpha, item.getAttribute("android:alpha"));
    }

    private static void assertRequiredNames(Map<String, String> values, String... names) {
        for (String name : names) {
            assertTrue("Missing resource token: " + name, values.containsKey(name));
        }
    }

    private static void assertPositiveDimensions(Map<String, String> dimensions) {
        for (Map.Entry<String, String> entry : dimensions.entrySet()) {
            assertTrue("Dimension must be non-negative: " + entry.getKey(),
                    parseDimension(entry.getValue()) >= 0f);
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
        return parse("values", relativePath);
    }

    private static Document parse(String resourceDirectory, String relativePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(resourceFile(resourceDirectory, relativePath));
    }

    private static File resourceFile(String resourceDirectory, String relativePath) {
        File workingDirectory = new File(System.getProperty("user.dir"));
        File moduleResource = new File(workingDirectory,
                "src/main/res/" + resourceDirectory + "/" + relativePath);
        if (moduleResource.isFile()) {
            return moduleResource;
        }
        return new File(workingDirectory,
                "app/src/main/res/" + resourceDirectory + "/" + relativePath);
    }
}
