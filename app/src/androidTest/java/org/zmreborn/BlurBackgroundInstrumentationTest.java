package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.zmreborn.theme.FrostedGlassDrawable;

public class BlurBackgroundInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {
    private static final String HORIZONTAL_PAGING = "2";
    private static final String TRANSPARENT_DOCK = "Transparent";
    private static final String VERTICAL_SCROLLING = "1";
    private static final int DRAWER_ALPHA = 96;
    private SharedPreferences mPreferences;
    private PreferenceSnapshot mSnapshot;

    public BlurBackgroundInstrumentationTest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getTargetContext();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mSnapshot = PreferenceSnapshot.capture(context, this.mPreferences);
        String drawerType = getName().contains("Paging")
                ? HORIZONTAL_PAGING : VERTICAL_SCROLLING;
        assertTrue("Blur test preferences must commit", this.mPreferences.edit()
                .putBoolean(context.getString(R.string.preferences_key_blur_backgrounds), true)
                .putString(context.getString(R.string.preferences_key_apps_grid_type), drawerType)
                .putString(context.getString(R.string.preferences_key_dock_background),
                        TRANSPARENT_DOCK)
                .putInt(context.getString(R.string.preferences_key_apps_grid_bg_alpha), DRAWER_ALPHA)
                .commit());
        getActivity();
        awaitWallpaperRefreshes();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            this.mSnapshot.restore(this.mPreferences);
        } finally {
            super.tearDown();
        }
    }

    public void testEnabledBlurAppliesFrostToDockAndVerticalDrawer() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertTrue("Vertical drawer must be active",
                launcher.mApplicationsView instanceof ApplicationsDrawerView);
        assertFrosted(launcher.mDock, "Dock");
        assertFrosted(launcher.mApplicationsView.getImplementingView(), "Vertical drawer");
    }

    public void testEnabledBlurAppliesFrostToDockAndPagingDrawer() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertTrue("Paging drawer must be active",
                launcher.mApplicationsView instanceof ApplicationsPagingView);
        assertFrosted(launcher.mDock, "Dock");
        assertFrosted(launcher.mApplicationsView.getImplementingView(), "Paging drawer");
    }

    public void testDisabledBlurRestoresSavedDockAndDrawerAlpha() throws Throwable {
        final Launcher launcher = getActivity();
        final String blurKey = launcher.getString(R.string.preferences_key_blur_backgrounds);
        final String dockKey = launcher.getString(R.string.preferences_key_dock_background);
        assertTrue("Blur preference must disable", this.mPreferences.edit()
                .putBoolean(blurKey, false).commit());

        runTestOnUiThread(new Runnable() {
            public void run() {
                launcher.applyBackgroundEffects();
            }
        });
        getInstrumentation().waitForIdleSync();

        assertNull("Transparent dock selection must be restored", launcher.mDock.getBackground());
        View drawer = launcher.mApplicationsView.getImplementingView();
        assertTrue("Drawer must restore solid alpha background",
                drawer.getBackground() instanceof ColorDrawable);
        ColorDrawable background = (ColorDrawable) drawer.getBackground();
        assertEquals("Drawer alpha must be restored", DRAWER_ALPHA,
                background.getColor() >>> 24);
        assertEquals("Blur toggle must not alter saved dock mode", TRANSPARENT_DOCK,
                this.mPreferences.getString(dockKey, null));
    }

    private void awaitWallpaperRefreshes() throws InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);
        Launcher.runAfterWallpaperRefreshesForTests(new Runnable() {
            public void run() {
                barrier.countDown();
            }
        });
        assertTrue("Wallpaper refresh executor must become idle",
                barrier.await(30, TimeUnit.SECONDS));
        getInstrumentation().waitForIdleSync();
    }

    private static void assertFrosted(View view, String label) {
        assertTrue(label + " must use frosted glass",
                view.getBackground() instanceof FrostedGlassDrawable);
    }

    private static final class PreferenceSnapshot {
        private final String blurKey;
        private final String drawerAlphaKey;
        private final String drawerTypeKey;
        private final String dockKey;
        private final Object blurValue;
        private final Object drawerAlphaValue;
        private final Object drawerTypeValue;
        private final Object dockValue;

        private PreferenceSnapshot(Context context, SharedPreferences preferences) {
            this.blurKey = context.getString(R.string.preferences_key_blur_backgrounds);
            this.drawerAlphaKey = context.getString(R.string.preferences_key_apps_grid_bg_alpha);
            this.drawerTypeKey = context.getString(R.string.preferences_key_apps_grid_type);
            this.dockKey = context.getString(R.string.preferences_key_dock_background);
            this.blurValue = value(preferences, this.blurKey);
            this.drawerAlphaValue = value(preferences, this.drawerAlphaKey);
            this.drawerTypeValue = value(preferences, this.drawerTypeKey);
            this.dockValue = value(preferences, this.dockKey);
        }

        static PreferenceSnapshot capture(Context context, SharedPreferences preferences) {
            return new PreferenceSnapshot(context, preferences);
        }

        void restore(SharedPreferences preferences) {
            SharedPreferences.Editor editor = preferences.edit();
            restoreValue(editor, this.blurKey, this.blurValue);
            restoreValue(editor, this.drawerAlphaKey, this.drawerAlphaValue);
            restoreValue(editor, this.drawerTypeKey, this.drawerTypeValue);
            restoreValue(editor, this.dockKey, this.dockValue);
            if (!editor.commit()) {
                throw new IllegalStateException("Blur test preferences failed to restore");
            }
        }

        private static Object value(SharedPreferences preferences, String key) {
            return preferences.contains(key) ? preferences.getAll().get(key) : null;
        }

        private static void restoreValue(SharedPreferences.Editor editor, String key,
                Object value) {
            if (value == null) {
                editor.remove(key);
                return;
            }
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
                return;
            }
            if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
                return;
            }
            editor.putString(key, (String) value);
        }
    }
}
