package org.zmreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import org.zmreborn.theme.WallpaperColorExtractor;

/**
 * Modern signal rail view rendering an indicator slider track with smooth progress bar.
 */
final class SignalRailView extends View {
    private static final int TRACK_ALPHA = 217;

    private final Paint paint = new Paint();
    private boolean vertical;
    private int trackColor;
    private int activeColor;
    private int trackThickness;
    private int activeThickness;
    private int inset;
    private int totalItems = 1;
    private float progress;

    SignalRailView(Context context) {
        this(context, false);
    }

    SignalRailView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    SignalRailView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.vertical = false;
        initializeView(context);
    }

    SignalRailView(Context context, boolean vertical) {
        super(context);
        this.vertical = vertical;
        initializeView(context);
    }

    private void initializeView(Context context) {
        refreshPalette();
        this.trackThickness = dimension(context, R.dimen.rail_thickness);
        this.activeThickness = dimension(context, R.dimen.rail_active_thickness);
        this.inset = dimension(context, R.dimen.rail_inset);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setAntiAlias(false);
    }

    /**
     * Refreshes dynamic colors from the current wallpaper palette.
     */
    void refreshPalette() {
        int surface = WallpaperColorExtractor.getSurface(getContext());
        this.trackColor = Color.argb(TRACK_ALPHA, Color.red(surface), Color.green(surface), Color.blue(surface));
        this.activeColor = WallpaperColorExtractor.getPrimary(getContext());
        invalidate();
    }

    /**
     * Sets the total number of items represented along the rail.
     */
    void setTotalItems(int totalItems) {
        this.totalItems = Math.max(1, totalItems);
        invalidate();
    }

    /**
     * Sets the fractional progress along the rail [0.0, 1.0].
     */
    void setProgress(float progress) {
        this.progress = clampProgress(progress);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.vertical) {
            drawVertical(canvas);
            return;
        }
        drawHorizontal(canvas);
    }

    private void drawHorizontal(Canvas canvas) {
        int start = this.inset;
        int end = getWidth() - this.inset;
        int trackTop = centeredStart(getHeight(), this.trackThickness);
        draw(canvas, start, trackTop, end, trackTop + this.trackThickness, this.trackColor);
        int activeLength = activeLength(end - start);
        int activeStart = start + activeOffset(end - start, activeLength);
        int activeTop = centeredStart(getHeight(), this.activeThickness);
        draw(canvas, activeStart, activeTop, activeStart + activeLength,
                activeTop + this.activeThickness, this.activeColor);
    }

    private void drawVertical(Canvas canvas) {
        int start = this.inset;
        int end = getHeight() - this.inset;
        int trackLeft = centeredStart(getWidth(), this.trackThickness);
        draw(canvas, trackLeft, start, trackLeft + this.trackThickness, end, this.trackColor);
        int activeLength = activeLength(end - start);
        int activeStart = start + activeOffset(end - start, activeLength);
        int activeLeft = centeredStart(getWidth(), this.activeThickness);
        draw(canvas, activeLeft, activeStart, activeLeft + this.activeThickness,
                activeStart + activeLength, this.activeColor);
    }

    private void draw(Canvas canvas, int left, int top, int right, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        this.paint.setColor(color);
        canvas.drawRect(left, top, right, bottom, this.paint);
    }

    private int activeLength(int trackLength) {
        if (trackLength <= 0) {
            return 0;
        }
        return Math.max(1, trackLength / this.totalItems);
    }

    private int activeOffset(int trackLength, int activeLength) {
        int maximum = Math.max(0, trackLength - activeLength);
        int requested = Math.round(this.progress * trackLength);
        return Math.min(requested, maximum);
    }

    private int centeredStart(int available, int thickness) {
        return Math.max(0, (available - thickness) / 2);
    }

    private float clampProgress(float candidateProgress) {
        if (Float.isNaN(candidateProgress) || candidateProgress < 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, candidateProgress);
    }

    private static int dimension(Context context, int resourceId) {
        return context.getResources().getDimensionPixelSize(resourceId);
    }
}
