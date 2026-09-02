package org.zmreborn;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

/**
 * AppWidgetHostView with touch interception and custom long-press detection for workspace drag routing.
 */
public class LauncherAppWidgetHostView extends AppWidgetHostView {
    private static final long WIDGET_LONG_CLICK_TIMEOUT = 700;
    private boolean hasPerformedLongPress;
    private LayoutInflater inflater;
    private CheckForLongPress pendingCheckForLongPress;

    /**
     * Constructs a launcher app widget host view with context.
     */
    public LauncherAppWidgetHostView(Context context) {
        super(context);
        this.inflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    @Override
    protected View getErrorView() {
        return this.inflater.inflate(R.layout.appwidget_error, this, false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.hasPerformedLongPress) {
            this.hasPerformedLongPress = false;
            return true;
        }
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                postCheckForLongClick();
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.hasPerformedLongPress = false;
                if (this.pendingCheckForLongPress == null) {
                    return false;
                }
                removeCallbacks(this.pendingCheckForLongPress);
                return false;
            default:
                return false;
        }
    }

    class CheckForLongPress implements Runnable {
        private int originalWindowAttachCount;

        CheckForLongPress() {
        }

        @Override
        public void run() {
            if (getParent() != null && hasWindowFocus()
                    && this.originalWindowAttachCount == getWindowAttachCount()
                    && !hasPerformedLongPress && performLongClick()) {
                hasPerformedLongPress = true;
            }
        }

        public void rememberWindowAttachCount() {
            this.originalWindowAttachCount = getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        this.hasPerformedLongPress = false;
        if (this.pendingCheckForLongPress == null) {
            this.pendingCheckForLongPress = new CheckForLongPress();
        }
        this.pendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(this.pendingCheckForLongPress, WIDGET_LONG_CLICK_TIMEOUT);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.hasPerformedLongPress = false;
        if (this.pendingCheckForLongPress != null) {
            removeCallbacks(this.pendingCheckForLongPress);
        }
    }
}
