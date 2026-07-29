package org.zmreborn;

import android.appwidget.AppWidgetProviderInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.view.View;

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
        final Fixture[] fixtures = new Fixture[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, false);
                View handle = findHandle(fixture.frame,
                        R.string.widget_resize_handle_right);
                int minimumTouchTarget = Math.round(48 * fixture.launcher
                        .getResources().getDisplayMetrics().density);

                assertTrue(fixture.frame.supportsResize());
                assertEquals(2, fixture.frame.getChildCount());
                assertEquals(minimumTouchTarget, handle.getMeasuredWidth());
                assertEquals(minimumTouchTarget, handle.getMeasuredHeight());
                assertTrue(handle.performClick());
                fixtures[0] = fixture;
            }
        });

        Fixture fixture = fixtures[0];
        assertNotNull("Valid resize must notify callback", fixture.callback.candidate);
        assertEquals(0, fixture.callback.candidate.cellX);
        assertEquals(0, fixture.callback.candidate.cellY);
        assertEquals(2, fixture.callback.candidate.spanX);
        assertEquals(1, fixture.callback.candidate.spanY);
        assertFalse("Valid resize must not cancel", fixture.callback.cancelled);
        assertWidgetPlacement(fixture.widget, 0, 0, 1, 1);
    }

    public void testOccupiedCandidateDoesNotCommitOrMoveViews() {
        final Fixture[] fixtures = new Fixture[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, true);
                View handle = findHandle(fixture.frame,
                        R.string.widget_resize_handle_right);
                assertTrue(handle.performClick());
                fixtures[0] = fixture;
            }
        });

        Fixture fixture = fixtures[0];
        assertNull("Occupied candidate must not commit", fixture.callback.candidate);
        assertFalse("Rejected accessibility action keeps selection active",
                fixture.callback.cancelled);
        assertWidgetPlacement(fixture.widget, 0, 0, 1, 1);
        assertWidgetPlacement(fixture.neighbor, 1, 0, 1, 1);
    }

    public void testNonResizableProviderExposesNoHandles() {
        final Fixture[] fixtures = new Fixture[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                fixtures[0] = createFixture(AppWidgetProviderInfo.RESIZE_NONE, false);
            }
        });

        assertFalse(fixtures[0].frame.supportsResize());
        assertEquals("Fixed provider must expose no resize handles",
                0, fixtures[0].frame.getChildCount());
    }

    public void testBothAxesExposeCornerHandles() {
        final Fixture[] fixtures = new Fixture[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                fixtures[0] = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL
                                | AppWidgetProviderInfo.RESIZE_VERTICAL,
                        false);
            }
        });

        WidgetResizeFrame frame = fixtures[0].frame;
        assertEquals(4, frame.getChildCount());
        assertNotNull(findHandle(frame, R.string.widget_resize_handle_top_left));
        assertNotNull(findHandle(frame, R.string.widget_resize_handle_bottom_right));
    }

    public void testOutsideTapCancelsWithoutMutation() {
        final Fixture[] fixtures = new Fixture[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Fixture fixture = createFixture(
                        AppWidgetProviderInfo.RESIZE_HORIZONTAL, false);
                long eventTime = System.currentTimeMillis();
                MotionEvent down = MotionEvent.obtain(eventTime, eventTime,
                        MotionEvent.ACTION_DOWN, LAYOUT_WIDTH - 1,
                        LAYOUT_HEIGHT - 1, 0);
                MotionEvent up = MotionEvent.obtain(eventTime, eventTime + 1,
                        MotionEvent.ACTION_UP, LAYOUT_WIDTH - 1,
                        LAYOUT_HEIGHT - 1, 0);
                try {
                    assertTrue(fixture.frame.onTouchEvent(down));
                    assertTrue(fixture.frame.onTouchEvent(up));
                } finally {
                    down.recycle();
                    up.recycle();
                }
                fixtures[0] = fixture;
            }
        });

        Fixture fixture = fixtures[0];
        assertTrue("Outside release must cancel selection", fixture.callback.cancelled);
        assertNull("Cancellation must not commit", fixture.callback.candidate);
        assertWidgetPlacement(fixture.widget, 0, 0, 1, 1);
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
        return new Fixture(launcher, widget, neighbor, frame, callback);
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

    private static void assertWidgetPlacement(View view, int cellX, int cellY,
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

        public void onWidgetResizeCancelled() {
            this.cancelled = true;
        }

        public void onWidgetResizeCommitted(
                CellLayout.ResizeCandidate resizeCandidate) {
            this.candidate = resizeCandidate;
        }
    }

    private static final class Fixture {
        final Launcher launcher;
        final View widget;
        final View neighbor;
        final WidgetResizeFrame frame;
        final RecordingCallback callback;

        Fixture(Launcher launcher, View widget, View neighbor,
                WidgetResizeFrame frame, RecordingCallback callback) {
            this.launcher = launcher;
            this.widget = widget;
            this.neighbor = neighbor;
            this.frame = frame;
            this.callback = callback;
        }
    }
}
