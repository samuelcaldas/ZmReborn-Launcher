package org.zeam;

import android.test.ActivityInstrumentationTestCase2;

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

    public void testDrawerOpenCloseRoundTrip() throws Throwable {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                ApplicationsView applicationsView = launcher.mApplicationsView;
                applicationsView.open(false);
                assertTrue("Drawer must be open", launcher.isApplicationsGridOpen());
                applicationsView.close(false);
                assertFalse("Drawer must be closed", launcher.isApplicationsGridOpen());
            }
        });
    }
}
