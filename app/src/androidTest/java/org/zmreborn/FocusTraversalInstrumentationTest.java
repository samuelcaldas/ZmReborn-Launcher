package org.zmreborn;

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;

/** Verifies Launcher focus traversal through its Activity dispatch boundary. */
public class FocusTraversalInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {

    /** Creates focus traversal instrumentation for Launcher. */
    public FocusTraversalInstrumentationTest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    /** Verifies Launcher starts with an active focused view. */
    public void testLauncherStartsWithFocus() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertNotNull("Launcher must have focused view", launcher.getCurrentFocus());
    }

    /** Verifies right navigation retains valid workspace focus. */
    public void testDPADRightNavigationFromWorkspace() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View currentCellLayout = focusCurrentCellLayout(launcher);
        getInstrumentation().waitForIdleSync();
        assertTrue("Current CellLayout must receive focus before right navigation",
                currentCellLayout.hasFocus());

        dispatchKeyDownUp(launcher, KeyEvent.KEYCODE_DPAD_RIGHT);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Right navigation must retain focus", launcher.getCurrentFocus());
    }

    /** Verifies left navigation retains valid workspace focus. */
    public void testDPADLeftNavigationFromWorkspace() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View currentCellLayout = focusCurrentCellLayout(launcher);
        getInstrumentation().waitForIdleSync();
        assertTrue("Current CellLayout must receive focus before left navigation",
                currentCellLayout.hasFocus());

        dispatchKeyDownUp(launcher, KeyEvent.KEYCODE_DPAD_LEFT);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Left navigation must retain focus", launcher.getCurrentFocus());
    }

    /** Verifies down navigation retains valid Launcher focus. */
    public void testDPADDownNavigationToDrawer() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View currentCellLayout = focusCurrentCellLayout(launcher);
        getInstrumentation().waitForIdleSync();
        assertTrue("Current CellLayout must receive focus before down navigation",
                currentCellLayout.hasFocus());

        dispatchKeyDownUp(launcher, KeyEvent.KEYCODE_DPAD_DOWN);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Down navigation must retain focus", launcher.getCurrentFocus());
    }

    private void dispatchKeyDownUp(final Launcher launcher, final int keyCode) {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                launcher.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                launcher.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            }
        });
    }

    private View focusCurrentCellLayout(final Launcher launcher) {
        final View[] cellLayout = new View[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                cellLayout[0] = launcher.mWorkspace.getChildAt(
                        launcher.mWorkspace.getCurrentScreen());
                cellLayout[0].setFocusableInTouchMode(true);
                cellLayout[0].requestFocus();
            }
        });
        return cellLayout[0];
    }

    /** Verifies up navigation retains valid drawer focus. */
    public void testDPADUpNavigationFromDrawer() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                launcher.mApplicationsView.open(false);
                View drawer = launcher.findViewById(R.id.apps_grid);
                drawer.setFocusableInTouchMode(true);
                drawer.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        View drawer = launcher.findViewById(R.id.apps_grid);
        assertTrue("Drawer must receive focus before up navigation", drawer.hasFocus());

        dispatchKeyDownUp(launcher, KeyEvent.KEYCODE_DPAD_UP);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Up navigation must retain focus", launcher.getCurrentFocus());
    }

    /** Verifies dock exposes keyboard-focusable descendants. */
    public void testDockItemsAreFocusable() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View dock = launcher.findViewById(R.id.dock);
        assertNotNull("Dock must exist", dock);
        assertEquals("Dock must be visible", View.VISIBLE, dock.getVisibility());
        assertTrue("Dock must expose focusable items", dock.hasFocusable());
    }

    /** Verifies open drawer exposes keyboard-focusable descendants. */
    public void testDrawerItemsAreFocusable() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                launcher.mApplicationsView.open(false);
            }
        });
        getInstrumentation().waitForIdleSync();

        View drawer = launcher.findViewById(R.id.apps_grid);
        assertNotNull("Drawer must exist", drawer);
        assertEquals("Drawer must be visible", View.VISIBLE, drawer.getVisibility());
        awaitFocusable(drawer, 30000L);
    }

    private void awaitFocusable(final View view, long timeoutMillis) {
        long deadline = SystemClock.uptimeMillis() + timeoutMillis;
        while (SystemClock.uptimeMillis() < deadline) {
            final boolean[] focusable = new boolean[1];
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    focusable[0] = view.hasFocusable();
                }
            });
            if (focusable[0]) {
                return;
            }
            SystemClock.sleep(50L);
        }
        fail("Drawer did not expose focusable items");
    }

    /** Verifies repeated local Tab traversal keeps a valid focus target. */
    public void testFocusCycleDoesNotCrash() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        for (int i = 0; i < 5; i++) {
            dispatchKeyDownUp(launcher, KeyEvent.KEYCODE_TAB);
            getInstrumentation().waitForIdleSync();
        }

        assertNotNull("Focus cycling must retain focus", launcher.getCurrentFocus());
    }

    /** Verifies initial focus state does not open drawer. */
    public void testFocusIsInitiallyNotInDrawer() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertFalse("Drawer must initially be closed", launcher.isApplicationsGridOpen());
    }
}
