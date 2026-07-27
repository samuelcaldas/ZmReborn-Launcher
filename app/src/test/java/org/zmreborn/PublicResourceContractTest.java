package org.zmreborn;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PublicResourceContractTest {
    @Test
    public void publicResourcesKeepStableNameTypeAndIdContract() throws Exception {
        NodeList nodes = parse().getDocumentElement().getElementsByTagName("public");
        List<String> actualEntries = new ArrayList<String>();
        Set<String> names = new HashSet<String>();
        Set<Long> ids = new HashSet<Long>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            String type = element.getAttribute("type");
            String name = element.getAttribute("name");
            long id = Long.parseLong(element.getAttribute("id"));
            assertTrue("Duplicate public resource name: " + type + "/" + name,
                    names.add(type + "/" + name));
            assertTrue("Duplicate public resource ID: " + id, ids.add(id));
            actualEntries.add(type + ":" + name + ":" + id);
        }
        assertEquals(expectedEntries(), actualEntries);
    }

    private static Document parse() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(resourceFile());
    }

    private static File resourceFile() {
        File workingDirectory = new File(System.getProperty("user.dir"));
        File moduleResource = new File(workingDirectory, "src/main/res/values/public.xml");
        if (moduleResource.isFile()) {
            return moduleResource;
        }
        return new File(workingDirectory, "app/src/main/res/values/public.xml");
    }

    private static List<String> expectedEntries() {
        List<String> entries = new ArrayList<String>();
        add(entries, "id", 16842752L,
                "dialog_message", "my_bar", "actual_value");
        add(entries, "attr", 2130771968L,
                "direction", "defaultScreen", "cellWidth", "cellHeight",
                "longAxisStartPadding", "longAxisEndPadding", "shortAxisStartPadding",
                "shortAxisEndPadding", "shortAxisCells", "longAxisCells", "texture",
                "className", "packageName", "screen", "x", "y", "spanX", "spanY", "icon",
                "title", "uri", "container", "orientation", "cells", "min", "max");
        add(entries, "drawable", 2130837504L,
                "applications_grid", "bg_appwidget_error", "delete_handle_normal",
                "delete_zone_selector", "dock_bg_bar_grey", "dock_bg_fade_to_black",
                "dock_bg_glow", "folder_background", "home_button_normal", "ic_launcher",
                "ic_launcher_add_folder", "ic_launcher_application", "ic_launcher_appwidget",
                "ic_launcher_folder", "ic_launcher_folder_open", "ic_launcher_settings",
                "ic_launcher_shortcut", "ic_launcher_wallpaper", "ic_menu_close_clear_cancel",
                "ic_menu_edit", "ic_search_widget", "normal_application_background",
                "overlay_uninstall", "pager_dot_normal", "pager_dot_selected", "pager_dots",
                "placeholder_google", "preview_bg", "preview_bg_focus", "preview_bg_press",
                "search_button_bg", "search_button_voice", "search_floater", "textfield_searchwidget",
                "textfield_searchwidget_default", "textfield_searchwidget_pressed",
                "textfield_searchwidget_selected", "timepicker_down_btn", "timepicker_down_disabled",
                "timepicker_down_disabled_focused", "timepicker_down_normal", "timepicker_down_pressed",
                "timepicker_down_selected", "timepicker_input", "timepicker_input_disabled",
                "timepicker_input_normal", "timepicker_input_pressed", "timepicker_input_selected",
                "timepicker_up_btn", "timepicker_up_disabled", "timepicker_up_disabled_focused",
                "timepicker_up_normal", "timepicker_up_pressed", "timepicker_up_selected", "trashcan",
                "trashcan_hover");
        add(entries, "layout", 2130903040L,
                "application", "application_boxed_grid", "application_boxed_page", "application_list",
                "apps_grid_view", "apps_page_view", "apps_paging_view", "appwidget_error",
                "dialog_colour_picker", "dialog_list_item", "dialog_seekbar_preference", "dockbar",
                "folder_icon", "launcher", "list_category", "list_item_2", "live_folder_grid",
                "live_folder_icon", "live_folder_list", "number_picker", "rename_folder", "small_application",
                "user_folder", "wallpaper_chooser", "wallpaper_item", "widget_search", "widget_span",
                "workspace_screen");
        add(entries, "anim", 2130968576L,
                "apps_scale_in", "apps_scale_out", "dock_fade_in", "dock_fade_out",
                "home_button_fade_in", "home_button_fade_out", "screen_indicator_fade_out");
        add(entries, "xml", 2131034112L, "default_workspace", "preferences");
        add(entries, "id", 2131099648L,
                "vertical", "horizontal", "name", "icon", "description", "all_apps_view", "all_apps",
                "view_pager", "apps_paging_screen_indicator", "colour_picker_view", "old_colour_panel",
                "new_colour_panel", "dock_scroll_view", "dock_item_holder", "drag_layer", "workspace",
                "cell1", "cell2", "cell3", "cell4", "cell5", "cell6", "cell7", "apps_grid_stub",
                "apps_grid", "home_button", "dock_stub", "dock", "delete_zone", "workspace_screen_indicator",
                "folder_name", "folder_button_rename", "folder_button_close", "folder_content", "increment",
                "timepicker_input", "decrement", "label", "wallpaper", "gallery", "set", "widget_search",
                "search_plate", "search_src_text", "search_voice_btn", "layout_root", "text_columns_span",
                "text_rows_span", "widget_columns_span", "widget_rows_span");
        add(entries, "color", 2131165184L,
                "window_background", "grid_dark_background", "bubble_dark_background", "delete_color_filter",
                "appwidget_error_color", "snag_callout_color", "gesture_color", "uncertain_gesture_color",
                "preferences_default_general_selector_colour_pressed",
                "preferences_default_general_selector_colour_focused", "bright_text_dark_focused");
        add(entries, "dimen", 2131230720L,
                "search_widget_inset", "gesture_thumbnail_inset", "gesture_thumbnail_size", "cell_width",
                "cell_height");
        add(entries, "array", 2131296256L,
                "extra_wallpapers", "preferences_entries_actions", "preferences_entry_values_actions",
                "preferences_values_workspace_screen_indicator_types", "preferences_entries_apps_grid_types",
                "preferences_entry_values_apps_grid_types", "preferences_values_dock_backgrounds",
                "preferences_values_dock_item_alignments_resets", "preferences_values_dock_item_widths",
                "wallpapers");
        add(entries, "string", 2131361792L,
                "preferences_key_system_settings", "preferences_key_general_fullscreen",
                "preferences_key_general_sensor_orientation", "preferences_key_general_selector_colour_pressed",
                "preferences_key_general_selector_colour_focused", "preferences_key_workspace_number_of_screens",
                "preferences_key_workspace_default_screen", "preferences_key_workspace_screen_indicator_type",
                "preferences_key_workspace_elastic_scrolling", "preferences_key_workspace_screen_looping",
                "preferences_key_workspace_content_grid_rows", "preferences_key_workspace_content_grid_columns",
                "preferences_key_workspace_content_grid_auto_fit", "preferences_key_workspace_show_shortcut_titles",
                "preferences_key_workspace_manage_wallpaper", "preferences_key_workspace_scroll_wallpaper",
                "preferences_key_action_home_button", "preferences_key_action_swipe_up",
                "preferences_key_action_swipe_down", "preferences_key_action_double_tap", "preferences_key_apps_grid_type",
                "preferences_key_apps_grid_animated", "preferences_key_apps_grid_remember_position",
                "preferences_key_apps_grid_bg_alpha", "preferences_key_apps_grid_content_rows_port",
                "preferences_key_apps_grid_content_columns_port", "preferences_key_apps_grid_content_rows_land",
                "preferences_key_apps_grid_content_columns_land",
                "preferences_key_apps_grid_vertical_scrolling_content_columns_port",
                "preferences_key_apps_grid_vertical_scrolling_content_columns_land",
                "preferences_key_apps_grid_horizontal_paging_content_rows_port",
                "preferences_key_apps_grid_horizontal_paging_content_columns_port",
                "preferences_key_apps_grid_horizontal_paging_content_rows_land",
                "preferences_key_apps_grid_horizontal_paging_content_columns_land", "preferences_key_dock_background",
                "preferences_key_dock_reset_home", "preferences_key_dock_reset_to", "preferences_key_dock_item_alignment",
                "preferences_key_dock_item_width", "preferences_key_restart", "preferences_key_reset",
                "preferences_key_application", "preferences_default_general_fullscreen",
                "preferences_default_general_sensor_orientation", "preferences_default_workspace_number_of_screens",
                "preferences_default_workspace_default_screen", "preferences_default_workspace_elastic_scrolling",
                "preferences_default_workspace_screen_looping", "preferences_default_workspace_content_grid_rows",
                "preferences_default_workspace_content_grid_columns", "preferences_default_workspace_content_grid_auto_fit",
                "preferences_default_workspace_show_shortcut_titles", "preferences_default_workspace_scroll_wallpaper",
                "preferences_default_apps_grid_animated", "preferences_default_apps_grid_remember_position",
                "preferences_default_apps_grid_bg_alpha",
                "preferences_default_apps_grid_vertical_scrolling_content_columns_port",
                "preferences_default_apps_grid_vertical_scrolling_content_columns_land",
                "preferences_default_apps_grid_horizontal_paging_content_rows_port",
                "preferences_default_apps_grid_horizontal_paging_content_columns_port",
                "preferences_default_apps_grid_horizontal_paging_content_rows_land",
                "preferences_default_apps_grid_horizontal_paging_content_columns_land",
                "preferences_default_action_home_button", "preferences_default_action_swipe_up",
                "preferences_default_action_swipe_down", "preferences_default_action_double_tap",
                "preferences_default_workspace_screen_indicator_type", "preferences_default_apps_grid_type",
                "preferences_default_dock_background", "preferences_default_dock_item_alignment",
                "preferences_default_dock_reset_to", "preferences_default_dock_item_width",
                "preferences_default_dock_reset_home", "application_name", "application_copyright", "folder_name",
                "chooser_wallpaper", "wallpaper_instructions", "add", "pick_wallpaper", "activity_not_found",
                "configure_wallpaper", "drop_to_uninstall", "rename_folder_label", "rename_folder_title", "button_no",
                "button_ok", "button_yes", "button_done", "button_close", "button_cancel", "group_applications",
                "group_add_apps_grid", "group_search", "group_folder", "group_add_shortcuts", "group_add_folders",
                "group_add_widgets", "dialog_title_widget_span", "dialog_text_columns", "dialog_text_rows",
                "dialog_colour_picker_title", "dialog_colour_picker_press_colour_to_apply", "dock_confirm_delete",
                "preferences_confirm_reset", "add_folder", "add_clock", "add_photo_frame", "add_search", "out_of_space",
                "shortcut_installed", "shortcut_uninstalled", "shortcut_duplicate", "title_select_shortcut",
                "title_select_live_folder", "menu_search", "menu_add", "menu_wallpaper", "menu_applications",
                "menu_preferences", "menu_settings", "menu_manage_apps", "menu_uninstall_apps", "search_hint",
                "gadget_error_text", "preferences_alert_dialog_restart_message",
                "preferences_alert_dialog_restart_button", "preferences_title_system_settings",
                "preferences_category_categories", "preferences_screen_title_general", "preferences_screen_title_workspace",
                "preferences_screen_title_action_bindings", "preferences_screen_title_applications_grid",
                "preferences_screen_title_dock", "preferences_category_application", "preferences_category_screens",
                "preferences_category_behaviour", "preferences_category_appearance", "preferences_category_wallpaper",
                "preferences_category_content", "preferences_category_content_grid", "preferences_title_general_fullscreen",
                "preferences_title_general_sensor_orientation", "preferences_title_general_selector_colour_pressed",
                "preferences_title_general_selector_colour_focused", "preferences_title_workspace_number_of_screens",
                "preferences_title_workspace_default_screen", "preferences_title_workspace_indicator_type",
                "preferences_title_workspace_elastic_scrolling", "preferences_title_workspace_screen_looping",
                "preferences_title_workspace_content_grid_rows", "preferences_title_workspace_content_grid_columns",
                "preferences_title_workspace_content_grid_auto_fit", "preferences_title_workspace_show_shortcut_titles",
                "preferences_title_workspace_manage_wallpaper", "preferences_title_workspace_scroll_wallpaper",
                "preferences_title_action_home_button", "preferences_title_action_swipe_up", "preferences_title_action_swipe_down",
                "preferences_title_action_double_tap", "preferences_title_apps_grid_type", "preferences_title_apps_grid_content",
                "preferences_title_apps_grid_content_rows_portrait", "preferences_title_apps_grid_content_columns_portrait",
                "preferences_title_apps_grid_content_rows_landscape", "preferences_title_apps_grid_content_columns_landscape",
                "preferences_title_apps_grid_animated", "preferences_title_apps_grid_remember_position",
                "preferences_title_apps_grid_bg_alpha", "preferences_title_dock_background", "preferences_title_dock_reset_home",
                "preferences_title_dock_reset_to", "preferences_title_dock_item_alignment", "preferences_title_dock_item_width",
                "preferences_title_reset", "preferences_title_restart", "preferences_summary_general_fullscreen",
                "preferences_summary_general_sensor_orientation", "preferences_summary_general_selector_colour_pressed",
                "preferences_summary_general_selector_colour_focused", "preferences_summary_workspace_number_of_screens",
                "preferences_summary_workspace_default_screen", "preferences_summary_workspace_screen_indicator_type",
                "preferences_summary_workspace_elastic_scrolling", "preferences_summary_workspace_screen_looping",
                "preferences_summary_workspace_content_grid_rows", "preferences_summary_workspace_content_grid_columns",
                "preferences_summary_workspace_content_grid_auto_fit", "preferences_summary_workspace_show_shortcut_titles",
                "preferences_summary_workspace_manage_wallpaper", "preferences_summary_workspace_scroll_wallpaper",
                "preferences_summary_action_home_button", "preferences_summary_action_swipe_up",
                "preferences_summary_action_swipe_down", "preferences_summary_action_double_tap", "preferences_summary_apps_grid_type",
                "preferences_summary_apps_grid_content", "preferences_summary_apps_grid_content_rows_portrait",
                "preferences_summary_apps_grid_content_columns_portrait", "preferences_summary_apps_grid_content_rows_landscape",
                "preferences_summary_apps_grid_content_columns_landscape", "preferences_summary_apps_grid_animated",
                "preferences_summary_apps_grid_remember_position", "preferences_summary_apps_grid_bg_alpha",
                "preferences_summary_dock_background", "preferences_summary_dock_reset_home", "preferences_summary_dock_reset_to",
                "preferences_summary_dock_item_alignment", "preferences_summary_dock_item_width", "preferences_summary_reset",
                "permlab_install_shortcut", "permdesc_install_shortcut", "permlab_uninstall_shortcut",
                "permdesc_uninstall_shortcut", "permlab_read_settings", "permdesc_read_settings", "permlab_write_settings",
                "permdesc_write_settings");
        add(entries, "style", 2131427328L,
                "WorkspaceIcon", "WorkspaceIcon.Portrait", "WorkspaceIcon.Landscape", "SearchButton", "Theme",
                "DockBarIcon");
        return entries;
    }

    private static void add(List<String> entries, String type, long firstId, String... names) {
        for (int index = 0; index < names.length; index++) {
            entries.add(type + ":" + names[index] + ":" + (firstId + index));
        }
    }
}
