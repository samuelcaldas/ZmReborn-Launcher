package org.zmreborn;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
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

    public void testAppearanceFingerprintRecreatesLauncherOnResume() {
        final Launcher launcher = getActivity();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(launcher);
        final String appearanceKey = launcher.getString(
                R.string.preferences_key_application_appearance);
        final boolean hadOriginalAppearance = preferences.contains(appearanceKey);
        final String originalAppearance = preferences.getString(appearanceKey, null);
        final String targetAppearance = Appearance.DARK.equals(
                Appearance.getSelectedAppearance(launcher)) ? Appearance.LIGHT : Appearance.DARK;
        final boolean originalRestart = Launcher.sRestart;
        android.app.Instrumentation.ActivityMonitor settingsMonitor = null;
        final ReplacementLauncherObserver recreationObserver =
                new ReplacementLauncherObserver(launcher);
        boolean recreationObserverRegistered = false;
        Launcher recreatedLauncher = null;

        try {
            settingsMonitor = getInstrumentation().addMonitor(Preferences.class.getName(), null, false);
            launcher.startActivity(new Intent(launcher, Preferences.class));
            final Preferences settings = (Preferences) getInstrumentation().waitForMonitorWithTimeout(
                    settingsMonitor, 30000L);
            assertNotNull("Preferences activity must open", settings);
            getInstrumentation().waitForIdleSync();
            assertTrue("Target appearance must persist", preferences.edit()
                    .putString(appearanceKey, targetAppearance).commit());
            Launcher.sRestart = true;
            launcher.getApplication().registerActivityLifecycleCallbacks(recreationObserver);
            recreationObserverRegistered = true;
            settings.runOnUiThread(new Runnable() {
                public void run() {
                    settings.finish();
                }
            });
            recreatedLauncher = recreationObserver.awaitLauncher();
            assertNotNull("Appearance change must recreate Launcher", recreatedLauncher);
            assertNotSame("Appearance change must replace Launcher", launcher, recreatedLauncher);
            getInstrumentation().waitForIdleSync();
            assertEquals("Appearance and restart changes must create exactly one replacement Launcher", 1,
                    recreationObserver.getReplacementLauncherCreationCount());
            assertEquals("Target appearance must persist", targetAppearance,
                    preferences.getString(appearanceKey, null));
            int expectedNightMode = Appearance.DARK.equals(targetAppearance)
                    ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
            assertEquals("Recreated Launcher must use target night mode", expectedNightMode,
                    recreatedLauncher.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK);
            assertNotNull("Workspace must be inflated after appearance recreation",
                    recreatedLauncher.findViewById(R.id.workspace));
        } finally {
            try {
                SharedPreferences.Editor editor = preferences.edit();
                if (hadOriginalAppearance) {
                    editor.putString(appearanceKey, originalAppearance);
                }
                if (!hadOriginalAppearance) {
                    editor.remove(appearanceKey);
                }
                assertTrue("Original appearance preference must be restored", editor.commit());
            } finally {
                Launcher.sRestart = originalRestart;
                try {
                    if (recreationObserverRegistered) {
                        launcher.getApplication().unregisterActivityLifecycleCallbacks(recreationObserver);
                    }
                } finally {
                    try {
                        finishReplacementLauncher(recreationObserver.getLatestLauncher());
                    } finally {
                        if (settingsMonitor != null) {
                            getInstrumentation().removeMonitor(settingsMonitor);
                        }
                    }
                }
            }
        }
    }

    public void testWorkspaceGridSettingsPersistAcrossLauncherRecreation() {
        final Launcher launcher = getActivity();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(launcher);
        final String rowsKey = launcher.getString(
                R.string.preferences_key_workspace_content_grid_rows);
        final String columnsKey = launcher.getString(
                R.string.preferences_key_workspace_content_grid_columns);
        final int defaultRows = Integer.parseInt(launcher.getString(
                R.string.preferences_default_workspace_content_grid_rows));
        final int defaultColumns = Integer.parseInt(launcher.getString(
                R.string.preferences_default_workspace_content_grid_columns));
        final boolean hadRows = preferences.contains(rowsKey);
        final boolean hadColumns = preferences.contains(columnsKey);
        final int originalRows = preferences.getInt(rowsKey, defaultRows);
        final int originalColumns = preferences.getInt(columnsKey, defaultColumns);
        final int targetRows = differentGridValue(originalRows);
        final int targetColumns = differentGridValue(originalColumns, targetRows);
        final boolean originalRestart = Launcher.sRestart;
        final int originalProcessId = Process.myPid();
        android.app.Instrumentation.ActivityMonitor settingsMonitor = null;
        final ReplacementLauncherObserver recreationObserver =
                new ReplacementLauncherObserver(launcher);
        boolean recreationObserverRegistered = false;
        Launcher recreatedLauncher = null;

        try {
            settingsMonitor = getInstrumentation().addMonitor(
                    Preferences.class.getName(), null, false);
            launcher.startActivity(new Intent(launcher, Preferences.class));
            final Preferences settings = (Preferences) getInstrumentation().waitForMonitorWithTimeout(
                    settingsMonitor, 30000L);
            assertNotNull("Preferences activity must open", settings);
            getInstrumentation().waitForIdleSync();
            changeWorkspaceGridPreference(settings, columnsKey, targetColumns,
                    originalColumns);
            int rowsBeforePause = changeWorkspaceGridPreference(settings, rowsKey, targetRows,
                    originalRows);
            assertEquals("Selected rows must update inline before persistence", targetRows,
                    ((InlineStepperPreference) settings.findPreference(rowsKey)).getValue());
            assertEquals("Selected columns must update inline before persistence", targetColumns,
                    ((InlineStepperPreference) settings.findPreference(columnsKey)).getValue());
            assertEquals("Latest grid change must remain pending inside input batch",
                    originalRows, rowsBeforePause);
            assertTrue("Grid changes must request Launcher restart", Launcher.sRestart);

            launcher.getApplication().registerActivityLifecycleCallbacks(recreationObserver);
            recreationObserverRegistered = true;
            settings.runOnUiThread(new Runnable() {
                public void run() {
                    settings.finish();
                }
            });
            recreatedLauncher = recreationObserver.awaitLauncher();
            assertNotNull("Grid change must recreate Launcher", recreatedLauncher);
            assertEquals("Grid reload must not terminate process", originalProcessId,
                    Process.myPid());
            getInstrumentation().waitForIdleSync();
            assertEquals("Rows must flush before Launcher resumes", targetRows,
                    preferences.getInt(rowsKey, -1));
            assertEquals("Columns must flush before Launcher resumes", targetColumns,
                    preferences.getInt(columnsKey, -1));
            assertEquals(targetRows, PreferencesUtil.getContentGridRows(recreatedLauncher));
            assertEquals(targetColumns, PreferencesUtil.getContentGridColumns(recreatedLauncher));
            assertWorkspaceGridGeometry(recreatedLauncher, targetRows, targetColumns);
        } finally {
            try {
                restoreIntPreference(preferences, rowsKey, hadRows, originalRows);
            } finally {
                try {
                    restoreIntPreference(preferences, columnsKey, hadColumns, originalColumns);
                } finally {
                    Launcher.sRestart = originalRestart;
                    try {
                        if (recreationObserverRegistered) {
                            launcher.getApplication().unregisterActivityLifecycleCallbacks(recreationObserver);
                        }
                    } finally {
                        try {
                            finishReplacementLauncher(recreationObserver.getLatestLauncher());
                        } finally {
                            if (settingsMonitor != null) {
                                getInstrumentation().removeMonitor(settingsMonitor);
                            }
                        }
                    }
                }
            }
        }
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
        float minimumTouchTarget = 48 * launcher.getResources().getDisplayMetrics().density;

        assertTrue("Dock height must be at least 48dp", dock.getHeight() >= minimumTouchTarget);
        assertTrue("Workspace must be measurable", workspace.getMeasuredHeight() > 0);
    }

    public void testSystemInsetWiringReachesDockWithoutBreakingLayout() throws Exception {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        launcher.runOnUiThread(new Runnable() {
            public void run() {
                launcher.getWindow().getDecorView().requestApplyInsets();
            }
        });
        getInstrumentation().waitForIdleSync();

        assertNotNull("Dock must receive a system-bar insets Rect once the DecorView "
                + "listener has fired", readPrivateRect(launcher.mDock, "mSystemBarInsets"));
        assertLauncherInsideSystemBars(launcher);
    }

    public void testCurrentCellLayoutClampsOversizedWidgetDimensions() {
        final Launcher launcher = getActivity();
        final CellLayout[] layouts = new CellLayout[1];
        final boolean[] measured = new boolean[1];
        final boolean[] geometryReady = new boolean[1];
        final int[] counts = new int[2];
        final int[][] spans = new int[1][];
        getInstrumentation().waitForIdleSync();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                CellLayout layout = (CellLayout) launcher.mWorkspace.getChildAt(
                        launcher.mWorkspace.getCurrentScreen());
                layouts[0] = layout;
                if (layout == null) {
                    return;
                }
                measured[0] = layout.getMeasuredWidth() > 0 && layout.getMeasuredHeight() > 0;
                geometryReady[0] = layout.isWidgetSizingGeometryReady();
                if (!geometryReady[0]) {
                    return;
                }
                counts[0] = layout.getCountX();
                counts[1] = layout.getCountY();
                spans[0] = layout.rectToCell(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        });

        assertNotNull("Current workspace page must be a CellLayout", layouts[0]);
        assertTrue("Current CellLayout must be measured", measured[0]);
        assertTrue("Widget-sizing geometry must be ready", geometryReady[0]);
        assertNotNull("Oversized widget dimensions must resolve to spans", spans[0]);
        assertEquals(counts[0], spans[0][0]);
        assertEquals(counts[1], spans[0][1]);
    }

    public void testUnmeasuredCellLayoutHasNoWidgetSizingGeometry() {
        final Launcher launcher = getActivity();
        final boolean[] geometryReady = new boolean[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                CellLayout layout = new CellLayout(launcher);
                geometryReady[0] = layout.isWidgetSizingGeometryReady();
            }
        });

        assertFalse("Unmeasured CellLayout geometry must not be ready", geometryReady[0]);
    }

    private int changeWorkspaceGridPreference(final Preferences settings, final String key,
            final int targetValue, final int storedFallback) {
        PreferenceScreen workspaceScreen = (PreferenceScreen) settings.getPreferenceScreen()
                .getPreference(1);
        if (workspaceScreen.getDialog() == null || !workspaceScreen.getDialog().isShowing()) {
            clickPreference(settings.getListView(), workspaceScreen);
        }
        Dialog workspaceDialog = workspaceScreen.getDialog();
        assertNotNull("Workspace preference screen must open", workspaceDialog);
        final ListView workspaceList = (ListView) workspaceDialog.findViewById(android.R.id.list);
        assertNotNull("Workspace preference list must exist", workspaceList);
        final InlineStepperPreference preference = (InlineStepperPreference) settings
                .findPreference(key);
        final ListAdapter adapter = workspaceList.getAdapter();
        final int position = preferencePosition(adapter, preference);
        assertTrue("Grid preference must exist in Workspace screen", position >= 0);
        final int[] storedValue = new int[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                View row = adapter.getView(position, null, workspaceList);
                int actionId = targetValue > preference.getValue()
                        ? R.id.increment : R.id.decrement;
                View action = row.findViewById(actionId);
                while (preference.getValue() != targetValue) {
                    assertTrue("Inline grid action must handle click", action.performClick());
                }
                assertEquals(String.valueOf(targetValue), ((android.widget.TextView) row
                        .findViewById(R.id.settings_numeric_value)).getText().toString());
                storedValue[0] = PreferenceManager.getDefaultSharedPreferences(settings)
                        .getInt(key, storedFallback);
            }
        });
        assertEquals("Grid preference object must retain selected value", targetValue,
                preference.getValue());
        return storedValue[0];
    }

    private void clickPreference(final ListView listView, final Preference preference) {
        final ListAdapter adapter = listView.getAdapter();
        final int position = preferencePosition(adapter, preference);
        assertTrue("Preference must be present in visible hierarchy", position >= 0);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                View row = adapter.getView(position, null, listView);
                listView.performItemClick(row, position, adapter.getItemId(position));
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private static int preferencePosition(ListAdapter adapter, Preference preference) {
        for (int position = 0; position < adapter.getCount(); position++) {
            if (adapter.getItem(position) == preference) {
                return position;
            }
        }
        return -1;
    }

    private void finishReplacementLauncher(final Launcher launcher) {
        if (launcher == null) {
            return;
        }
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                if (launcher.isFinishing() || launcher.isDestroyed()) {
                    return;
                }
                launcher.finish();
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private static final class ReplacementLauncherObserver
            implements Application.ActivityLifecycleCallbacks {
        private final Launcher originalLauncher;
        private final IdentityHashMap<Launcher, Boolean> replacementLaunchers =
                new IdentityHashMap<Launcher, Boolean>();
        private Launcher latestReplacementLauncher;

        ReplacementLauncherObserver(Launcher originalLauncher) {
            this.originalLauncher = originalLauncher;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (!(activity instanceof Launcher) || activity == originalLauncher) {
                return;
            }
            Launcher replacementLauncher = (Launcher) activity;
            synchronized (this) {
                if (replacementLaunchers.containsKey(replacementLauncher)) {
                    return;
                }
                replacementLaunchers.put(replacementLauncher, Boolean.TRUE);
                latestReplacementLauncher = replacementLauncher;
                notifyAll();
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        Launcher awaitLauncher() {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            long deadline = SystemClock.uptimeMillis() + 30000L;
            synchronized (this) {
                while (latestReplacementLauncher == null) {
                    long remaining = deadline - SystemClock.uptimeMillis();
                    if (remaining <= 0L) {
                        return null;
                    }
                    try {
                        wait(remaining);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
                return latestReplacementLauncher;
            }
        }

        synchronized int getReplacementLauncherCreationCount() {
            return replacementLaunchers.size();
        }

        synchronized Launcher getLatestLauncher() {
            return latestReplacementLauncher;
        }
    }

    private void assertWorkspaceGridGeometry(final Launcher launcher, final int rows,
            final int columns) {
        final CellLayout[] currentLayout = new CellLayout[1];
        long deadline = SystemClock.uptimeMillis() + 30000L;
        while (currentLayout[0] == null && SystemClock.uptimeMillis() < deadline) {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    int currentScreen = launcher.mWorkspace.getCurrentScreen();
                    View child = launcher.mWorkspace.getChildAt(currentScreen);
                    if (!(child instanceof CellLayout)) {
                        return;
                    }
                    CellLayout layout = (CellLayout) child;
                    if (!layout.isWidgetSizingGeometryReady()) {
                        return;
                    }
                    currentLayout[0] = layout;
                }
            });
            if (currentLayout[0] == null) {
                SystemClock.sleep(100L);
            }
        }
        assertNotNull("Recreated Launcher must bind a layout-ready current CellLayout",
                currentLayout[0]);
        assertEquals(columns, currentLayout[0].getCountX());
        assertEquals(rows, currentLayout[0].getCountY());
    }

    private static int differentGridValue(int originalValue) {
        return differentGridValue(originalValue, -1);
    }

    private static int differentGridValue(int originalValue, int excludedValue) {
        for (int candidate = 3; candidate <= 8; candidate++) {
            if (candidate != originalValue && candidate != excludedValue) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("No distinct workspace grid value is available");
    }

    private static void restoreIntPreference(SharedPreferences preferences, String key,
            boolean hadValue, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        if (hadValue) {
            editor.putInt(key, value);
        } else {
            editor.remove(key);
        }
        assertTrue("Original grid preference must be restored", editor.commit());
    }

    private static Rect readPrivateRect(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Rect) field.get(target);
    }
}
