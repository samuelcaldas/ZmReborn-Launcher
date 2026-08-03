package org.zmreborn.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

/** Returns validated fixture configuration results to the hosting launcher. */
public final class ExternalWidgetConfigurationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int appWidgetId = requiredAppWidgetId(getIntent());
        ExternalWidgetProvider.publishConfigured(this,
                AppWidgetManager.getInstance(this), appWidgetId);
        setResult(RESULT_OK, configurationResult(appWidgetId));
        finish();
    }

    private static int requiredAppWidgetId(Intent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("Widget fixture configuration requires launch intent");
        }
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            throw new IllegalArgumentException("Widget fixture configuration requires app-widget ID");
        }
        return appWidgetId;
    }

    private static Intent configurationResult(int appWidgetId) {
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return result;
    }
}
