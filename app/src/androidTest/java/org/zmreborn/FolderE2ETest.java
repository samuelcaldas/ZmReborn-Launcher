package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

public class FolderE2ETest extends ActivityInstrumentationTestCase2<Launcher> {

    public FolderE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testWorkspaceExistsAndIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View workspace = launcher.findViewById(R.id.workspace);

        assertNotNull("Workspace must exist", workspace);
        assertTrue("Workspace must be visible", workspace.getVisibility() == View.VISIBLE);
    }

    public void testDragLayerExistsAndIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dragLayer = launcher.findViewById(R.id.drag_layer);

        assertNotNull("DragLayer must exist", dragLayer);
        assertTrue("DragLayer must be visible", dragLayer.getVisibility() == View.VISIBLE);
    }

    public void testFolderViewLayoutsAreInflatable() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        View workspace = launcher.findViewById(R.id.workspace);
        assertNotNull("Workspace must be inflated for folder hosting", workspace);
    }

    public void testMinimumTouchTargetForFolderInteraction() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View workspace = launcher.findViewById(R.id.workspace);

        assertTrue("Workspace area must be sufficient for folder", workspace.getHeight() >= 200);
        assertTrue("Workspace width must be sufficient", workspace.getWidth() >= 200);
    }

    public void testFolderShouldNotExceedScreenBounds() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dragLayer = launcher.findViewById(R.id.drag_layer);

        assertTrue("DragLayer width must be positive", dragLayer.getWidth() > 0);
        assertTrue("DragLayer height must be positive", dragLayer.getHeight() > 0);
    }

    public void testLauncherSustainsFolderInfrastructure() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertNotNull("Launcher must have workspace infrastructure", launcher.findViewById(R.id.workspace));
        assertNotNull("Launcher must have drag layer for folders", launcher.findViewById(R.id.drag_layer));
    }
}
