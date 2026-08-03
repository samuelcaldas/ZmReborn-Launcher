package org.zmreborn;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import org.zmreborn.widget.ExternalWidgetFixture;
import org.zmreborn.widget.WidgetBindAuthority;
import org.zmreborn.widget.WidgetPickerDialog;
import org.zmreborn.widget.WidgetPickerTouch;

/** Executes external widget insertion from production picker touch through persistence. */
public class WidgetInsertionE2ETest
        extends ActivityInstrumentationTestCase2<Launcher> {
    private static final long PICKER_DISMISS_TIMEOUT_MILLIS = 5000L;

    /** Creates external widget insertion end-to-end test for Launcher. */
    public WidgetInsertionE2ETest() {
        super(Launcher.class);
    }

    /** Verifies fixture provider touch binds, configures, inserts, persists, and cleans up. */
    public void testProviderTouchInsertsConfiguredExternalWidget() throws Exception {
        final Launcher launcher = getActivity();
        final AppWidgetManager manager = AppWidgetManager.getInstance(launcher);
        awaitWorkspaceUnlocked(launcher);
        final ComponentName provider = ExternalWidgetFixture.provider(
                getInstrumentation().getContext());
        ExternalWidgetFixture.requireProvider(manager, provider);
        final int[] existingIds = launcher.getAppWidgetHost().getAppWidgetIds();
        final WidgetBindAuthority.Grant grant = WidgetBindAuthority.ensure(
                getInstrumentation(), launcher, provider);

        try {
            final WidgetPickerDialog[] picker = new WidgetPickerDialog[]{
                    LauncherWidgetPickerTestAccess.openFromAddDialog(
                            getInstrumentation(), launcher)};
            assertTrue("Production widget picker must be visible",
                    isShowingOnMain(picker[0]));
            WidgetPickerTouch.tapProvider(getInstrumentation(), picker[0], provider);
            awaitPickerDismissal(picker[0]);
            WidgetInsertionLocator.LocatedWidget widget = WidgetInsertionLocator.await(
                    getInstrumentation(), launcher, provider, existingIds);
            WidgetInsertionAssertions.verify(getInstrumentation(), launcher,
                    manager, provider, widget);
        } finally {
            try {
                getInstrumentation().runOnMainSync(new Runnable() {
                    public void run() {
                        LauncherWidgetPickerTestAccess.dismiss(launcher);
                        WidgetInsertionCleanup.removeNewWidgets(launcher, existingIds);
                    }
                });
            } finally {
                grant.revoke();
            }
        }
    }

    private void awaitWorkspaceUnlocked(final Launcher launcher) {
        long deadline = SystemClock.uptimeMillis() + PICKER_DISMISS_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            final boolean[] locked = new boolean[1];
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    locked[0] = launcher.isWorkspaceLocked();
                }
            });
            if (!locked[0]) {
                return;
            }
            SystemClock.sleep(25L);
        }
        fail("Launcher workspace remained locked before widget insertion");
    }

    private void awaitPickerDismissal(WidgetPickerDialog picker) {
        long deadline = SystemClock.uptimeMillis() + PICKER_DISMISS_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            if (!isShowingOnMain(picker)) {
                return;
            }
            SystemClock.sleep(25L);
        }
        fail("Provider touch did not dismiss production widget picker");
    }

    private boolean isShowingOnMain(final WidgetPickerDialog picker) {
        final boolean[] showing = new boolean[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                showing[0] = picker.isShowing();
            }
        });
        return showing[0];
    }
}
