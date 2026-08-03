package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;

/** Verifies widget insertion test cleanup against Launcher pending state. */
public class WidgetInsertionCleanupInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {

    /** Creates pending widget cleanup instrumentation coverage for Launcher. */
    public WidgetInsertionCleanupInstrumentationTest() {
        super(Launcher.class);
    }

    /** Verifies cleanup cancels pending placement before deleting its host ID. */
    public void testCleanupCancelsPendingWidgetPlacement() {
        final Launcher launcher = getActivity();
        final int[] existingIds = launcher.getAppWidgetHost().getAppWidgetIds();
        final int[] pendingId = new int[1];

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                pendingId[0] = launcher.getAppWidgetHost().allocateAppWidgetId();
                LauncherWidgetPickerTestAccess.installPendingPlacement(
                        launcher, pendingId[0]);
            }
        });
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    WidgetInsertionCleanup.removeNewWidgets(launcher, existingIds);
                }
            });
            assertEquals(-1, LauncherWidgetPickerTestAccess.pendingId(launcher));
            assertFalse(LauncherWidgetPickerTestAccess.pendingPlacement(launcher));
            assertFalse(LauncherWidgetPickerTestAccess.hasPendingPlacementListener(
                    launcher));
        } finally {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    LauncherWidgetPickerTestAccess.cancelPendingWidget(launcher);
                }
            });
        }
    }
}
