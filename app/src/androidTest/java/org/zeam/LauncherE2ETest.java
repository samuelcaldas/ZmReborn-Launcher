package org.zeam;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import org.zeam.Launcher;

public class LauncherE2ETest extends ActivityInstrumentationTestCase2<Launcher> {

    public LauncherE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Start the launcher activity
        getActivity();
    }

    public void testLauncherViewsInflated() {
        Launcher launcher = getActivity();
        assertNotNull("Launcher activity must not be null", launcher);
        getInstrumentation().waitForIdleSync();
        assertNotNull("Workspace must be inflated", launcher.findViewById(R.id.workspace));
        assertNotNull("DragLayer must be inflated", launcher.findViewById(R.id.drag_layer));
        assertNotNull("Dockbar must be inflated", launcher.findViewById(R.id.dock));
    }

    public void testDrawerStaysInsideDragLayerBounds() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dragLayer = launcher.findViewById(R.id.drag_layer);
        View drawer = launcher.findViewById(R.id.apps_grid);
        assertNotNull("Drawer must be inflated", drawer);
        assertTrue(drawer.getRight() <= dragLayer.getWidth());
        assertTrue(drawer.getBottom() <= dragLayer.getHeight());
        assertTrue(drawer.getLeft() >= 0);
        assertTrue(drawer.getTop() >= 0);
    }

    public void testDrawerPreferencesKeepPositiveGridValues() {
        Launcher launcher = getActivity();
        assertTrue(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsPortrait(launcher) > 0);
        assertTrue(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsLandscape(launcher) > 0);
        assertTrue(PreferencesUtil.getAppsGridHorizontalPagingContentRowsPortrait(launcher) > 0);
        assertTrue(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsPortrait(launcher) > 0);
    }
}
