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

    private final CellLayout mCellLayout;
    private final View mWidgetView;
    private final Callback mCallback;
    private final boolean mHorizontalResizeEnabled;
    private final boolean mVerticalResizeEnabled;
    private final int mMinimumSpanX;
    private final int mMinimumSpanY;
    private final int mHandleSize;
    private final int mTouchSlopSquared;
    private final Rect mCandidateBounds = new Rect();
    private final int[] mCellLocation = new int[2];
    private final int[] mFrameLocation = new int[2];
    private final int[] mCellPoint = new int[2];
    private final Paint mOutlinePaint = new Paint(1);
    private final Paint mLabelBackgroundPaint = new Paint(1);
    private final Paint mLabelPaint = new Paint(1);

    private ResizeHandleView mActiveHandle;
    private CellLayout.ResizeCandidate mCandidate;
    private final CellLayout.ResizeCandidate mOriginalCandidate;
    private boolean mCandidateValid;
    private boolean mTrackWidgetDrag;
    private float mWidgetDragDownX;
    private float mWidgetDragDownY;
    private boolean mFinished;

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
        this.mCellLayout = cellLayout;
        this.mWidgetView = widgetView;
        this.mCallback = callback;
        this.mHorizontalResizeEnabled = supportsHorizontalResize(providerInfo);
        this.mVerticalResizeEnabled = supportsVerticalResize(providerInfo);
        CellLayout.LayoutParams params = (CellLayout.LayoutParams) widgetView.getLayoutParams();
        int[] minimumSpans = calculateMinimumSpans(providerInfo);
        this.mMinimumSpanX = this.mHorizontalResizeEnabled
                ? minimumSpans[0] : params.cellHSpan;
        this.mMinimumSpanY = this.mVerticalResizeEnabled
                ? minimumSpans[1] : params.cellVSpan;
        this.mOriginalCandidate = new CellLayout.ResizeCandidate(params.cellX, params.cellY,
                params.cellHSpan, params.cellVSpan);
        this.mCandidate = this.mOriginalCandidate;
        this.mCandidateValid = true;
        this.mHandleSize = dimensionToPixels(HANDLE_SIZE_DP);
        int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mTouchSlopSquared = touchSlop * touchSlop;
        configureDrawing();
        addSupportedHandles();
    }

    boolean supportsResize() {
        return this.mHorizontalResizeEnabled || this.mVerticalResizeEnabled;
    }

    private void configureDrawing() {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        this.mOutlinePaint.setStyle(Paint.Style.STROKE);
        this.mOutlinePaint.setStrokeWidth(dimensionToPixels(OUTLINE_WIDTH_DP));
        this.mLabelBackgroundPaint.setStyle(Paint.Style.FILL);
        this.mLabelPaint.setTextAlign(Paint.Align.CENTER);
        this.mLabelPaint.setTextSize(dimensionToPixels(14));
    }

    private int[] calculateMinimumSpans(AppWidgetProviderInfo providerInfo) {
        int width = preferredResizeDimension(providerInfo.minResizeWidth,
                providerInfo.minWidth);
        int height = preferredResizeDimension(providerInfo.minResizeHeight,
                providerInfo.minHeight);
        return this.mCellLayout.rectToCell(width, height);
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
        if (this.mHorizontalResizeEnabled && this.mVerticalResizeEnabled) {
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
        if (this.mHorizontalResizeEnabled) {
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
            public boolean onTouch(View view, MotionEvent event) {
                boolean handled = onHandleTouch(handle, event);
                if (handled && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return handled;
            }
        });
        handle.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                resizeOneCell(handle);
            }
        });
        addView(handle, new FrameLayout.LayoutParams(this.mHandleSize, this.mHandleSize));
    }

    private boolean onHandleTouch(ResizeHandleView handle, MotionEvent event) {
        if (this.mFinished) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.mActiveHandle = handle;
                updatePreview(handle, event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.mActiveHandle == handle) {
                    updatePreview(handle, event);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (this.mActiveHandle == handle) {
                    updatePreview(handle, event);
                    finishResize();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (this.mActiveHandle == handle) {
                    finishCancelled();
                }
                return true;
            default:
                return true;
        }
    }

    private void updatePreview(ResizeHandleView handle, MotionEvent event) {
        updateCellPoint(handle, event);
        CellLayout.LayoutParams params = (CellLayout.LayoutParams) this.mWidgetView.getLayoutParams();
        this.mCandidate = this.mCellLayout.findResizeCandidate(params,
                this.mCellPoint[0], this.mCellPoint[1], handle.horizontalEdge,
                handle.verticalEdge, this.mMinimumSpanX, this.mMinimumSpanY);
        this.mCandidateValid = this.mCellLayout.isResizeCandidateVacant(this.mCandidate,
                this.mWidgetView);
        requestLayout();
        invalidate();
    }

    private void updateCellPoint(ResizeHandleView handle, MotionEvent event) {
        this.mCellLayout.getLocationOnScreen(this.mCellLocation);
        this.mCellPoint[0] = Math.round(event.getRawX() - this.mCellLocation[0]);
        this.mCellPoint[1] = Math.round(event.getRawY() - this.mCellLocation[1]);
        if (handle.horizontalEdge == CellLayout.RESIZE_EDGE_END) {
            this.mCellPoint[0]--;
        }
        if (handle.verticalEdge == CellLayout.RESIZE_EDGE_END) {
            this.mCellPoint[1]--;
        }
    }

    private void finishResize() {
        if (this.mCandidateValid && !this.mCandidate.matches(
                (CellLayout.LayoutParams) this.mWidgetView.getLayoutParams())) {
            finishCommitted(this.mCandidate);
            return;
        }
        finishCancelled();
    }

    private void resizeOneCell(ResizeHandleView handle) {
        if (this.mFinished) {
            return;
        }
        CellLayout.LayoutParams params = (CellLayout.LayoutParams) this.mWidgetView.getLayoutParams();
        CellLayout.ResizeCandidate candidate = CellLayout.calculateResizeCandidate(
                params.cellX, params.cellY, params.cellHSpan, params.cellVSpan,
                keyboardPointerCell(params.cellX, params.cellHSpan, handle.horizontalEdge),
                keyboardPointerCell(params.cellY, params.cellVSpan, handle.verticalEdge),
                handle.horizontalEdge, handle.verticalEdge, this.mMinimumSpanX,
                this.mMinimumSpanY, this.mCellLayout.getCountX(), this.mCellLayout.getCountY());
        if (!this.mCellLayout.isResizeCandidateVacant(candidate, this.mWidgetView)) {
            return;
        }
        if (candidate.matches(params)) {
            return;
        }
        finishCommitted(candidate);
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

    private void finishCommitted(CellLayout.ResizeCandidate candidate) {
        if (this.mFinished) {
            return;
        }
        this.mFinished = true;
        this.mCallback.onWidgetResizeCommitted(candidate);
    }

    private void finishCancelled() {
        if (this.mFinished) {
            return;
        }
        this.mFinished = true;
        this.mCallback.onWidgetResizeCancelled();
    }

    private void finishDragRequested() {
        if (this.mFinished) {
            return;
        }
        this.mFinished = true;
        this.mCallback.onWidgetDragRequested();
    }

    private boolean isWidgetDragPastTouchSlop(MotionEvent event) {
        float distanceX = event.getX() - this.mWidgetDragDownX;
        float distanceY = event.getY() - this.mWidgetDragDownY;
        return (distanceX * distanceX) + (distanceY * distanceY)
                > this.mTouchSlopSquared;
    }

    /** Handles body drags and dismissal taps outside resize handles. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mFinished) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.mTrackWidgetDrag = false;
                updateCandidateBounds();
                if (this.mActiveHandle == null && this.mCandidateBounds.contains(
                        Math.round(event.getX()), Math.round(event.getY()))) {
                    this.mTrackWidgetDrag = true;
                    this.mWidgetDragDownX = event.getX();
                    this.mWidgetDragDownY = event.getY();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.mTrackWidgetDrag && isWidgetDragPastTouchSlop(event)) {
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
            int halfSize = this.mHandleSize / 2;
            handle.layout(centerX - halfSize, centerY - halfSize,
                    centerX + halfSize, centerY + halfSize);
        }
    }

    private int handleCenterX(ResizeHandleView handle) {
        if (handle.horizontalEdge == CellLayout.RESIZE_EDGE_START) {
            return this.mCandidateBounds.left;
        }
        if (handle.horizontalEdge == CellLayout.RESIZE_EDGE_END) {
            return this.mCandidateBounds.right;
        }
        return this.mCandidateBounds.centerX();
    }

    private int handleCenterY(ResizeHandleView handle) {
        if (handle.verticalEdge == CellLayout.RESIZE_EDGE_START) {
            return this.mCandidateBounds.top;
        }
        if (handle.verticalEdge == CellLayout.RESIZE_EDGE_END) {
            return this.mCandidateBounds.bottom;
        }
        return this.mCandidateBounds.centerY();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updateCandidateBounds();
        int outlineColor = getResources().getColor(this.mCandidateValid
                ? R.color.m3_primary : R.color.m3_error);
        this.mOutlinePaint.setColor(outlineColor);
        canvas.drawRect(this.mCandidateBounds, this.mOutlinePaint);
        drawSpanLabel(canvas);
    }

    private void updateCandidateBounds() {
        CellLayout.ResizeCandidate candidate = visibleCandidate();
        this.mCellLayout.cellToPoint(candidate.cellX, candidate.cellY, this.mCellPoint);
        int[] size = this.mCellLayout.spanToPixels(candidate.spanX, candidate.spanY);
        this.mCellLayout.getLocationOnScreen(this.mCellLocation);
        getLocationOnScreen(this.mFrameLocation);
        int left = this.mCellLocation[0] - this.mFrameLocation[0] + this.mCellPoint[0];
        int top = this.mCellLocation[1] - this.mFrameLocation[1] + this.mCellPoint[1];
        this.mCandidateBounds.set(left, top, left + size[0], top + size[1]);
    }

    private CellLayout.ResizeCandidate visibleCandidate() {
        if (this.mCandidate != null) {
            return this.mCandidate;
        }
        return this.mOriginalCandidate;
    }

    private void drawSpanLabel(Canvas canvas) {
        CellLayout.ResizeCandidate candidate = visibleCandidate();
        int labelId = this.mCandidateValid
                ? R.string.widget_resize_span
                : R.string.widget_resize_invalid_span;
        String text = getResources().getString(labelId,
                candidate.spanX, candidate.spanY);
        float labelWidth = this.mLabelPaint.measureText(text) + dimensionToPixels(16);
        float labelHeight = dimensionToPixels(28);
        float centerX = this.mCandidateBounds.centerX();
        float top = Math.max(0, this.mCandidateBounds.top - labelHeight);
        float left = Math.max(0, centerX - (labelWidth / 2.0f));
        float right = Math.min(getWidth(), centerX + (labelWidth / 2.0f));
        this.mLabelBackgroundPaint.setColor(getResources().getColor(R.color.m3_surface));
        canvas.drawRect(left, top, right, top + labelHeight, this.mLabelBackgroundPaint);
        this.mLabelPaint.setColor(getResources().getColor(R.color.m3_on_surface));
        float baseline = top + ((labelHeight - (this.mLabelPaint.descent()
                + this.mLabelPaint.ascent())) / 2.0f);
        canvas.drawText(text, centerX, baseline, this.mLabelPaint);
    }

    private int dimensionToPixels(int dimension) {
        return Math.round(dimension * getResources().getDisplayMetrics().density);
    }

    private static final class ResizeHandleView extends View {
        final int horizontalEdge;
        final int verticalEdge;
        private final int mRadius;
        private final Paint mPaint = new Paint(1);

        ResizeHandleView(Context context, int horizontalEdge, int verticalEdge, int radius) {
            super(context);
            this.horizontalEdge = horizontalEdge;
            this.verticalEdge = verticalEdge;
            this.mRadius = radius;
            setWillNotDraw(false);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            this.mPaint.setColor(getResources().getColor(R.color.m3_primary));
            canvas.drawCircle(getWidth() / 2.0f, getHeight() / 2.0f,
                    this.mRadius, this.mPaint);
        }
    }
}
