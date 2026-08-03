package org.zmreborn.widget;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import org.zmreborn.R;

/** Dispatches actual pointer events through visible production widget-picker rows. */
public final class WidgetPickerTouch {
    private static final long ROW_TIMEOUT_MILLIS = 5000L;

    private WidgetPickerTouch() {
    }

    /** Taps visible provider card through picker ListView ACTION_DOWN and ACTION_UP dispatch. */
    public static void tapProvider(Instrumentation instrumentation,
            WidgetPickerDialog picker, ComponentName provider) {
        validate(instrumentation, picker, provider);
        long deadline = SystemClock.uptimeMillis() + ROW_TIMEOUT_MILLIS;
        String[] state = new String[1];
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            if (dispatchProviderTap(instrumentation, picker, provider, state)) {
                return;
            }
            SystemClock.sleep(25L);
        }
        throw new AssertionError("Widget picker did not render provider row: " + provider
                + " state=" + state[0]);
    }

    private static void validate(Instrumentation instrumentation,
            WidgetPickerDialog picker, ComponentName provider) {
        if (instrumentation == null || picker == null || provider == null) {
            throw new IllegalArgumentException("Widget picker touch requires instrumentation, picker, and provider");
        }
    }

    private static boolean dispatchProviderTap(final Instrumentation instrumentation,
            final WidgetPickerDialog picker, final ComponentName provider,
            final String[] state) {
        final boolean[] tapped = new boolean[1];
        instrumentation.runOnMainSync(new Runnable() {
            public void run() {
                ListView list = requireList(picker);
                ListAdapter adapter = list.getAdapter();
                if (adapter == null) {
                    state[0] = "adapter=loading";
                    return;
                }
                int position = providerPosition(adapter, provider);
                state[0] = "adapter=" + adapter.getCount() + " position=" + position;
                if (position < 0) {
                    return;
                }
                View row = visibleRow(list, position);
                if (row == null) {
                    list.setSelectionFromTop(position, list.getPaddingTop());
                    state[0] += " first=" + list.getFirstVisiblePosition()
                            + " children=" + list.getChildCount()
                            + " list=" + list.getWidth() + "x" + list.getHeight();
                    return;
                }
                requireProviderRow(row, provider);
                dispatchTap(list, row);
                tapped[0] = true;
            }
        });
        return tapped[0];
    }

    private static ListView requireList(WidgetPickerDialog picker) {
        ListView list = (ListView) picker.findViewById(R.id.widget_picker_list);
        if (list == null) {
            throw new IllegalStateException("Widget picker must contain production ListView");
        }
        return list;
    }

    private static int providerPosition(ListAdapter adapter, ComponentName provider) {
        for (int position = 0; position < adapter.getCount(); position++) {
            Object item = adapter.getItem(position);
            if (isProvider(item, provider)) {
                return position;
            }
        }
        return -1;
    }

    private static boolean isProvider(Object item, ComponentName provider) {
        if (!(item instanceof WidgetPickerEntry)) {
            return false;
        }
        WidgetPickerEntry entry = (WidgetPickerEntry) item;
        return !entry.isSearch() && entry.getProvider() != null
                && provider.equals(entry.getProvider().provider);
    }

    private static View visibleRow(ListView list, int position) {
        int childIndex = position - list.getFirstVisiblePosition();
        if (childIndex < 0 || childIndex >= list.getChildCount()) {
            return null;
        }
        View row = list.getChildAt(childIndex);
        if (row.getWidth() <= 0 || row.getHeight() <= 0) {
            return null;
        }
        return row;
    }

    private static void requireProviderRow(View row, ComponentName provider) {
        if (!isProvider(row.getTag(), provider)) {
            throw new AssertionError("Visible widget-picker row does not match provider: "
                    + provider);
        }
    }

    private static void dispatchTap(ListView list, View row) {
        long downTime = SystemClock.uptimeMillis();
        float x = row.getLeft() + (row.getWidth() / 2.0f);
        float y = row.getTop() + (row.getHeight() / 2.0f);
        MotionEvent down = MotionEvent.obtain(downTime, downTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(downTime, downTime + 1L,
                MotionEvent.ACTION_UP, x, y, 0);
        try {
            if (!list.dispatchTouchEvent(down) || !list.dispatchTouchEvent(up)) {
                throw new AssertionError("Widget picker ListView did not handle card touch");
            }
        } finally {
            down.recycle();
            up.recycle();
        }
    }
}
