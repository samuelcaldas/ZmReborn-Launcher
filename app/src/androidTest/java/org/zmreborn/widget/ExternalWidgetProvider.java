package org.zmreborn.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/** Provides deterministic external RemoteViews content for widget insertion tests. */
public final class ExternalWidgetProvider extends AppWidgetProvider {
    private static final String PREFERENCES = "external_widget_fixture";
    private static final String CONFIGURED_MARKER_PREFIX =
            "Configured external widget fixture marker ";
    private static final String UNCONFIGURED_MARKER_PREFIX =
            "Unconfigured external widget fixture marker ";

    /** Publishes fixture marker content to every allocated widget instance. */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        validateUpdate(context, appWidgetManager, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            publishCurrent(context, appWidgetManager, appWidgetId);
        }
    }

    /** Clears durable configuration state when widget IDs are deleted. */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (context == null || appWidgetIds == null) {
            throw new IllegalArgumentException(
                    "Widget fixture deletion requires context and IDs");
        }
        SharedPreferences.Editor editor = preferences(context).edit();
        for (int appWidgetId : appWidgetIds) {
            editor.remove(configurationKey(appWidgetId));
        }
        editor.apply();
    }

    static void publishConfigured(Context context, AppWidgetManager manager,
            int appWidgetId) {
        validateWidget(context, manager, appWidgetId);
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putBoolean(configurationKey(appWidgetId), true);
        if (!editor.commit()) {
            throw new IllegalStateException(
                    "Widget fixture could not persist configuration state");
        }
        publishMarker(context, manager, appWidgetId,
                CONFIGURED_MARKER_PREFIX);
    }

    private static void publishCurrent(Context context,
            AppWidgetManager manager, int appWidgetId) {
        validateWidget(context, manager, appWidgetId);
        String markerPrefix = UNCONFIGURED_MARKER_PREFIX;
        if (preferences(context).getBoolean(
                configurationKey(appWidgetId), false)) {
            markerPrefix = CONFIGURED_MARKER_PREFIX;
        }
        publishMarker(context, manager, appWidgetId, markerPrefix);
    }

    private static void publishMarker(Context context,
            AppWidgetManager manager, int appWidgetId, String markerPrefix) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                resourceId(context, "layout", "external_widget_remote_views"));
        views.setTextViewText(resourceId(context, "id",
                "external_widget_marker"), markerPrefix + appWidgetId);
        manager.updateAppWidget(appWidgetId, views);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private static String configurationKey(int appWidgetId) {
        return "configured_" + appWidgetId;
    }

    private static void validateUpdate(Context context, AppWidgetManager manager,
            int[] appWidgetIds) {
        if (context == null || manager == null || appWidgetIds == null) {
            throw new IllegalArgumentException(
                    "Widget fixture update requires context, manager, and IDs");
        }
    }

    private static void validateWidget(Context context, AppWidgetManager manager,
            int appWidgetId) {
        if (context == null || manager == null) {
            throw new IllegalArgumentException(
                    "Widget fixture publish requires context and manager");
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            throw new IllegalArgumentException(
                    "Widget fixture received invalid app-widget ID");
        }
    }

    private static int resourceId(Context context, String type, String name) {
        int resourceId = context.getResources().getIdentifier(
                name, type, context.getPackageName());
        if (resourceId == 0) {
            throw new IllegalStateException(
                    "Widget fixture resource is unavailable: " + type + "/" + name);
        }
        return resourceId;
    }
}
