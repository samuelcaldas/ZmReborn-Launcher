package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class AccessibilityE2ETest extends ActivityInstrumentationTestCase2<Launcher> {
    private static final String HORIZONTAL_DRAWER_TYPE = "2";
    private static final String PAGER_INDICATOR_TEST =
            "testDrawerPagerIndicatorAccessibilityMatchesPageCount";
    private SharedPreferences mDrawerPreferences;
    private String mDrawerTypeKey;
    private String mOriginalDrawerType;
    private boolean mHadOriginalDrawerType;
    private boolean mDrawerTypeOverridden;
    private int mOriginalPagingRows;
    private int mOriginalPagingColumns;
    private boolean mPagingDimensionsCaptured;

    public AccessibilityE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (PAGER_INDICATOR_TEST.equals(getName())) {
            Context targetContext = getInstrumentation().getTargetContext();
            this.mDrawerPreferences = PreferenceManager.getDefaultSharedPreferences(targetContext);
            this.mDrawerTypeKey = targetContext.getString(R.string.preferences_key_apps_grid_type);
            this.mHadOriginalDrawerType = this.mDrawerPreferences.contains(this.mDrawerTypeKey);
            this.mOriginalDrawerType = this.mDrawerPreferences.getString(this.mDrawerTypeKey, null);
            capturePagingDimensions();
            assertTrue("Horizontal drawer preference must save", this.mDrawerPreferences.edit()
                    .putString(this.mDrawerTypeKey, HORIZONTAL_DRAWER_TYPE).commit());
            this.mDrawerTypeOverridden = true;
            return;
        }
        getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            restorePagingDimensions();
            restoreDrawerType();
        } finally {
            super.tearDown();
        }
    }

    public void testDrawerViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View drawer = launcher.findViewById(R.id.apps_grid);

        assertNotNull("Drawer must exist for accessibility", drawer);
    }

    public void testWorkspaceViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View workspace = launcher.findViewById(R.id.workspace);

        assertNotNull("Workspace must be accessible", workspace);
        assertTrue("Workspace must be visible", workspace.getVisibility() == View.VISIBLE);
    }

    public void testDockViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dock = launcher.findViewById(R.id.dock);

        assertNotNull("Dock must exist for accessibility", dock);
        assertTrue("Dock must be visible", dock.getVisibility() == View.VISIBLE);
    }

    public void testDrawerStateOverlayIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View stateOverlay = launcher.findViewById(R.id.apps_state_overlay);
        View stateMessage = launcher.findViewById(R.id.apps_state_message);

        assertNotNull("State overlay must be accessible", stateOverlay);
        assertNotNull("State message must be accessible", stateMessage);
    }

    public void testDrawerPagerIndicatorAccessibilityMatchesPageCount() throws Throwable {
        final Launcher launcher = getActivity();
        final boolean[] drawerStateCaptured = new boolean[1];
        final int[] originalApplicationsState = new int[1];
        final boolean[] originalDrawerWasOpen = new boolean[1];
        final boolean[] productionPagingDrawer = new boolean[1];
        final View[] drawerView = new View[1];
        final View[] indicatorView = new View[1];
        final int[] indicatorVisibility = new int[1];
        final int[] pageCount = new int[1];
        final int[] currentPage = new int[1];
        final String[] indicatorDescription = new String[1];

        getInstrumentation().waitForIdleSync();
        try {
            runOnUiThread(new UiAction() {
                public void run() throws Throwable {
                    originalApplicationsState[0] = getApplicationsState(launcher);
                    originalDrawerWasOpen[0] = launcher.isApplicationsGridLogicallyOpen();
                    drawerStateCaptured[0] = true;
                    ApplicationsView applicationsView = launcher.mApplicationsView;
                    productionPagingDrawer[0] = applicationsView instanceof ApplicationsPagingView;
                    if (!productionPagingDrawer[0]) {
                        return;
                    }

                    ApplicationsPagingView pagingView = (ApplicationsPagingView) applicationsView;
                    pagingView.setNumRows(1);
                    applicationsView.setNumColumns(1);
                    applicationsView.setApplications(createPagingApplications(launcher));
                    applicationsView.clearState();
                    invokeApplicationsGridMethod(launcher, "openApplicationsGrid", false);

                    View drawer = applicationsView.getImplementingView();
                    View indicator = drawer.findViewById(R.id.apps_paging_screen_indicator);
                    View pager = drawer.findViewById(R.id.view_pager);
                    drawerView[0] = drawer;
                    indicatorView[0] = indicator;
                    if (indicator != null) {
                        indicatorVisibility[0] = indicator.getVisibility();
                        CharSequence description = indicator.getContentDescription();
                        indicatorDescription[0] = description == null ? null : description.toString();
                    }
                    if (pager instanceof ViewPager) {
                        pageCount[0] = ((ViewPager) pager).getPageCount();
                        currentPage[0] = ((ViewPager) pager).getCurrentPageIndex();
                    }
                }
            });

            assertEquals("Horizontal drawer preference must be active", 2,
                    PreferencesUtil.getAppsGridType(launcher));
            assertTrue("Launcher.setupViews must inflate the horizontal paging drawer",
                    productionPagingDrawer[0]);
            assertNotNull("Drawer implementing view must exist", drawerView[0]);
            assertNotNull("Drawer page indicator must exist", indicatorView[0]);
            assertEquals("Injected applications must create two drawer pages", 2, pageCount[0]);
            assertEquals("Drawer must open on its first page", 0, currentPage[0]);
            assertEquals("Drawer page indicator must be visible", View.VISIBLE, indicatorVisibility[0]);
            assertEquals("Drawer page indicator description must identify its page and count",
                    launcher.getString(R.string.accessibility_page_indicator,
                            currentPage[0] + 1, pageCount[0]), indicatorDescription[0]);
        } finally {
            try {
                runOnUiThread(new UiAction() {
                    public void run() throws Throwable {
                        if (drawerStateCaptured[0]) {
                            restoreDrawerState(launcher, originalApplicationsState[0],
                                    originalDrawerWasOpen[0]);
                        }
                    }
                });
            } finally {
                try {
                    restorePagingDimensions();
                } finally {
                    restoreDrawerType();
                }
            }
        }
    }

    public void testDragLayerIsAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        View dragLayer = launcher.findViewById(R.id.drag_layer);

        assertNotNull("DragLayer must exist for accessibility", dragLayer);
    }

    public void testAllPrimaryViewsAreAccessible() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();

        assertNotNull("Workspace accessibility", launcher.findViewById(R.id.workspace));
        assertNotNull("DragLayer accessibility", launcher.findViewById(R.id.drag_layer));
        assertNotNull("Dock accessibility", launcher.findViewById(R.id.dock));
        assertNotNull("Apps grid accessibility", launcher.findViewById(R.id.apps_grid));
    }

    private static ArrayList<ApplicationItemInfo> createPagingApplications(Launcher launcher) {
        ArrayList<ApplicationItemInfo> applications = new ArrayList<ApplicationItemInfo>();
        for (int index = 0; index < 2; index++) {
            ApplicationItemInfo application = new ApplicationItemInfo();
            application.title = "Accessibility drawer application " + index;
            application.icon = launcher.getPackageManager().getDefaultActivityIcon();
            applications.add(application);
        }
        return applications;
    }

    private static int getPagingDimension(String fieldName) throws Exception {
        Field field = ApplicationsPagingView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static void setPagingDimension(String fieldName, int value) throws Exception {
        Field field = ApplicationsPagingView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(null, value);
    }

    private static int getApplicationsState(Launcher launcher) throws Exception {
        Field field = Launcher.class.getDeclaredField("mApplicationsState");
        field.setAccessible(true);
        return field.getInt(launcher);
    }

    private static void setApplicationsState(Launcher launcher, int state) throws Exception {
        Field field = Launcher.class.getDeclaredField("mApplicationsState");
        field.setAccessible(true);
        field.setInt(launcher, state);
    }

    private static void invokeApplicationsGridMethod(
            Launcher launcher, String methodName, boolean animated) throws Exception {
        Method method = Launcher.class.getDeclaredMethod(methodName, Boolean.TYPE);
        method.setAccessible(true);
        method.invoke(launcher, Boolean.valueOf(animated));
    }

    private static void restoreDrawerState(Launcher launcher, int originalApplicationsState,
            boolean originalDrawerWasOpen) throws Exception {
        if (launcher.isApplicationsGridLogicallyOpen()) {
            invokeApplicationsGridMethod(launcher, "closeApplicationsGrid", false);
        }
        setApplicationsState(launcher, originalApplicationsState);
        if (originalDrawerWasOpen) {
            invokeApplicationsGridMethod(launcher, "openApplicationsGrid", false);
        }
    }

    private void capturePagingDimensions() throws Exception {
        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                mOriginalPagingRows = getPagingDimension("sRows");
                mOriginalPagingColumns = getPagingDimension("sColumns");
                mPagingDimensionsCaptured = true;
            }
        });
    }

    private void restorePagingDimensions() throws Exception {
        if (!this.mPagingDimensionsCaptured) {
            return;
        }
        runOnUiThread(new UiAction() {
            public void run() throws Throwable {
                setPagingDimension("sRows", mOriginalPagingRows);
                setPagingDimension("sColumns", mOriginalPagingColumns);
                mPagingDimensionsCaptured = false;
            }
        });
    }

    private void restoreDrawerType() {
        if (!this.mDrawerTypeOverridden) {
            return;
        }
        SharedPreferences.Editor editor = this.mDrawerPreferences.edit();
        if (this.mHadOriginalDrawerType) {
            editor.putString(this.mDrawerTypeKey, this.mOriginalDrawerType);
        } else {
            editor.remove(this.mDrawerTypeKey);
        }
        assertTrue("Drawer type preference must restore", editor.commit());
        this.mDrawerTypeOverridden = false;
    }

    private void runOnUiThread(final UiAction action) throws Exception {
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
        if (failure[0] instanceof Exception) {
            throw (Exception) failure[0];
        }
        if (failure[0] instanceof Error) {
            throw (Error) failure[0];
        }
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
    }

    private interface UiAction {
        void run() throws Throwable;
    }
}
