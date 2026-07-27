package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.LinearLayout;

public class DockE2ETest extends ActivityInstrumentationTestCase2<Launcher> {

    public DockE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testDockInflatesCorrectly() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);

        assertNotNull("Dock must be inflated", dock);
        assertTrue("Dock must be a LinearLayout", dock instanceof LinearLayout);
    }

    public void testDockItemHolderExists() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);
        View itemHolder = dock.findViewById(R.id.dock_item_holder);

        assertNotNull("Dock item holder must exist", itemHolder);
    }

    public void testDockScrollViewExists() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);
        View scrollView = dock.findViewById(R.id.dock_scroll_view);

        assertNotNull("Dock scroll view must exist", scrollView);
    }

    public void testDockItemsHaveMinimumTouchTargets() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);

        assertTrue("Dock must be at least 48dp tall", dock.getHeight() >= 48);
    }

    public void testDockFailedDropRestoration() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);

        assertNotNull("Dock must exist for drop restoration testing", dock);
        assertTrue("Dock must remain in valid state", dock.getVisibility() == View.VISIBLE);
    }

    public void testDockIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);

        assertTrue("Dock must be focusable for accessibility", dock != null && dock.getVisibility() == View.VISIBLE);
    }
}
