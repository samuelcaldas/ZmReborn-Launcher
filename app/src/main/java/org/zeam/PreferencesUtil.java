package org.zeam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class PreferencesUtil {
    private static SharedPreferences sSharedPreferences;

    private static SharedPreferences getSharedPreferences(Context context) {
        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return sSharedPreferences;
    }

    static int getNumberOfScreens(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_workspace_number_of_screens), Integer.parseInt(context.getString(R.string.preferences_default_workspace_number_of_screens)));
    }

    static int getDefaultScreen(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_workspace_default_screen), Integer.parseInt(context.getString(R.string.preferences_default_workspace_default_screen))) - 1;
    }

    static String getScreenIndicator(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.preferences_key_workspace_screen_indicator_type), context.getString(R.string.preferences_default_workspace_screen_indicator_type));
    }

    static boolean useSensorOrientation(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_general_sensor_orientation), Boolean.parseBoolean(context.getString(R.string.preferences_default_general_sensor_orientation)));
    }

    static boolean isManageWallpaperEnabled(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        Display defaultDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        boolean defaultValue = false;
        defaultDisplay.getMetrics(displayMetrics);
        if (displayMetrics.densityDpi < 240 || ((Build.MODEL != null && Build.MODEL.contains("Sensation")) || Build.VERSION.SDK_INT >= 14)) {
            defaultValue = true;
        }
        boolean enabled = sharedPreferences.getBoolean(context.getString(R.string.preferences_key_workspace_manage_wallpaper), defaultValue);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(R.string.preferences_key_workspace_manage_wallpaper), enabled);
        editor.commit();
        return enabled;
    }

    static boolean isScrollWallpaperEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_workspace_scroll_wallpaper), Boolean.parseBoolean(context.getString(R.string.preferences_default_workspace_scroll_wallpaper)));
    }

    static boolean isFullscreenEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_general_fullscreen), Boolean.parseBoolean(context.getString(R.string.preferences_default_general_fullscreen)));
    }

    static void setFullscreenEnabled(Context context, boolean fullscreen) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(context.getString(R.string.preferences_key_general_fullscreen), Boolean.valueOf(fullscreen).booleanValue());
        editor.commit();
    }

    static boolean isShowShortcutTitlesEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_workspace_show_shortcut_titles), Boolean.parseBoolean(context.getString(R.string.preferences_default_workspace_show_shortcut_titles)));
    }

    static int getContentGridRows(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_workspace_content_grid_rows), Integer.parseInt(context.getString(R.string.preferences_default_workspace_content_grid_rows)));
    }

    static int getContentGridColumns(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_workspace_content_grid_columns), Integer.parseInt(context.getString(R.string.preferences_default_workspace_content_grid_columns)));
    }

    static boolean isAutoFitContentGridItemsEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_workspace_content_grid_auto_fit), Boolean.parseBoolean(context.getString(R.string.preferences_default_workspace_content_grid_auto_fit)));
    }

    static boolean isElasticScrollingEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_workspace_elastic_scrolling), Boolean.parseBoolean(context.getString(R.string.preferences_default_workspace_elastic_scrolling)));
    }

    static boolean isScreenLoopingEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_workspace_screen_looping), Boolean.parseBoolean(context.getString(R.string.preferences_default_workspace_screen_looping)));
    }

    static int getSelectorPressedColorHex(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_general_selector_colour_pressed), context.getResources().getColor(R.color.preferences_default_general_selector_colour_pressed));
    }

    static int getSelectorFocusedColorHex(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_general_selector_colour_focused), context.getResources().getColor(R.color.preferences_default_general_selector_colour_focused));
    }

    static int getActionBindingForHomeButton(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.preferences_key_action_home_button), context.getString(R.string.preferences_default_action_home_button)));
    }

    static int getActionBindingForSwipeUp(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.preferences_key_action_swipe_up), context.getString(R.string.preferences_default_action_swipe_up)));
    }

    static int getActionBindingForSwipeDown(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.preferences_key_action_swipe_down), context.getString(R.string.preferences_default_action_swipe_down)));
    }

    static int getActionBindingForDoubleTap(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.preferences_key_action_double_tap), context.getString(R.string.preferences_default_action_double_tap)));
    }

    static int getAppsGridType(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.preferences_key_apps_grid_type), context.getString(R.string.preferences_default_apps_grid_type)));
    }

    static int getAppsGridVerticalScrollingContentColumnsPortrait(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_apps_grid_vertical_scrolling_content_columns_port), Integer.parseInt(context.getString(R.string.preferences_default_apps_grid_vertical_scrolling_content_columns_port)));
    }

    static int getAppsGridVerticalScrollingContentColumnsLandscape(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_apps_grid_vertical_scrolling_content_columns_land), Integer.parseInt(context.getString(R.string.preferences_default_apps_grid_vertical_scrolling_content_columns_land)));
    }

    static int getAppsGridHorizontalPagingContentRowsPortrait(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_rows_port), Integer.parseInt(context.getString(R.string.preferences_default_apps_grid_horizontal_paging_content_rows_port)));
    }

    static int getAppsGridHorizontalPagingContentColumnsPortrait(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_columns_port), Integer.parseInt(context.getString(R.string.preferences_default_apps_grid_horizontal_paging_content_columns_port)));
    }

    static int getAppsGridHorizontalPagingContentRowsLandscape(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_rows_land), Integer.parseInt(context.getString(R.string.preferences_default_apps_grid_horizontal_paging_content_rows_land)));
    }

    static int getAppsGridHorizontalPagingContentColumnsLandscape(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_columns_land), Integer.parseInt(context.getString(R.string.preferences_default_apps_grid_horizontal_paging_content_columns_land)));
    }

    static boolean rememberApplicationsPosition(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_apps_grid_remember_position), Boolean.parseBoolean(context.getString(R.string.preferences_default_apps_grid_remember_position)));
    }

    static boolean isAnimateAppsGridEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_apps_grid_animated), Boolean.parseBoolean(context.getString(R.string.preferences_default_apps_grid_animated)));
    }

    static int getAppsGridBackgroundAlpha(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.preferences_key_apps_grid_bg_alpha), Integer.parseInt(context.getString(R.string.preferences_default_apps_grid_bg_alpha)));
    }

    static String getDockBackgroundType(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.preferences_key_dock_background), context.getString(R.string.preferences_default_dock_background));
    }

    static boolean getDockResetHome(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.preferences_key_dock_reset_home), Boolean.parseBoolean(context.getString(R.string.preferences_default_dock_reset_home)));
    }

    static String getDockResetTo(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.preferences_key_dock_reset_to), context.getString(R.string.preferences_default_dock_reset_to));
    }

    static String getDockItemWidth(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.preferences_key_dock_item_width), context.getString(R.string.preferences_default_dock_item_width));
    }

    static String getDockItemAlignment(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.preferences_key_dock_item_alignment), context.getString(R.string.preferences_default_dock_item_alignment));
    }
}
