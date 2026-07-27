package org.zmreborn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SettingsResourceContractTest {
    private static final String[][] PREFERENCE_CONTRACT = {
            {"preferences_key_general_fullscreen", "general_fullscreen", "@string/preferences_default_general_fullscreen", "preferences_summary_general_fullscreen"},
            {"preferences_key_general_sensor_orientation", "general_orientation_mode", "@string/preferences_default_general_sensor_orientation", "preferences_summary_general_sensor_orientation"},
            {"preferences_key_application_language", "application_language", "@string/preferences_default_application_language", "preferences_summary_application_language"},
            {"preferences_key_general_selector_colour_pressed", "general_selector_colour_pressed", "@color/preferences_default_general_selector_colour_pressed", "preferences_summary_general_selector_colour_pressed"},
            {"preferences_key_general_selector_colour_focused", "general_selector_colour_focused", "@color/preferences_default_general_selector_colour_focused", "preferences_summary_general_selector_colour_focused"},
            {"preferences_key_workspace_number_of_screens", "workspace_number_of_screens", "@string/preferences_default_workspace_number_of_screens", "preferences_summary_workspace_number_of_screens"},
            {"preferences_key_workspace_default_screen", "workspace_default_screen", "@string/preferences_default_workspace_default_screen", "preferences_summary_workspace_default_screen"},
            {"preferences_key_workspace_screen_indicator_type", "workspace_screen_indicator", "@string/preferences_default_workspace_screen_indicator_type", "preferences_summary_workspace_screen_indicator_type"},
            {"preferences_key_workspace_elastic_scrolling", "workspace_elastic_scrolling", "@string/preferences_default_workspace_elastic_scrolling", "preferences_summary_workspace_elastic_scrolling"},
            {"preferences_key_workspace_screen_looping", "workspace_screen_looping", "@string/preferences_default_workspace_screen_looping", "preferences_summary_workspace_screen_looping"},
            {"preferences_key_workspace_content_grid_columns", "workspace_content_grid_columns", "@string/preferences_default_workspace_content_grid_columns", "preferences_summary_workspace_content_grid_columns"},
            {"preferences_key_workspace_content_grid_rows", "workspace_content_grid_rows", "@string/preferences_default_workspace_content_grid_rows", "preferences_summary_workspace_content_grid_rows"},
            {"preferences_key_workspace_content_grid_auto_fit", "workspace_content_grid_auto_fit", "@string/preferences_default_workspace_content_grid_auto_fit", "preferences_summary_workspace_content_grid_auto_fit"},
            {"preferences_key_workspace_show_shortcut_titles", "draw_shortcut_titles", "@string/preferences_default_workspace_show_shortcut_titles", "preferences_summary_workspace_show_shortcut_titles"},
            {"preferences_key_workspace_manage_wallpaper", "workspace_manage_wallpaper", null, "preferences_summary_workspace_manage_wallpaper"},
            {"preferences_key_workspace_scroll_wallpaper", "workspace_scroll_wallpaper", "@string/preferences_default_workspace_scroll_wallpaper", "preferences_summary_workspace_scroll_wallpaper"},
            {"preferences_key_apps_grid_type", "apps_grid_type", "@string/preferences_default_apps_grid_type", "preferences_summary_apps_grid_type"},
            {"preferences_key_apps_grid_animated", "apps_grid_animated", "@string/preferences_default_apps_grid_animated", "preferences_summary_apps_grid_animated"},
            {"preferences_key_apps_grid_remember_position", "apps_grid_remember_pos", "@string/preferences_default_apps_grid_remember_position", "preferences_summary_apps_grid_remember_position"},
            {"preferences_key_apps_grid_bg_alpha", "apps_grid_alpha", "@string/preferences_default_apps_grid_bg_alpha", "preferences_summary_apps_grid_bg_alpha"},
            {"preferences_key_apps_grid_content_rows_port", "apps_grid_content_rows_port", null, "preferences_summary_apps_grid_content_rows_portrait"},
            {"preferences_key_apps_grid_content_columns_port", "apps_grid_content_columns_port", null, "preferences_summary_apps_grid_content_columns_portrait"},
            {"preferences_key_apps_grid_content_rows_land", "apps_grid_content_rows_land", null, "preferences_summary_apps_grid_content_rows_landscape"},
            {"preferences_key_apps_grid_content_columns_land", "apps_grid_content_columns_land", null, "preferences_summary_apps_grid_content_columns_landscape"},
            {"preferences_key_action_swipe_up", "action_swipe_up", "@string/preferences_default_action_swipe_up", "preferences_summary_action_swipe_up"},
            {"preferences_key_action_swipe_down", "action_swipe_down", "@string/preferences_default_action_swipe_down", "preferences_summary_action_swipe_down"},
            {"preferences_key_action_home_button", "action_home_button", "@string/preferences_default_action_home_button", "preferences_summary_action_home_button"},
            {"preferences_key_action_double_tap", "action_double_tap", "@string/preferences_default_action_double_tap", "preferences_summary_action_double_tap"},
            {"preferences_key_dock_reset_home", "dock_reset_home", "@string/preferences_default_dock_reset_home", "preferences_summary_dock_reset_home"},
            {"preferences_key_dock_reset_to", "dock_reset_to", "@string/preferences_default_dock_reset_to", "preferences_summary_dock_reset_to"},
            {"preferences_key_dock_background", "dock_bg", "@string/preferences_default_dock_background", "preferences_summary_dock_background"},
            {"preferences_key_dock_item_alignment", "dock_item_alignment", "@string/preferences_default_dock_item_alignment", "preferences_summary_dock_item_alignment"},
            {"preferences_key_dock_item_width", "dock_item_width", "@string/preferences_default_dock_item_width", "preferences_summary_dock_item_width"},
            {"preferences_key_restart", "restart", null, null},
            {"preferences_key_reset", "reset", null, "preferences_summary_reset"},
            {"preferences_key_application", "application", null, "application_copyright"}
    };

    @Test
    public void preferencesKeepAllKeysDefaultsStoredValuesAndSummaries() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        Map<String, String> strings = values("main/res/values/strings.xml", "string");
        assertEquals(36, countPreferences(root));
        assertFalse("Preference dependencies must remain explicit in Java", containsDependency(root));
        assertEquals(PREFERENCE_CONTRACT.length, countPreferences(root));
        for (String[] contract : PREFERENCE_CONTRACT) {
            Element preference = findPreference(root, contract[0]);
            assertNotNull("Missing preference: " + contract[0], preference);
            assertEquals("@string/" + contract[0], preference.getAttribute("android:key"));
            assertEquals(contract[1], strings.get(contract[0]));
            if (contract[3] == null) {
                assertFalse("Unexpected summary for " + contract[0], preference.hasAttribute("android:summary"));
            } else {
                assertEquals("@string/" + contract[3], preference.getAttribute("android:summary"));
            }
            if (contract[2] == null) {
                assertFalse("Unexpected default for " + contract[0], preference.hasAttribute("android:defaultValue"));
            } else {
                assertEquals(contract[2], preference.getAttribute("android:defaultValue"));
            }
        }
    }

    @Test
    public void preferenceCollectionsKeepStoredEntryValues() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        assertListBinding(root, "preferences_key_application_language", "preferences_entries_application_languages", "preferences_values_application_languages");
        assertListBinding(root, "preferences_key_workspace_screen_indicator_type", "preferences_entries_workspace_screen_indicator_types", "preferences_values_workspace_screen_indicator_types");
        assertListBinding(root, "preferences_key_apps_grid_type", "preferences_entries_apps_grid_types", "preferences_entry_values_apps_grid_types");
        assertListBinding(root, "preferences_key_action_swipe_up", "preferences_entries_actions", "preferences_entry_values_actions");
        assertListBinding(root, "preferences_key_dock_background", "preferences_entries_dock_backgrounds", "preferences_values_dock_backgrounds");
        assertListBinding(root, "preferences_key_dock_item_alignment", "preferences_entries_dock_item_alignments_resets", "preferences_values_dock_item_alignments_resets");
        assertListBinding(root, "preferences_key_dock_item_width", "preferences_entries_dock_item_widths", "preferences_values_dock_item_widths");
    }

    @Test
    public void preferenceRangesAndDynamicRangesRemainBounded() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        assertRange(root, "preferences_key_workspace_number_of_screens", "1", "7");
        assertRange(root, "preferences_key_workspace_default_screen", "1", "7");
        assertRange(root, "preferences_key_workspace_content_grid_columns", "3", "8");
        assertRange(root, "preferences_key_workspace_content_grid_rows", "3", "8");
        Element alpha = findPreference(root, "preferences_key_apps_grid_bg_alpha");
        assertEquals("255", alpha.getAttribute("launcher:max"));
        assertFalse(alpha.hasAttribute("launcher:min"));

        String preferences = read("main/java/org/zmreborn/Preferences.java");
        assertTrue(preferences.contains("appsGridContentColumnsPortrait.setMin(4)"));
        assertTrue(preferences.contains("appsGridContentColumnsPortrait.setMax(6)"));
        assertTrue(preferences.contains("appsGridContentColumnsLandscape.setMin(4)"));
        assertTrue(preferences.contains("appsGridContentColumnsLandscape.setMax(6)"));
        assertTrue(preferences.contains("appsGridContentRowsPortrait.setMin(1)"));
        assertTrue(preferences.contains("appsGridContentRowsPortrait.setMax(6)"));
        assertTrue(preferences.contains("appsGridContentRowsLandscape.setMin(1)"));
        assertTrue(preferences.contains("appsGridContentRowsLandscape.setMax(5)"));
        assertTrue(preferences.contains("appsGridContentColumnsLandscape.setMax(8)"));
        assertTrue(preferences.contains("preferences_key_apps_grid_vertical_scrolling_content_columns_port"));
        assertTrue(preferences.contains("preferences_key_apps_grid_vertical_scrolling_content_columns_land"));
        assertTrue(preferences.contains("preferences_key_apps_grid_horizontal_paging_content_rows_port"));
        assertTrue(preferences.contains("preferences_key_apps_grid_horizontal_paging_content_columns_port"));
        assertTrue(preferences.contains("preferences_key_apps_grid_horizontal_paging_content_rows_land"));
        assertTrue(preferences.contains("preferences_key_apps_grid_horizontal_paging_content_columns_land"));
        String preferencesUtil = read("main/java/org/zmreborn/PreferencesUtil.java");
        assertTrue(preferencesUtil.contains("preferences_key_apps_grid_vertical_scrolling_content_columns_port"));
        assertTrue(preferencesUtil.contains("preferences_key_apps_grid_vertical_scrolling_content_columns_land"));
        assertTrue(preferencesUtil.contains("preferences_key_apps_grid_horizontal_paging_content_rows_port"));
        assertTrue(preferencesUtil.contains("preferences_key_apps_grid_horizontal_paging_content_columns_port"));
        assertTrue(preferencesUtil.contains("preferences_key_apps_grid_horizontal_paging_content_rows_land"));
        assertTrue(preferencesUtil.contains("preferences_key_apps_grid_horizontal_paging_content_columns_land"));
    }

    @Test
    public void settingsSurfacesKeepBindingGeometryAndFocusTreatment() throws Exception {
        String row = read("main/res/layout/settings_preference.xml");
        assertTrue(row.contains("@android:id/title"));
        assertTrue(row.contains("@android:id/summary"));
        assertTrue(row.contains("@android:id/widget_frame"));
        assertTrue(row.contains("@dimen/settings_row_height"));
        assertTrue(row.contains("@color/zm_reborn_fog"));
        assertTrue(row.contains("@color/zm_reborn_steel"));
        assertTrue(row.contains("@style/TextAppearance.ZmReborn.Title"));
        assertTrue(row.contains("@style/TextAppearance.ZmReborn.Label"));
        assertTrue(row.contains("android:clickable=\"false\""));
        assertTrue(row.contains("android:focusable=\"false\""));
        String checkbox = read("main/res/layout/settings_checkbox_widget.xml");
        assertTrue(checkbox.contains("@android:id/checkbox"));
        assertTrue(checkbox.contains("@dimen/minimum_touch_target"));
        assertTrue(checkbox.contains("android:clickable=\"false\""));
        assertTrue(checkbox.contains("android:focusable=\"false\""));
        String category = read("main/res/layout/settings_preference_category.xml");
        assertTrue(category.contains("@color/zm_reborn_amber"));
        assertTrue(category.contains("@dimen/settings_row_height"));
        assertTrue(category.contains("@style/TextAppearance.ZmReborn.Category"));
        String selector = read("main/res/drawable/settings_preference_selector.xml");
        assertTrue(selector.contains("android:state_selected=\"true\""));
        assertTrue(selector.contains("android:state_focused=\"true\""));
        assertTrue(selector.contains("@color/zm_reborn_amber"));
        assertFalse(selector.toLowerCase().contains("gradient"));
        assertFalse(selector.toLowerCase().contains("shadow"));
    }

    @Test
    public void destructiveResetAndAboutRowsKeepOriginalBehavior() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        Element reset = findPreference(root, "preferences_key_reset");
        assertEquals("org.zmreborn.SettingsPreference", reset.getTagName());
        assertEquals("Preference", findPreference(root, "preferences_key_application").getTagName());
        String preferences = read("main/java/org/zmreborn/Preferences.java");
        assertTrue(preferences.contains("applicationPreference.setSelectable(false)"));
        assertTrue(preferences.contains("preferences_confirm_reset"));
        assertTrue(preferences.contains("resetAlertRestart()"));
        assertTrue(preferences.contains("getPreferencesFile(this).delete()"));
        String resetStyle = read("main/java/org/zmreborn/SettingsPreference.java");
        assertTrue(resetStyle.contains("R.color.zm_reborn_ember"));
    }

    @Test
    public void wallpaperAndWidgetSurfacesPreserveSelectionAndBinding() throws Exception {
        String wallpaper = read("main/res/layout/wallpaper_chooser.xml");
        assertTrue(wallpaper.contains("@color/zm_reborn_slate"));
        assertTrue(wallpaper.contains("@drawable/settings_preference_selector"));
        assertTrue(wallpaper.contains("@dimen/minimum_touch_target"));
        assertTrue(wallpaper.contains("@+id/gallery"));
        String wallpaperItem = read("main/res/layout/wallpaper_item.xml");
        assertTrue(wallpaperItem.contains("@drawable/settings_preference_selector"));
        assertTrue(wallpaperItem.contains("@dimen/minimum_touch_target"));
        String chooser = read("main/java/org/zmreborn/WallpaperChooser.java");
        assertTrue(chooser.contains("wallpaperManager.setResource"));
        assertTrue(chooser.contains("isWallpaperPosition"));
        String span = read("main/res/layout/widget_span.xml");
        assertTrue(span.contains("@+id/widget_columns_span"));
        assertTrue(span.contains("@+id/widget_rows_span"));
        assertTrue(span.contains("@color/zm_reborn_slate"));
        String error = read("main/res/layout/appwidget_error.xml");
        assertTrue(error.contains("@color/zm_reborn_ember"));
        assertFalse(error.contains("@drawable/bg_appwidget_error"));
        String launcher = read("main/java/org/zmreborn/Launcher.java");
        assertTrue(launcher.contains("R.layout.widget_span"));
        assertTrue(launcher.contains("R.id.widget_columns_span"));
        assertTrue(launcher.contains("R.id.widget_rows_span"));
        assertTrue(launcher.contains("hostView.setAppWidget"));
    }

    private static void assertListBinding(Element root, String key, String entries, String values) {
        Element preference = findPreference(root, key);
        assertEquals("@array/" + entries, preference.getAttribute("android:entries"));
        assertEquals("@array/" + values, preference.getAttribute("android:entryValues"));
    }

    private static void assertRange(Element root, String key, String min, String max) {
        Element preference = findPreference(root, key);
        assertEquals(min, preference.getAttribute("launcher:min"));
        assertEquals(max, preference.getAttribute("launcher:max"));
    }

    private static Element findPreference(Element root, String keyResource) {
        NodeList nodes = root.getElementsByTagName("*");
        String expected = "@string/" + keyResource;
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            if (expected.equals(element.getAttribute("android:key"))) {
                return element;
            }
        }
        fail("Missing preference: " + keyResource);
        return null;
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

    private static Map<String, String> values(String relativePath, String tagName) throws Exception {
        NodeList nodes = parse(relativePath).getElementsByTagName(tagName);
        Map<String, String> result = new HashMap<String, String>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            result.put(element.getAttribute("name"), element.getTextContent());
        }
        return result;
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

    private static String read(String relativePath) throws Exception {
        File file = resourceFile(relativePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));
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
        File moduleResource = new File(workingDirectory, "src/" + relativePath);
        if (moduleResource.isFile()) {
            return moduleResource;
        }
        return new File(workingDirectory, "app/src/" + relativePath);
    }
}
