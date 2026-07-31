package org.zmreborn;

import android.appwidget.AppWidgetProviderInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class WidgetResizeInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {
    private static final int LAYOUT_WIDTH = 400;
    private static final int LAYOUT_HEIGHT = 800;

    public WidgetResizeInstrumentationTest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testHorizontalHandleCommitsOneCellCandidate() {
        runOnMain(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, false);
                View handle = findHandle(fixture.frame,
                        R.string.widget_resize_handle_right);
                int minimumTarget = Math.round(48 * getActivity()
                        .getResources().getDisplayMetrics().density);

                assertTrue(fixture.frame.supportsResize());
                assertEquals(2, fixture.frame.getChildCount());
                assertEquals(minimumTarget, handle.getMeasuredWidth());
                assertEquals(minimumTarget, handle.getMeasuredHeight());
                assertTrue(handle.performClick());
                assertNotNull("Valid resize must notify callback",
                        fixture.callback.candidate);
                assertCandidate(fixture.callback.candidate, 0, 0, 2, 1);
                assertFalse("Valid resize must not cancel",
                        fixture.callback.cancelled);
                assertEquals("Handle interaction must not request drag", 0,
                        fixture.callback.dragRequests);
                assertPlacement(fixture.widget, 0, 0, 1, 1);
            }
        });
    }

    public void testOccupiedCandidateDoesNotCommitOrMoveViews() {
        runOnMain(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, true);
                View handle = findHandle(fixture.frame,
                        R.string.widget_resize_handle_right);

                assertTrue(handle.performClick());
                assertNull("Occupied candidate must not commit",
                        fixture.callback.candidate);
                assertFalse("Rejected action keeps selection active",
                        fixture.callback.cancelled);
                assertPlacement(fixture.widget, 0, 0, 1, 1);
                assertPlacement(fixture.neighbor, 1, 0, 1, 1);
            }
        });
    }

    public void testNonResizableProviderExposesNoHandles() {
        runOnMain(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_NONE, false);
                assertFalse(fixture.frame.supportsResize());
                assertEquals("Fixed provider must expose no resize handles",
                        0, fixture.frame.getChildCount());
            }
        });
    }

    public void testBothAxesExposeCornerHandles() {
        runOnMain(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL
                                | AppWidgetProviderInfo.RESIZE_VERTICAL,
                        false);
                assertEquals(4, fixture.frame.getChildCount());
                assertNotNull(findHandle(fixture.frame,
                        R.string.widget_resize_handle_top_left));
                assertNotNull(findHandle(fixture.frame,
                        R.string.widget_resize_handle_bottom_right));
            }
        });
    }

    public void testWidgetBodyDragRequestsOnceWithoutResizeCallbacks() {
        runOnMain(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, false);
                int touchSlop = ViewConfiguration.get(getActivity())
                        .getScaledTouchSlop();

                sendWidgetBodyDrag(fixture.frame, fixture.widget, touchSlop + 1);

                assertEquals("Body drag must request drag once", 1,
                        fixture.callback.dragRequests);
                assertNull("Body drag must not commit resize",
                        fixture.callback.candidate);
                assertFalse("Body drag must not cancel resize",
                        fixture.callback.cancelled);
                assertPlacement(fixture.widget, 0, 0, 1, 1);
            }
        });
    }

    public void testOutsideTapCancelsWithoutMutation() {
        runOnMain(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, false);
                sendOutsideTap(fixture.frame);
                assertTrue("Outside release must cancel selection",
                        fixture.callback.cancelled);
                assertNull("Cancellation must not commit",
                        fixture.callback.candidate);
                assertPlacement(fixture.widget, 0, 0, 1, 1);
            }
        });
    }

    public void testFrameAccessibilityClickCancelsWithoutMutation() {
        runOnMain(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, false);
                assertTrue(fixture.frame.performClick());
                assertTrue("Frame accessibility click must cancel selection",
                        fixture.callback.cancelled);
                assertNull("Cancellation must not commit",
                        fixture.callback.candidate);
                assertPlacement(fixture.widget, 0, 0, 1, 1);
            }
        });
    }

    private void runOnMain(final Runnable action) {
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
        if (failure[0] instanceof Error) {
            throw (Error) failure[0];
        }
        if (failure[0] instanceof RuntimeException) {
            throw (RuntimeException) failure[0];
        }
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    private Fixture createFixture(int resizeMode, boolean addNeighbor) {
        Launcher launcher = getActivity();
        CellLayout layout = new CellLayout(launcher);
        View widget = new View(launcher);
        layout.addView(widget, new CellLayout.LayoutParams(0, 0, 1, 1));
        View neighbor = null;
        if (addNeighbor) {
            neighbor = new View(launcher);
            layout.addView(neighbor, new CellLayout.LayoutParams(1, 0, 1, 1));
        }
        measureAndLayout(layout);
        assertTrue("Fixture CellLayout geometry must be ready",
                layout.isWidgetSizingGeometryReady());

        RecordingCallback callback = new RecordingCallback();
        WidgetResizeFrame frame = new WidgetResizeFrame(launcher, layout,
                widget, providerInfo(resizeMode), callback);
        measureAndLayout(frame);
        return new Fixture(widget, neighbor, frame, callback);
    }

    private static AppWidgetProviderInfo providerInfo(int resizeMode) {
        AppWidgetProviderInfo providerInfo = new AppWidgetProviderInfo();
        providerInfo.resizeMode = resizeMode;
        providerInfo.minWidth = 1;
        providerInfo.minHeight = 1;
        providerInfo.minResizeWidth = 1;
        providerInfo.minResizeHeight = 1;
        return providerInfo;
    }

    private static void measureAndLayout(View view) {
        int width = View.MeasureSpec.makeMeasureSpec(
                LAYOUT_WIDTH, View.MeasureSpec.EXACTLY);
        int height = View.MeasureSpec.makeMeasureSpec(
                LAYOUT_HEIGHT, View.MeasureSpec.EXACTLY);
        view.measure(width, height);
        view.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);
    }

    private static void sendWidgetBodyDrag(WidgetResizeFrame frame, View widget,
            int distance) {
        int x = widget.getLeft() + (widget.getWidth() / 2);
        int y = widget.getTop() + (widget.getHeight() / 2);
        long eventTime = System.currentTimeMillis();
        MotionEvent down = MotionEvent.obtain(eventTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent move = MotionEvent.obtain(eventTime, eventTime + 1,
                MotionEvent.ACTION_MOVE, x + distance, y, 0);
        MotionEvent up = MotionEvent.obtain(eventTime, eventTime + 2,
                MotionEvent.ACTION_UP, x + distance, y, 0);
        try {
            assertTrue(frame.onTouchEvent(down));
            assertTrue(frame.onTouchEvent(move));
            assertTrue(frame.onTouchEvent(up));
        } finally {
            down.recycle();
            move.recycle();
            up.recycle();
        }
    }

    private static void sendOutsideTap(WidgetResizeFrame frame) {
        long eventTime = System.currentTimeMillis();
        MotionEvent down = MotionEvent.obtain(eventTime, eventTime,
                MotionEvent.ACTION_DOWN, LAYOUT_WIDTH - 1, LAYOUT_HEIGHT - 1, 0);
        MotionEvent up = MotionEvent.obtain(eventTime, eventTime + 1,
                MotionEvent.ACTION_UP, LAYOUT_WIDTH - 1, LAYOUT_HEIGHT - 1, 0);
        try {
            assertTrue(frame.onTouchEvent(down));
            assertTrue(frame.onTouchEvent(up));
        } finally {
            down.recycle();
            up.recycle();
        }
    }

    private View findHandle(WidgetResizeFrame frame, int descriptionId) {
        String description = getActivity().getString(descriptionId);
        for (int index = 0; index < frame.getChildCount(); index++) {
            View child = frame.getChildAt(index);
            if (description.contentEquals(child.getContentDescription())) {
                return child;
            }
        }
        fail("Missing resize handle: " + description);
        return null;
    }

    private static void assertCandidate(CellLayout.ResizeCandidate candidate,
            int cellX, int cellY, int spanX, int spanY) {
        assertEquals(cellX, candidate.cellX);
        assertEquals(cellY, candidate.cellY);
        assertEquals(spanX, candidate.spanX);
        assertEquals(spanY, candidate.spanY);
    }

    private static void assertPlacement(View view, int cellX, int cellY,
            int spanX, int spanY) {
        CellLayout.LayoutParams params =
                (CellLayout.LayoutParams) view.getLayoutParams();
        assertEquals(cellX, params.cellX);
        assertEquals(cellY, params.cellY);
        assertEquals(spanX, params.cellHSpan);
        assertEquals(spanY, params.cellVSpan);
    }

    private static final class RecordingCallback
            implements WidgetResizeFrame.Callback {
        CellLayout.ResizeCandidate candidate;
        boolean cancelled;
        int dragRequests;

        public void onWidgetDragRequested() {
            this.dragRequests++;
        }

        public void onWidgetResizeCancelled() {
            this.cancelled = true;
        }

        public void onWidgetResizeCommitted(
                CellLayout.ResizeCandidate resizeCandidate) {
            this.candidate = resizeCandidate;
        }
    }

    private static final class Fixture {
        final View widget;
        final View neighbor;
        final WidgetResizeFrame frame;
        final RecordingCallback callback;

        Fixture(View widget, View neighbor, WidgetResizeFrame frame,
                RecordingCallback callback) {
            this.widget = widget;
            this.neighbor = neighbor;
            this.frame = frame;
            this.callback = callback;
        }
    }
}
