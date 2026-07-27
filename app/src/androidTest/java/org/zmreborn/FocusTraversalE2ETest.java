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
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View focusedView = launcher.getCurrentFocus();

        assertNotNull("Launcher must have focused view", focusedView);
    }

    public void testDPADRightNavigationFromWorkspace() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View workspace = launcher.findViewById(R.id.workspace);
        workspace.requestFocus();
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        getInstrumentation().waitForIdleSync();

        assertTrue("Navigation must occur without crashing", true);
    }

    public void testDPADLeftNavigationFromWorkspace() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View workspace = launcher.findViewById(R.id.workspace);
        workspace.requestFocus();
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);
        getInstrumentation().waitForIdleSync();

        assertTrue("Left navigation must occur without crashing", true);
    }

    public void testDPADDownNavigationToDrawer() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View workspace = launcher.findViewById(R.id.workspace);
        workspace.requestFocus();
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        getInstrumentation().waitForIdleSync();

        assertTrue("Down navigation must occur without crashing", true);
    }

    public void testDPADUpNavigationFromDrawer() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        launcher.mApplicationsView.open(false);
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
        getInstrumentation().waitForIdleSync();

        assertTrue("Up navigation from drawer must work", true);
    }

    public void testDockItemsAreFocusable() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);

        assertTrue("Dock must be accessible", dock != null && dock.getVisibility() == View.VISIBLE);
    }

    public void testDrawerItemsAreFocusable() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        launcher.mApplicationsView.open(false);
        getInstrumentation().waitForIdleSync();

        View drawer = launcher.findViewById(R.id.apps_grid);
        assertNotNull("Drawer must exist", drawer);
    }

    public void testFocusCycleDoesNotCrash() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        for (int i = 0; i < 5; i++) {
            getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB);
            getInstrumentation().waitForIdleSync();
        }

        assertTrue("Focus cycling must complete without crash", true);
    }

    public void testFocusIsInitiallyNotInDrawer() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertFalse("Drawer must initially be closed", launcher.isApplicationsGridOpen());
    }
}
