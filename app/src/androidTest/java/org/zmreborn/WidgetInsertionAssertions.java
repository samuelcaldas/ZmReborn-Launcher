package org.zmreborn;

import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.database.Cursor;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

/** Verifies externally configured widget host, model, and persistence state. */
final class WidgetInsertionAssertions {
    private static final long RENDER_TIMEOUT_MILLIS = 5000L;
    private static final String MARKER_PREFIX =
            "Configured external widget fixture marker ";

    private WidgetInsertionAssertions() {
    }

    static void verify(Instrumentation instrumentation, Launcher launcher,
            AppWidgetManager manager, ComponentName provider,
            WidgetInsertionLocator.LocatedWidget widget) {
        validate(instrumentation, launcher, manager, provider, widget);
        assertBoundProvider(manager, provider, widget.info.appWidgetId);
        assertTagAndSpan(widget);
        assertRenderedMarker(instrumentation, widget);
        assertNoPendingState(launcher);
        assertPersisted(launcher, widget.info);
    }

    private static void validate(Instrumentation instrumentation,
            Launcher launcher, AppWidgetManager manager, ComponentName provider,
            WidgetInsertionLocator.LocatedWidget widget) {
        if (instrumentation == null || launcher == null || manager == null
                || provider == null || widget == null) {
            throw new IllegalArgumentException(
                    "Widget assertion requires instrumentation, launcher, manager, provider, and widget");
        }
    }

    private static void assertBoundProvider(AppWidgetManager manager,
            ComponentName provider, int appWidgetId) {
        AppWidgetProviderInfo bound = manager.getAppWidgetInfo(appWidgetId);
        if (bound == null || !provider.equals(bound.provider)) {
            throw new AssertionError("Inserted widget is not bound to fixture provider: "
                    + provider);
        }
    }

    private static void assertTagAndSpan(WidgetInsertionLocator.LocatedWidget widget) {
        if (widget.view.getTag() != widget.info
                || widget.view.getAppWidgetId() != widget.info.appWidgetId) {
            throw new AssertionError("Inserted host view does not retain widget model tag");
        }
        CellLayout.LayoutParams params = (CellLayout.LayoutParams) widget.view.getLayoutParams();
        if (widget.info.spanX <= 0 || widget.info.spanY <= 0
                || params.cellHSpan != widget.info.spanX
                || params.cellVSpan != widget.info.spanY) {
            throw new AssertionError("Inserted widget span does not match CellLayout placement");
        }
    }

    private static void assertRenderedMarker(Instrumentation instrumentation,
            final WidgetInsertionLocator.LocatedWidget widget) {
        int markerId = markerId(instrumentation);
        String expected = MARKER_PREFIX + widget.info.appWidgetId;
        long deadline = SystemClock.uptimeMillis() + RENDER_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            CharSequence text = markerTextOnMain(instrumentation, widget,
                    markerId);
            if (expected.contentEquals(text)) {
                return;
            }
            SystemClock.sleep(25L);
        }
        throw new AssertionError(
                "Inserted external widget did not render configured marker: "
                        + expected);
    }

    private static int markerId(Instrumentation instrumentation) {
        String packageName = instrumentation.getContext().getPackageName();
        int markerId = instrumentation.getContext().getResources().getIdentifier(
                "external_widget_marker", "id", packageName);
        if (markerId == 0) {
            throw new IllegalStateException(
                    "External widget marker resource is unavailable");
        }
        return markerId;
    }

    private static CharSequence markerTextOnMain(
            Instrumentation instrumentation,
            final WidgetInsertionLocator.LocatedWidget widget,
            final int markerId) {
        final CharSequence[] text = new CharSequence[1];
        instrumentation.runOnMainSync(new Runnable() {
            public void run() {
                View marker = widget.view.findViewById(markerId);
                if (marker instanceof TextView
                        && marker.getVisibility() == View.VISIBLE) {
                    text[0] = ((TextView) marker).getText();
                }
            }
        });
        return text[0];
    }

    private static void assertNoPendingState(Launcher launcher) {
        if (LauncherWidgetPickerTestAccess.pendingId(launcher) != -1
                || LauncherWidgetPickerTestAccess.pendingPlacement(launcher)) {
            throw new AssertionError("Widget insertion left pending Launcher state");
        }
    }

    private static void assertPersisted(Launcher launcher, LauncherAppWidgetInfo info) {
        Cursor cursor = launcher.getContentResolver().query(
                LauncherSettings.Favorites.CONTENT_URI,
                new String[]{LauncherSettings.BaseLauncherColumns.ITEM_TYPE,
                        LauncherSettings.Favorites.APPWIDGET_ID,
                        LauncherSettings.Favorites.SPANX,
                        LauncherSettings.Favorites.SPANY},
                LauncherSettings.Favorites.APPWIDGET_ID + "=?",
                new String[]{String.valueOf(info.appWidgetId)}, null);
        if (cursor == null) {
            throw new AssertionError("Widget insertion did not return persistence cursor");
        }
        try {
            assertSinglePersistedWidget(cursor, info);
        } finally {
            cursor.close();
        }
    }

    private static void assertSinglePersistedWidget(Cursor cursor,
            LauncherAppWidgetInfo info) {
        if (cursor.getCount() != 1 || !cursor.moveToFirst()) {
            throw new AssertionError("Widget insertion did not persist exactly one widget row");
        }
        if (cursor.getInt(0) != LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                || cursor.getInt(1) != info.appWidgetId
                || cursor.getInt(2) != info.spanX || cursor.getInt(3) != info.spanY) {
            throw new AssertionError("Persisted widget row does not match inserted widget state");
        }
    }
}
