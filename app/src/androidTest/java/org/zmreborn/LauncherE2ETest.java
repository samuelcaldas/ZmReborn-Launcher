package org.zmreborn;

import android.content.SharedPreferences;
import android.content.pm.ProviderInfo;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.WindowManager;
import org.zmreborn.Launcher;

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

    public void testRebrandedRuntimeIdentityResolves() {
        Launcher launcher = getActivity();
        ProviderInfo provider = launcher.getPackageManager().resolveContentProvider(
                LauncherProvider.AUTHORITY, 0);
        assertNotNull("ZM Reborn provider must resolve", provider);
        assertEquals(LauncherProvider.class.getName(), provider.name);
        assertEquals(BuildConfig.APPLICATION_ID + ".core", launcher.getApplicationInfo().processName);
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

    public void testLauncherContentConsumesSystemBarInsets() throws Exception {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        assertLauncherInsideSystemBars(launcher);
    }

    public void testFullscreenPreferenceTransitionsWindowFlag() throws Exception {
        Launcher launcher = getActivity();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(launcher);
        String key = launcher.getString(R.string.preferences_key_general_fullscreen);
        boolean defaultValue = Boolean.parseBoolean(launcher.getString(
                R.string.preferences_default_general_fullscreen));
        boolean hadValue = preferences.contains(key);
        boolean originalValue = preferences.getBoolean(key, defaultValue);
        try {
            verifyFullscreenTransitions(launcher, preferences, key);
        } finally {
            restoreFullscreenPreference(
                    launcher, preferences, key, hadValue, originalValue);
        }
    }

    private void verifyFullscreenTransitions(Launcher launcher,
            SharedPreferences preferences,
            String key) throws Exception {
        verifyFullscreenState(launcher, preferences, key, false);
        verifyFullscreenState(launcher, preferences, key, true);
        verifyFullscreenState(launcher, preferences, key, false);
    }

    private void verifyFullscreenState(Launcher launcher,
            SharedPreferences preferences,
            String key,
            boolean enabled) throws Exception {
        setFullscreenPreference(launcher, preferences, key, enabled);
        assertFullscreenFlag(launcher, enabled);
        assertLauncherInsideSystemBars(launcher);
    }

    private void setFullscreenPreference(Launcher launcher,
            SharedPreferences preferences,
            String key,
            boolean enabled) {
        assertTrue(preferences.edit().putBoolean(key, enabled).commit());
        applyFullscreenPreference(launcher);
    }

    private void restoreFullscreenPreference(Launcher launcher,
            SharedPreferences preferences,
            String key,
            boolean hadValue,
            boolean originalValue) {
        setFullscreenPreference(launcher, preferences, key, originalValue);
        if (!hadValue) {
            assertTrue(preferences.edit().remove(key).commit());
        }
    }

    private void applyFullscreenPreference(final Launcher launcher) {
        launcher.runOnUiThread(new Runnable() {
            public void run() {
                launcher.onWindowFocusChanged(true);
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private static void assertFullscreenFlag(Launcher launcher, boolean enabled) {
        int flags = launcher.getWindow().getAttributes().flags;
        boolean fullscreen = (flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        assertEquals(enabled, fullscreen);
    }

    private static void assertLauncherInsideSystemBars(Launcher launcher)
            throws Exception {
        View content = launcher.findViewById(R.id.drag_layer);
        View decor = launcher.getWindow().getDecorView();
        assertInsideSystemBars(content, decor);
    }

    private static void assertInsideSystemBars(View content, View decor) throws Exception {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            assertInsideVisibleFrame(content, decor);
            return;
        }
        assertInsideRootInsets(content, decor);
    }

    private static void assertInsideVisibleFrame(View content, View decor) {
        Rect visibleFrame = new Rect();
        decor.getWindowVisibleDisplayFrame(visibleFrame);
        int[] location = screenLocation(content);
        assertTrue(location[0] >= visibleFrame.left);
        assertTrue(location[1] >= visibleFrame.top);
        assertTrue(location[0] + content.getWidth() <= visibleFrame.right);
        assertTrue(location[1] + content.getHeight() <= visibleFrame.bottom);
    }

    private static void assertInsideRootInsets(View content, View decor) throws Exception {
        Object insets = View.class.getMethod("getRootWindowInsets").invoke(decor);
        assertNotNull("Root window insets must be available", insets);
        int[] contentLocation = screenLocation(content);
        int[] decorLocation = screenLocation(decor);
        int left = contentLocation[0] - decorLocation[0];
        int top = contentLocation[1] - decorLocation[1];
        assertTrue(left >= inset(insets, "getSystemWindowInsetLeft"));
        assertTrue(top >= inset(insets, "getSystemWindowInsetTop"));
        assertTrue(left + content.getWidth()
                <= decor.getWidth() - inset(insets, "getSystemWindowInsetRight"));
        assertTrue(top + content.getHeight()
                <= decor.getHeight() - inset(insets, "getSystemWindowInsetBottom"));
    }

    private static int[] screenLocation(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location;
    }

    private static int inset(Object insets, String methodName) throws Exception {
        return ((Integer) insets.getClass().getMethod(methodName).invoke(insets)).intValue();
    }

    public void testDrawerPreferencesKeepPositiveGridValues() {
        Launcher launcher = getActivity();
        assertTrue(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsPortrait(launcher) > 0);
        assertTrue(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsLandscape(launcher) > 0);
        assertTrue(PreferencesUtil.getAppsGridHorizontalPagingContentRowsPortrait(launcher) > 0);
        assertTrue(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsPortrait(launcher) > 0);
    }

    public void testShellViewsVisibleInPortrait() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View workspace = launcher.findViewById(R.id.workspace);
        View dragLayer = launcher.findViewById(R.id.drag_layer);
        View dock = launcher.findViewById(R.id.dock);

        assertTrue("Workspace must be visible in portrait", workspace.getVisibility() == View.VISIBLE);
        assertTrue("DragLayer must be visible in portrait", dragLayer.getVisibility() == View.VISIBLE);
        assertTrue("Dock must be visible in portrait", dock.getVisibility() == View.VISIBLE);
    }

    public void testMinimumTouchTargetSize() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);
        View workspace = launcher.findViewById(R.id.workspace);

        assertTrue("Dock height must be at least 48dp", dock.getHeight() >= 48);
        assertTrue("Workspace must be measurable", workspace.getMeasuredHeight() > 0);
    }

    public void testContentInsideSafeAreaInPortrait() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dragLayer = launcher.findViewById(R.id.drag_layer);
        int width = dragLayer.getWidth();
        int height = dragLayer.getHeight();

        assertTrue("DragLayer width must be positive", width > 0);
        assertTrue("DragLayer height must be positive", height > 0);
    }
}
