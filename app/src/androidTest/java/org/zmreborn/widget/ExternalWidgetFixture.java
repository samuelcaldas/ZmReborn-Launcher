package org.zmreborn.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import java.util.List;

/** Locates deterministic external widget-provider metadata in instrumentation tests. */
public final class ExternalWidgetFixture {
    private ExternalWidgetFixture() {
    }

    /** Returns test-APK component for deterministic external provider fixture. */
    public static ComponentName provider(Context testContext) {
        if (testContext == null) {
            throw new IllegalArgumentException("Widget fixture requires test context");
        }
        return new ComponentName(testContext.getPackageName(),
                ExternalWidgetProvider.class.getName());
    }

    /** Returns installed metadata for fixture provider or fails with its component. */
    public static AppWidgetProviderInfo requireProvider(AppWidgetManager manager,
            ComponentName component) {
        if (manager == null || component == null) {
            throw new IllegalArgumentException("Widget fixture requires manager and component");
        }
        List<AppWidgetProviderInfo> providers = manager.getInstalledProviders();
        if (providers == null) {
            throw new AssertionError("Widget manager returned no installed providers");
        }
        for (AppWidgetProviderInfo provider : providers) {
            if (provider != null && component.equals(provider.provider)) {
                return provider;
            }
        }
        throw new AssertionError("Missing instrumentation widget provider: " + component);
    }
}
