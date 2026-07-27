package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

public class AccessibilityE2ETest extends ActivityInstrumentationTestCase2<Launcher> {

    public AccessibilityE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testDrawerViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View drawer = launcher.findViewById(R.id.apps_grid);

        assertNotNull("Drawer must exist for accessibility", drawer);
    }

    public void testWorkspaceViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View workspace = launcher.findViewById(R.id.workspace);

        assertNotNull("Workspace must be accessible", workspace);
        assertTrue("Workspace must be visible", workspace.getVisibility() == View.VISIBLE);
    }

    public void testDockViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);

        assertNotNull("Dock must exist for accessibility", dock);
        assertTrue("Dock must be visible", dock.getVisibility() == View.VISIBLE);
    }

    public void testDrawerStateOverlayIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View stateOverlay = launcher.findViewById(R.id.apps_state_overlay);
        View stateMessage = launcher.findViewById(R.id.apps_state_message);

        assertNotNull("State overlay must be accessible", stateOverlay);
        assertNotNull("State message must be accessible", stateMessage);
    }

    public void testPageIndicatorExists() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View screenIndicator = launcher.findViewById(R.id.apps_paging_screen_indicator);

        assertNotNull("Page indicator must exist for accessibility", screenIndicator);
    }

    public void testDragLayerIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dragLayer = launcher.findViewById(R.id.drag_layer);

        assertNotNull("DragLayer must exist for accessibility", dragLayer);
    }

    public void testAllPrimaryViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertNotNull("Workspace accessibility", launcher.findViewById(R.id.workspace));
        assertNotNull("DragLayer accessibility", launcher.findViewById(R.id.drag_layer));
        assertNotNull("Dock accessibility", launcher.findViewById(R.id.dock));
        assertNotNull("Apps grid accessibility", launcher.findViewById(R.id.apps_grid));
    }
}
