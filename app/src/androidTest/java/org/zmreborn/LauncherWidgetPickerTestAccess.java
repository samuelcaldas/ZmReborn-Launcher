package org.zmreborn;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.zmreborn.widget.WidgetPickerDialog;

/** Drives launcher widget selection through the visible production Add dialog. */
final class LauncherWidgetPickerTestAccess {
    private static final long ACTION_TIMEOUT_MILLIS = 5000L;

    private LauncherWidgetPickerTestAccess() {
    }

    static WidgetPickerDialog openFromAddDialog(
            Instrumentation instrumentation, final Launcher launcher) {
        validate(instrumentation, launcher);
        showAddDialog(instrumentation, launcher);
        selectWidgetsAction(instrumentation,
                launcher.getString(R.string.group_add_widgets));
        return awaitPicker(instrumentation, launcher);
    }

    static int pendingId(Launcher launcher) {
        return ((Integer) getField(launcher, "mPendingAppWidgetId")).intValue();
    }

    static boolean pendingPlacement(Launcher launcher) {
        return ((Boolean) getField(launcher,
                "mPendingAppWidgetPlacement")).booleanValue();
    }

    static boolean hasPendingPlacementListener(Launcher launcher) {
        return getField(launcher, "mPendingAppWidgetPlacementListener") != null;
    }

    static void installPendingPlacement(Launcher launcher, int appWidgetId) {
        if (launcher == null || appWidgetId == -1) {
            throw new IllegalArgumentException(
                    "Pending placement requires Launcher and allocated widget ID");
        }
        final CellLayout layout = currentLayout(launcher);
        View.OnLayoutChangeListener listener =
                new View.OnLayoutChangeListener() {
            public void onLayoutChange(View view, int left, int top,
                    int right, int bottom, int oldLeft, int oldTop,
                    int oldRight, int oldBottom) {
            }
        };
        layout.addOnLayoutChangeListener(listener);
        setField(launcher, "mPendingAppWidgetId", Integer.valueOf(appWidgetId));
        setField(launcher, "mPendingAppWidgetPlacement", Boolean.TRUE);
        setField(launcher, "mPendingAppWidgetPlacementLayout", layout);
        setField(launcher, "mPendingAppWidgetPlacementListener", listener);
    }

    static void cancelPendingWidget(Launcher launcher) {
        invokeLauncherMethod(launcher, "releasePendingAppWidgetId",
                new Class<?>[]{android.content.Intent.class},
                new Object[]{null});
    }

    static void dismiss(Launcher launcher) {
        WidgetPickerDialog picker = currentOrNull(launcher);
        if (picker != null && picker.isShowing()) {
            picker.dismiss();
        }
    }

    private static void validate(Instrumentation instrumentation,
            Launcher launcher) {
        if (instrumentation == null || launcher == null
                || launcher.mWorkspace == null) {
            throw new IllegalArgumentException(
                    "Widget picker test requires instrumentation and Launcher workspace");
        }
    }

    private static void showAddDialog(Instrumentation instrumentation,
            final Launcher launcher) {
        instrumentation.runOnMainSync(new Runnable() {
            public void run() {
                launcher.showAddDialog(emptyTarget(launcher.mWorkspace));
            }
        });
    }

    private static void selectWidgetsAction(Instrumentation instrumentation,
            CharSequence label) {
        long deadline = SystemClock.uptimeMillis() + ACTION_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            if (clickMatchingNode(instrumentation.getUiAutomation(), label)) {
                return;
            }
            SystemClock.sleep(25L);
        }
        throw new AssertionError(
                "Launcher Add dialog did not expose clickable Widgets action");
    }

    private static boolean clickMatchingNode(UiAutomation automation,
            CharSequence label) {
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        if (root == null) {
            return false;
        }
        List<AccessibilityNodeInfo> nodes =
                root.findAccessibilityNodeInfosByText(label.toString());
        try {
            return clickMatchingNode(automation, nodes, label);
        } finally {
            recycle(nodes);
            root.recycle();
        }
    }

    private static boolean clickMatchingNode(UiAutomation automation,
            List<AccessibilityNodeInfo> nodes, CharSequence label) {
        for (AccessibilityNodeInfo node : nodes) {
            if (matches(node, label) && tapNode(automation, node)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(AccessibilityNodeInfo node,
            CharSequence label) {
        return node.isVisibleToUser() && (label.equals(node.getText())
                || label.equals(node.getContentDescription()));
    }

    private static boolean tapNode(UiAutomation automation,
            AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) {
            return false;
        }
        return injectTap(automation, bounds.centerX(), bounds.centerY());
    }

    private static boolean injectTap(UiAutomation automation,
            float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = touchEvent(downTime, downTime,
                MotionEvent.ACTION_DOWN, x, y);
        MotionEvent up = touchEvent(downTime, downTime + 1L,
                MotionEvent.ACTION_UP, x, y);
        try {
            return automation.injectInputEvent(down, true)
                    && automation.injectInputEvent(up, true);
        } finally {
            down.recycle();
            up.recycle();
        }
    }

    private static MotionEvent touchEvent(long downTime, long eventTime,
            int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        return event;
    }

    private static void recycle(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) {
            node.recycle();
        }
    }

    private static WidgetPickerDialog awaitPicker(
            Instrumentation instrumentation, final Launcher launcher) {
        long deadline = SystemClock.uptimeMillis() + ACTION_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            final WidgetPickerDialog[] picker = new WidgetPickerDialog[1];
            instrumentation.runOnMainSync(new Runnable() {
                public void run() {
                    picker[0] = currentOrNull(launcher);
                }
            });
            if (picker[0] != null && picker[0].isShowing()) {
                return picker[0];
            }
            SystemClock.sleep(25L);
        }
        throw new AssertionError("Widgets action did not open production picker");
    }

    private static CellLayout.CellInfo emptyTarget(Workspace workspace) {
        int screen = workspace.getCurrentScreen();
        if (!(workspace.getChildAt(screen) instanceof CellLayout)) {
            throw new IllegalStateException(
                    "Widget picker target screen is not a CellLayout");
        }
        CellLayout.CellInfo target = new CellLayout.CellInfo();
        target.valid = true;
        target.screen = screen;
        target.cellX = -1;
        target.cellY = -1;
        return target;
    }

    private static CellLayout currentLayout(Launcher launcher) {
        int screen = launcher.mWorkspace.getCurrentScreen();
        View child = launcher.mWorkspace.getChildAt(screen);
        if (!(child instanceof CellLayout)) {
            throw new IllegalStateException(
                    "Pending widget target screen is not a CellLayout");
        }
        return (CellLayout) child;
    }

    private static WidgetPickerDialog currentOrNull(Launcher launcher) {
        Object picker = getField(launcher, "mWidgetPickerDialog");
        if (picker == null) {
            return null;
        }
        if (!(picker instanceof WidgetPickerDialog)) {
            throw new IllegalStateException(
                    "Launcher widget picker field has unexpected type");
        }
        return (WidgetPickerDialog) picker;
    }

    private static Object getField(Launcher launcher, String name) {
        try {
            Field field = Launcher.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(launcher);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Launcher." + name,
                    exception);
        }
    }

    private static void setField(Launcher launcher, String name,
            Object value) {
        try {
            Field field = Launcher.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(launcher, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set Launcher." + name,
                    exception);
        }
    }

    private static void invokeLauncherMethod(Launcher launcher, String name,
            Class<?>[] parameterTypes, Object[] arguments) {
        try {
            Method method = Launcher.class.getDeclaredMethod(
                    name, parameterTypes);
            method.setAccessible(true);
            method.invoke(launcher, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke Launcher." + name,
                    exception);
        }
    }
}
