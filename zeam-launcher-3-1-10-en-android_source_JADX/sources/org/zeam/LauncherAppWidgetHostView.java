package org.zeam;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

public class LauncherAppWidgetHostView extends AppWidgetHostView {
    private static final long WIDGET_LONG_CLICK_TIMEOUT = 700;
    /* access modifiers changed from: private */
    public boolean mHasPerformedLongPress;
    private LayoutInflater mInflater;
    private CheckForLongPress mPendingCheckForLongPress;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    /* access modifiers changed from: protected */
    public View getErrorView() {
        return this.mInflater.inflate(C0041R.layout.appwidget_error, this, false);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mHasPerformedLongPress) {
            this.mHasPerformedLongPress = false;
            return true;
        }
        switch (motionEvent.getAction()) {
            case 0:
                postCheckForLongClick();
                return false;
            case 1:
            case 3:
                this.mHasPerformedLongPress = false;
                if (this.mPendingCheckForLongPress == null) {
                    return false;
                }
                removeCallbacks(this.mPendingCheckForLongPress);
                return false;
            default:
                return false;
        }
    }

    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        CheckForLongPress() {
        }

        public void run() {
            if (LauncherAppWidgetHostView.this.getParent() != null && LauncherAppWidgetHostView.this.hasWindowFocus() && this.mOriginalWindowAttachCount == LauncherAppWidgetHostView.this.getWindowAttachCount() && !LauncherAppWidgetHostView.this.mHasPerformedLongPress && LauncherAppWidgetHostView.this.performLongClick()) {
                LauncherAppWidgetHostView.this.mHasPerformedLongPress = true;
            }
        }

        public void rememberWindowAttachCount() {
            this.mOriginalWindowAttachCount = LauncherAppWidgetHostView.this.getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress == null) {
            this.mPendingCheckForLongPress = new CheckForLongPress();
        }
        this.mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(this.mPendingCheckForLongPress, WIDGET_LONG_CLICK_TIMEOUT);
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress != null) {
            removeCallbacks(this.mPendingCheckForLongPress);
        }
    }
}
