package org.zmreborn;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * Overlay frame for interactive cell-snapped widget resizing on workspace desktops.
 */
@SuppressLint("ViewConstructor")
// TODO(move): belongs in org.zmreborn.widget after CellLayout APIs are isolated.
final class WidgetResizeFrame extends FrameLayout {
    private static final int HANDLE_SIZE_DP = 48;
    private static final int HANDLE_RADIUS_DP = 8;
    private static final int OUTLINE_WIDTH_DP = 2;

    interface Callback {
        void onWidgetDragRequested();

        void onWidgetResizeCancelled();

        void onWidgetResizeCommitted(CellLayout.ResizeCandidate candidate);
    }

    private final CellLayout cellLayout;
    private final View widgetView;
    private final Callback callback;
    private final boolean horizontalResizeEnabled;
    private final boolean verticalResizeEnabled;
    private final int minimumSpanX;
    private final int minimumSpanY;
    private final int handleSize;
    private final int touchSlopSquared;
    private final Rect candidateBounds = new Rect();
    private final int[] cellLocation = new int[2];
    private final int[] frameLocation = new int[2];
    private final int[] cellPoint = new int[2];
    private final Paint outlinePaint = new Paint(1);
    private final Paint labelBackgroundPaint = new Paint(1);
    private final Paint labelPaint = new Paint(1);

    private ResizeHandleView activeHandle;
    private CellLayout.ResizeCandidate candidate;
    private final CellLayout.ResizeCandidate originalCandidate;
    private boolean candidateValid;
    private boolean trackWidgetDrag;
    private float widgetDragDownX;
    private float widgetDragDownY;
    private boolean finished;

    WidgetResizeFrame(Context context, CellLayout cellLayout, View widgetView,
            AppWidgetProviderInfo providerInfo, Callback callback) {
        super(context);
        if (cellLayout == null || widgetView == null || providerInfo == null
                || callback == null) {
            throw new IllegalArgumentException("Resize frame requires widget state");
        }
        if (!(widgetView.getLayoutParams() instanceof CellLayout.LayoutParams)) {
            throw new IllegalArgumentException("Widget must use CellLayout.LayoutParams");
        }
        this.cellLayout = cellLayout;
        this.widgetView = widgetView;
        this.callback = callback;
        this.horizontalResizeEnabled = supportsHorizontalResize(providerInfo);
        this.verticalResizeEnabled = supportsVerticalResize(providerInfo);
        CellLayout.LayoutParams params = (CellLayout.LayoutParams) widgetView.getLayoutParams();
        int[] minimumSpans = calculateMinimumSpans(providerInfo);
        this.minimumSpanX = this.horizontalResizeEnabled
                ? minimumSpans[0] : params.cellHSpan;
        this.minimumSpanY = this.verticalResizeEnabled
                ? minimumSpans[1] : params.cellVSpan;
        this.originalCandidate = new CellLayout.ResizeCandidate(params.cellX, params.cellY,
                params.cellHSpan, params.cellVSpan);
        this.candidate = this.originalCandidate;
        this.candidateValid = true;
        this.handleSize = dimensionToPixels(HANDLE_SIZE_DP);
        int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.touchSlopSquared = touchSlop * touchSlop;
        configureDrawing();
        addSupportedHandles();
    }

    boolean supportsResize() {
        return this.horizontalResizeEnabled || this.verticalResizeEnabled;
    }

    private void configureDrawing() {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        this.outlinePaint.setStyle(Paint.Style.STROKE);
        this.outlinePaint.setStrokeWidth(dimensionToPixels(OUTLINE_WIDTH_DP));
        this.labelBackgroundPaint.setStyle(Paint.Style.FILL);
        this.labelPaint.setTextAlign(Paint.Align.CENTER);
        this.labelPaint.setTextSize(dimensionToPixels(14));
    }

    private int[] calculateMinimumSpans(AppWidgetProviderInfo providerInfo) {
        int width = preferredResizeDimension(providerInfo.minResizeWidth,
                providerInfo.minWidth);
        int height = preferredResizeDimension(providerInfo.minResizeHeight,
                providerInfo.minHeight);
        return this.cellLayout.rectToCellFromDp(width, height);
    }

    private int preferredResizeDimension(int resizeDimension, int minimumDimension) {
        if (resizeDimension > 0) {
            return resizeDimension;
        }
        return Math.max(0, minimumDimension);
    }

    private static boolean supportsHorizontalResize(AppWidgetProviderInfo providerInfo) {
        return (providerInfo.resizeMode & AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0;
    }

    private static boolean supportsVerticalResize(AppWidgetProviderInfo providerInfo) {
        return (providerInfo.resizeMode & AppWidgetProviderInfo.RESIZE_VERTICAL) != 0;
    }

    private void addSupportedHandles() {
        if (!supportsResize()) {
            return;
        }
        if (this.horizontalResizeEnabled && this.verticalResizeEnabled) {
            addHandle(CellLayout.RESIZE_EDGE_START, CellLayout.RESIZE_EDGE_START,
                    R.string.widget_resize_handle_top_left);
            addHandle(CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_START,
                    R.string.widget_resize_handle_top_right);
            addHandle(CellLayout.RESIZE_EDGE_START, CellLayout.RESIZE_EDGE_END,
                    R.string.widget_resize_handle_bottom_left);
            addHandle(CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_END,
                    R.string.widget_resize_handle_bottom_right);
            return;
        }
        if (this.horizontalResizeEnabled) {
            addHandle(CellLayout.RESIZE_EDGE_START, CellLayout.RESIZE_EDGE_NONE,
                    R.string.widget_resize_handle_left);
            addHandle(CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_NONE,
                    R.string.widget_resize_handle_right);
            return;
        }
        addHandle(CellLayout.RESIZE_EDGE_NONE, CellLayout.RESIZE_EDGE_START,
                R.string.widget_resize_handle_top);
        addHandle(CellLayout.RESIZE_EDGE_NONE, CellLayout.RESIZE_EDGE_END,
                R.string.widget_resize_handle_bottom);
    }

    private void addHandle(int horizontalEdge, int verticalEdge, int descriptionId) {
        final ResizeHandleView handle = new ResizeHandleView(getContext(), horizontalEdge,
                verticalEdge, dimensionToPixels(HANDLE_RADIUS_DP));
        handle.setContentDescription(getResources().getString(descriptionId));
        handle.setFocusable(true);
        handle.setClickable(true);
        handle.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                boolean handled = onHandleTouch(handle, event);
                if (handled && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return handled;
            }
        });
        handle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                resizeOneCell(handle);
            }
        });
        addView(handle, new FrameLayout.LayoutParams(this.handleSize, this.handleSize));
    }

    private boolean onHandleTouch(ResizeHandleView handle, MotionEvent event) {
        if (this.finished) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.activeHandle = handle;
                updatePreview(handle, event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.activeHandle == handle) {
                    updatePreview(handle, event);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (this.activeHandle == handle) {
                    updatePreview(handle, event);
                    finishResize();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (this.activeHandle == handle) {
                    finishCancelled();
                }
                return true;
            default:
                return true;
        }
    }

    private void updatePreview(ResizeHandleView handle, MotionEvent event) {
        updateCellPoint(handle, event);
        CellLayout.LayoutParams params = (CellLayout.LayoutParams) this.widgetView.getLayoutParams();
        this.candidate = this.cellLayout.findResizeCandidate(params,
                this.cellPoint[0], this.cellPoint[1], handle.horizontalEdge,
                handle.verticalEdge, this.minimumSpanX, this.minimumSpanY);
        this.candidateValid = this.cellLayout.isResizeCandidateVacant(this.candidate,
                this.widgetView);
        requestLayout();
        invalidate();
    }

    private void updateCellPoint(ResizeHandleView handle, MotionEvent event) {
        this.cellLayout.getLocationOnScreen(this.cellLocation);
        this.cellPoint[0] = Math.round(event.getRawX() - this.cellLocation[0]);
        this.cellPoint[1] = Math.round(event.getRawY() - this.cellLocation[1]);
        if (handle.horizontalEdge == CellLayout.RESIZE_EDGE_END) {
            this.cellPoint[0]--;
        }
        if (handle.verticalEdge == CellLayout.RESIZE_EDGE_END) {
            this.cellPoint[1]--;
        }
    }

    private void finishResize() {
        if (this.candidateValid && !this.candidate.matches(
                (CellLayout.LayoutParams) this.widgetView.getLayoutParams())) {
            finishCommitted(this.candidate);
            return;
        }
        finishCancelled();
    }

    private void resizeOneCell(ResizeHandleView handle) {
        if (this.finished) {
            return;
        }
        CellLayout.LayoutParams params = (CellLayout.LayoutParams) this.widgetView.getLayoutParams();
        CellLayout.ResizeCandidate resCandidate = CellLayout.calculateResizeCandidate(
                params.cellX, params.cellY, params.cellHSpan, params.cellVSpan,
                keyboardPointerCell(params.cellX, params.cellHSpan, handle.horizontalEdge),
                keyboardPointerCell(params.cellY, params.cellVSpan, handle.verticalEdge),
                handle.horizontalEdge, handle.verticalEdge, this.minimumSpanX,
                this.minimumSpanY, this.cellLayout.getCountX(), this.cellLayout.getCountY());
        if (!this.cellLayout.isResizeCandidateVacant(resCandidate, this.widgetView)) {
            return;
        }
        if (resCandidate.matches(params)) {
            return;
        }
        finishCommitted(resCandidate);
    }

    private int keyboardPointerCell(int cell, int span, int edge) {
        if (edge == CellLayout.RESIZE_EDGE_START) {
            return cell - 1;
        }
        if (edge == CellLayout.RESIZE_EDGE_END) {
            return cell + span;
        }
        return cell;
    }

    private void finishCommitted(CellLayout.ResizeCandidate resCandidate) {
        if (this.finished) {
            return;
        }
        this.finished = true;
        this.callback.onWidgetResizeCommitted(resCandidate);
    }

    private void finishCancelled() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        this.callback.onWidgetResizeCancelled();
    }

    private void finishDragRequested() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        this.callback.onWidgetDragRequested();
    }

    private boolean isWidgetDragPastTouchSlop(MotionEvent event) {
        float distanceX = event.getX() - this.widgetDragDownX;
        float distanceY = event.getY() - this.widgetDragDownY;
        return (distanceX * distanceX) + (distanceY * distanceY)
                > this.touchSlopSquared;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.finished) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.trackWidgetDrag = false;
                updateCandidateBounds();
                if (this.activeHandle == null && this.candidateBounds.contains(
                        Math.round(event.getX()), Math.round(event.getY()))) {
                    this.trackWidgetDrag = true;
                    this.widgetDragDownX = event.getX();
                    this.widgetDragDownY = event.getY();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.trackWidgetDrag && isWidgetDragPastTouchSlop(event)) {
                    finishDragRequested();
                }
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishCancelled();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        finishCancelled();
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutHandles();
    }

    private void layoutHandles() {
        updateCandidateBounds();
        int count = getChildCount();
        for (int index = 0; index < count; index++) {
            ResizeHandleView handle = (ResizeHandleView) getChildAt(index);
            int centerX = handleCenterX(handle);
            int centerY = handleCenterY(handle);
            int halfSize = this.handleSize / 2;
            handle.layout(centerX - halfSize, centerY - halfSize,
                    centerX + halfSize, centerY + halfSize);
        }
    }

    private int handleCenterX(ResizeHandleView handle) {
        if (handle.horizontalEdge == CellLayout.RESIZE_EDGE_START) {
            return this.candidateBounds.left;
        }
        if (handle.horizontalEdge == CellLayout.RESIZE_EDGE_END) {
            return this.candidateBounds.right;
        }
        return this.candidateBounds.centerX();
    }

    private int handleCenterY(ResizeHandleView handle) {
        if (handle.verticalEdge == CellLayout.RESIZE_EDGE_START) {
            return this.candidateBounds.top;
        }
        if (handle.verticalEdge == CellLayout.RESIZE_EDGE_END) {
            return this.candidateBounds.bottom;
        }
        return this.candidateBounds.centerY();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updateCandidateBounds();
        int outlineColor = getResources().getColor(this.candidateValid
                ? R.color.m3_primary : R.color.m3_error);
        this.outlinePaint.setColor(outlineColor);
        canvas.drawRect(this.candidateBounds, this.outlinePaint);
        drawSpanLabel(canvas);
    }

    private void updateCandidateBounds() {
        CellLayout.ResizeCandidate resCandidate = visibleCandidate();
        this.cellLayout.cellToPoint(resCandidate.cellX, resCandidate.cellY, this.cellPoint);
        int[] size = this.cellLayout.spanToPixels(resCandidate.spanX, resCandidate.spanY);
        this.cellLayout.getLocationOnScreen(this.cellLocation);
        getLocationOnScreen(this.frameLocation);
        int left = this.cellLocation[0] - this.frameLocation[0] + this.cellPoint[0];
        int top = this.cellLocation[1] - this.frameLocation[1] + this.cellPoint[1];
        this.candidateBounds.set(left, top, left + size[0], top + size[1]);
    }

    private CellLayout.ResizeCandidate visibleCandidate() {
        if (this.candidate != null) {
            return this.candidate;
        }
        return this.originalCandidate;
    }

    private void drawSpanLabel(Canvas canvas) {
        CellLayout.ResizeCandidate resCandidate = visibleCandidate();
        int labelId = this.candidateValid
                ? R.string.widget_resize_span
                : R.string.widget_resize_invalid_span;
        String text = getResources().getString(labelId,
                resCandidate.spanX, resCandidate.spanY);
        float labelWidth = this.labelPaint.measureText(text) + dimensionToPixels(16);
        float labelHeight = dimensionToPixels(28);
        float centerX = this.candidateBounds.centerX();
        float top = Math.max(0, this.candidateBounds.top - labelHeight);
        float left = Math.max(0, centerX - (labelWidth / 2.0f));
        float right = Math.min(getWidth(), centerX + (labelWidth / 2.0f));
        this.labelBackgroundPaint.setColor(getResources().getColor(R.color.m3_surface));
        canvas.drawRect(left, top, right, top + labelHeight, this.labelBackgroundPaint);
        this.labelPaint.setColor(getResources().getColor(R.color.m3_on_surface));
        float baseline = top + ((labelHeight - (this.labelPaint.descent()
                + this.labelPaint.ascent())) / 2.0f);
        canvas.drawText(text, centerX, baseline, this.labelPaint);
    }

    private int dimensionToPixels(int dimension) {
        return Math.round(dimension * getResources().getDisplayMetrics().density);
    }

    private static final class ResizeHandleView extends View {
        final int horizontalEdge;
        final int verticalEdge;
        private final int radius;
        private final Paint paint = new Paint(1);

        ResizeHandleView(Context context, int horizontalEdge, int verticalEdge, int radius) {
            super(context);
            this.horizontalEdge = horizontalEdge;
            this.verticalEdge = verticalEdge;
            this.radius = radius;
            setWillNotDraw(false);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            this.paint.setColor(getResources().getColor(R.color.m3_primary));
            canvas.drawCircle(getWidth() / 2.0f, getHeight() / 2.0f,
                    this.radius, this.paint);
        }
    }
}
