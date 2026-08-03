package org.zmreborn;

import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.SystemClock;
import android.view.View;

/** Finds fixture-backed host views after asynchronous widget insertion. */
final class WidgetInsertionLocator {
    private static final long INSERTION_TIMEOUT_MILLIS = 10000L;

    private WidgetInsertionLocator() {
    }

    static LocatedWidget await(Instrumentation instrumentation, Launcher launcher,
            ComponentName provider, int[] existingIds) {
        validate(instrumentation, launcher, provider, existingIds);
        long deadline = SystemClock.uptimeMillis() + INSERTION_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            LocatedWidget located = findFixtureOnMain(instrumentation,
                    launcher, provider, existingIds);
            if (located != null) {
                return located;
            }
            SystemClock.sleep(50L);
        }
        throw new AssertionError("Launcher did not insert new fixture widget: "
                + provider);
    }

    private static void validate(Instrumentation instrumentation,
            Launcher launcher, ComponentName provider, int[] existingIds) {
        if (instrumentation == null || launcher == null || provider == null
                || existingIds == null) {
            throw new IllegalArgumentException(
                    "Widget locator requires instrumentation, launcher, provider, and baseline IDs");
        }
    }

    static LocatedWidget findById(Launcher launcher, int appWidgetId) {
        Workspace workspace = launcher.mWorkspace;
        if (workspace == null) {
            return null;
        }
        for (int screen = 0; screen < workspace.getChildCount(); screen++) {
            LocatedWidget located = findInLayout((CellLayout) workspace.getChildAt(screen),
                    appWidgetId);
            if (located != null) {
                return located;
            }
        }
        return null;
    }

    private static LocatedWidget findFixtureOnMain(
            final Instrumentation instrumentation, final Launcher launcher,
            final ComponentName provider, final int[] existingIds) {
        final LocatedWidget[] located = new LocatedWidget[1];
        instrumentation.runOnMainSync(new Runnable() {
            public void run() {
                located[0] = findFixture(launcher, provider, existingIds);
            }
        });
        return located[0];
    }

    private static LocatedWidget findFixture(Launcher launcher,
            ComponentName provider, int[] existingIds) {
        AppWidgetManager manager = AppWidgetManager.getInstance(launcher);
        LocatedWidget located = null;
        int newProviderIds = 0;
        for (int appWidgetId : launcher.getAppWidgetHost().getAppWidgetIds()) {
            if (contains(existingIds, appWidgetId)
                    || !isBoundTo(manager, appWidgetId, provider)) {
                continue;
            }
            newProviderIds++;
            LocatedWidget candidate = findById(launcher, appWidgetId);
            if (candidate != null) {
                located = candidate;
            }
        }
        if (newProviderIds > 1) {
            throw new AssertionError(
                    "Widget insertion allocated multiple new fixture IDs");
        }
        return located;
    }

    private static boolean contains(int[] values, int expected) {
        for (int value : values) {
            if (value == expected) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBoundTo(AppWidgetManager manager, int appWidgetId,
            ComponentName provider) {
        android.appwidget.AppWidgetProviderInfo info = manager.getAppWidgetInfo(appWidgetId);
        return info != null && provider.equals(info.provider);
    }

    private static LocatedWidget findInLayout(CellLayout layout, int appWidgetId) {
        for (int index = 0; index < layout.getChildCount(); index++) {
            View child = layout.getChildAt(index);
            if (child instanceof LauncherAppWidgetHostView
                    && child.getTag() instanceof LauncherAppWidgetInfo) {
                LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) child.getTag();
                if (info.appWidgetId == appWidgetId) {
                    return new LocatedWidget(layout, (LauncherAppWidgetHostView) child, info);
                }
            }
        }
        return null;
    }

    static final class LocatedWidget {
        final CellLayout layout;
        final LauncherAppWidgetHostView view;
        final LauncherAppWidgetInfo info;

        LocatedWidget(CellLayout layout, LauncherAppWidgetHostView view,
                LauncherAppWidgetInfo info) {
            this.layout = layout;
            this.view = view;
            this.info = info;
        }
    }
}
