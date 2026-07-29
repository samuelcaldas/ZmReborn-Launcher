package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;

public class FocusTraversalE2ETest extends ActivityInstrumentationTestCase2<Launcher> {

    public FocusTraversalE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testLauncherStartsWithFocus() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertNotNull("Launcher must have focused view", launcher.getCurrentFocus());
    }

    public void testDPADRightNavigationFromWorkspace() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View currentCellLayout = focusCurrentCellLayout(launcher);
        getInstrumentation().waitForIdleSync();
        assertTrue("Current CellLayout must receive focus before right navigation",
                currentCellLayout.hasFocus());

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Right navigation must retain focus", launcher.getCurrentFocus());
    }

    public void testDPADLeftNavigationFromWorkspace() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View currentCellLayout = focusCurrentCellLayout(launcher);
        getInstrumentation().waitForIdleSync();
        assertTrue("Current CellLayout must receive focus before left navigation",
                currentCellLayout.hasFocus());

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Left navigation must retain focus", launcher.getCurrentFocus());
    }

    public void testDPADDownNavigationToDrawer() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View currentCellLayout = focusCurrentCellLayout(launcher);
        getInstrumentation().waitForIdleSync();
        assertTrue("Current CellLayout must receive focus before down navigation",
                currentCellLayout.hasFocus());

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Down navigation must retain focus", launcher.getCurrentFocus());
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

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
        getInstrumentation().waitForIdleSync();

        assertNotNull("Up navigation must retain focus", launcher.getCurrentFocus());
    }

    public void testDockItemsAreFocusable() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View dock = launcher.findViewById(R.id.dock);
        assertNotNull("Dock must exist", dock);
        assertEquals("Dock must be visible", View.VISIBLE, dock.getVisibility());
        assertTrue("Dock must expose focusable items", dock.hasFocusable());
    }

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
        assertTrue("Drawer must expose focusable items", drawer.hasFocusable());
    }

    public void testFocusCycleDoesNotCrash() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        for (int i = 0; i < 5; i++) {
            getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB);
            getInstrumentation().waitForIdleSync();
        }

        assertNotNull("Focus cycling must retain focus", launcher.getCurrentFocus());
    }

    public void testFocusIsInitiallyNotInDrawer() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertFalse("Drawer must initially be closed", launcher.isApplicationsGridOpen());
    }
}
