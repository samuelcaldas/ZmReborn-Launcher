package org.zmreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import org.zmreborn.theme.WallpaperColorExtractor;

final class SignalRailView extends View {
    private final Paint mPaint = new Paint();
    private boolean mVertical;
    private int mTrackColor;
    private int mActiveColor;
    private int mTrackThickness;
    private int mActiveThickness;
    private int mInset;
    private int mTotalItems = 1;
    private float mProgress;

    SignalRailView(Context context) {
        this(context, false);
    }

    SignalRailView(Context context, android.util.AttributeSet attrs) {
        this(context, attrs, 0);
    }

    SignalRailView(Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mVertical = false;
        initializeView(context);
    }

    SignalRailView(Context context, boolean vertical) {
        super(context);
        this.mVertical = vertical;
        initializeView(context);
    }

    private void initializeView(Context context) {
        refreshPalette();
        this.mTrackThickness = dimension(context, R.dimen.rail_thickness);
        this.mActiveThickness = dimension(context, R.dimen.rail_active_thickness);
        this.mInset = dimension(context, R.dimen.rail_inset);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setAntiAlias(false);
    }

    void refreshPalette() {
        int surface = WallpaperColorExtractor.getSurface(getContext());
        this.mTrackColor = Color.argb(217, Color.red(surface), Color.green(surface),
                Color.blue(surface));
        this.mActiveColor = WallpaperColorExtractor.getPrimary(getContext());
        invalidate();
    }

    void setTotalItems(int totalItems) {
        this.mTotalItems = Math.max(1, totalItems);
        invalidate();
    }

    void setProgress(float progress) {
        this.mProgress = clampProgress(progress);
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        if (this.mVertical) {
            drawVertical(canvas);
            return;
        }
        drawHorizontal(canvas);
    }

    private void drawHorizontal(Canvas canvas) {
        int start = this.mInset;
        int end = getWidth() - this.mInset;
        int trackTop = centeredStart(getHeight(), this.mTrackThickness);
        draw(canvas, start, trackTop, end, trackTop + this.mTrackThickness, this.mTrackColor);
        int activeLength = activeLength(end - start);
        int activeStart = start + activeOffset(end - start, activeLength);
        int activeTop = centeredStart(getHeight(), this.mActiveThickness);
        draw(canvas, activeStart, activeTop, activeStart + activeLength,
                activeTop + this.mActiveThickness, this.mActiveColor);
    }

    private void drawVertical(Canvas canvas) {
        int start = this.mInset;
        int end = getHeight() - this.mInset;
        int trackLeft = centeredStart(getWidth(), this.mTrackThickness);
        draw(canvas, trackLeft, start, trackLeft + this.mTrackThickness, end, this.mTrackColor);
        int activeLength = activeLength(end - start);
        int activeStart = start + activeOffset(end - start, activeLength);
        int activeLeft = centeredStart(getWidth(), this.mActiveThickness);
        draw(canvas, activeLeft, activeStart, activeLeft + this.mActiveThickness,
                activeStart + activeLength, this.mActiveColor);
    }

    private void draw(Canvas canvas, int left, int top, int right, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        this.mPaint.setColor(color);
        canvas.drawRect(left, top, right, bottom, this.mPaint);
    }

    private int activeLength(int trackLength) {
        if (trackLength <= 0) {
            return 0;
        }
        return Math.max(1, trackLength / this.mTotalItems);
    }

    private int activeOffset(int trackLength, int activeLength) {
        int maximum = Math.max(0, trackLength - activeLength);
        int requested = Math.round(this.mProgress * trackLength);
        return Math.min(requested, maximum);
    }

    private int centeredStart(int available, int thickness) {
        return Math.max(0, (available - thickness) / 2);
    }

    private float clampProgress(float progress) {
        if (Float.isNaN(progress) || progress < 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, progress);
    }

    private static int dimension(Context context, int resourceId) {
        return context.getResources().getDimensionPixelSize(resourceId);
    }
}
