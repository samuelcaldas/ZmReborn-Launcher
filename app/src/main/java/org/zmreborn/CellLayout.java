package org.zmreborn;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Grid-based container view organizing desktop shortcuts, folders, and widgets into discrete cells.
 */
public class CellLayout extends ViewGroup {
    static final int RESIZE_EDGE_NONE = 0;
    static final int RESIZE_EDGE_START = 1;
    static final int RESIZE_EDGE_END = 2;

    private int cellHeight;
    private final CellInfo cellInfo;
    private int cellWidth;
    private final int[] cellXY;
    private int columns;
    private boolean dirtyTag;
    private final RectF dragRect;
    private int heightGap;
    private boolean lastDownOnOccupiedCell;
    private int longAxisCells;
    private int longAxisEndPadding;
    private int longAxisStartPadding;
    private boolean[][] occupied;
    private boolean portrait;
    private final Rect rect;
    private int rows;
    private int shortAxisCells;
    private int shortAxisEndPadding;
    private int shortAxisStartPadding;
    private final WallpaperManager wallpaperManager;
    private int widthGap;

    /**
     * Constructs a cell layout with context.
     */
    public CellLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructs a cell layout with context and XML attributes.
     */
    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a cell layout with context, XML attributes, and default style.
     */
    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.rect = new Rect();
        this.cellInfo = new CellInfo();
        this.cellXY = new int[2];
        this.dragRect = new RectF();
        this.lastDownOnOccupiedCell = false;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        this.cellWidth = typedArray.getDimensionPixelSize(0, 10);
        this.cellHeight = typedArray.getDimensionPixelSize(1, 10);
        this.longAxisStartPadding = typedArray.getDimensionPixelSize(2, 10);
        this.longAxisEndPadding = typedArray.getDimensionPixelSize(3, 10);
        this.shortAxisStartPadding = typedArray.getDimensionPixelSize(4, 10);
        this.shortAxisEndPadding = typedArray.getDimensionPixelSize(5, 10);
        this.rows = PreferencesUtil.getContentGridRows(getContext());
        this.columns = PreferencesUtil.getContentGridColumns(getContext());
        typedArray.recycle();
        setAlwaysDrawnWithCacheEnabled(false);
        this.wallpaperManager = WallpaperManager.getInstance(getContext());
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    /**
     * Gets the horizontal cell count depending on screen orientation.
     */
    public int getCountX() {
        return this.portrait ? this.shortAxisCells : this.longAxisCells;
    }

    /**
     * Gets the vertical cell count depending on screen orientation.
     */
    public int getCountY() {
        return this.portrait ? this.longAxisCells : this.shortAxisCells;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        ((LayoutParams) params).regenerateId = true;
        super.addView(child, index, params);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getParent() instanceof ViewGroup) {
            this.cellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        CellInfo info = this.cellInfo;
        if (action == MotionEvent.ACTION_DOWN) {
            return handleInterceptDown(motionEvent, info);
        }
        if (action == MotionEvent.ACTION_UP) {
            resetCellInfo(info);
        }
        return false;
    }

    private boolean handleInterceptDown(MotionEvent motionEvent, CellInfo info) {
        Rect frame = this.rect;
        int x = ((int) motionEvent.getX()) + getScrollX();
        int y = ((int) motionEvent.getY()) + getScrollY();
        boolean found = findChildUnderPoint(x, y, frame, info);
        this.lastDownOnOccupiedCell = found;
        if (!found) {
            populateVacantDownCell(x, y, info);
        }
        setTag(info);
        return false;
    }

    private boolean findChildUnderPoint(int x, int y, Rect frame, CellInfo info) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE || child.getAnimation() != null) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                    info.cell = child;
                    info.cellX = layoutParams.cellX;
                    info.cellY = layoutParams.cellY;
                    info.spanX = layoutParams.cellHSpan;
                    info.spanY = layoutParams.cellVSpan;
                    info.valid = true;
                    this.dirtyTag = false;
                    return true;
                }
            }
        }
        return false;
    }

    private void populateVacantDownCell(int x, int y, CellInfo info) {
        int[] targetCellXY = this.cellXY;
        pointToCellExact(x, y, targetCellXY);
        int xCount = this.portrait ? this.shortAxisCells : this.longAxisCells;
        int yCount = this.portrait ? this.longAxisCells : this.shortAxisCells;
        boolean[][] occupiedGrid = this.occupied;
        findOccupiedCells(xCount, yCount, occupiedGrid, null);
        info.cell = null;
        info.cellX = targetCellXY[0];
        info.cellY = targetCellXY[1];
        info.spanX = 1;
        info.spanY = 1;
        info.valid = targetCellXY[0] >= 0 && targetCellXY[1] >= 0 && targetCellXY[0] < xCount
                && targetCellXY[1] < yCount && !occupiedGrid[targetCellXY[0]][targetCellXY[1]];
        this.dirtyTag = true;
    }

    private void resetCellInfo(CellInfo info) {
        info.cell = null;
        info.cellX = -1;
        info.cellY = -1;
        info.spanX = 0;
        info.spanY = 0;
        info.valid = false;
        this.dirtyTag = false;
        setTag(info);
    }

    @Override
    public CellInfo getTag() {
        CellInfo info = (CellInfo) super.getTag();
        if (info != null && this.dirtyTag && info.valid) {
            int xCount = this.portrait ? this.shortAxisCells : this.longAxisCells;
            int yCount = this.portrait ? this.longAxisCells : this.shortAxisCells;
            boolean[][] occupiedGrid = this.occupied;
            findOccupiedCells(xCount, yCount, occupiedGrid, null);
            findIntersectingVacantCells(info, info.cellX, info.cellY, xCount, yCount, occupiedGrid);
            this.dirtyTag = false;
        }
        return info;
    }

    private static void findIntersectingVacantCells(CellInfo cellInfo, int x, int y, int xCount, int yCount, boolean[][] occupied) {
        cellInfo.maxVacantSpanX = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanXSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanYSpanX = Integer.MIN_VALUE;
        cellInfo.clearVacantCells();
        try {
            if (!occupied[x][y]) {
                cellInfo.current.set(x, y, x, y);
                findVacantCell(cellInfo.current, xCount, yCount, occupied, cellInfo);
            }
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    private static void findVacantCell(Rect current, int xCount, int yCount, boolean[][] occupied, CellInfo cellInfo) {
        for (int l = 0; l < xCount; l++) {
            for (int r = l; r < xCount; r++) {
                for (int t = 0; t < yCount; t++) {
                    int b = t;
                    while (b < yCount && isRowEmpty(b, l, r, occupied)) {
                        current.left = l;
                        current.right = r;
                        current.top = t;
                        current.bottom = b;
                        addVacantCell(current, cellInfo);
                        b++;
                    }
                }
            }
        }
    }

    /**
     * Queries whether all cells in the rectangular span [x0..x1, y0..y1] are empty.
     */
    public static boolean isEmpty(int x0, int x1, int y0, int y1, boolean[][] occupied) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                if (occupied[x][y]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void addVacantCell(Rect current, CellInfo cellInfo) {
        CellInfo.VacantCell cell = CellInfo.VacantCell.acquire();
        cell.cellX = current.left;
        cell.cellY = current.top;
        cell.spanX = (current.right - current.left) + 1;
        cell.spanY = (current.bottom - current.top) + 1;
        if (cell.spanX > cellInfo.maxVacantSpanX) {
            cellInfo.maxVacantSpanX = cell.spanX;
            cellInfo.maxVacantSpanXSpanY = cell.spanY;
        }
        if (cell.spanY > cellInfo.maxVacantSpanY) {
            cellInfo.maxVacantSpanY = cell.spanY;
            cellInfo.maxVacantSpanYSpanX = cell.spanX;
        }
        cellInfo.vacantCells.add(cell);
    }

    private static boolean isRowEmpty(int y, int left, int right, boolean[][] occupied) {
        for (int x = left; x <= right; x++) {
            if (occupied[x][y]) {
                return false;
            }
        }
        return true;
    }

    CellInfo findAllVacantCells(boolean[] occupiedCells, View ignoreView) {
        int xCount = this.portrait ? this.shortAxisCells : this.longAxisCells;
        int yCount = this.portrait ? this.longAxisCells : this.shortAxisCells;
        boolean[][] occupiedGrid = this.occupied;
        if (occupiedCells != null) {
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    occupiedGrid[x][y] = occupiedCells[(y * xCount) + x];
                }
            }
        } else {
            findOccupiedCells(xCount, yCount, occupiedGrid, ignoreView);
        }
        return findAllVacantCellsFromOccupied(occupiedGrid, xCount, yCount);
    }

    /**
     * Resolves all vacant rectangular cells from the given occupied occupancy grid.
     */
    public CellInfo findAllVacantCellsFromOccupied(boolean[][] occupied, int xCount, int yCount) {
        CellInfo info = new CellInfo();
        info.cellX = -1;
        info.cellY = -1;
        info.spanY = 0;
        info.spanX = 0;
        info.maxVacantSpanX = Integer.MIN_VALUE;
        info.maxVacantSpanXSpanY = Integer.MIN_VALUE;
        info.maxVacantSpanY = Integer.MIN_VALUE;
        info.maxVacantSpanYSpanX = Integer.MIN_VALUE;
        info.screen = this.cellInfo.screen;
        findVacantCell(info.current, xCount, yCount, occupied, info);
        info.valid = !info.vacantCells.isEmpty();
        return info;
    }

    void pointToCellExact(int x, int y, int[] result) {
        int hStartPadding = this.portrait ? this.shortAxisStartPadding : this.longAxisStartPadding;
        int vStartPadding = this.portrait ? this.longAxisStartPadding : this.shortAxisStartPadding;
        result[0] = (x - hStartPadding) / (this.cellWidth + this.widthGap);
        result[1] = (y - vStartPadding) / (this.cellHeight + this.heightGap);
        int xAxis = this.portrait ? this.shortAxisCells : this.longAxisCells;
        int yAxis = this.portrait ? this.longAxisCells : this.shortAxisCells;
        result[0] = Math.max(0, Math.min(xAxis - 1, result[0]));
        result[1] = Math.max(0, Math.min(yAxis - 1, result[1]));
    }

    void cellToPoint(int cX, int cY, int[] result) {
        int hStartPadding = this.portrait ? this.shortAxisStartPadding : this.longAxisStartPadding;
        int vStartPadding = this.portrait ? this.longAxisStartPadding : this.shortAxisStartPadding;
        result[0] = ((this.cellWidth + this.widthGap) * cX) + hStartPadding;
        result[1] = ((this.cellHeight + this.heightGap) * cY) + vStartPadding;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tmpCellW;
        int tmpCellH;
        boolean autoFit = PreferencesUtil.isAutoFitContentGridItemsEnabled(getContext());
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == 0 || heightSpecMode == 0) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        this.portrait = heightSpecSize > widthSpecSize;
        if (this.portrait) {
            this.longAxisCells = this.rows;
            this.shortAxisCells = this.columns;
            tmpCellW = ((widthSpecSize - this.shortAxisStartPadding) - this.shortAxisEndPadding) / this.columns;
            tmpCellH = ((heightSpecSize - this.longAxisStartPadding) - this.longAxisEndPadding) / this.rows;
        } else {
            this.shortAxisCells = this.rows;
            this.longAxisCells = this.columns;
            tmpCellW = ((widthSpecSize - this.longAxisStartPadding) - this.longAxisEndPadding) / this.columns;
            tmpCellH = ((heightSpecSize - this.shortAxisStartPadding) - this.shortAxisEndPadding) / this.rows;
        }
        if (autoFit) {
            this.cellWidth = tmpCellW;
            this.cellHeight = tmpCellH;
        }
        if (this.occupied == null) {
            if (this.portrait) {
                this.occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, this.shortAxisCells, this.longAxisCells);
            } else {
                this.occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, this.longAxisCells, this.shortAxisCells);
            }
        }
        int sCells = this.shortAxisCells;
        int lCells = this.longAxisCells;
        int lStartPad = this.longAxisStartPadding;
        int lEndPad = this.longAxisEndPadding;
        int sStartPad = this.shortAxisStartPadding;
        int sEndPad = this.shortAxisEndPadding;
        int cWidth = this.cellWidth;
        int cHeight = this.cellHeight;
        this.portrait = heightSpecSize > widthSpecSize;
        int numShortGaps = sCells - 1;
        int numLongGaps = lCells - 1;
        if (this.portrait) {
            this.heightGap = (((heightSpecSize - lStartPad) - lEndPad) - (cHeight * lCells)) / numLongGaps;
            int hSpaceLeft = ((widthSpecSize - sStartPad) - sEndPad) - (cWidth * sCells);
            this.widthGap = numShortGaps > 0 ? hSpaceLeft / numShortGaps : 0;
        } else {
            this.widthGap = (((widthSpecSize - lStartPad) - lEndPad) - (cWidth * lCells)) / numLongGaps;
            int vSpaceLeft = ((heightSpecSize - sStartPad) - sEndPad) - (cHeight * sCells);
            this.heightGap = numShortGaps > 0 ? vSpaceLeft / numShortGaps : 0;
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            if (this.portrait) {
                layoutParams.setup(cWidth, cHeight, this.widthGap, this.heightGap, sStartPad, lStartPad, autoFit);
            } else {
                layoutParams.setup(cWidth, cHeight, this.widthGap, this.heightGap, lStartPad, sStartPad, autoFit);
            }
            if (layoutParams.regenerateId) {
                child.setId(((getId() & 255) << 16) | ((layoutParams.cellX & 255) << 8) | (layoutParams.cellY & 255));
                layoutParams.regenerateId = false;
            }
            child.measure(View.MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                int childLeft = layoutParams.x;
                int childTop = layoutParams.y;
                child.layout(childLeft, childTop, layoutParams.width + childLeft, layoutParams.height + childTop);
                if (layoutParams.dropped) {
                    layoutParams.dropped = false;
                    int[] targetCellXY = this.cellXY;
                    getLocationOnScreen(targetCellXY);
                    if (this.wallpaperManager != null) {
                        this.wallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop",
                                targetCellXY[0] + childLeft + (layoutParams.width / 2),
                                targetCellXY[1] + childTop + (layoutParams.height / 2), 0, null);
                    }
                }
            }
        }
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            view.buildDrawingCache(true);
        }
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

    int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY, CellInfo vacantCells, int[] recycle) {
        int[] bestXY = recycle != null ? recycle : new int[2];
        int[] targetCellXY = this.cellXY;
        double bestDistance = Double.MAX_VALUE;
        if (vacantCells == null || !vacantCells.valid) {
            return null;
        }
        int size = vacantCells.vacantCells.size();
        for (int i = 0; i < size; i++) {
            CellInfo.VacantCell cell = vacantCells.vacantCells.get(i);
            if (cell.spanX == spanX && cell.spanY == spanY) {
                cellToPoint(cell.cellX, cell.cellY, targetCellXY);
                double distance = Math.sqrt(Math.pow((double) (targetCellXY[0] - pixelX), 2.0d)
                        + Math.pow((double) (targetCellXY[1] - pixelY), 2.0d));
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    bestXY[0] = cell.cellX;
                    bestXY[1] = cell.cellY;
                }
            }
        }
        if (bestDistance >= Double.MAX_VALUE) {
            return null;
        }
        return bestXY;
    }

    void onDropChild(View child, int[] targetXY) {
        if (child != null) {
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            layoutParams.cellX = targetXY[0];
            layoutParams.cellY = targetXY[1];
            layoutParams.isDragging = false;
            layoutParams.dropped = true;
            this.dragRect.setEmpty();
            child.requestLayout();
            invalidate();
        }
    }

    void onDropAborted(View child) {
        if (child != null) {
            ((LayoutParams) child.getLayoutParams()).isDragging = false;
            invalidate();
        }
        this.dragRect.setEmpty();
    }

    void onDragChild(View child) {
        ((LayoutParams) child.getLayoutParams()).isDragging = true;
        this.dragRect.setEmpty();
    }

    /**
     * Converts a rectangular pixel bounding size to clamped grid cell spans.
     */
    public int[] rectToCell(int width, int height) {
        return calculateClampedSpans(width, height, this.cellWidth, this.cellHeight,
                this.widthGap, this.heightGap, getAvailableColumns(), getAvailableRows());
    }

    /**
     * Resolves cell spans for a widget's dp-denominated minimum size, converting to pixels
     * against this layout's density before span calculation.
     */
    public int[] rectToCellFromDp(int widthDp, int heightDp) {
        float density = getResources().getDisplayMetrics().density;
        return calculateClampedSpansFromDp(widthDp, heightDp, density, this.cellWidth,
                this.cellHeight, this.widthGap, this.heightGap,
                getAvailableColumns(), getAvailableRows());
    }

    static int[] calculateClampedSpansFromDp(int widthDp, int heightDp, float density,
            int cellWidth, int cellHeight, int widthGap, int heightGap,
            int availableColumns, int availableRows) {
        int widthPx = Math.round(widthDp * density);
        int heightPx = Math.round(heightDp * density);
        return calculateClampedSpans(widthPx, heightPx, cellWidth, cellHeight,
                widthGap, heightGap, availableColumns, availableRows);
    }

    int[] spanToPixels(int spanX, int spanY) {
        return new int[]{calculateSpanPixels(spanX, this.cellWidth, this.widthGap),
                calculateSpanPixels(spanY, this.cellHeight, this.heightGap)};
    }

    boolean isWidgetSizingGeometryReady() {
        return isLaidOut() && !isLayoutRequested()
                && getMeasuredWidth() > 0 && getMeasuredHeight() > 0
                && getWidth() > 0 && getHeight() > 0
                && getCountX() > 0 && getCountY() > 0
                && this.cellWidth > 0 && this.cellHeight > 0
                && ((long) this.cellWidth + this.widthGap) > 0
                && ((long) this.cellHeight + this.heightGap) > 0;
    }

    static int[] calculateClampedSpans(int width, int height, int cellWidth, int cellHeight,
            int widthGap, int heightGap, int availableColumns, int availableRows) {
        int spanX = calculateSpan(width, cellWidth, widthGap);
        int spanY = calculateSpan(height, cellHeight, heightGap);
        return new int[]{clampSpan(spanX, availableColumns), clampSpan(spanY, availableRows)};
    }

    static int calculateSpan(int requestedSize, int cellSize, int gap) {
        if (cellSize <= 0) {
            throw new IllegalArgumentException("cellSize must be positive");
        }
        long stride = (long) cellSize + gap;
        if (stride <= 0) {
            throw new IllegalArgumentException("cellSize + gap must be positive");
        }
        long required = Math.max(0L, requestedSize) + gap;
        long span = required <= 0 ? 1L
                : (required / stride) + (required % stride == 0 ? 0L : 1L);
        return (int) Math.min(Integer.MAX_VALUE, span);
    }

    static int clampSpan(int calculatedSpan, int availableCells) {
        return Math.min(Math.max(1, calculatedSpan), Math.max(1, availableCells));
    }

    static int calculateSpanPixels(int span, int cellSize, int gap) {
        if (cellSize <= 0) {
            throw new IllegalArgumentException("cellSize must be positive");
        }
        long stride = (long) cellSize + gap;
        if (stride <= 0) {
            throw new IllegalArgumentException("cellSize + gap must be positive");
        }
        long safeSpan = Math.max(1L, span);
        long pixels = cellSize + ((safeSpan - 1L) * stride);
        return (int) Math.min(Integer.MAX_VALUE, pixels);
    }

    ResizeCandidate findResizeCandidate(LayoutParams layoutParams,
            int pointerX, int pointerY, int horizontalEdge, int verticalEdge,
            int minimumSpanX, int minimumSpanY) {
        if (layoutParams == null || !isWidgetSizingGeometryReady()) {
            return null;
        }
        int[] cell = new int[2];
        pointToCellExact(pointerX, pointerY, cell);
        return calculateResizeCandidate(layoutParams.cellX, layoutParams.cellY,
                layoutParams.cellHSpan, layoutParams.cellVSpan, cell[0], cell[1],
                horizontalEdge, verticalEdge, minimumSpanX, minimumSpanY,
                getCountX(), getCountY());
    }

    boolean isResizeCandidateVacant(ResizeCandidate candidate, View ignoredView) {
        if (candidate == null || ignoredView == null) {
            return false;
        }
        int xCount = getCountX();
        int yCount = getCountY();
        if (!candidate.isWithinBounds(xCount, yCount)) {
            return false;
        }
        findOccupiedCells(xCount, yCount, this.occupied, ignoredView);
        return isEmpty(candidate.cellX, candidate.lastCellX(),
                candidate.cellY, candidate.lastCellY(), this.occupied);
    }

    boolean applyResizeCandidate(View child, ResizeCandidate candidate) {
        if (child == null || !isResizeCandidateVacant(candidate, child)) {
            return false;
        }
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (!(params instanceof LayoutParams)) {
            return false;
        }
        LayoutParams layoutParams = (LayoutParams) params;
        if (candidate.matches(layoutParams)) {
            return false;
        }
        layoutParams.cellX = candidate.cellX;
        layoutParams.cellY = candidate.cellY;
        layoutParams.cellHSpan = candidate.spanX;
        layoutParams.cellVSpan = candidate.spanY;
        layoutParams.regenerateId = true;
        child.requestLayout();
        requestLayout();
        invalidate();
        return true;
    }

    static ResizeCandidate calculateResizeCandidate(int cellX, int cellY,
            int spanX, int spanY, int pointerCellX, int pointerCellY,
            int horizontalEdge, int verticalEdge, int minimumSpanX,
            int minimumSpanY, int columnCount, int rowCount) {
        validateResizeEdge(horizontalEdge);
        validateResizeEdge(verticalEdge);
        validateGridDimensions(columnCount, rowCount);
        if (!isWithinBounds(cellX, cellY, spanX, spanY, columnCount, rowCount)) {
            return null;
        }
        int safeMinimumSpanX = clampSpan(minimumSpanX, columnCount);
        int safeMinimumSpanY = clampSpan(minimumSpanY, rowCount);
        int lastCellX = calculateResizeLastCell(cellX, spanX, pointerCellX,
                horizontalEdge, safeMinimumSpanX, columnCount);
        int lastCellY = calculateResizeLastCell(cellY, spanY, pointerCellY,
                verticalEdge, safeMinimumSpanY, rowCount);
        int resizedCellX = calculateResizeCellStart(cellX, spanX, pointerCellX,
                horizontalEdge, safeMinimumSpanX);
        int resizedCellY = calculateResizeCellStart(cellY, spanY, pointerCellY,
                verticalEdge, safeMinimumSpanY);
        int resizedSpanX = (lastCellX - resizedCellX) + 1;
        int resizedSpanY = (lastCellY - resizedCellY) + 1;
        if (resizedSpanX < safeMinimumSpanX || resizedSpanY < safeMinimumSpanY) {
            return null;
        }
        return new ResizeCandidate(resizedCellX, resizedCellY,
                resizedSpanX, resizedSpanY);
    }

    private static void validateResizeEdge(int edge) {
        if (edge < RESIZE_EDGE_NONE || edge > RESIZE_EDGE_END) {
            throw new IllegalArgumentException("Unknown resize edge");
        }
    }

    private static void validateGridDimensions(int columnCount, int rowCount) {
        if (columnCount <= 0 || rowCount <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive");
        }
    }

    private static int calculateResizeLastCell(int cell, int span, int pointerCell,
            int edge, int minimumSpan, int availableCells) {
        if (edge != RESIZE_EDGE_END) {
            return cell + span - 1;
        }
        return clamp(pointerCell, cell + minimumSpan - 1, availableCells - 1);
    }

    private static int calculateResizeCellStart(int cell, int span, int pointerCell,
            int edge, int minimumSpan) {
        if (edge != RESIZE_EDGE_START) {
            return cell;
        }
        int lastCell = cell + span - 1;
        return clamp(pointerCell, 0, lastCell - minimumSpan + 1);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static boolean isWithinBounds(int cellX, int cellY, int spanX,
            int spanY, int columnCount, int rowCount) {
        return cellX >= 0 && cellY >= 0 && spanX > 0 && spanY > 0
                && cellX + spanX <= columnCount && cellY + spanY <= rowCount;
    }

    private int getAvailableColumns() {
        int col = getCountX();
        return Math.max(1, col > 0 ? col : this.columns);
    }

    private int getAvailableRows() {
        int row = getCountY();
        return Math.max(1, row > 0 ? row : this.rows);
    }

    /**
     * Finds the first available vacant cell spanning spanX by spanY.
     */
    public boolean getVacantCell(int[] vacant, int spanX, int spanY) {
        int xCount = this.portrait ? this.shortAxisCells : this.longAxisCells;
        int yCount = this.portrait ? this.longAxisCells : this.shortAxisCells;
        boolean[][] occupiedGrid = this.occupied;
        findOccupiedCells(xCount, yCount, occupiedGrid, null);
        return findVacantCell(vacant, spanX, spanY, xCount, yCount, occupiedGrid);
    }

    static boolean findVacantCell(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied) {
        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                if (!occupied[x][y] && isSpanAvailable(x, y, spanX, spanY, xCount, yCount, occupied)) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSpanAvailable(int startX, int startY, int spanX, int spanY,
            int xCount, int yCount, boolean[][] occupied) {
        if (startX + spanX > xCount || startY + spanY > yCount) {
            return false;
        }
        for (int i = startX; i < startX + spanX; i++) {
            for (int j = startY; j < startY + spanY; j++) {
                if (occupied[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean[] getOccupiedCells() {
        int xCount = this.portrait ? this.shortAxisCells : this.longAxisCells;
        int yCount = this.portrait ? this.longAxisCells : this.shortAxisCells;
        boolean[][] occupiedGrid = this.occupied;
        findOccupiedCells(xCount, yCount, occupiedGrid, null);
        boolean[] flat = new boolean[xCount * yCount];
        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                flat[(y * xCount) + x] = occupiedGrid[x][y];
            }
        }
        return flat;
    }

    private void findOccupiedCells(int xCount, int yCount, boolean[][] occupiedGrid, View ignoreView) {
        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                occupiedGrid[x][y] = false;
            }
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (!(child instanceof Folder) && !child.equals(ignoreView)) {
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                int x2 = layoutParams.cellX;
                while (x2 < layoutParams.cellX + layoutParams.cellHSpan && x2 < xCount) {
                    int y2 = layoutParams.cellY;
                    while (y2 < layoutParams.cellY + layoutParams.cellVSpan && y2 < yCount) {
                        if (x2 != -1 && y2 != -1) {
                            occupiedGrid[x2][y2] = true;
                        }
                        y2++;
                    }
                    x2++;
                }
            }
        }
    }

    /**
     * Returns true if the last touch down event occurred on an occupied cell.
     */
    public boolean lastDownOnOccupiedCell() {
        return this.lastDownOnOccupiedCell;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    /**
     * Per-child layout parameters specifying cell coordinate positions and spans within a CellLayout.
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        @ViewDebug.ExportedProperty
        public int cellHSpan;
        @ViewDebug.ExportedProperty
        public int cellVSpan;
        @ViewDebug.ExportedProperty
        public int cellX;
        @ViewDebug.ExportedProperty
        public int cellY;
        boolean dropped;
        public boolean isDragging;
        boolean regenerateId;

        @ViewDebug.ExportedProperty
        int x;
        @ViewDebug.ExportedProperty
        int y;

        /**
         * Constructs layout params from XML attributes.
         */
        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        /**
         * Constructs layout params from another ViewGroup.LayoutParams instance.
         */
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        /**
         * Constructs layout params with explicit cell coordinates and spans.
         */
        public LayoutParams(int cellX2, int cellY2, int cellHSpan2, int cellVSpan2) {
            super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            this.cellX = cellX2;
            this.cellY = cellY2;
            this.cellHSpan = cellHSpan2;
            this.cellVSpan = cellVSpan2;
        }

        /**
         * Computes pixel dimensions and margins from the parent cell layout metrics.
         */
        public void setup(int cWidth, int cHeight, int wGap, int hGap, int hStartPadding, int vStartPadding, boolean autoStretch) {
            int myCellHSpan = this.cellHSpan;
            int myCellVSpan = this.cellVSpan;
            int myCellX = this.cellX;
            int myCellY = this.cellY;
            this.width = (((myCellHSpan * cWidth) + ((myCellHSpan - 1) * wGap)) - this.leftMargin) - this.rightMargin;
            this.height = (((myCellVSpan * cHeight) + ((myCellVSpan - 1) * hGap)) - this.topMargin) - this.bottomMargin;
            if (autoStretch) {
                this.width = (calculateSpanPixels(myCellHSpan, cWidth, wGap) - this.rightMargin) - this.leftMargin;
                this.height = (calculateSpanPixels(myCellVSpan, cHeight, hGap) - this.bottomMargin) - this.topMargin;
            }
            this.x = ((cWidth + wGap) * myCellX) + hStartPadding + this.leftMargin;
            this.y = ((cHeight + hGap) * myCellY) + vStartPadding + this.topMargin;
        }
    }

    static final class ResizeCandidate {
        final int cellX;
        final int cellY;
        final int spanX;
        final int spanY;

        ResizeCandidate(int cellX, int cellY, int spanX, int spanY) {
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        int lastCellX() {
            return this.cellX + this.spanX - 1;
        }

        int lastCellY() {
            return this.cellY + this.spanY - 1;
        }

        boolean matches(LayoutParams layoutParams) {
            return this.cellX == layoutParams.cellX
                    && this.cellY == layoutParams.cellY
                    && this.spanX == layoutParams.cellHSpan
                    && this.spanY == layoutParams.cellVSpan;
        }

        boolean isWithinBounds(int columnCount, int rowCount) {
            return CellLayout.isWithinBounds(this.cellX, this.cellY,
                    this.spanX, this.spanY, columnCount, rowCount);
        }
    }

    static final class CellInfo implements ContextMenu.ContextMenuInfo {
        View cell;
        int cellX;
        int cellY;
        final Rect current = new Rect();
        int maxVacantSpanX;
        int maxVacantSpanXSpanY;
        int maxVacantSpanY;
        int maxVacantSpanYSpanX;
        int screen;
        int spanX;
        int spanY;
        final ArrayList<VacantCell> vacantCells = new ArrayList<>(100);
        boolean valid;

        CellInfo() {
        }

        static final class VacantCell {
            private static final int POOL_LIMIT = 100;
            private static int sAcquiredCount = 0;
            private static final Object sLock = new Object();
            private static VacantCell sRoot;
            int cellX;
            int cellY;
            private VacantCell next;
            int spanX;
            int spanY;

            VacantCell() {
            }

            static VacantCell acquire() {
                VacantCell vacantCell;
                synchronized (sLock) {
                    if (sRoot == null) {
                        vacantCell = new VacantCell();
                    } else {
                        vacantCell = sRoot;
                        sRoot = vacantCell.next;
                        sAcquiredCount--;
                    }
                }
                return vacantCell;
            }

            void release() {
                synchronized (sLock) {
                    if (sAcquiredCount < POOL_LIMIT) {
                        sAcquiredCount++;
                        this.next = sRoot;
                        sRoot = this;
                    }
                }
            }

            @Override
            public String toString() {
                return "VacantCell[x=" + this.cellX + ", y=" + this.cellY + ", spanX=" + this.spanX + ", spanY=" + this.spanY + "]";
            }
        }

        void clearVacantCells() {
            ArrayList<VacantCell> list = this.vacantCells;
            int count = list.size();
            for (int i = 0; i < count; i++) {
                list.get(i).release();
            }
            list.clear();
        }

        public void findVacantCellsFromOccupied(boolean[] occupiedGrid, int xCount, int yCount) {
            if (this.cellX < 0 || this.cellY < 0) {
                this.maxVacantSpanXSpanY = Integer.MIN_VALUE;
                this.maxVacantSpanX = Integer.MIN_VALUE;
                this.maxVacantSpanYSpanX = Integer.MIN_VALUE;
                this.maxVacantSpanY = Integer.MIN_VALUE;
                clearVacantCells();
                return;
            }
            boolean[][] unflattened = (boolean[][]) Array.newInstance(Boolean.TYPE, xCount, yCount);
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    unflattened[x][y] = occupiedGrid[(y * xCount) + x];
                }
            }
            CellLayout.findIntersectingVacantCells(this, this.cellX, this.cellY, xCount, yCount, unflattened);
        }

        boolean findCellForSpan(int[] targetCellXY, int spanX2, int spanY2) {
            return findCellForSpan(targetCellXY, spanX2, spanY2, true);
        }

        boolean findCellForSpan(int[] targetCellXY, int spanX2, int spanY2, boolean clear) {
            ArrayList<VacantCell> list = this.vacantCells;
            int count = list.size();
            boolean found = false;
            if (this.spanX >= spanX2 && this.spanY >= spanY2) {
                targetCellXY[0] = this.cellX;
                targetCellXY[1] = this.cellY;
                found = true;
            }
            for (int i = 0; i < count; i++) {
                VacantCell cell2 = list.get(i);
                if (cell2.spanX == spanX2 && cell2.spanY == spanY2) {
                    targetCellXY[0] = cell2.cellX;
                    targetCellXY[1] = cell2.cellY;
                    found = true;
                    break;
                }
            }
            for (int i2 = 0; i2 < count; i2++) {
                VacantCell cell3 = list.get(i2);
                if (cell3.spanX >= spanX2 && cell3.spanY >= spanY2) {
                    targetCellXY[0] = cell3.cellX;
                    targetCellXY[1] = cell3.cellY;
                    found = true;
                    break;
                }
            }
            if (clear) {
                clearVacantCells();
            }
            return found;
        }

        @Override
        public String toString() {
            return "Cell[view=" + (this.cell == null ? "null" : this.cell.getClass()) + ", x=" + this.cellX + ", y=" + this.cellY + "]";
        }
    }
}
