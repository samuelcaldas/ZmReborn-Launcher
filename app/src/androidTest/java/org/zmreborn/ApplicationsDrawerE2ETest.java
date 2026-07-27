package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;

public class ApplicationsDrawerE2ETest extends ActivityInstrumentationTestCase2<Launcher> {

    public ApplicationsDrawerE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testApplicationsViewInflates() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        assertNotNull("Applications view must exist", launcher.mApplicationsView);
        assertNotNull("Implementing view must exist", launcher.mApplicationsView.getImplementingView());
    }

    public void testDrawerOpenCloseRoundTrip() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        ApplicationsView applicationsView = launcher.mApplicationsView;
        applicationsView.open(false);
        assertTrue("Drawer must be open", launcher.isApplicationsGridOpen());
        applicationsView.close(false);
        assertFalse("Drawer must be closed", launcher.isApplicationsGridOpen());
    }

    public void testDrawerUsesSharedStateOverlay() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        assertNotNull("Drawer state overlay must exist",
                launcher.findViewById(R.id.apps_state_overlay));
        assertNotNull("Drawer state message must exist",
                launcher.findViewById(R.id.apps_state_message));
        assertNotNull("Drawer retry action must exist",
                launcher.findViewById(R.id.apps_state_retry));
        assertNotNull("Drawer close action must exist",
                launcher.findViewById(R.id.apps_state_close));
    }

    public void testDrawerGrantsFocusToFirstItemOnOpen() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        ApplicationsView applicationsView = launcher.mApplicationsView;
        applicationsView.open(false);
        getInstrumentation().waitForIdleSync();

        assertTrue("Drawer must be open", launcher.isApplicationsGridOpen());
        assertNotNull("Applications view must have implementing view",
                applicationsView.getImplementingView());
    }

    public void testDrawerHandlesEmptyStateWithFocus() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        ApplicationsView applicationsView = launcher.mApplicationsView;
        assertNotNull("Implementing view must handle focus even when empty",
                applicationsView.getImplementingView());
    }

    public void testDrawerNavigationDoesNotCrashOnEdges() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        ApplicationsView applicationsView = launcher.mApplicationsView;
        applicationsView.open(false);
        getInstrumentation().waitForIdleSync();

        applicationsView.close(false);
        getInstrumentation().waitForIdleSync();
        assertFalse("Drawer must be closed", launcher.isApplicationsGridOpen());
    }

    public void testDrawerStateOverlayIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View stateOverlay = launcher.findViewById(R.id.apps_state_overlay);
        View stateMessage = launcher.findViewById(R.id.apps_state_message);

        assertNotNull("State overlay must exist", stateOverlay);
        assertNotNull("State message must exist", stateMessage);
    }

    public void testDrawerStateRetryActionExists() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View retryButton = launcher.findViewById(R.id.apps_state_retry);

        assertNotNull("Retry button must exist", retryButton);
        assertTrue("Retry button must be focusable", retryButton.isFocusable());
    }

    public void testDrawerStateCloseActionExists() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View closeButton = launcher.findViewById(R.id.apps_state_close);

        assertNotNull("Close button must exist", closeButton);
        assertTrue("Close button must be focusable", closeButton.isFocusable());
    }

    public void testDrawerImplementingViewUsesCorrectAdapter() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        ApplicationsView applicationsView = launcher.mApplicationsView;
        View implementingView = applicationsView.getImplementingView();

        assertNotNull("Implementing view must exist", implementingView);
    }

    public void testDrawerOpenCloseMultipleTimes() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        ApplicationsView applicationsView = launcher.mApplicationsView;

        for (int i = 0; i < 3; i++) {
            applicationsView.open(false);
            assertTrue("Drawer must be open on iteration " + i, launcher.isApplicationsGridOpen());
            applicationsView.close(false);
            assertFalse("Drawer must be closed on iteration " + i, launcher.isApplicationsGridOpen());
        }
    }

    public void testPagingViewCreatesScrollableViewportPages() {
        final Launcher launcher = getActivity();
        final ViewPager[] pagerHolder = new ViewPager[1];
        launcher.runOnUiThread(new Runnable() {
            public void run() {
                pagerHolder[0] = addTwoPagePager(launcher);
            }
        });
        getInstrumentation().waitForIdleSync();

        ViewPager pager = pagerHolder[0];
        LinearLayout pageHolder = (LinearLayout) pager.getChildAt(0);
        assertEquals(2, pageHolder.getChildCount());
        assertEquals(pager.getWidth(), pageHolder.getChildAt(0).getWidth());
        assertEquals(pager.getWidth(), pageHolder.getChildAt(1).getWidth());
        assertTrue("Multiple pages must create horizontal scroll range", pager.canScrollHorizontally(1));
    }

    private static ViewPager addTwoPagePager(Launcher launcher) {
        ViewPager pager = new ViewPager(launcher);
        launcher.addContentView(pager, new FrameLayout.LayoutParams(320, 240));
        ArrayList<View> pages = new ArrayList<View>();
        pages.add(new View(launcher));
        pages.add(new View(launcher));
        pager.setPagingViews(pages);
        return pager;
    }

    public void testDrawerMinimumTouchTargets() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View retryButton = launcher.findViewById(R.id.apps_state_retry);
        View closeButton = launcher.findViewById(R.id.apps_state_close);

        assertTrue("Retry button height must be at least 48dp", retryButton.getHeight() >= 48);
        assertTrue("Close button height must be at least 48dp", closeButton.getHeight() >= 48);
    }
}
