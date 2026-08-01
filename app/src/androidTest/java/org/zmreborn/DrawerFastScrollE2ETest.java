package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import java.util.ArrayList;

/** Verifies fast-scroll navigation through real drawer touch input. */
public class DrawerFastScrollE2ETest extends ActivityInstrumentationTestCase2<Launcher> {
    private static final int APPLICATIONS_PER_SECTION = 2;
    private static final int Z_SECTION_FIRST_POSITION =
            ('Z' - 'A') * APPLICATIONS_PER_SECTION;
    private static final long APPLICATION_LOAD_TIMEOUT_MS = 30000L;
    private static final long FAST_SCROLL_HIDE_TIMEOUT_MS = 5000L;
    private static final long VIEW_STATE_TIMEOUT_MS = 10000L;
    private static final String VERTICAL_SCROLLING = "1";
    private DrawerPreferenceSnapshot mPreferenceSnapshot;
    private SharedPreferences mPreferences;

    public DrawerFastScrollE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getTargetContext();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mPreferenceSnapshot = DrawerPreferenceSnapshot.capture(context, this.mPreferences);
        this.mPreferenceSnapshot.applyDeterministicState(this.mPreferences);
        getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (this.mPreferenceSnapshot != null && this.mPreferences != null) {
                this.mPreferenceSnapshot.restore(this.mPreferences);
            }
        } finally {
            super.tearDown();
        }
    }

    public void testVerticalDrawerFastScrollNavigatesAlphabeticalSections() throws Throwable {
        final Launcher launcher = getActivity();
        assertTrue("Vertical drawer preference must select ApplicationsDrawerView",
                launcher.mApplicationsView instanceof ApplicationsDrawerView);
        final ApplicationsDrawerView drawer = (ApplicationsDrawerView)
                launcher.mApplicationsView;
        awaitApplicationsLoaded(drawer, APPLICATION_LOAD_TIMEOUT_MS);
        final ArrayList<ApplicationItemInfo> originalApplications =
                new ArrayList<ApplicationItemInfo>();
        final ArrayList<ApplicationItemInfo> alphabeticalApplications =
                createAlphabeticalApplications(launcher);

        try {
            installAlphabeticalApplications(drawer, originalApplications,
                    alphabeticalApplications);
            revealFastScroll(drawer);
            assertFastScrollLayoutAndSelectZ(launcher, drawer);
            assertZSelectionAndAutoHide(drawer);
            resetAndRevealFastScroll(drawer);
            assertClosingAndSearchSuppressFastScroll(drawer);
        } finally {
            restoreApplications(drawer, originalApplications);
        }
    }

    private void awaitApplicationsLoaded(
            final ApplicationsDrawerView drawer, long timeoutMillis) {
        long deadline = SystemClock.uptimeMillis() + timeoutMillis;
        while (SystemClock.uptimeMillis() < deadline) {
            if (hasApplications(drawer)) {
                return;
            }
            SystemClock.sleep(50L);
        }
        fail("Drawer applications did not finish loading");
    }

    private boolean hasApplications(final ApplicationsDrawerView drawer) {
        final boolean[] ready = new boolean[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                ListAdapter adapter = drawer.getGridView().getAdapter();
                ready[0] = adapter != null && adapter.getCount() > 0;
            }
        });
        return ready[0];
    }

    private void installAlphabeticalApplications(final ApplicationsDrawerView drawer,
            final ArrayList<ApplicationItemInfo> originalApplications,
            final ArrayList<ApplicationItemInfo> alphabeticalApplications) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                ListAdapter adapter = drawer.getGridView().getAdapter();
                if (adapter != null) {
                    originalApplications.addAll(adapterItems(adapter));
                }
                drawer.open(false);
                drawer.setApplications(alphabeticalApplications);
                assertEquals("Alphabetical fixture must populate drawer",
                        alphabeticalApplications.size(),
                        drawer.getGridView().getAdapter().getCount());
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private void assertZSelectionAndAutoHide(
            ApplicationsDrawerView drawer) {
        awaitGridContainsPosition(drawer.getGridView(),
                Z_SECTION_FIRST_POSITION, VIEW_STATE_TIMEOUT_MS);
        awaitVisibility(drawer.getFastScrollView(), View.GONE,
                FAST_SCROLL_HIDE_TIMEOUT_MS);
    }

    private void resetAndRevealFastScroll(
            final ApplicationsDrawerView drawer) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                drawer.getGridView().setSelection(0);
            }
        });
        revealFastScroll(drawer);
    }

    private void restoreApplications(final ApplicationsDrawerView drawer,
            final ArrayList<ApplicationItemInfo> originalApplications) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                drawer.setApplications(originalApplications);
                drawer.close(false);
            }
        });
    }

    private void revealFastScroll(
            final ApplicationsDrawerView drawer) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                ApplicationsGridView grid = drawer.getGridView();
                assertTrue("Grid must have width before scroll input", grid.getWidth() > 0);
                assertTrue("Grid must have height before scroll input", grid.getHeight() > 0);
                dispatchGridDrag(grid);
            }
        });
        DrawerFastScrollView fastScroll = drawer.getFastScrollView();
        awaitVisibility(fastScroll, View.VISIBLE, VIEW_STATE_TIMEOUT_MS);
        awaitMeasured(fastScroll, VIEW_STATE_TIMEOUT_MS);
    }

    private static void dispatchGridDrag(ApplicationsGridView grid) {
        long upTime = SystemClock.uptimeMillis();
        long downTime = upTime - 600L;
        float centerX = grid.getWidth() / 2.0f;
        float bottomY = grid.getHeight() * 0.75f;
        float topY = grid.getHeight() * 0.25f;
        dispatchGridEvent(grid, downTime, downTime, MotionEvent.ACTION_DOWN,
                centerX, bottomY);
        dispatchGridEvent(grid, downTime, downTime + 100L, MotionEvent.ACTION_MOVE,
                centerX, topY);
        dispatchGridEvent(grid, downTime, upTime - 50L, MotionEvent.ACTION_MOVE,
                centerX, topY);
        dispatchGridEvent(grid, downTime, upTime, MotionEvent.ACTION_UP,
                centerX, topY);
    }

    private static void dispatchGridEvent(ApplicationsGridView grid, long downTime,
            long eventTime, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            assertTrue("Grid must accept drag event " + action,
                    grid.dispatchTouchEvent(event));
        } finally {
            event.recycle();
        }
    }

    private void assertFastScrollLayoutAndSelectZ(
            final Launcher launcher, final ApplicationsDrawerView drawer) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                DrawerFastScrollView fastScroll = drawer.getFastScrollView();
                ImageButton clearSearch = (ImageButton) drawer.findViewById(
                        R.id.drawer_search_clear);
                if (fastScroll.getVisibility() != View.VISIBLE) {
                    dispatchGridDrag(drawer.getGridView());
                }
                assertEquals("Fast scroll must be visible during grid motion", View.VISIBLE,
                        fastScroll.getVisibility());
                int initialLayoutDirection = drawer.getLayoutDirection();
                try {
                    drawer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    assertLeftToRightLayout(launcher, drawer, fastScroll, clearSearch);
                    drawer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    assertRightToLeftLayout(drawer, fastScroll, clearSearch);
                    drawer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    assertLeftToRightLayout(launcher, drawer, fastScroll, clearSearch);
                    assertScrollableFixture(drawer.getGridView());
                    assertTrue("Bottom rail touch must select final section",
                            dispatchFastScrollTap(fastScroll));
                    assertEquals("Bottom rail touch must select Z",
                            launcher.getString(
                                    R.string.accessibility_fast_scroll_section, "Z"),
                            fastScroll.getContentDescription());
                } finally {
                    drawer.setLayoutDirection(initialLayoutDirection);
                }
            }
        });
    }

    private static void assertLeftToRightLayout(Launcher launcher,
            ApplicationsDrawerView drawer, DrawerFastScrollView fastScroll,
            ImageButton clearSearch) {
        int minimumTouchTarget = minimumTouchTarget(launcher);
        assertEquals("Grid DPAD-right must reach visible fast scroll",
                R.id.drawer_fast_scroll, drawer.getGridView().getNextFocusRightId());
        assertTrue("LTR grid must reserve rail space at physical right",
                drawer.getGridView().getPaddingRight() >= minimumTouchTarget);
        assertEquals("LTR clear action must return left to search input",
                R.id.drawer_search_input, clearSearch.getNextFocusLeftId());
        assertTrue("Fast scroll must retain a 48dp horizontal touch target",
                fastScroll.getWidth() >= minimumTouchTarget);
    }

    private static void assertRightToLeftLayout(ApplicationsDrawerView drawer,
            DrawerFastScrollView fastScroll, ImageButton clearSearch) {
        int minimumTouchTarget = Math.round(48.0f
                * drawer.getResources().getDisplayMetrics().density);
        assertEquals("RTL grid DPAD-left must reach fast scroll",
                R.id.drawer_fast_scroll, drawer.getGridView().getNextFocusLeftId());
        assertEquals("RTL grid DPAD-right must stay in grid",
                R.id.apps_grid_content, drawer.getGridView().getNextFocusRightId());
        assertEquals("RTL rail DPAD-right must return to grid",
                R.id.apps_grid_content, fastScroll.getNextFocusRightId());
        assertEquals("RTL clear action must return right to search input",
                R.id.drawer_search_input, clearSearch.getNextFocusRightId());
        assertTrue("RTL grid must reserve rail space at physical left",
                drawer.getGridView().getPaddingLeft() >= minimumTouchTarget);
    }

    private static int minimumTouchTarget(Launcher launcher) {
        return Math.round(48.0f * launcher.getResources().getDisplayMetrics().density);
    }

    private static void assertScrollableFixture(ApplicationsGridView grid) {
        assertNotNull("Alphabetical fixture must have adapter", grid.getAdapter());
        int itemCount = grid.getAdapter().getCount();
        int visibleCount = grid.getChildCount();
        assertTrue("Alphabetical fixture must exceed visible grid capacity: items="
                        + itemCount + ", visible=" + visibleCount,
                itemCount > visibleCount);
    }

    private static boolean dispatchFastScrollTap(DrawerFastScrollView fastScroll) {
        long eventTime = SystemClock.uptimeMillis();
        float x = fastScroll.getWidth() / 2.0f;
        float y = fastScroll.getHeight() - 1.0f;
        boolean downAccepted = dispatchFastScrollEvent(fastScroll, eventTime,
                eventTime, MotionEvent.ACTION_DOWN, x, y);
        boolean upAccepted = dispatchFastScrollEvent(fastScroll, eventTime,
                eventTime + 16L, MotionEvent.ACTION_UP, x, y);
        return downAccepted && upAccepted;
    }

    private static boolean dispatchFastScrollEvent(DrawerFastScrollView fastScroll,
            long downTime, long eventTime, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            return fastScroll.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }

    private void assertClosingAndSearchSuppressFastScroll(
            final ApplicationsDrawerView drawer) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                DrawerFastScrollView fastScroll = drawer.getFastScrollView();
                int positionBeforeClose = drawer.getGridView().getFirstVisiblePosition();
                assertTrue("Drawer close must begin", drawer.close(true));
                assertFalse("Closing grid input must be suppressed",
                        drawer.getGridView().isEnabled());
                assertFalse("Closing rail input must be suppressed",
                        dispatchFastScrollTap(fastScroll));
                assertEquals("Closing rail input must not change grid position",
                        positionBeforeClose, drawer.getGridView().getFirstVisiblePosition());
                assertSearchSuppressesFastScroll(drawer, fastScroll);
            }
        });
    }

    private static void assertSearchSuppressesFastScroll(
            ApplicationsDrawerView drawer, DrawerFastScrollView fastScroll) {
        drawer.open(false);
        assertTrue("Reopened grid must accept input", drawer.getGridView().isEnabled());
        assertTrue("Reopened fast scroll must accept input", fastScroll.isEnabled());
        ((EditText) drawer.findViewById(R.id.drawer_search_input)).setText("Alpha");
        assertEquals("Fast scroll must hide while search reorders results", View.GONE,
                fastScroll.getVisibility());
        assertEquals("Grid DPAD-right must skip hidden fast scroll",
                R.id.apps_grid_content, drawer.getGridView().getNextFocusRightId());
    }

    private void awaitGridContainsPosition(
            final ApplicationsGridView grid, int position, long timeoutMillis) {
        long deadline = SystemClock.uptimeMillis() + timeoutMillis;
        while (SystemClock.uptimeMillis() < deadline) {
            if (isGridPositionVisible(grid, position)) {
                return;
            }
            SystemClock.sleep(50L);
        }
        fail("Selected section position did not become visible: " + position);
    }

    private boolean isGridPositionVisible(
            final ApplicationsGridView grid, final int position) {
        final boolean[] visible = new boolean[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                visible[0] = grid.getFirstVisiblePosition() <= position
                        && grid.getLastVisiblePosition() >= position;
            }
        });
        return visible[0];
    }

    private void awaitMeasured(final View view, long timeoutMillis) {
        long deadline = SystemClock.uptimeMillis() + timeoutMillis;
        while (SystemClock.uptimeMillis() < deadline) {
            if (hasPositiveDimensions(view)) {
                return;
            }
            SystemClock.sleep(50L);
        }
        fail("View dimensions did not become positive within " + timeoutMillis + "ms");
    }

    private boolean hasPositiveDimensions(final View view) {
        final boolean[] measured = new boolean[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                measured[0] = view.getWidth() > 0 && view.getHeight() > 0;
            }
        });
        return measured[0];
    }

    private void awaitVisibility(final View view, int expectedVisibility, long timeoutMillis) {
        long deadline = SystemClock.uptimeMillis() + timeoutMillis;
        while (SystemClock.uptimeMillis() < deadline) {
            if (visibility(view) == expectedVisibility) {
                return;
            }
            SystemClock.sleep(50L);
        }
        fail("View visibility did not become " + expectedVisibility
                + " within " + timeoutMillis + "ms");
    }

    private int visibility(final View view) {
        final int[] visibility = new int[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                visibility[0] = view.getVisibility();
            }
        });
        return visibility[0];
    }

    private static ArrayList<ApplicationItemInfo> adapterItems(ListAdapter adapter) {
        ArrayList<ApplicationItemInfo> items = new ArrayList<ApplicationItemInfo>();
        for (int position = 0; position < adapter.getCount(); position++) {
            items.add((ApplicationItemInfo) adapter.getItem(position));
        }
        return items;
    }

    private static ArrayList<ApplicationItemInfo> createAlphabeticalApplications(
            Launcher launcher) {
        ArrayList<ApplicationItemInfo> applications = new ArrayList<ApplicationItemInfo>();
        Drawable sharedIcon = launcher.getPackageManager().getDefaultActivityIcon();
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            for (int index = 0; index < APPLICATIONS_PER_SECTION; index++) {
                ApplicationItemInfo application = new ApplicationItemInfo();
                application.title = letter + " alphabetical application " + index;
                application.componentName = "fast.scroll." + letter + "." + index;
                application.icon = sharedIcon;
                applications.add(application);
            }
        }
        return applications;
    }

    private static final class DrawerPreferenceSnapshot {
        private final String blurKey;
        private final String drawerTypeKey;
        private final Object blurValue;
        private final Object drawerTypeValue;

        private DrawerPreferenceSnapshot(Context context, SharedPreferences preferences) {
            this.blurKey = context.getString(R.string.preferences_key_blur_backgrounds);
            this.drawerTypeKey = context.getString(R.string.preferences_key_apps_grid_type);
            this.blurValue = value(preferences, this.blurKey);
            this.drawerTypeValue = value(preferences, this.drawerTypeKey);
        }

        static DrawerPreferenceSnapshot capture(
                Context context, SharedPreferences preferences) {
            return new DrawerPreferenceSnapshot(context, preferences);
        }

        void applyDeterministicState(SharedPreferences preferences) {
            boolean committed = preferences.edit()
                    .putString(this.drawerTypeKey, VERTICAL_SCROLLING)
                    .putBoolean(this.blurKey, false)
                    .commit();
            if (!committed) {
                throw new IllegalStateException("Drawer test preferences failed to commit");
            }
        }

        void restore(SharedPreferences preferences) {
            SharedPreferences.Editor editor = preferences.edit();
            restoreValue(editor, this.blurKey, this.blurValue);
            restoreValue(editor, this.drawerTypeKey, this.drawerTypeValue);
            if (!editor.commit()) {
                throw new IllegalStateException("Drawer test preferences failed to restore");
            }
        }

        private static Object value(SharedPreferences preferences, String key) {
            return preferences.contains(key) ? preferences.getAll().get(key) : null;
        }

        private static void restoreValue(
                SharedPreferences.Editor editor, String key, Object value) {
            if (value == null) {
                editor.remove(key);
                return;
            }
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
                return;
            }
            if (value instanceof String) {
                editor.putString(key, (String) value);
                return;
            }
            throw new IllegalStateException("Unsupported drawer preference type for " + key);
        }
    }
}
