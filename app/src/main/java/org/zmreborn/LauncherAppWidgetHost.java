package org.zmreborn;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/**
 * Custom AppWidgetHost creating launcher-specialized widget host views.
 */
public class LauncherAppWidgetHost extends AppWidgetHost {
    /**
     * Constructs a launcher app widget host with host identifier.
     */
    public LauncherAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        return new LauncherAppWidgetHostView(context);
    }
}
