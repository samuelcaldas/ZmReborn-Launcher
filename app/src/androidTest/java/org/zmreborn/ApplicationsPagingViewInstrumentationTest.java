package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

public class ApplicationsPagingViewInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {

    public ApplicationsPagingViewInstrumentationTest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
        getInstrumentation().waitForIdleSync();
    }

    public void testAllInjectedAppsAppearAsChildrenAcrossPages() throws Throwable {
        final int totalApps = 13;
        final int rows = 2;
        final int columns = 3;
        final List<View>[] pageChildrenHolder = new List[1];

        runTestOnUiThread(new Runnable() {
            public void run() {
                List<ApplicationItemInfo> apps = makeApplicationItemInfos(totalApps);
                int capacity = rows * columns;
                int pageCount = (totalApps + capacity - 1) / capacity;
                pageChildrenHolder[0] = new ArrayList<View>();
                for (int page = 0; page < pageCount; page++) {
                    int start = page * capacity;
                    int end = Math.min(start + capacity, totalApps);
                    List<ApplicationItemInfo> pageApps = apps.subList(start, end);
                    ApplicationsPageView pageView = new ApplicationsPageView(getActivity());
                    pageView.populatePage(false, rows, columns, pageApps, null, null);
                    int width = 301;
                    int height = 400;
                    pageView.measure(
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    pageView.layout(0, 0, width, height);
                    for (int i = 0; i < pageView.getChildCount(); i++) {
                        pageChildrenHolder[0].add(pageView.getChildAt(i));
                    }
                }
            }
        });

        assertEquals("Every app must appear exactly once across all pages",
                totalApps, pageChildrenHolder[0].size());
    }

    public void testCellsAreContiguousAcrossFullWidth() throws Throwable {
        final int columns = 3;
        final int rows = 2;
        final int width = 301;
        final int height = 400;
        final int[] trailingGapHolder = new int[1];

        runTestOnUiThread(new Runnable() {
            public void run() {
                List<ApplicationItemInfo> apps = makeApplicationItemInfos(rows * columns);
                ApplicationsPageView pageView = new ApplicationsPageView(getActivity());
                pageView.populatePage(false, rows, columns, apps, null, null);
                pageView.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                pageView.layout(0, 0, width, height);
                int maxRight = 0;
                for (int i = 0; i < pageView.getChildCount(); i++) {
                    View child = pageView.getChildAt(i);
                    if (child.getRight() > maxRight) {
                        maxRight = child.getRight();
                    }
                }
                trailingGapHolder[0] = width - maxRight;
            }
        });

        assertEquals("No trailing gap: last cell must reach viewport right edge",
                0, trailingGapHolder[0]);
    }

    public void testPartialFinalPageFirstCellStartsAtLeadingSlot() throws Throwable {
        final int columns = 3;
        final int rows = 2;
        final int capacity = rows * columns;
        final int width = 300;
        final int height = 400;
        final int[] secondPageFirstChildLeftHolder = new int[1];
        final int[] firstPageFirstChildLeftHolder = new int[1];

        runTestOnUiThread(new Runnable() {
            public void run() {
                List<ApplicationItemInfo> allApps = makeApplicationItemInfos(capacity + 1);

                ApplicationsPageView firstPage = new ApplicationsPageView(getActivity());
                firstPage.populatePage(false, rows, columns, allApps.subList(0, capacity), null, null);
                firstPage.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                firstPage.layout(0, 0, width, height);

                ApplicationsPageView secondPage = new ApplicationsPageView(getActivity());
                secondPage.populatePage(false, rows, columns,
                        allApps.subList(capacity, capacity + 1), null, null);
                secondPage.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                secondPage.layout(0, 0, width, height);

                firstPageFirstChildLeftHolder[0] = firstPage.getChildAt(0).getLeft();
                secondPageFirstChildLeftHolder[0] = secondPage.getChildAt(0).getLeft();
            }
        });

        assertEquals("Partial final page first item must start at same slot as full page first item",
                firstPageFirstChildLeftHolder[0], secondPageFirstChildLeftHolder[0]);
    }

    public void testPageViewGroupChildrenFillMeasuredDimensions() throws Throwable {
        final int rows = 4;
        final int columns = 4;
        final int width = 480;
        final int height = 640;
        final boolean[] allChildrenInBoundsHolder = new boolean[1];
        final int[] childCountHolder = new int[1];

        runTestOnUiThread(new Runnable() {
            public void run() {
                List<ApplicationItemInfo> apps = makeApplicationItemInfos(rows * columns);
                ApplicationsPageView pageView = new ApplicationsPageView(getActivity());
                pageView.populatePage(false, rows, columns, apps, null, null);
                pageView.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                pageView.layout(0, 0, width, height);
                childCountHolder[0] = pageView.getChildCount();
                boolean allInBounds = true;
                for (int i = 0; i < pageView.getChildCount(); i++) {
                    View child = pageView.getChildAt(i);
                    if (child.getLeft() < 0 || child.getTop() < 0
                            || child.getRight() > width || child.getBottom() > height) {
                        allInBounds = false;
                    }
                }
                allChildrenInBoundsHolder[0] = allInBounds;
            }
        });

        assertEquals("Full-capacity page must have exact child count",
                rows * columns, childCountHolder[0]);
        assertTrue("All children must be within page bounds", allChildrenInBoundsHolder[0]);
    }

    private static List<ApplicationItemInfo> makeApplicationItemInfos(int count) {
        List<ApplicationItemInfo> items = new ArrayList<ApplicationItemInfo>(count);
        for (int i = 0; i < count; i++) {
            ApplicationItemInfo info = new ApplicationItemInfo();
            info.title = "App " + i;
            items.add(info);
        }
        return items;
    }
}
