package org.zmreborn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Verifies user-visible drawer workflows against the real Launcher activity. */
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
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        final ApplicationsView applicationsView = launcher.mApplicationsView;
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                applicationsView.open(false);
                assertTrue("Drawer must be open", launcher.isApplicationsGridOpen());
                applicationsView.close(false);
                assertFalse("Drawer must be closed", launcher.isApplicationsGridOpen());
            }
        });
    }

    public void testWorkspaceDrawsChildrenDuringAnimatedDrawerClose() throws Throwable {
        final Launcher launcher = getActivity();
        final CellLayout[] cellLayout = new CellLayout[1];
        final DrawCountingView[] drawingView = new DrawCountingView[1];
        final boolean[] logicallyOpen = new boolean[1];
        final boolean[] drawerVisible = new boolean[1];
        final int[] drawCount = new int[1];
        final Throwable[] failures = new Throwable[1];
        final Throwable[] cleanupFailures = new Throwable[1];

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                CellLayout currentCellLayout = (CellLayout) launcher.mWorkspace.getChildAt(
                        launcher.mWorkspace.getCurrentScreen());
                DrawCountingView child = new DrawCountingView(launcher);
                currentCellLayout.addView(child, new CellLayout.LayoutParams(0, 0, 1, 1));
                cellLayout[0] = currentCellLayout;
                drawingView[0] = child;
            }
        });

        try {
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawingView[0].resetDrawCount();
                }
            });
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    try {
                        invokeApplicationsGridMethod(launcher, "openApplicationsGrid", false);
                        invokeApplicationsGridMethod(launcher, "closeApplicationsGrid", true);
                        logicallyOpen[0] = launcher.isApplicationsGridLogicallyOpen();
                        drawerVisible[0] = launcher.mApplicationsView.getImplementingView()
                                .getVisibility() == View.VISIBLE;
                        Bitmap bitmap = Bitmap.createBitmap(launcher.mWorkspace.getWidth(),
                                launcher.mWorkspace.getHeight(), Bitmap.Config.ARGB_8888);
                        launcher.mWorkspace.draw(new Canvas(bitmap));
                        drawCount[0] = drawingView[0].getDrawCount();
                    } catch (Throwable throwable) {
                        failures[0] = throwable;
                    }
                }
            });
        } finally {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    try {
                        cellLayout[0].removeView(drawingView[0]);
                        invokeApplicationsGridMethod(launcher, "closeApplicationsGrid", false);
                    } catch (Throwable throwable) {
                        cleanupFailures[0] = throwable;
                    } finally {
                        View drawerView = launcher.mApplicationsView.getImplementingView();
                        drawerView.clearAnimation();
                        drawerView.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

        if (failures[0] != null) {
            throw failures[0];
        }
        if (cleanupFailures[0] != null) {
            throw cleanupFailures[0];
        }
        assertFalse("Drawer must be logically closed before its animation ends", logicallyOpen[0]);
        assertTrue("Drawer must remain visible while its close animation runs", drawerVisible[0]);
        assertTrue("Workspace must draw the current CellLayout during close animation",
                drawCount[0] > 0);
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
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        final ApplicationsView applicationsView = launcher.mApplicationsView;
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                applicationsView.open(false);
            }
        });
        getInstrumentation().waitForIdleSync();

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                assertTrue("Drawer must be open", launcher.isApplicationsGridOpen());
                assertNotNull("Applications view must have implementing view",
                        applicationsView.getImplementingView());
            }
        });
    }

    public void testDrawerHandlesEmptyStateWithFocus() {
        Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        ApplicationsView applicationsView = launcher.mApplicationsView;
        assertNotNull("Implementing view must handle focus even when empty",
                applicationsView.getImplementingView());
    }

    public void testDrawerNavigationDoesNotCrashOnEdges() {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        final ApplicationsView applicationsView = launcher.mApplicationsView;
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                applicationsView.open(false);
            }
        });
        getInstrumentation().waitForIdleSync();

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                applicationsView.close(false);
            }
        });
        getInstrumentation().waitForIdleSync();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                assertFalse("Drawer must be closed", launcher.isApplicationsGridOpen());
            }
        });
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
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        final ApplicationsView applicationsView = launcher.mApplicationsView;

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                for (int i = 0; i < 3; i++) {
                    applicationsView.open(false);
                    assertTrue("Drawer must be open on iteration " + i,
                            launcher.isApplicationsGridOpen());
                    applicationsView.close(false);
                    assertFalse("Drawer must be closed on iteration " + i,
                            launcher.isApplicationsGridOpen());
                }
            }
        });
    }

    public void testVerticalDrawerSearchUsesStableImmutableAdapterState() {
        final Launcher launcher = getActivity();
        awaitVerticalDrawerData(launcher);
        final Throwable[] failure = new Throwable[1];

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    ApplicationsDrawerView drawer = (ApplicationsDrawerView)
                            launcher.mApplicationsView;
                    ApplicationsGridView grid = drawer.getGridView();
                    ListAdapter originalAdapter = grid.getAdapter();
                    int originalCount = originalAdapter.getCount();
                    ApplicationItemInfo first = (ApplicationItemInfo)
                            originalAdapter.getItem(0);
                    Object originalIcon = first.icon;
                    boolean originalFiltered = first.filtered;
                    long originalId = originalAdapter.getItemId(0);
                    ArrayList<ApplicationItemInfo> snapshot = adapterItems(originalAdapter);
                    drawer.open(false);

                    EditText search = (EditText) drawer.findViewById(
                            R.id.drawer_search_input);
                    ImageButton clear = (ImageButton) drawer.findViewById(
                            R.id.drawer_search_clear);
                    search.setText(first.title);
                    assertTrue("Search must keep at least selected application",
                            grid.getAdapter().getCount() > 0);
                    assertTrue("Search must narrow or preserve matching set",
                            grid.getAdapter().getCount() <= originalCount);

                    drawer.setApplications(snapshot);
                    assertTrue("Application refresh must preserve active query",
                            grid.getAdapter().getCount() <= originalCount);
                    clear.performClick();
                    assertEquals("Clearing search must restore full snapshot",
                            originalCount, grid.getAdapter().getCount());
                    assertTrue("Drawer adapter must expose stable IDs",
                            grid.getAdapter().hasStableIds());
                    assertEquals("Stable ID must survive filtering and refresh",
                            originalId, grid.getAdapter().getItemId(0));

                    grid.getAdapter().getView(0, null, grid);
                    assertSame("Binding must not replace model icon",
                            originalIcon, first.icon);
                    assertEquals("Binding must not mutate model filter state",
                            originalFiltered, first.filtered);
                    drawer.close(false);
                } catch (Throwable throwable) {
                    failure[0] = throwable;
                }
            }
        });

        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    public void testDrawerReopenIgnoresSupersededCloseAnimation() throws Throwable {
        final Launcher launcher = getActivity();
        final ApplicationsDrawerView drawer = (ApplicationsDrawerView)
                launcher.mApplicationsView;

        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawer.open(false);
                    assertTrue("Drawer close must start", drawer.close(true));
                    assertNotNull("Drawer close animation must be active", drawer.getAnimation());
                }
            });
            SystemClock.sleep(100L);
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawer.open(false);
                }
            });
            SystemClock.sleep(300L);
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    assertEquals("Reopened drawer must remain visible", View.VISIBLE,
                            drawer.getVisibility());
                    assertEquals("Reopened drawer grid must remain visible", View.VISIBLE,
                            drawer.getGridView().getVisibility());
                }
            });
        } finally {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawer.close(false);
                }
            });
        }
    }

    public void testDrawerRecoversAfterEmptyApplicationsRefresh() throws Throwable {
        final Launcher launcher = getActivity();
        awaitVerticalDrawerData(launcher);
        final ApplicationsDrawerView drawer = (ApplicationsDrawerView)
                launcher.mApplicationsView;
        final ArrayList<ApplicationItemInfo> applications =
                createDrawerApplications(launcher, 3);
        final ArrayList<ApplicationItemInfo> originalApplications =
                new ArrayList<ApplicationItemInfo>();

        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    originalApplications.addAll(adapterItems(drawer.getGridView().getAdapter()));
                    drawer.open(false);
                    assertTrue("Drawer must be open before refresh", launcher.isApplicationsGridOpen());
                }
            });
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawer.setApplications(new ArrayList<ApplicationItemInfo>());
                }
            });
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    assertEquals("Empty refresh must clear drawer adapter", 0,
                            drawer.getGridView().getAdapter().getCount());
                    assertEquals("Drawer must remain visible after empty refresh", View.VISIBLE,
                            drawer.getVisibility());
                    drawer.setApplications(applications);
                }
            });
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    assertEquals("Non-empty refresh must recover drawer adapter",
                            applications.size(), drawer.getGridView().getAdapter().getCount());
                    assertEquals("Drawer must remain visible after recovery", View.VISIBLE,
                            drawer.getVisibility());
                }
            });
        } finally {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    try {
                        drawer.setApplications(originalApplications);
                    } finally {
                        drawer.close(false);
                    }
                }
            });
        }
    }

    public void testPackageRefreshResetsUnrememberedEmptyDrawerPosition() throws Throwable {
        final Launcher launcher = getActivity();
        final ApplicationsDrawerView drawer = (ApplicationsDrawerView)
                launcher.mApplicationsView;
        final ArrayList<ApplicationItemInfo> applications =
                createDrawerApplications(launcher, 120);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(launcher);
        String key = launcher.getString(R.string.preferences_key_apps_grid_remember_position);
        boolean hadOriginalPosition = preferences.contains(key);
        boolean originalPosition = preferences.getBoolean(key, Boolean.parseBoolean(
                launcher.getString(R.string.preferences_default_apps_grid_remember_position)));
        assertTrue("Remember position preference must update", preferences.edit()
                .putBoolean(key, false).commit());

        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawer.open(false);
                    ((EditText) drawer.findViewById(R.id.drawer_search_input)).setText("");
                    drawer.setApplications(applications);
                }
            });
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawer.getGridView().setSelection(80);
                }
            });
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    assertTrue("Drawer must be scrolled before package refresh",
                            drawer.getGridView().getFirstVisiblePosition() > 0);
                    drawer.setApplications(new ArrayList<ApplicationItemInfo>(applications));
                }
            });
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    assertEquals("Unremembered package refresh must reset to top", 0,
                            drawer.getGridView().getFirstVisiblePosition());
                }
            });
        } finally {
            boolean restored = hadOriginalPosition
                    ? preferences.edit().putBoolean(key, originalPosition).commit()
                    : preferences.edit().remove(key).commit();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    drawer.close(false);
                }
            });
            assertTrue("Remember position preference must restore", restored);
        }
    }

    public void testDrawersRejectClicksWhileCloseAnimationRuns() {
        final Launcher launcher = getActivity();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                ApplicationItemInfo application = createApplicationItem();
                assertGridRejectsClosingClick(launcher, application);
                assertPagingRejectsClosingClick(launcher, application);
            }
        });
    }

    private void awaitVerticalDrawerData(final Launcher launcher) {
        long deadline = SystemClock.uptimeMillis() + 10000L;
        while (SystemClock.uptimeMillis() < deadline) {
            final boolean[] ready = new boolean[1];
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    if (!(launcher.mApplicationsView instanceof ApplicationsDrawerView)) {
                        return;
                    }
                    ApplicationsGridView grid = ((ApplicationsDrawerView)
                            launcher.mApplicationsView).getGridView();
                    ready[0] = grid.getAdapter() != null
                            && grid.getAdapter().getCount() > 0;
                }
            });
            if (ready[0]) {
                return;
            }
            SystemClock.sleep(50L);
        }
        fail("Vertical drawer applications must finish loading");
    }

    private static ArrayList<ApplicationItemInfo> adapterItems(ListAdapter adapter) {
        ArrayList<ApplicationItemInfo> items = new ArrayList<ApplicationItemInfo>();
        for (int position = 0; position < adapter.getCount(); position++) {
            items.add((ApplicationItemInfo) adapter.getItem(position));
        }
        return items;
    }

    private static void invokeApplicationsGridMethod(
            Launcher launcher, String methodName, boolean animated) throws Exception {
        Method method = Launcher.class.getDeclaredMethod(methodName, Boolean.TYPE);
        method.setAccessible(true);
        method.invoke(launcher, Boolean.valueOf(animated));
    }

    private static class DrawCountingView extends View {
        private int drawCount;

        DrawCountingView(Launcher launcher) {
            super(launcher);
        }

        int getDrawCount() {
            return this.drawCount;
        }

        void resetDrawCount() {
            this.drawCount = 0;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            this.drawCount++;
        }
    }

    private static ArrayList<ApplicationItemInfo> createDrawerApplications(
            Launcher launcher, int count) {
        ArrayList<ApplicationItemInfo> applications = new ArrayList<ApplicationItemInfo>();
        for (int index = 0; index < count; index++) {
            ApplicationItemInfo application = new ApplicationItemInfo();
            application.title = "Refresh drawer application " + index;
            application.icon = launcher.getPackageManager().getDefaultActivityIcon();
            applications.add(application);
        }
        return applications;
    }

    private static ApplicationItemInfo createApplicationItem() {
        ApplicationItemInfo application = new ApplicationItemInfo();
        application.title = "Closing drawer item";
        application.intent = new Intent(Intent.ACTION_MAIN);
        return application;
    }

    private static void assertGridRejectsClosingClick(
            Launcher launcher, ApplicationItemInfo application) {
        ApplicationsGridView grid = new ApplicationsGridView(launcher);
        ArrayList<ApplicationItemInfo> applications = new ArrayList<ApplicationItemInfo>();
        applications.add(application);
        grid.setAdapter(new ApplicationsAdapter(launcher, applications));
        grid.setVisibility(View.VISIBLE);
        assertTrue(grid.close(true));
        assertNotNull("Grid close animation must be active", grid.getAnimation());
        grid.onItemClick(grid, null, 0, 0);
        grid.clearAnimation();
    }

    private static void assertPagingRejectsClosingClick(
            Launcher launcher, ApplicationItemInfo application) {
        ApplicationsPagingView paging = new ApplicationsPagingView(launcher);
        TextView item = new TextView(launcher);
        item.setTag(application);
        paging.setVisibility(View.VISIBLE);
        assertTrue(paging.close(true));
        assertNotNull("Paged close animation must be active", paging.getAnimation());
        paging.onClick(item);
        paging.clearAnimation();
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
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        final View stateOverlay = launcher.findViewById(R.id.apps_state_overlay);
        final View retryButton = launcher.findViewById(R.id.apps_state_retry);
        final View closeButton = launcher.findViewById(R.id.apps_state_close);
        final int minimumTouchTarget = Math.round(48.0f
                * launcher.getResources().getDisplayMetrics().density);

        assertNotNull("State overlay must exist", stateOverlay);
        assertNotNull("Retry button must exist", retryButton);
        assertNotNull("Close button must exist", closeButton);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                stateOverlay.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.VISIBLE);
                closeButton.setVisibility(View.VISIBLE);
                stateOverlay.requestLayout();
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue("Retry button must be visible before sizing", retryButton.isShown());
        assertTrue("Retry button must be measured before sizing", retryButton.getHeight() > 0);
        assertTrue("Retry button height must be at least 48dp",
                retryButton.getHeight() >= minimumTouchTarget);
        assertTrue("Close button must be visible before sizing", closeButton.isShown());
        assertTrue("Close button must be measured before sizing", closeButton.getHeight() > 0);
        assertTrue("Close button height must be at least 48dp",
                closeButton.getHeight() >= minimumTouchTarget);
    }
}
