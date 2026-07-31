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
    private static final String[][] BOOLEAN_PREFERENCE_CONTRACT = {
            {"preferences_key_general_fullscreen", "false"},
            {"preferences_key_general_sensor_orientation", "false"},
            {"preferences_key_workspace_elastic_scrolling", "true"},
            {"preferences_key_workspace_screen_looping", "true"},
            {"preferences_key_workspace_content_grid_auto_fit", "false"},
            {"preferences_key_workspace_show_shortcut_titles", "true"},
            {"preferences_key_workspace_manage_wallpaper", "true"},
            {"preferences_key_workspace_scroll_wallpaper", "true"},
            {"preferences_key_apps_grid_animated", "true"},
            {"preferences_key_apps_grid_remember_position", "true"},
            {"preferences_key_dock_reset_home", "true"}
    };

    private static final String[][] PREFERENCE_CONTRACT = {
            {"preferences_key_general_fullscreen", "general_fullscreen", "preferences_title_general_fullscreen", "@string/preferences_default_general_fullscreen", "preferences_summary_general_fullscreen"},
            {"preferences_key_general_sensor_orientation", "general_orientation_mode", "preferences_title_general_sensor_orientation", "@string/preferences_default_general_sensor_orientation", "preferences_summary_general_sensor_orientation"},
            {"preferences_key_application_language", "application_language", "preferences_title_application_language", "@string/preferences_default_application_language", "preferences_summary_application_language"},
            {"preferences_key_application_appearance", "application_appearance", "preferences_title_application_appearance", "@string/preferences_default_application_appearance", "preferences_summary_application_appearance"},
            {"preferences_key_workspace_number_of_screens", "workspace_number_of_screens", "preferences_title_workspace_number_of_screens", "@string/preferences_default_workspace_number_of_screens", "preferences_summary_workspace_number_of_screens"},
            {"preferences_key_workspace_default_screen", "workspace_default_screen", "preferences_title_workspace_default_screen", "@string/preferences_default_workspace_default_screen", "preferences_summary_workspace_default_screen"},
            {"preferences_key_workspace_screen_indicator_type", "workspace_screen_indicator", "preferences_title_workspace_indicator_type", "@string/preferences_default_workspace_screen_indicator_type", "preferences_summary_workspace_screen_indicator_type"},
            {"preferences_key_workspace_elastic_scrolling", "workspace_elastic_scrolling", "preferences_title_workspace_elastic_scrolling", "@string/preferences_default_workspace_elastic_scrolling", "preferences_summary_workspace_elastic_scrolling"},
            {"preferences_key_workspace_screen_looping", "workspace_screen_looping", "preferences_title_workspace_screen_looping", "@string/preferences_default_workspace_screen_looping", "preferences_summary_workspace_screen_looping"},
            {"preferences_key_workspace_content_grid_columns", "workspace_content_grid_columns", "preferences_title_workspace_content_grid_columns", "@string/preferences_default_workspace_content_grid_columns", "preferences_summary_workspace_content_grid_columns"},
            {"preferences_key_workspace_content_grid_rows", "workspace_content_grid_rows", "preferences_title_workspace_content_grid_rows", "@string/preferences_default_workspace_content_grid_rows", "preferences_summary_workspace_content_grid_rows"},
            {"preferences_key_workspace_content_grid_auto_fit", "workspace_content_grid_auto_fit", "preferences_title_workspace_content_grid_auto_fit", "@string/preferences_default_workspace_content_grid_auto_fit", "preferences_summary_workspace_content_grid_auto_fit"},
            {"preferences_key_workspace_show_shortcut_titles", "draw_shortcut_titles", "preferences_title_workspace_show_shortcut_titles", "@string/preferences_default_workspace_show_shortcut_titles", "preferences_summary_workspace_show_shortcut_titles"},
            {"preferences_key_workspace_manage_wallpaper", "workspace_manage_wallpaper", "preferences_title_workspace_manage_wallpaper", "@string/preferences_default_workspace_manage_wallpaper", "preferences_summary_workspace_manage_wallpaper"},
            {"preferences_key_workspace_scroll_wallpaper", "workspace_scroll_wallpaper", "preferences_title_workspace_scroll_wallpaper", "@string/preferences_default_workspace_scroll_wallpaper", "preferences_summary_workspace_scroll_wallpaper"},
            {"preferences_key_apps_grid_type", "apps_grid_type", "preferences_title_apps_grid_type", "@string/preferences_default_apps_grid_type", "preferences_summary_apps_grid_type"},
            {"preferences_key_apps_grid_density", "apps_grid_density", "preferences_title_apps_grid_density", "@string/preferences_default_apps_grid_density", "preferences_summary_apps_grid_density"},
            {"preferences_key_apps_grid_animated", "apps_grid_animated", "preferences_title_apps_grid_animated", "@string/preferences_default_apps_grid_animated", "preferences_summary_apps_grid_animated"},
            {"preferences_key_apps_grid_remember_position", "apps_grid_remember_pos", "preferences_title_apps_grid_remember_position", "@string/preferences_default_apps_grid_remember_position", "preferences_summary_apps_grid_remember_position"},
            {"preferences_key_apps_grid_bg_alpha", "apps_grid_alpha", "preferences_title_apps_grid_bg_alpha", "@string/preferences_default_apps_grid_bg_alpha", "preferences_summary_apps_grid_bg_alpha"},
            {"preferences_key_apps_grid_content_rows_port", "apps_grid_content_rows_port", "preferences_title_apps_grid_content_rows_portrait", null, "preferences_summary_apps_grid_content_rows_portrait"},
            {"preferences_key_apps_grid_content_columns_port", "apps_grid_content_columns_port", "preferences_title_apps_grid_content_columns_portrait", null, "preferences_summary_apps_grid_content_columns_portrait"},
            {"preferences_key_apps_grid_content_rows_land", "apps_grid_content_rows_land", "preferences_title_apps_grid_content_rows_landscape", null, "preferences_summary_apps_grid_content_rows_landscape"},
            {"preferences_key_apps_grid_content_columns_land", "apps_grid_content_columns_land", "preferences_title_apps_grid_content_columns_landscape", null, "preferences_summary_apps_grid_content_columns_landscape"},
            {"preferences_key_action_swipe_up", "action_swipe_up", "preferences_title_action_swipe_up", "@string/preferences_default_action_swipe_up", "preferences_summary_action_swipe_up"},
            {"preferences_key_action_swipe_down", "action_swipe_down", "preferences_title_action_swipe_down", "@string/preferences_default_action_swipe_down", "preferences_summary_action_swipe_down"},
            {"preferences_key_action_home_button", "action_home_button", "preferences_title_action_home_button", "@string/preferences_default_action_home_button", "preferences_summary_action_home_button"},
            {"preferences_key_action_double_tap", "action_double_tap", "preferences_title_action_double_tap", "@string/preferences_default_action_double_tap", "preferences_summary_action_double_tap"},
            {"preferences_key_dock_reset_home", "dock_reset_home", "preferences_title_dock_reset_home", "@string/preferences_default_dock_reset_home", "preferences_summary_dock_reset_home"},
            {"preferences_key_dock_reset_to", "dock_reset_to", "preferences_title_dock_reset_to", "@string/preferences_default_dock_reset_to", "preferences_summary_dock_reset_to"},
            {"preferences_key_dock_background", "dock_bg", "preferences_title_dock_background", "@string/preferences_default_dock_background", "preferences_summary_dock_background"},
            {"preferences_key_dock_item_alignment", "dock_item_alignment", "preferences_title_dock_item_alignment", "@string/preferences_default_dock_item_alignment", "preferences_summary_dock_item_alignment"},
            {"preferences_key_dock_item_width", "dock_item_width", "preferences_title_dock_item_width", "@string/preferences_default_dock_item_width", "preferences_summary_dock_item_width"},
            {"preferences_key_restart", "restart", "preferences_title_restart", null, null},
            {"preferences_key_reset", "reset", "preferences_title_reset", null, "preferences_summary_reset"},
            {"preferences_key_application", "application", null, null, "application_copyright"}
    };

    @Test
    public void preferencesKeepAllKeysDefaultsStoredValuesAndSummaries() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        Map<String, String> strings = values("main/res/values/strings.xml", "string");
        strings.putAll(values("main/res/values/defaults.xml", "string"));
        assertEquals(36, countPreferences(root));
        assertFalse("Preference dependencies must remain explicit in Java", containsDependency(root));
        assertEquals(PREFERENCE_CONTRACT.length, countPreferences(root));
        for (String[] contract : PREFERENCE_CONTRACT) {
            Element preference = findPreference(root, contract[0]);
            assertNotNull("Missing preference: " + contract[0], preference);
            assertEquals("@string/" + contract[0], preference.getAttribute("android:key"));
            assertEquals(contract[1], strings.get(contract[0]));
            if (contract[2] == null) {
                assertFalse("Unexpected title for " + contract[0], preference.hasAttribute("android:title"));
            } else {
                assertEquals("@string/" + contract[2], preference.getAttribute("android:title"));
            }
            if (contract[4] == null) {
                assertFalse("Unexpected summary for " + contract[0], preference.hasAttribute("android:summary"));
            } else {
                assertEquals("@string/" + contract[4], preference.getAttribute("android:summary"));
            }
            if (contract[3] == null) {
                assertFalse("Unexpected default for " + contract[0], preference.hasAttribute("android:defaultValue"));
            } else {
                assertEquals(contract[3], preference.getAttribute("android:defaultValue"));
            }
        }
    }

    @Test
    public void booleanPreferencesUsePlatformSwitches() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        Map<String, String> strings = values("main/res/values/strings.xml", "string");
        strings.putAll(values("main/res/values/defaults.xml", "string"));
        assertEquals(BOOLEAN_PREFERENCE_CONTRACT.length,
                root.getElementsByTagName("SwitchPreference").getLength());
        for (String[] contract : BOOLEAN_PREFERENCE_CONTRACT) {
            Element preference = findPreference(root, contract[0]);
            assertEquals("SwitchPreference", preference.getTagName());
            assertBooleanDefault(preference, strings, contract[0], contract[1]);
        }
        assertEquals(0, root.getElementsByTagName("CheckBoxPreference").getLength());

        Element styles = parse("main/res/values/styles.xml").getDocumentElement();
        Element settingsTheme = findNamedElement(styles, "style", "SettingsTheme");
        assertEquals("@style/SettingsPreferenceStyle",
                styleItem(settingsTheme, "android:preferenceStyle"));
        assertEquals("@style/SettingsPreferenceStyle",
                styleItem(settingsTheme, "android:dialogPreferenceStyle"));
        Element preferenceStyle = findNamedElement(styles, "style", "SettingsPreferenceStyle");
        assertEquals("@layout/settings_preference", styleItem(preferenceStyle, "android:layout"));
        assertEquals("@style/SettingsPreferenceCategoryStyle",
                styleItem(settingsTheme, "android:preferenceCategoryStyle"));
        Element preferenceCategoryStyle = findNamedElement(styles, "style",
                "SettingsPreferenceCategoryStyle");
        assertEquals("@layout/settings_preference_category",
                styleItem(preferenceCategoryStyle, "android:layout"));
        assertEquals("@style/SettingsSwitchPreferenceStyle",
                styleItem(settingsTheme, "android:switchPreferenceStyle"));
        Element switchPreferenceStyle = findNamedElement(styles, "style",
                "SettingsSwitchPreferenceStyle");
        assertEquals("@layout/settings_preference",
                styleItem(switchPreferenceStyle, "android:layout"));
        assertEquals("@layout/settings_switch_widget",
                styleItem(switchPreferenceStyle, "android:widgetLayout"));

        Element manifest = parse("main/AndroidManifest.xml").getDocumentElement();
        Element preferencesActivity = findElementByAttribute(manifest, "android:name",
                ".Preferences");
        assertEquals("activity", preferencesActivity.getTagName());
        assertEquals("@style/SettingsTheme", preferencesActivity.getAttribute("android:theme"));

        assertColorStateList("main/res/color/settings_switch_thumb_tint.xml",
                "@color/m3_outline_variant", "@color/m3_on_primary", "@color/m3_outline");
        assertColorStateList("main/res/color/settings_switch_track_tint.xml",
                "@color/m3_surface_variant", "@color/m3_primary", "@color/m3_surface_variant");
    }

    @Test
    public void preferenceCollectionsKeepStoredEntryValues() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        assertListBinding(root, "preferences_key_application_language", "preferences_entries_application_languages", "preferences_values_application_languages");
        assertListBinding(root, "preferences_key_application_appearance", "preferences_entries_application_appearances", "preferences_values_application_appearances");
        assertListBinding(root, "preferences_key_workspace_screen_indicator_type", "preferences_entries_workspace_screen_indicator_types", "preferences_values_workspace_screen_indicator_types");
        assertListBinding(root, "preferences_key_apps_grid_type", "preferences_entries_apps_grid_types", "preferences_entry_values_apps_grid_types");
        assertListBinding(root, "preferences_key_apps_grid_density", "preferences_entries_apps_grid_densities", "preferences_entry_values_apps_grid_densities");
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
        assertTrue(preferences.contains("appsGridDensity.setEnabled(appsGridType == 1)"));
        assertTrue(preferences.contains("appsGridContentColumnsPortrait.setEnabled(false)"));
        assertTrue(preferences.contains("appsGridContentColumnsLandscape.setEnabled(false)"));
        assertTrue(preferences.contains("appsGridContentRowsPortrait.setMin(1)"));
        assertTrue(preferences.contains("appsGridContentRowsPortrait.setMax(6)"));
        assertTrue(preferences.contains("appsGridContentRowsLandscape.setMin(1)"));
        assertTrue(preferences.contains("appsGridContentRowsLandscape.setMax(5)"));
        assertTrue(preferences.contains("appsGridContentColumnsLandscape.setMax(8)"));
        assertFalse(preferences.contains("preferences_key_apps_grid_vertical_scrolling_content_columns_port"));
        assertFalse(preferences.contains("preferences_key_apps_grid_vertical_scrolling_content_columns_land"));
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
    public void numericPreferencesUseInlineDebouncedControls() throws Exception {
        Element root = parse("main/res/xml/preferences.xml").getDocumentElement();
        String[] stepperKeys = {
                "preferences_key_workspace_number_of_screens",
                "preferences_key_workspace_default_screen",
                "preferences_key_workspace_content_grid_columns",
                "preferences_key_workspace_content_grid_rows",
                "preferences_key_apps_grid_content_columns_port",
                "preferences_key_apps_grid_content_rows_port",
                "preferences_key_apps_grid_content_columns_land",
                "preferences_key_apps_grid_content_rows_land"
        };
        for (String key : stepperKeys) {
            Element preference = findPreference(root, key);
            assertEquals("org.zmreborn.InlineStepperPreference", preference.getTagName());
            assertFalse(preference.hasAttribute("android:dialogMessage"));
        }
        assertEquals(stepperKeys.length,
                root.getElementsByTagName("org.zmreborn.InlineStepperPreference").getLength());
        assertEquals(0,
                root.getElementsByTagName("org.zmreborn.DialogSeekBarPreference").getLength());

        Element transparency = findPreference(root, "preferences_key_apps_grid_bg_alpha");
        assertEquals("org.zmreborn.InlineSliderPreference", transparency.getTagName());
        assertFalse(transparency.hasAttribute("android:dialogMessage"));
        assertEquals("@string/preferences_category_appearance",
                ((Element) transparency.getParentNode()).getAttribute("android:title"));
        assertEquals("@string/preferences_screen_title_applications_grid",
                ((Element) transparency.getParentNode().getParentNode()).getAttribute("android:title"));

        String base = read("main/java/org/zmreborn/DebouncedIntegerPreference.java");
        assertTrue(base.contains("DEBOUNCE_MILLIS = 250L"));
        assertTrue(base.contains(
                "ATTRIBUTE_NAMESPACE = \"http://schemas.android.com/apk/res-auto\""));
        assertTrue(base.contains("attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, \"min\", 0)"));
        assertTrue(base.contains("attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, \"max\", 100)"));

        Element stepper = parse("main/res/layout/settings_stepper_widget.xml")
                .getDocumentElement();
        assertEquals("LinearLayout", stepper.getTagName());
        Element decrement = findElementByAttribute(stepper, "android:id", "@+id/decrement");
        Element increment = findElementByAttribute(stepper, "android:id", "@+id/increment");
        assertNumericActionGeometry(decrement);
        assertNumericActionGeometry(increment);
        Element value = findElementByAttribute(stepper, "android:id", "@+id/settings_numeric_value");
        assertEquals("@dimen/minimum_touch_target", value.getAttribute("android:minWidth"));
        assertEquals("@dimen/minimum_touch_target", value.getAttribute("android:minHeight"));

        Element sliderRow = parse("main/res/layout/settings_inline_slider_preference.xml")
                .getDocumentElement();
        Element slider = findElementByAttribute(sliderRow, "android:id",
                "@+id/settings_inline_slider");
        assertEquals("SeekBar", slider.getTagName());
        assertEquals("@dimen/minimum_touch_target", slider.getAttribute("android:minHeight"));

        Element standardRow = parse("main/res/layout/settings_preference.xml")
                .getDocumentElement();
        assertEquals("@dimen/space_16", standardRow.getAttribute("android:paddingStart"));
        assertEquals("@dimen/space_12", standardRow.getAttribute("android:paddingEnd"));
        assertFalse(standardRow.hasAttribute("android:paddingLeft"));
        assertFalse(standardRow.hasAttribute("android:paddingRight"));
        Element widgetFrame = findElementByAttribute(standardRow, "android:id",
                "@android:id/widget_frame");
        assertEquals("@dimen/space_8", widgetFrame.getAttribute("android:paddingStart"));
        assertFalse(widgetFrame.hasAttribute("android:paddingLeft"));

        assertEnabledColorStateList("main/res/color/settings_numeric_icon_tint.xml",
                "@color/m3_outline_variant", "@color/m3_primary");
    }

    @Test
    public void settingsSurfacesKeepBindingGeometryAndFocusTreatment() throws Exception {
        Element row = parse("main/res/layout/settings_preference.xml").getDocumentElement();
        assertEquals("LinearLayout", row.getTagName());
        assertEquals("@dimen/settings_row_height", row.getAttribute("android:minHeight"));
        assertEquals("@drawable/settings_preference_selector", row.getAttribute("android:background"));
        assertEquals("false", row.getAttribute("android:clickable"));
        assertEquals("false", row.getAttribute("android:focusable"));
        Element title = findElementByAttribute(row, "android:id", "@android:id/title");
        assertEquals("TextView", title.getTagName());
        assertEquals("@style/TextAppearance.ZmReborn.Title",
                title.getAttribute("android:textAppearance"));
        assertEquals("@color/m3_on_surface", title.getAttribute("android:textColor"));
        Element summary = findElementByAttribute(row, "android:id", "@android:id/summary");
        assertEquals("TextView", summary.getTagName());
        assertEquals("@style/TextAppearance.ZmReborn.Label",
                summary.getAttribute("android:textAppearance"));
        assertEquals("@color/m3_on_surface_variant", summary.getAttribute("android:textColor"));
        assertEquals("LinearLayout", findElementByAttribute(row, "android:id",
                "@android:id/widget_frame").getTagName());

        Element switchRoot = parse("main/res/layout/settings_switch_widget.xml").getDocumentElement();
        assertEquals("Switch", switchRoot.getTagName());
        assertEquals("@android:id/switch_widget", switchRoot.getAttribute("android:id"));
        assertEquals("@android:style/Widget.Material.CompoundButton.Switch",
                switchRoot.getAttribute("style"));
        assertEquals("@dimen/minimum_touch_target", switchRoot.getAttribute("android:layout_width"));
        assertEquals("@dimen/minimum_touch_target", switchRoot.getAttribute("android:layout_height"));
        assertEquals("@dimen/minimum_touch_target", switchRoot.getAttribute("android:minWidth"));
        assertEquals("@dimen/minimum_touch_target", switchRoot.getAttribute("android:minHeight"));
        assertEquals("@color/settings_switch_thumb_tint", switchRoot.getAttribute("android:thumbTint"));
        assertEquals("@color/settings_switch_track_tint", switchRoot.getAttribute("android:trackTint"));
        assertEquals("false", switchRoot.getAttribute("android:showText"));
        assertEquals("false", switchRoot.getAttribute("android:clickable"));
        assertEquals("false", switchRoot.getAttribute("android:focusable"));

        Element category = parse("main/res/layout/settings_preference_category.xml").getDocumentElement();
        assertEquals("TextView", category.getTagName());
        assertEquals("@android:id/title", category.getAttribute("android:id"));
        assertEquals("@color/m3_primary", category.getAttribute("android:textColor"));
        assertEquals("@dimen/settings_row_height", category.getAttribute("android:minHeight"));
        assertEquals("@style/TextAppearance.ZmReborn.Category",
                category.getAttribute("android:textAppearance"));

        Element selectorRoot = parse("main/res/drawable/settings_preference_selector.xml")
                .getDocumentElement();
        assertEquals("ripple", selectorRoot.getTagName());
        assertEquals("@color/m3_ripple_primary", selectorRoot.getAttribute("android:color"));
        assertEquals(2, directElementCount(selectorRoot));
        NodeList nestedSelectors = selectorRoot.getElementsByTagName("selector");
        assertEquals(1, nestedSelectors.getLength());
        Element nestedSelector = (Element) nestedSelectors.item(0);
        Node selectorContentItem = nestedSelector.getParentNode();
        assertTrue(selectorContentItem instanceof Element);
        assertEquals("item", ((Element) selectorContentItem).getTagName());
        assertEquals(selectorRoot, selectorContentItem.getParentNode());
        assertEquals(3, directElementCount(nestedSelector));
        Element selectedItem = directItemAt(nestedSelector, 0);
        assertEquals(1, selectedItem.getAttributes().getLength());
        assertEquals("true", selectedItem.getAttribute("android:state_selected"));
        assertPreferenceRowShape(selectedItem, "@color/m3_surface_variant");
        Element focusedItem = directItemAt(nestedSelector, 1);
        assertEquals(1, focusedItem.getAttributes().getLength());
        assertEquals("true", focusedItem.getAttribute("android:state_focused"));
        assertPreferenceRowShape(focusedItem, "@color/m3_surface_variant");
        Element defaultItem = directItemAt(nestedSelector, 2);
        assertEquals(0, defaultItem.getAttributes().getLength());
        assertUnstrokedPreferenceRowShape(defaultItem, "@android:color/transparent");
        String preferences = read("main/java/org/zmreborn/Preferences.java");
        assertTrue(preferences.contains("WallpaperColorExtractor.getSurface(this)"));
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
        assertTrue(preferences.contains("preferences.edit().clear().commit()"));
        assertFalse(preferences.contains("getPreferencesFile(this).delete()"));
        String resetStyle = read("main/java/org/zmreborn/SettingsPreference.java");
        assertTrue(resetStyle.contains("R.color.zm_reborn_ember"));
    }

    @Test
    public void drawerIndicatorPlacedInDragLayer() throws Exception {
        String portPaging = read("main/res/layout-port/apps_paging_view.xml");
        assertFalse("Drawer indicator must not live inside paging view (port)",
                portPaging.contains("apps_paging_screen_indicator"));

        String landPaging = read("main/res/layout-land/apps_paging_view.xml");
        assertFalse("Drawer indicator must not live inside paging view (land)",
                landPaging.contains("apps_paging_screen_indicator"));

        String portLauncher = read("main/res/layout-port/launcher.xml");
        assertFalse("Unified design: apps_paging_screen_indicator must not exist in DragLayer (port)",
                portLauncher.contains("apps_paging_screen_indicator"));
        assertTrue("Workspace indicator must remain in DragLayer (port)",
                portLauncher.contains("workspace_screen_indicator"));

        String landLauncher = read("main/res/layout-land/launcher.xml");
        assertFalse("Unified design: apps_paging_screen_indicator must not exist in DragLayer (land)",
                landLauncher.contains("apps_paging_screen_indicator"));
        assertTrue("Workspace indicator must remain in DragLayer (land)",
                landLauncher.contains("workspace_screen_indicator"));

        String pagingView = read("main/java/org/zmreborn/ApplicationsPagingView.java");
        assertTrue("ApplicationsPagingView must expose configureIndicator",
                pagingView.contains("configureIndicator("));
        assertFalse("ApplicationsPagingView must not self-find indicator by resource ID",
                pagingView.contains("R.id.apps_paging_screen_indicator"));

        String launcher = read("main/java/org/zmreborn/Launcher.java");
        assertTrue("Launcher must have resolveDrawerIndicatorType helper",
                launcher.contains("resolveDrawerIndicatorType"));
        assertTrue("Home button must be guarded for paging drawer",
                launcher.contains("instanceof ApplicationsPagingView"));
        assertFalse("Launcher must not reference removed apps_paging_screen_indicator ID",
                launcher.contains("R.id.apps_paging_screen_indicator"));
    }

    @Test
    public void defaultsPlacedInDedicatedFile() throws Exception {
        String[] expectedDefaults = {
            "preferences_default_general_fullscreen",
            "preferences_default_general_sensor_orientation",
            "preferences_default_workspace_number_of_screens",
            "preferences_default_workspace_default_screen",
            "preferences_default_workspace_elastic_scrolling",
            "preferences_default_workspace_screen_looping",
            "preferences_default_workspace_content_grid_rows",
            "preferences_default_workspace_content_grid_columns",
            "preferences_default_workspace_content_grid_auto_fit",
            "preferences_default_workspace_show_shortcut_titles",
            "preferences_default_workspace_manage_wallpaper",
            "preferences_default_workspace_scroll_wallpaper",
            "preferences_default_apps_grid_animated",
            "preferences_default_apps_grid_remember_position",
            "preferences_default_apps_grid_bg_alpha",
            "preferences_default_apps_grid_vertical_scrolling_content_columns_port",
            "preferences_default_apps_grid_vertical_scrolling_content_columns_land",
            "preferences_default_apps_grid_horizontal_paging_content_rows_port",
            "preferences_default_apps_grid_horizontal_paging_content_columns_port",
            "preferences_default_apps_grid_horizontal_paging_content_rows_land",
            "preferences_default_apps_grid_horizontal_paging_content_columns_land",
            "preferences_default_action_home_button",
            "preferences_default_action_swipe_up",
            "preferences_default_action_swipe_down",
            "preferences_default_action_double_tap",
            "preferences_default_workspace_screen_indicator_type",
            "preferences_default_apps_grid_type",
            "preferences_default_apps_grid_density",
            "preferences_default_dock_background",
            "preferences_default_dock_item_alignment",
            "preferences_default_dock_reset_to",
            "preferences_default_dock_item_width",
            "preferences_default_dock_reset_home",
            "preferences_default_application_language",
            "preferences_default_application_appearance",
        };
        Map<String, String> defaults = values("main/res/values/defaults.xml", "string");
        Map<String, String> strings = values("main/res/values/strings.xml", "string");
        assertEquals("defaults.xml must contain exactly 35 entries",
                expectedDefaults.length, defaults.size());
        for (String name : expectedDefaults) {
            assertTrue("defaults.xml must contain " + name, defaults.containsKey(name));
            assertFalse("strings.xml must not contain " + name, strings.containsKey(name));
        }
        String preferencesUtil = read("main/java/org/zmreborn/PreferencesUtil.java");
        assertFalse("PreferencesUtil must not hardcode manage_wallpaper default",
                preferencesUtil.contains("getBoolean(\n") && preferencesUtil.contains(", true)"));
        assertTrue("PreferencesUtil must reference manage_wallpaper default resource",
                preferencesUtil.contains("preferences_default_workspace_manage_wallpaper"));
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
        assertFalse(launcher.contains("R.layout.widget_span"));
        assertFalse(launcher.contains("R.id.widget_columns_span"));
        assertFalse(launcher.contains("R.id.widget_rows_span"));
        assertTrue(launcher.contains("rectToCell"));
        assertTrue(launcher.contains("updateAppWidgetOptions"));
        assertTrue(launcher.contains("RUNTIME_STATE_PENDING_APPWIDGET_ID"));
        assertTrue(launcher.contains("releasePendingAppWidgetId(data)"));
        assertTrue(launcher.contains("hostView.setAppWidget"));
    }

    private static void assertBooleanDefault(Element preference, Map<String, String> strings,
            String key, String expected) throws Exception {
        if (preference.hasAttribute("android:defaultValue")) {
            assertEquals(expected, resolveString(strings,
                    preference.getAttribute("android:defaultValue")));
            return;
        }
        fail("SwitchPreference " + key + " missing android:defaultValue");
    }

    private static String resolveString(Map<String, String> strings, String reference) {
        assertTrue("Boolean default must reference a string: " + reference,
                reference.startsWith("@string/"));
        String value = strings.get(reference.substring("@string/".length()));
        assertNotNull("Missing default string: " + reference, value);
        return value.trim();
    }

    private static Element findNamedElement(Element root, String tagName, String name) {
        Element matching = null;
        NodeList nodes = root.getElementsByTagName(tagName);
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            if (name.equals(element.getAttribute("name"))) {
                assertTrue("Duplicate " + tagName + ": " + name, matching == null);
                matching = element;
            }
        }
        assertNotNull("Missing " + tagName + ": " + name, matching);
        return matching;
    }

    private static String styleItem(Element style, String name) {
        return findNamedElement(style, "item", name).getTextContent().trim();
    }

    private static void assertNumericActionGeometry(Element action) {
        assertEquals("ImageButton", action.getTagName());
        assertEquals("@dimen/minimum_touch_target", action.getAttribute("android:layout_width"));
        assertEquals("@dimen/minimum_touch_target", action.getAttribute("android:layout_height"));
        assertEquals("@dimen/minimum_touch_target", action.getAttribute("android:minWidth"));
        assertEquals("@dimen/minimum_touch_target", action.getAttribute("android:minHeight"));
        assertEquals("@drawable/settings_numeric_action_background",
                action.getAttribute("android:background"));
        assertEquals("@color/settings_numeric_icon_tint", action.getAttribute("android:tint"));
        assertEquals("true", action.getAttribute("android:clickable"));
        assertEquals("true", action.getAttribute("android:focusable"));
    }

    private static void assertEnabledColorStateList(String relativePath, String disabledColor,
            String defaultColor) throws Exception {
        Element selector = parse(relativePath).getDocumentElement();
        assertEquals("selector", selector.getTagName());
        assertEquals(2, directElementCount(selector));
        assertColorState(directItemAt(selector, 0), "android:state_enabled", "false",
                disabledColor);
        Element defaultItem = directItemAt(selector, 1);
        assertEquals(1, defaultItem.getAttributes().getLength());
        assertEquals(defaultColor, defaultItem.getAttribute("android:color"));
        assertNoChildContent(defaultItem);
    }

    private static void assertColorStateList(String relativePath, String disabledColor,
            String checkedColor, String defaultColor) throws Exception {
        Element selector = parse(relativePath).getDocumentElement();
        assertEquals("selector", selector.getTagName());
        assertEquals(3, directElementCount(selector));
        assertColorState(directItemAt(selector, 0), "android:state_enabled", "false",
                disabledColor);
        assertColorState(directItemAt(selector, 1), "android:state_checked", "true",
                checkedColor);
        Element defaultItem = directItemAt(selector, 2);
        assertEquals(1, defaultItem.getAttributes().getLength());
        assertEquals(defaultColor, defaultItem.getAttribute("android:color"));
        assertNoChildContent(defaultItem);
    }

    private static void assertColorState(Element item, String stateName, String stateValue,
            String color) {
        assertEquals(2, item.getAttributes().getLength());
        assertEquals(stateValue, item.getAttribute(stateName));
        assertEquals(color, item.getAttribute("android:color"));
        assertNoChildContent(item);
    }

    private static Element directItemAt(Element parent, int index) {
        int directElementIndex = 0;
        NodeList children = parent.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            if (child instanceof Element && directElementIndex++ == index) {
                Element item = (Element) child;
                assertEquals("item", item.getTagName());
                return item;
            }
        }
        fail("Missing item at direct element index " + index);
        return null;
    }

    private static void assertNoChildContent(Element element) {
        assertEquals(0, element.getChildNodes().getLength());
    }

    private static void assertPreferenceRowShape(Element item, String fillColor) {
        assertEquals(1, directElementCount(item));
        Element shape = findDirectChild(item, "shape");
        assertEquals(2, directElementCount(shape));
        Element solid = findDirectChild(shape, "solid");
        assertEquals(1, solid.getAttributes().getLength());
        assertEquals(fillColor, solid.getAttribute("android:color"));
        Element stroke = findDirectChild(shape, "stroke");
        assertEquals(2, stroke.getAttributes().getLength());
        assertEquals("2dp", stroke.getAttribute("android:width"));
        assertEquals("@color/m3_primary", stroke.getAttribute("android:color"));
    }

    private static void assertUnstrokedPreferenceRowShape(Element item, String fillColor) {
        assertEquals(1, directElementCount(item));
        Element shape = findDirectChild(item, "shape");
        assertEquals(1, directElementCount(shape));
        Element solid = findDirectChild(shape, "solid");
        assertEquals(1, solid.getAttributes().getLength());
        assertEquals(fillColor, solid.getAttribute("android:color"));
    }

    private static Element findStateItem(Element selector, String stateName, String stateValue) {
        Element matching = null;
        NodeList children = selector.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element && "item".equals(((Element) child).getTagName())
                    && stateValue.equals(((Element) child).getAttribute(stateName))) {
                assertTrue("Duplicate selector item for " + stateName, matching == null);
                matching = (Element) child;
            }
        }
        assertNotNull("Missing selector item for " + stateName, matching);
        return matching;
    }

    private static Element findDefaultItem(Element selector) {
        Element matching = null;
        NodeList children = selector.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element && "item".equals(((Element) child).getTagName())
                    && !hasStateAttribute((Element) child)) {
                assertTrue("Duplicate default selector item", matching == null);
                matching = (Element) child;
            }
        }
        assertNotNull("Missing default selector item", matching);
        return matching;
    }

    private static boolean hasStateAttribute(Element element) {
        for (int index = 0; index < element.getAttributes().getLength(); index++) {
            if (element.getAttributes().item(index).getNodeName().startsWith("android:state_")) {
                return true;
            }
        }
        return false;
    }

    private static int directElementCount(Element parent) {
        int count = 0;
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element) {
                count++;
            }
        }
        return count;
    }

    private static Element findDirectChild(Element parent, String tagName) {
        Element matching = null;
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                assertTrue("Duplicate " + tagName + " child", matching == null);
                matching = (Element) child;
            }
        }
        assertNotNull("Missing " + tagName + " child", matching);
        return matching;
    }

    private static Element findElementByAttribute(Element root, String attribute, String value) {
        Element matching = null;
        if (value.equals(root.getAttribute(attribute))) {
            matching = root;
        }
        NodeList nodes = root.getElementsByTagName("*");
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            if (value.equals(element.getAttribute(attribute))) {
                assertTrue("Duplicate element with " + attribute + ": " + value,
                        matching == null);
                matching = element;
            }
        }
        assertNotNull("Missing element with " + attribute + ": " + value, matching);
        return matching;
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
