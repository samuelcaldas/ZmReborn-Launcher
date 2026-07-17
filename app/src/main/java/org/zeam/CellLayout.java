package org.zeam;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
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

public class CellLayout extends ViewGroup {
    private int mCellHeight;
    private final CellInfo mCellInfo;
    private int mCellWidth;
    int[] mCellXY;
    private int mColumns;
    private boolean mDirtyTag;
    private RectF mDragRect;
    private int mHeightGap;
    private boolean mLastDownOnOccupiedCell;
    private int mLongAxisCells;
    private int mLongAxisEndPadding;
    private int mLongAxisStartPadding;
    boolean[][] mOccupied;
    private boolean mPortrait;
    private final Rect mRect;
    private int mRows;
    private int mShortAxisCells;
    private int mShortAxisEndPadding;
    private int mShortAxisStartPadding;
    private final WallpaperManager mWallpaperManager;
    private int mWidthGap;

    public CellLayout(Context context) {
        this(context, (AttributeSet) null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRect = new Rect();
        this.mCellInfo = new CellInfo();
        this.mCellXY = new int[2];
        this.mDragRect = new RectF();
        this.mLastDownOnOccupiedCell = false;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        this.mCellWidth = typedArray.getDimensionPixelSize(0, 10);
        this.mCellHeight = typedArray.getDimensionPixelSize(1, 10);
        this.mLongAxisStartPadding = typedArray.getDimensionPixelSize(2, 10);
        this.mLongAxisEndPadding = typedArray.getDimensionPixelSize(3, 10);
        this.mShortAxisStartPadding = typedArray.getDimensionPixelSize(4, 10);
        this.mShortAxisEndPadding = typedArray.getDimensionPixelSize(5, 10);
        this.mRows = PreferencesUtil.getContentGridRows(getContext());
        this.mColumns = PreferencesUtil.getContentGridColumns(getContext());
        typedArray.recycle();
        setAlwaysDrawnWithCacheEnabled(false);
        this.mWallpaperManager = WallpaperManager.getInstance(getContext());
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    /* access modifiers changed from: package-private */
    public int getCountX() {
        return this.mPortrait ? this.mShortAxisCells : this.mLongAxisCells;
    }

    /* access modifiers changed from: package-private */
    public int getCountY() {
        return this.mPortrait ? this.mLongAxisCells : this.mShortAxisCells;
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        ((LayoutParams) params).regenerateId = true;
        super.addView(child, index, params);
    }

    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect rect = new Rect();
            child.getDrawingRect(rect);
            requestRectangleOnScreen(rect);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int yCount;
        int action = motionEvent.getAction();
        CellInfo cellInfo = this.mCellInfo;
        if (action == 0) {
            Rect frame = this.mRect;
            int x = ((int) motionEvent.getX()) + getScrollX();
            int y = ((int) motionEvent.getY()) + getScrollY();
            boolean found = false;
            int i = getChildCount() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                View child = getChildAt(i);
                if (child.getVisibility() == 0 || child.getAnimation() != null) {
                    child.getHitRect(frame);
                    if (frame.contains(x, y)) {
                        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                        cellInfo.cell = child;
                        cellInfo.cellX = layoutParams.cellX;
                        cellInfo.cellY = layoutParams.cellY;
                        cellInfo.spanX = layoutParams.cellHSpan;
                        cellInfo.spanY = layoutParams.cellVSpan;
                        cellInfo.valid = true;
                        found = true;
                        this.mDirtyTag = false;
                        break;
                    }
                }
                i--;
            }
            this.mLastDownOnOccupiedCell = found;
            if (!found) {
                int[] cellXY = this.mCellXY;
                pointToCellExact(x, y, cellXY);
                boolean portrait = this.mPortrait;
                int xCount = portrait ? this.mShortAxisCells : this.mLongAxisCells;
                if (portrait) {
                    yCount = this.mLongAxisCells;
                } else {
                    yCount = this.mShortAxisCells;
                }
                boolean[][] occupied = this.mOccupied;
                findOccupiedCells(xCount, yCount, occupied, (View) null);
                cellInfo.cell = null;
                cellInfo.cellX = cellXY[0];
                cellInfo.cellY = cellXY[1];
                cellInfo.spanX = 1;
                cellInfo.spanY = 1;
                cellInfo.valid = cellXY[0] >= 0 && cellXY[1] >= 0 && cellXY[0] < xCount && cellXY[1] < yCount && !occupied[cellXY[0]][cellXY[1]];
                this.mDirtyTag = true;
            }
            setTag(cellInfo);
            return false;
        } else if (action != 1) {
            return false;
        } else {
            cellInfo.cell = null;
            cellInfo.cellX = -1;
            cellInfo.cellY = -1;
            cellInfo.spanX = 0;
            cellInfo.spanY = 0;
            cellInfo.valid = false;
            this.mDirtyTag = false;
            setTag(cellInfo);
            return false;
        }
    }

    public CellInfo getTag() {
        CellInfo info = (CellInfo) super.getTag();
        if (this.mDirtyTag && info.valid) {
            boolean portrait = this.mPortrait;
            int xCount = portrait ? this.mShortAxisCells : this.mLongAxisCells;
            int yCount = portrait ? this.mLongAxisCells : this.mShortAxisCells;
            boolean[][] occupied = this.mOccupied;
            findOccupiedCells(xCount, yCount, occupied, (View) null);
            findIntersectingVacantCells(info, info.cellX, info.cellY, xCount, yCount, occupied);
            this.mDirtyTag = false;
        }
        return info;
    }

    /* access modifiers changed from: private */
    public static void findIntersectingVacantCells(CellInfo cellInfo, int x, int y, int xCount, int yCount, boolean[][] occupied) {
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
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private static void findVacantCell(Rect current, int xCount, int yCount, boolean[][] occupied, CellInfo cellInfo) {
        int l = 0;
        while (l < xCount) {
            int r = l;
            while (r < xCount) {
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
                r++;
            }
            l++;
        }
    }

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

    /* access modifiers changed from: package-private */
    public CellInfo findAllVacantCells(boolean[] occupiedCells, View ignoreView) {
        boolean portrait = this.mPortrait;
        int xCount = portrait ? this.mShortAxisCells : this.mLongAxisCells;
        int yCount = portrait ? this.mLongAxisCells : this.mShortAxisCells;
        boolean[][] occupied = this.mOccupied;
        if (occupiedCells != null) {
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    occupied[x][y] = occupiedCells[(y * xCount) + x];
                }
            }
        } else {
            findOccupiedCells(xCount, yCount, occupied, ignoreView);
        }
        return findAllVacantCellsFromOccupied(occupied, xCount, yCount);
    }

    /* access modifiers changed from: package-private */
    public CellInfo findAllVacantCellsFromOccupied(boolean[][] occupied, int xCount, int yCount) {
        boolean z = false;
        CellInfo cellInfo = new CellInfo();
        cellInfo.cellX = -1;
        cellInfo.cellY = -1;
        cellInfo.spanY = 0;
        cellInfo.spanX = 0;
        cellInfo.maxVacantSpanX = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanXSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanYSpanX = Integer.MIN_VALUE;
        cellInfo.screen = this.mCellInfo.screen;
        findVacantCell(cellInfo.current, xCount, yCount, occupied, cellInfo);
        if (cellInfo.vacantCells.size() > 0) {
            z = true;
        }
        cellInfo.valid = z;
        return cellInfo;
    }

    /* access modifiers changed from: package-private */
    public void pointToCellExact(int x, int y, int[] result) {
        boolean portrait = this.mPortrait;
        int hStartPadding = portrait ? this.mShortAxisStartPadding : this.mLongAxisStartPadding;
        int vStartPadding = portrait ? this.mLongAxisStartPadding : this.mShortAxisStartPadding;
        result[0] = (x - hStartPadding) / (this.mCellWidth + this.mWidthGap);
        result[1] = (y - vStartPadding) / (this.mCellHeight + this.mHeightGap);
        int xAxis = portrait ? this.mShortAxisCells : this.mLongAxisCells;
        int yAxis = portrait ? this.mLongAxisCells : this.mShortAxisCells;
        if (result[0] < 0) {
            result[0] = 0;
        }
        if (result[0] >= xAxis) {
            result[0] = xAxis - 1;
        }
        if (result[1] < 0) {
            result[1] = 0;
        }
        if (result[1] >= yAxis) {
            result[1] = yAxis - 1;
        }
    }

    /* access modifiers changed from: package-private */
    public void cellToPoint(int cellX, int cellY, int[] result) {
        boolean portrait = this.mPortrait;
        int hStartPadding = portrait ? this.mShortAxisStartPadding : this.mLongAxisStartPadding;
        int vStartPadding = portrait ? this.mLongAxisStartPadding : this.mShortAxisStartPadding;
        result[0] = ((this.mCellWidth + this.mWidthGap) * cellX) + hStartPadding;
        result[1] = ((this.mCellHeight + this.mHeightGap) * cellY) + vStartPadding;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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
        this.mPortrait = heightSpecSize > widthSpecSize;
        int i = this.mCellWidth;
        int i2 = this.mCellHeight;
        if (this.mPortrait) {
            this.mLongAxisCells = this.mRows;
            this.mShortAxisCells = this.mColumns;
            tmpCellW = ((widthSpecSize - this.mShortAxisStartPadding) - this.mShortAxisEndPadding) / this.mColumns;
            tmpCellH = ((heightSpecSize - this.mLongAxisStartPadding) - this.mLongAxisEndPadding) / this.mRows;
        } else {
            this.mShortAxisCells = this.mRows;
            this.mLongAxisCells = this.mColumns;
            tmpCellW = ((widthSpecSize - this.mLongAxisStartPadding) - this.mLongAxisEndPadding) / this.mColumns;
            tmpCellH = ((heightSpecSize - this.mShortAxisStartPadding) - this.mShortAxisEndPadding) / this.mRows;
        }
        if (autoFit) {
            this.mCellWidth = tmpCellW;
            this.mCellHeight = tmpCellH;
        }
        if (this.mOccupied == null) {
            if (this.mPortrait) {
                this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mShortAxisCells, this.mLongAxisCells});
            } else {
                this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mLongAxisCells, this.mShortAxisCells});
            }
        }
        int shortAxisCells = this.mShortAxisCells;
        int longAxisCells = this.mLongAxisCells;
        int longAxisStartPadding = this.mLongAxisStartPadding;
        int longAxisEndPadding = this.mLongAxisEndPadding;
        int shortAxisStartPadding = this.mShortAxisStartPadding;
        int shortAxisEndPadding = this.mShortAxisEndPadding;
        int cellWidth = this.mCellWidth;
        int cellHeight = this.mCellHeight;
        this.mPortrait = heightSpecSize > widthSpecSize;
        int numShortGaps = shortAxisCells - 1;
        int numLongGaps = longAxisCells - 1;
        if (this.mPortrait) {
            this.mHeightGap = (((heightSpecSize - longAxisStartPadding) - longAxisEndPadding) - (cellHeight * longAxisCells)) / numLongGaps;
            int hSpaceLeft = ((widthSpecSize - shortAxisStartPadding) - shortAxisEndPadding) - (cellWidth * shortAxisCells);
            if (numShortGaps > 0) {
                this.mWidthGap = hSpaceLeft / numShortGaps;
            } else {
                this.mWidthGap = 0;
            }
        } else {
            this.mWidthGap = (((widthSpecSize - longAxisStartPadding) - longAxisEndPadding) - (cellWidth * longAxisCells)) / numLongGaps;
            int vSpaceLeft = ((heightSpecSize - shortAxisStartPadding) - shortAxisEndPadding) - (cellHeight * shortAxisCells);
            if (numShortGaps > 0) {
                this.mHeightGap = vSpaceLeft / numShortGaps;
            } else {
                this.mHeightGap = 0;
            }
        }
        int count = getChildCount();
        for (int i3 = 0; i3 < count; i3++) {
            View child = getChildAt(i3);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            if (this.mPortrait) {
                layoutParams.setup(cellWidth, cellHeight, this.mWidthGap, this.mHeightGap, shortAxisStartPadding, longAxisStartPadding, autoFit);
            } else {
                layoutParams.setup(cellWidth, cellHeight, this.mWidthGap, this.mHeightGap, longAxisStartPadding, shortAxisStartPadding, autoFit);
            }
            if (layoutParams.regenerateId) {
                child.setId(((getId() & 255) << 16) | ((layoutParams.cellX & 255) << 8) | (layoutParams.cellY & 255));
                layoutParams.regenerateId = false;
            }
            child.measure(View.MeasureSpec.makeMeasureSpec(layoutParams.width, 1073741824), View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824));
        }
        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                int childLeft = layoutParams.f0x;
                int childTop = layoutParams.f1y;
                child.layout(childLeft, childTop, layoutParams.width + childLeft, layoutParams.height + childTop);
                if (layoutParams.dropped) {
                    layoutParams.dropped = false;
                    int[] cellXY = this.mCellXY;
                    getLocationOnScreen(cellXY);
                    this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop", cellXY[0] + childLeft + (layoutParams.width / 2), cellXY[1] + childTop + (layoutParams.height / 2), 0, (Bundle) null);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void setChildrenDrawingCacheEnabled(boolean enabled) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            view.buildDrawingCache(true);
        }
    }

    /* access modifiers changed from: protected */
    public void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

    /* access modifiers changed from: package-private */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY, CellInfo vacantCells, int[] recycle) {
        int[] bestXY = recycle != null ? recycle : new int[2];
        int[] cellXY = this.mCellXY;
        double bestDistance = Double.MAX_VALUE;
        if (!vacantCells.valid) {
            return null;
        }
        int size = vacantCells.vacantCells.size();
        for (int i = 0; i < size; i++) {
            CellInfo.VacantCell cell = vacantCells.vacantCells.get(i);
            if (cell.spanX == spanX && cell.spanY == spanY) {
                cellToPoint(cell.cellX, cell.cellY, cellXY);
                double distance = Math.sqrt(Math.pow((double) (cellXY[0] - pixelX), 2.0d) + Math.pow((double) (cellXY[1] - pixelY), 2.0d));
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

    /* access modifiers changed from: package-private */
    public void onDropChild(View child, int[] targetXY) {
        if (child != null) {
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            layoutParams.cellX = targetXY[0];
            layoutParams.cellY = targetXY[1];
            layoutParams.isDragging = false;
            layoutParams.dropped = true;
            this.mDragRect.setEmpty();
            child.requestLayout();
            invalidate();
        }
    }

    /* access modifiers changed from: package-private */
    public void onDropAborted(View child) {
        if (child != null) {
            ((LayoutParams) child.getLayoutParams()).isDragging = false;
            invalidate();
        }
        this.mDragRect.setEmpty();
    }

    /* access modifiers changed from: package-private */
    public void onDragChild(View child) {
        ((LayoutParams) child.getLayoutParams()).isDragging = true;
        this.mDragRect.setEmpty();
    }

    public int[] rectToCell(int width, int height) {
        Resources resources = getResources();
        int smallerSize = Math.min(resources.getDimensionPixelSize(R.dimen.cell_width), resources.getDimensionPixelSize(R.dimen.cell_height));
        return new int[]{(width + smallerSize) / smallerSize, (height + smallerSize) / smallerSize};
    }

    public boolean getVacantCell(int[] vacant, int spanX, int spanY) {
        boolean portrait = this.mPortrait;
        int xCount = portrait ? this.mShortAxisCells : this.mLongAxisCells;
        int yCount = portrait ? this.mLongAxisCells : this.mShortAxisCells;
        boolean[][] occupied = this.mOccupied;
        findOccupiedCells(xCount, yCount, occupied, (View) null);
        return findVacantCell(vacant, spanX, spanY, xCount, yCount, occupied);
    }

    static boolean findVacantCell(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied) {
        boolean available;
        boolean available2;
        int x = 0;
        while (x < xCount) {
            int y = 0;
            while (y < yCount) {
                if (occupied[x][y]) {
                    available = false;
                } else {
                    available = true;
                }
                available2 = available;
                for (int i = x; i < (x + spanX) - 1 && x < xCount; i++) {
                    for (int j = y; j < (y + spanY) - 1 && y < yCount; j++) {
                        if (!available2 || occupied[i][j]) {
                            available2 = false;
                        } else {
                            available2 = true;
                        }
                        if (!available2) {
                            break;
                        }
                    }
                }
                if (available2) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
                y++;
            }
            x++;
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean[] getOccupiedCells() {
        boolean portrait = this.mPortrait;
        int xCount = portrait ? this.mShortAxisCells : this.mLongAxisCells;
        int yCount = portrait ? this.mLongAxisCells : this.mShortAxisCells;
        boolean[][] occupied = this.mOccupied;
        findOccupiedCells(xCount, yCount, occupied, (View) null);
        boolean[] flat = new boolean[(xCount * yCount)];
        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                flat[(y * xCount) + x] = occupied[x][y];
            }
        }
        return flat;
    }

    private void findOccupiedCells(int xCount, int yCount, boolean[][] occupied, View ignoreView) {
        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                occupied[x][y] = false;
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
                        if (!(x2 == -1 || y2 == -1)) {
                            occupied[x2][y2] = true;
                        }
                        y2++;
                    }
                    x2++;
                }
            }
        }
    }

    public boolean lastDownOnOccupiedCell() {
        return this.mLastDownOnOccupiedCell;
    }

    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    /* access modifiers changed from: protected */
    public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

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

        /* renamed from: x */
        int f0x;
        @ViewDebug.ExportedProperty

        /* renamed from: y */
        int f1y;

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(int cellX2, int cellY2, int cellHSpan2, int cellVSpan2) {
            super(-1, -1);
            this.cellX = cellX2;
            this.cellY = cellY2;
            this.cellHSpan = cellHSpan2;
            this.cellVSpan = cellVSpan2;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap, int hStartPadding, int vStartPadding, boolean autoStretch) {
            int myCellHSpan = this.cellHSpan;
            int myCellVSpan = this.cellVSpan;
            int myCellX = this.cellX;
            int myCellY = this.cellY;
            this.width = (((myCellHSpan * cellWidth) + ((myCellHSpan - 1) * widthGap)) - this.leftMargin) - this.rightMargin;
            this.height = (((myCellVSpan * cellHeight) + ((myCellVSpan - 1) * heightGap)) - this.topMargin) - this.bottomMargin;
            if (autoStretch) {
                this.width = ((cellWidth * myCellHSpan) - this.rightMargin) - this.leftMargin;
                this.height = cellHeight * myCellVSpan;
            }
            this.f0x = ((cellWidth + widthGap) * myCellX) + hStartPadding + this.leftMargin;
            this.f1y = ((cellHeight + heightGap) * myCellY) + vStartPadding + this.topMargin;
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

            /* access modifiers changed from: package-private */
            public void release() {
                synchronized (sLock) {
                    if (sAcquiredCount < POOL_LIMIT) {
                        sAcquiredCount++;
                        this.next = sRoot;
                        sRoot = this;
                    }
                }
            }

            public String toString() {
                return "VacantCell[x=" + this.cellX + ", y=" + this.cellY + ", spanX=" + this.spanX + ", spanY=" + this.spanY + "]";
            }
        }

        /* access modifiers changed from: package-private */
        public void clearVacantCells() {
            ArrayList<VacantCell> list = this.vacantCells;
            int count = list.size();
            for (int i = 0; i < count; i++) {
                list.get(i).release();
            }
            list.clear();
        }

        /* access modifiers changed from: package-private */
        public void findVacantCellsFromOccupied(boolean[] occupied, int xCount, int yCount) {
            if (this.cellX < 0 || this.cellY < 0) {
                this.maxVacantSpanXSpanY = Integer.MIN_VALUE;
                this.maxVacantSpanX = Integer.MIN_VALUE;
                this.maxVacantSpanYSpanX = Integer.MIN_VALUE;
                this.maxVacantSpanY = Integer.MIN_VALUE;
                clearVacantCells();
                return;
            }
            boolean[][] unflattened = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{xCount, yCount});
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    unflattened[x][y] = occupied[(y * xCount) + x];
                }
            }
            CellLayout.findIntersectingVacantCells(this, this.cellX, this.cellY, xCount, yCount, unflattened);
        }

        /* access modifiers changed from: package-private */
        public boolean findCellForSpan(int[] cellXY, int spanX2, int spanY2) {
            return findCellForSpan(cellXY, spanX2, spanY2, true);
        }

        /* access modifiers changed from: package-private */
        public boolean findCellForSpan(int[] cellXY, int spanX2, int spanY2, boolean clear) {
            ArrayList<VacantCell> list = this.vacantCells;
            int count = list.size();
            boolean found = false;
            if (this.spanX >= spanX2 && this.spanY >= spanY2) {
                cellXY[0] = this.cellX;
                cellXY[1] = this.cellY;
                found = true;
            }
            int i = 0;
            while (true) {
                if (i < count) {
                    VacantCell cell2 = list.get(i);
                    if (cell2.spanX == spanX2 && cell2.spanY == spanY2) {
                        cellXY[0] = cell2.cellX;
                        cellXY[1] = cell2.cellY;
                        found = true;
                        break;
                    }
                    i++;
                } else {
                    break;
                }
            }
            int i2 = 0;
            while (true) {
                if (i2 < count) {
                    VacantCell cell3 = list.get(i2);
                    if (cell3.spanX >= spanX2 && cell3.spanY >= spanY2) {
                        cellXY[0] = cell3.cellX;
                        cellXY[1] = cell3.cellY;
                        found = true;
                        break;
                    }
                    i2++;
                } else {
                    break;
                }
            }
            if (clear) {
                clearVacantCells();
            }
            return found;
        }

        public String toString() {
            return "Cell[view=" + (this.cell == null ? "null" : this.cell.getClass()) + ", x=" + this.cellX + ", y=" + this.cellY + "]";
        }
    }
}
