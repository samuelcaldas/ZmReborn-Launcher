package org.zmreborn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.zmreborn.theme.WallpaperColorExtractor;

public class WallpaperPaletteE2ETest extends ActivityInstrumentationTestCase2<Launcher> {
    private static final String WALLPAPER_COLORS_PREFERENCES =
            "org.zmreborn.theme.wallpaper_colors";
    private static final int CACHE_SCHEMA_VERSION = 2;
    private static final String DARK_PREFIX = "dark_";
    private static final String LIGHT_PREFIX = "light_";
    private static final String ON_PRIMARY_KEY = "on_primary";
    private static final String ON_SURFACE_KEY = "on_surface";
    private static final String OUTLINE_KEY = "outline";
    private static final String PRIMARY_KEY = "primary";
    private static final String SCHEMA_KEY = "schema";
    private static final String SURFACE_KEY = "surface";
    private static final String SURFACE_VARIANT_KEY = "surface_variant";
    private static final int TEST_PRIMARY = 0xff8338ec;
    private static final int TEST_ON_PRIMARY = 0xff0d1117;
    private static final int TEST_SURFACE = 0xff183a37;
    private static final int TEST_ON_SURFACE = 0xfffefae0;
    private static final int TEST_SURFACE_VARIANT = 0xff264653;
    private static final int TEST_OUTLINE = 0xffe76f51;
    private static final int STALE_COLOR = 0xff010203;
    private static final long ASYNC_TIMEOUT_MILLIS = 30000L;
    private static final String VERTICAL_DRAWER_TYPE = "1";
    private SharedPreferences mDefaultPreferences;
    private String mDrawerTypeKey;
    private String mOriginalDrawerType;
    private boolean mHadOriginalDrawerType;

    public WallpaperPaletteE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context targetContext = getInstrumentation().getTargetContext();
        this.mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(targetContext);
        this.mDrawerTypeKey = targetContext.getString(R.string.preferences_key_apps_grid_type);
        this.mHadOriginalDrawerType = this.mDefaultPreferences.contains(this.mDrawerTypeKey);
        this.mOriginalDrawerType = this.mDefaultPreferences.getString(this.mDrawerTypeKey, null);
        assertTrue("Vertical drawer preference must commit", this.mDefaultPreferences.edit()
                .putString(this.mDrawerTypeKey, VERTICAL_DRAWER_TYPE).commit());
        getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SharedPreferences.Editor editor = this.mDefaultPreferences.edit();
            if (this.mHadOriginalDrawerType) {
                editor.putString(this.mDrawerTypeKey, this.mOriginalDrawerType);
            } else {
                editor.remove(this.mDrawerTypeKey);
            }
            assertTrue("Drawer preference must restore", editor.commit());
        } finally {
            super.tearDown();
        }
    }

    public void testApplyWallpaperPaletteReappliesOpenSurfaces() throws Throwable {
        final Launcher launcher = getActivity();
        final SharedPreferences palettePreferences = launcher.getSharedPreferences(
                WALLPAPER_COLORS_PREFERENCES, Context.MODE_PRIVATE);
        final int configuredAlpha = PreferencesUtil.getAppsGridBackgroundAlpha(launcher);
        Map<String, Object> paletteSnapshot = null;
        boolean wasRefreshSuppressed = Launcher.sSuppressWallpaperRefreshForTests;
        Launcher.setWallpaperRefreshSuppressedForTests(true);

        try {
            getInstrumentation().waitForIdleSync();
            awaitPaletteRefreshQueue();
            awaitLauncherData(launcher);
            paletteSnapshot = snapshotPreferences(palettePreferences);
            writeTestPalette(launcher, palettePreferences);
            final int expectedPrimary = WallpaperColorExtractor.getPrimary(launcher);
            final int expectedOnPrimary = WallpaperColorExtractor.getOnPrimary(launcher);
            final int expectedSurface = WallpaperColorExtractor.getSurface(launcher);
            final int expectedOnSurface = WallpaperColorExtractor.getOnSurface(launcher);
            final int expectedSurfaceVariant = WallpaperColorExtractor.getSurfaceVariant(launcher);
            final int expectedOutline = WallpaperColorExtractor.getOutline(launcher);
            if (Build.VERSION.SDK_INT >= 31) {
                assertEquals("System-dynamic primary must ignore cache poison",
                        launcher.getColor(R.color.m3_primary), expectedPrimary);
                assertEquals("System-dynamic on-primary must ignore cache poison",
                        launcher.getColor(R.color.m3_on_primary), expectedOnPrimary);
                assertEquals("System-dynamic surface must ignore cache poison",
                        launcher.getColor(R.color.m3_surface), expectedSurface);
                assertEquals("System-dynamic on-surface must ignore cache poison",
                        launcher.getColor(R.color.m3_on_surface), expectedOnSurface);
                assertEquals("System-dynamic surface variant must ignore cache poison",
                        launcher.getColor(R.color.m3_surface_variant), expectedSurfaceVariant);
                assertEquals("System-dynamic outline must ignore cache poison",
                        launcher.getColor(R.color.m3_outline), expectedOutline);
            }
            if (Build.VERSION.SDK_INT < 31) {
                assertEquals("Current-brightness cache must win over stale opposite primary",
                        TEST_PRIMARY, expectedPrimary);
                assertEquals("Current-brightness cache must win over stale opposite on-primary",
                        TEST_ON_PRIMARY, expectedOnPrimary);
                assertEquals("Current-brightness cache must win over stale opposite surface",
                        TEST_SURFACE, expectedSurface);
                assertEquals("Current-brightness cache must win over stale opposite on-surface",
                        TEST_ON_SURFACE, expectedOnSurface);
                assertEquals("Current-brightness cache must win over stale opposite surface variant",
                        TEST_SURFACE_VARIANT, expectedSurfaceVariant);
                assertEquals("Current-brightness cache must win over stale opposite outline",
                        TEST_OUTLINE, expectedOutline);
            }
            assertTrue("Vertical drawer must be active for palette reapplication",
                    launcher.mApplicationsView instanceof ApplicationsDrawerView);
            verifyDrawerPalette(launcher, configuredAlpha, expectedOnSurface, expectedSurface);
            verifyUserFolderPalette(launcher, expectedOnSurface);
            verifyAppListFolderPalette(launcher, expectedPrimary, expectedOnSurface);
        } finally {
            try {
                runOnUiThread(new UiAction() {
                    public void run() throws Throwable {
                        try {
                            dismissAppListFolder(launcher);
                        } finally {
                            try {
                                invokeLauncherMethod(launcher, "closeFolder", new Class<?>[0]);
                            } finally {
                                invokeLauncherMethod(launcher, "closeApplicationsGrid",
                                        new Class<?>[] {Boolean.TYPE}, Boolean.FALSE);
                            }
                        }
                    }
                });
            } finally {
                try {
                    if (paletteSnapshot != null) {
                        restorePreferences(palettePreferences, paletteSnapshot);
                        runOnUiThread(new UiAction() {
                            public void run() throws Throwable {
                                invokeLauncherMethod(launcher, "applyWallpaperPalette",
                                        new Class<?>[0]);
                            }
                        });
                    }
                } finally {
                    Launcher.setWallpaperRefreshSuppressedForTests(wasRefreshSuppressed);
                }
            }
        }
    }

    private void verifyDrawerPalette(final Launcher launcher, int configuredAlpha,
            int expectedOnSurface, int expectedSurface) throws Throwable {
        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                invokeLauncherMethod(launcher, "openApplicationsGrid",
                        new Class<?>[] {Boolean.TYPE}, Boolean.FALSE);
            }
        });
        getInstrumentation().waitForIdleSync();

        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                TextView label = findFirstVisibleTextView(
                        launcher.mApplicationsView.getImplementingView());
                if (label == null) {
                    throw new IllegalStateException("Drawer has no visible application label");
                }
                label.setTextColor(STALE_COLOR);
                invokeLauncherMethod(launcher, "applyWallpaperPalette", new Class<?>[0]);
            }
        });
        getInstrumentation().waitForIdleSync();

        final int[] labelColor = new int[1];
        final int[] backgroundColor = new int[1];
        runOnUiThread(new UiAction() {
            public void run() {
                TextView label = findFirstVisibleTextView(
                        launcher.mApplicationsView.getImplementingView());
                if (label == null) {
                    throw new IllegalStateException("Drawer has no visible application label");
                }
                if (!(launcher.mApplicationsView.getImplementingView()
                        .getBackground() instanceof ColorDrawable)) {
                    throw new IllegalStateException("Drawer background is not a ColorDrawable");
                }
                labelColor[0] = label.getCurrentTextColor();
                backgroundColor[0] = ((ColorDrawable) launcher.mApplicationsView
                        .getImplementingView().getBackground()).getColor();
            }
        });

        assertEquals("Drawer label must use the live on-surface color", expectedOnSurface,
                labelColor[0]);
        assertEquals("Drawer background must use the live surface and configured alpha",
                Color.argb(configuredAlpha, Color.red(expectedSurface), Color.green(expectedSurface),
                        Color.blue(expectedSurface)), backgroundColor[0]);
    }

    private void verifyUserFolderPalette(final Launcher launcher, int expectedOnSurface)
            throws Throwable {
        final UserFolderInfo folderInfo = new UserFolderInfo();
        folderInfo.container = -200;
        folderInfo.title = "Palette user folder";
        final Folder[] folder = new Folder[1];
        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                invokeLauncherMethod(launcher, "openFolder", new Class<?>[] {FolderInfo.class},
                        folderInfo);
                folder[0] = launcher.mWorkspace.getOpenFolder();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertNotNull("User folder must open", folder[0]);

        final int[] titleColor = new int[1];
        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                TextView title = (TextView) folder[0].findViewById(R.id.folder_name);
                if (title == null) {
                    throw new IllegalStateException("User folder has no name view");
                }
                title.setTextColor(STALE_COLOR);
                invokeLauncherMethod(launcher, "applyWallpaperPalette", new Class<?>[0]);
                titleColor[0] = title.getCurrentTextColor();
            }
        });
        assertEquals("Open user folder title must use the live on-surface color", expectedOnSurface,
                titleColor[0]);

        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                invokeLauncherMethod(launcher, "closeFolder", new Class<?>[0]);
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private void verifyAppListFolderPalette(final Launcher launcher, int expectedPrimary,
            int expectedOnSurface) throws Throwable {
        final String folderTitle = "Palette app-list folder";
        final AppListFolderInfo folderInfo = new AppListFolderInfo(901L, folderTitle,
                Collections.<ApplicationItemInfo>emptyList());
        final AlertDialog[] dialog = new AlertDialog[1];
        runOnUiThread(new UiAction() {
            public void run() throws Exception {
                launcher.openAppListFolder(folderInfo);
                dialog[0] = getAppListFolderDialog(launcher);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertNotNull("Empty app-list folder dialog must open", dialog[0]);

        final int[] titleColor = new int[1];
        final int[] emptyColor = new int[1];
        final String emptyText = launcher.getString(R.string.app_list_folder_empty);
        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                if (dialog[0].getWindow() == null) {
                    throw new IllegalStateException("App-list folder dialog has no window");
                }
                View decor = dialog[0].getWindow().getDecorView();
                TextView title = findTextViewByText(decor, folderTitle);
                TextView empty = findTextViewByText(decor, emptyText);
                if (title == null) {
                    throw new IllegalStateException("App-list folder custom title is missing");
                }
                if (empty == null) {
                    throw new IllegalStateException("App-list folder empty view is missing");
                }
                title.setTextColor(STALE_COLOR);
                empty.setTextColor(STALE_COLOR);
                invokeLauncherMethod(launcher, "applyWallpaperPalette", new Class<?>[0]);
                titleColor[0] = title.getCurrentTextColor();
                emptyColor[0] = empty.getCurrentTextColor();
            }
        });

        assertEquals("App-list folder title must use the live primary color", expectedPrimary,
                titleColor[0]);
        assertEquals("App-list folder empty view must use the live on-surface color",
                expectedOnSurface, emptyColor[0]);
    }

    public void testWallpaperChangedReceiverRefreshesCachedPalette() throws Throwable {
        Launcher launcher = getActivity();
        final Context targetContext = getInstrumentation().getTargetContext();
        SharedPreferences palettePreferences = launcher.getSharedPreferences(
                WALLPAPER_COLORS_PREFERENCES, Context.MODE_PRIVATE);
        boolean wasRefreshSuppressed = Launcher.sSuppressWallpaperRefreshForTests;
        Map<String, Object> paletteSnapshot = null;

        try {
            getInstrumentation().waitForIdleSync();
            awaitPaletteRefreshQueue();
            paletteSnapshot = snapshotPreferences(palettePreferences);
            writeTestPalette(launcher, palettePreferences);
            final String currentPrefix = palettePrefix(launcher);
            Launcher.setWallpaperRefreshSuppressedForTests(false);
            assertTrue("Sentinel palette must commit", palettePreferences.edit()
                    .putInt(currentPrefix + PRIMARY_KEY, STALE_COLOR).commit());
            runOnUiThread(new UiAction() {
                public void run() {
                    Launcher.dispatchWallpaperRefreshForTests(targetContext);
                }
            });
            awaitPaletteRefreshQueue();
            if (Build.VERSION.SDK_INT >= 31) {
                assertEquals("System-dynamic refresh must leave cache poison untouched", STALE_COLOR,
                        palettePreferences.getInt(currentPrefix + PRIMARY_KEY, 0));
            }
            if (Build.VERSION.SDK_INT < 31) {
                assertFalse("Wallpaper receiver must replace the sentinel palette", palettePreferences
                        .getInt(currentPrefix + PRIMARY_KEY, STALE_COLOR) == STALE_COLOR);
            }
        } finally {
            try {
                if (paletteSnapshot != null) {
                    restorePreferences(palettePreferences, paletteSnapshot);
                    runOnUiThread(new UiAction() {
                        public void run() throws Throwable {
                            invokeLauncherMethod(launcher, "applyWallpaperPalette",
                                    new Class<?>[0]);
                        }
                    });
                }
            } finally {
                Launcher.setWallpaperRefreshSuppressedForTests(wasRefreshSuppressed);
                getInstrumentation().waitForIdleSync();
            }
        }
    }

    private void awaitPaletteRefreshQueue() throws InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);
        Launcher.runAfterWallpaperRefreshesForTests(new Runnable() {
            public void run() {
                barrier.countDown();
            }
        });
        assertTrue("Wallpaper palette refresh must finish",
                barrier.await(ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        getInstrumentation().waitForIdleSync();
    }

    private void awaitLauncherData(final Launcher launcher) {
        long deadline = SystemClock.uptimeMillis() + ASYNC_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            final boolean[] ready = new boolean[1];
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    if (!(launcher.mApplicationsView instanceof ApplicationsDrawerView)) {
                        return;
                    }
                    ApplicationsGridView gridView = ((ApplicationsDrawerView)
                            launcher.mApplicationsView).getGridView();
                    ready[0] = !launcher.isWorkspaceLocked() && gridView.getAdapter() != null
                            && gridView.getAdapter().getCount() > 0;
                }
            });
            if (ready[0]) {
                return;
            }
            SystemClock.sleep(100L);
        }
        fail("Launcher applications and workspace must finish loading");
    }

    private void writeTestPalette(Context context, SharedPreferences preferences) {
        String currentPrefix = palettePrefix(context);
        String stalePrefix = opposingPalettePrefix(currentPrefix);
        SharedPreferences.Editor editor = preferences.edit()
                .clear()
                .putInt(SCHEMA_KEY, CACHE_SCHEMA_VERSION);
        writePalette(editor, currentPrefix, TEST_PRIMARY, TEST_ON_PRIMARY, TEST_SURFACE,
                TEST_ON_SURFACE, TEST_SURFACE_VARIANT, TEST_OUTLINE);
        writePalette(editor, stalePrefix, STALE_COLOR, STALE_COLOR, STALE_COLOR, STALE_COLOR,
                STALE_COLOR, STALE_COLOR);
        assertTrue("Test palette must commit", editor.commit());
    }

    private static void writePalette(SharedPreferences.Editor editor, String prefix, int primary,
            int onPrimary, int surface, int onSurface, int surfaceVariant, int outline) {
        editor.putInt(prefix + PRIMARY_KEY, primary)
                .putInt(prefix + ON_PRIMARY_KEY, onPrimary)
                .putInt(prefix + SURFACE_KEY, surface)
                .putInt(prefix + ON_SURFACE_KEY, onSurface)
                .putInt(prefix + SURFACE_VARIANT_KEY, surfaceVariant)
                .putInt(prefix + OUTLINE_KEY, outline);
    }

    private static String palettePrefix(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            return DARK_PREFIX;
        }
        return LIGHT_PREFIX;
    }

    private static String opposingPalettePrefix(String prefix) {
        if (LIGHT_PREFIX.equals(prefix)) {
            return DARK_PREFIX;
        }
        return LIGHT_PREFIX;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> snapshotPreferences(SharedPreferences preferences) {
        Map<String, Object> snapshot = new HashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Set) {
                snapshot.put(entry.getKey(), new HashSet<String>((Set<String>) value));
                continue;
            }
            snapshot.put(entry.getKey(), value);
        }
        return snapshot;
    }

    private void restorePreferences(SharedPreferences preferences, Map<String, Object> snapshot) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            restorePreference(editor, entry.getKey(), entry.getValue());
        }
        assertTrue("Wallpaper palette preferences must restore", editor.commit());
    }

    @SuppressWarnings("unchecked")
    private static void restorePreference(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof Boolean) {
            editor.putBoolean(key, ((Boolean) value).booleanValue());
            return;
        }
        if (value instanceof Float) {
            editor.putFloat(key, ((Float) value).floatValue());
            return;
        }
        if (value instanceof Integer) {
            editor.putInt(key, ((Integer) value).intValue());
            return;
        }
        if (value instanceof Long) {
            editor.putLong(key, ((Long) value).longValue());
            return;
        }
        if (value instanceof String) {
            editor.putString(key, (String) value);
            return;
        }
        if (value instanceof Set) {
            editor.putStringSet(key, new HashSet<String>((Set<String>) value));
            return;
        }
        throw new IllegalArgumentException("Unsupported SharedPreferences value for " + key);
    }

    private void runOnUiThread(final UiAction action) throws Throwable {
        final Throwable[] failure = new Throwable[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    action.run();
                } catch (Throwable throwable) {
                    failure[0] = throwable;
                }
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    private static void invokeLauncherMethod(Launcher launcher, String methodName,
            Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Method method = Launcher.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(launcher, arguments);
    }

    private static AlertDialog getAppListFolderDialog(Launcher launcher) throws Exception {
        Field field = Launcher.class.getDeclaredField("mAppListFolderDialog");
        field.setAccessible(true);
        return (AlertDialog) field.get(launcher);
    }

    private static void dismissAppListFolder(Launcher launcher) throws Exception {
        AlertDialog dialog = getAppListFolderDialog(launcher);
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private static TextView findFirstVisibleTextView(View view) {
        if (view instanceof TextView && view.getVisibility() == View.VISIBLE && view.isShown()) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            TextView textView = findFirstVisibleTextView(group.getChildAt(index));
            if (textView != null) {
                return textView;
            }
        }
        return null;
    }

    private static TextView findTextViewByText(View view, String expectedText) {
        if (view instanceof TextView && expectedText.equals(viewText((TextView) view))) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            TextView textView = findTextViewByText(group.getChildAt(index), expectedText);
            if (textView != null) {
                return textView;
            }
        }
        return null;
    }

    private static String viewText(TextView view) {
        CharSequence text = view.getText();
        if (text == null) {
            return "";
        }
        return text.toString();
    }

    private interface UiAction {
        void run() throws Throwable;
    }
}
