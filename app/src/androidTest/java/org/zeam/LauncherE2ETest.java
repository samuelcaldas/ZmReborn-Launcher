package org.zeam;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.FrameLayout;
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

        // Wait for views to attach
        getInstrumentation().waitForIdleSync();

        // Assert critical views exist in layout
        assertNotNull("Workspace must be inflated", launcher.findViewById(R.id.workspace));
        assertNotNull("DragLayer must be inflated", launcher.findViewById(R.id.drag_layer));
        assertNotNull("Dockbar must be inflated", launcher.findViewById(R.id.dock));
    }
}
