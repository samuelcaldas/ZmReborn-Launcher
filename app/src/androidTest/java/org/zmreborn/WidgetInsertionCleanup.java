package org.zmreborn;

import android.view.ViewParent;

/** Removes host, model, persistence, and ID state created by widget insertion tests. */
final class WidgetInsertionCleanup {
    private WidgetInsertionCleanup() {
    }

    static void removeNewWidgets(Launcher launcher, int[] existingIds) {
        if (launcher == null || existingIds == null) {
            throw new IllegalArgumentException("Widget cleanup requires launcher and host ID baseline");
        }
        cancelNewPendingWidget(launcher, existingIds);
        int[] currentIds = launcher.getAppWidgetHost().getAppWidgetIds();
        for (int appWidgetId : currentIds) {
            if (!contains(existingIds, appWidgetId)) {
                removeWidget(launcher, appWidgetId);
            }
        }
    }

    private static void cancelNewPendingWidget(Launcher launcher,
            int[] existingIds) {
        int pendingId = LauncherWidgetPickerTestAccess.pendingId(launcher);
        if (pendingId == -1) {
            if (LauncherWidgetPickerTestAccess.pendingPlacement(launcher)) {
                LauncherWidgetPickerTestAccess.cancelPendingWidget(launcher);
            }
            return;
        }
        if (!contains(existingIds, pendingId)) {
            LauncherWidgetPickerTestAccess.cancelPendingWidget(launcher);
        }
    }

    private static void removeWidget(Launcher launcher, int appWidgetId) {
        WidgetInsertionLocator.LocatedWidget located = WidgetInsertionLocator.findById(
                launcher, appWidgetId);
        if (located != null) {
            removeView(located);
            removeModel(located.info);
            LauncherModel.deleteItemFromDatabase(launcher, located.info);
        }
        deletePersistedWidget(launcher, appWidgetId);
        launcher.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
    }

    private static void deletePersistedWidget(Launcher launcher, int appWidgetId) {
        launcher.getContentResolver().delete(
                LauncherSettings.Favorites.CONTENT_URI,
                LauncherSettings.Favorites.APPWIDGET_ID + "=?",
                new String[]{String.valueOf(appWidgetId)});
    }

    private static void removeView(WidgetInsertionLocator.LocatedWidget located) {
        ViewParent parent = located.view.getParent();
        if (!(parent instanceof CellLayout)) {
            throw new IllegalStateException("Inserted widget host view has no CellLayout parent");
        }
        CellLayout layout = (CellLayout) parent;
        layout.removeView(located.view);
        layout.requestLayout();
    }

    private static void removeModel(LauncherAppWidgetInfo info) {
        LauncherModel model = Launcher.getModel();
        if (model.isDesktopLoaded()) {
            model.removeDesktopAppWidget(info);
        }
    }

    private static boolean contains(int[] values, int expected) {
        for (int value : values) {
            if (value == expected) {
                return true;
            }
        }
        return false;
    }
}
