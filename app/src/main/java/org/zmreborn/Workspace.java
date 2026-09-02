package org.zmreborn;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import android.widget.TextView;
import java.util.ArrayList;
import org.zmreborn.compat.GestureExclusionCompat;

/**
 * Main launcher desktop workspace coordinating screen layouts, scrolling, wallpapers, and drag-drop interactions.
 */
public class Workspace extends ViewGroup implements DropTarget, DragSource, DragScroller,
        GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnDoubleTapListener {

    private static final int ACTION_OPEN_APPLICATIONS = 2;
    private static final int INVALID_SCREEN = -1;
    private static final int PREVIEWS_CLOSED = 4;
    private static final int PREVIEWS_CLOSING = 2;
    private static final int PREVIEWS_OPEN = 3;
    private static final int PREVIEWS_OPENING = 1;
    private static final int SNAP_VELOCITY = 500;
    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private static final int TOUCH_SWIPE_DOWN_GESTURE = 2;
    private static final int TOUCH_SWIPE_UP_GESTURE = 3;

    private boolean allowLongPress;
    private int animationDuration;
    private final WorkspaceBlurController blurController;
    final Rect clipBounds;
    private int columns;
    private float currentSpan;
    private int defaultScreen;
    private boolean desktopCache;
    private int[][] distro;
    private CellLayout.CellInfo dragInfo;
    private DragController dragger;
    final Rect drawerBounds;
    int drawerContentHeight;
    int drawerContentWidth;
    private boolean elasticScrolling;
    private boolean enableOvershootInterpolatorOnScrollFinish;
    private boolean firstLayout;
    private GestureDetector gestureDetector;
    private boolean isAnimating;
    private float lastMotionX;
    private float lastMotionY;
    private Launcher launcher;
    private boolean liveWallpaperSupport;
    private boolean locked;
    private View.OnLongClickListener longClickListener;
    private int maximumVelocity;
    private int maxPreviewHeight;
    private int maxPreviewWidth;
    private int nextScreen;
    private OvershootInterpolator overshootInterpolator;
    private Paint paint;
    private boolean previews;
    private int rows;
    private ScaleGestureDetector scaleGestureDetector;
    int screenCount;
    protected int screenCurrent;
    int screensLoaded;
    private Scroller scroller;
    private int scrollingBounce;
    private long startTime;
    private int status;
    private Rect systemGestureInsets;
    private int[] targetCell;
    private int[] tempCell;
    private int[] tempEstimate;
    private int touchSlop;
    private int touchState;
    private CellLayout.CellInfo vacantCache;
    private VelocityTracker velocityTracker;
    private boolean wallpaperDraw;
    private BitmapDrawable wallpaperDrawable;
    private boolean wallpaperLoaded;
    private final WallpaperManager wallpaperManager;
    private float wallpaperOffset;
    private boolean wallpaperScroll;
    private int wallpaperWidth;
    private int wallpaperYOffset;

    /**
     * Constructs a Workspace with context and XML attributes.
     */
    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a Workspace with context, XML attributes, and default style.
     */
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.blurController = new WorkspaceBlurController(this);
        this.firstLayout = true;
        this.nextScreen = INVALID_SCREEN;
        this.targetCell = null;
        this.touchState = 0;
        this.currentSpan = -1.0f;
        this.vacantCache = null;
        this.tempCell = new int[2];
        this.tempEstimate = new int[2];
        this.drawerBounds = new Rect();
        this.clipBounds = new Rect();
        this.screenCount = 0;
        this.screensLoaded = 0;
        this.wallpaperDraw = true;
        this.wallpaperScroll = true;
        this.liveWallpaperSupport = true;
        this.scrollingBounce = 50;
        this.isAnimating = false;
        this.previews = false;
        this.status = 4;
        this.animationDuration = 330;
        this.distro = new int[][]{new int[]{1}, new int[]{2}, new int[]{1, 2}, new int[]{2, 2},
                new int[]{2, 1, 2}, new int[]{2, 2, 2}, new int[]{2, 3, 2}};
        this.desktopCache = true;
        this.wallpaperManager = WallpaperManager.getInstance(context);
        this.screenCount = PreferencesUtil.getNumberOfScreens(context);
        this.defaultScreen = PreferencesUtil.getDefaultScreen(context);
        if (this.defaultScreen > this.screenCount + INVALID_SCREEN) {
            this.defaultScreen = 0;
        }
        initWorkspace();
    }

    private void initWorkspace() {
        Context context = getContext();
        this.overshootInterpolator = new OvershootInterpolator();
        this.scroller = new Scroller(context, this.overshootInterpolator);
        this.screenCurrent = this.defaultScreen;
        Launcher.setScreen(this.screenCurrent);
        this.paint = new Paint();
        this.paint.setDither(false);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.maximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.touchSlop = viewConfiguration.getScaledTouchSlop();
        this.rows = PreferencesUtil.getContentGridRows(context);
        this.columns = PreferencesUtil.getContentGridColumns(context);
        this.desktopCache = true;
        this.gestureDetector = new GestureDetector(this);
        this.gestureDetector.setOnDoubleTapListener(this);
        this.scaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    private static class OvershootInterpolator implements Interpolator {
        private final float tension;

        public OvershootInterpolator() {
            this(true);
        }

        public OvershootInterpolator(boolean enable) {
            this.tension = enable ? 1.5f : 0.0f;
        }

        @Override
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * (((this.tension + 1.0f) * t2) + this.tension)) + 1.0f;
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        if (this.screensLoaded < this.screenCount) {
            this.screensLoaded++;
            super.addView(child, index, params);
        }
    }

    @Override
    public void addView(View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, params);
    }

    private Search findSearchWidget(CellLayout screen) {
        int count = screen.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = screen.getChildAt(i);
            if (view instanceof Search) {
                return (Search) view;
            }
        }
        return null;
    }

    /**
     * Finds the search bar widget on the currently active workspace screen.
     */
    public Search findSearchWidgetOnCurrentScreen() {
        return findSearchWidget((CellLayout) getChildAt(this.screenCurrent));
    }

    Folder getOpenFolder() {
        CellLayout currentScreen = (CellLayout) getChildAt(this.screenCurrent);
        int count = currentScreen.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = currentScreen.getChildAt(i);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
            if (layoutParams.cellHSpan == this.columns && layoutParams.cellVSpan == this.rows && (child instanceof Folder)) {
                return (Folder) child;
            }
        }
        return null;
    }

    ArrayList<Folder> getOpenFolders() {
        int screens = getChildCount();
        ArrayList<Folder> folders = new ArrayList<>(screens);
        for (int screen = 0; screen < screens; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
                if (layoutParams.cellHSpan == this.columns && layoutParams.cellVSpan == this.rows && (child instanceof Folder)) {
                    folders.add((Folder) child);
                    break;
                }
            }
        }
        return folders;
    }

    boolean isDefaultScreenShowing() {
        return this.screenCurrent == this.defaultScreen;
    }

    int getCurrentScreen() {
        return this.screenCurrent;
    }

    void setCurrentScreen(int currentScreen) {
        clearVacantCache();
        this.screenCurrent = Math.max(0, Math.min(currentScreen, getChildCount() + INVALID_SCREEN));
        scrollTo(this.screenCurrent * getWidth(), 0);
        invalidate();
    }

    void addInCurrentScreen(View child, int x, int y, int spanX, int spanY) {
        addInScreen(child, this.screenCurrent, x, y, spanX, spanY, false);
    }

    void addInCurrentScreen(View child, int x, int y, int spanX, int spanY, boolean insert) {
        addInScreen(child, this.screenCurrent, x, y, spanX, spanY, insert);
    }

    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, screen, x, y, spanX, spanY, false);
    }

    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert) {
        if (screen >= 0 && screen < getChildCount() && x < this.columns && y < this.rows) {
            clearVacantCache();
            CellLayout cellLayout = (CellLayout) getChildAt(screen);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new CellLayout.LayoutParams(x, y, spanX, spanY);
            } else {
                layoutParams.cellX = x;
                layoutParams.cellY = y;
                layoutParams.cellHSpan = spanX;
                layoutParams.cellVSpan = spanY;
            }
            cellLayout.addView(child, insert ? 0 : INVALID_SCREEN, layoutParams);
            if (!(child instanceof Folder)) {
                child.setOnLongClickListener(this.longClickListener);
            }
            if (this.launcher != null) {
                this.launcher.updateWorkspaceEmptyTip();
            }
        }
    }

    void addWidget(View view, Widget widget, boolean insert) {
        addInScreen(view, widget.screen, widget.cellX, widget.cellY, widget.spanX, widget.spanY, insert);
    }

    CellLayout.CellInfo findAllVacantCells(boolean[] occupied) {
        CellLayout group = (CellLayout) getChildAt(this.screenCurrent);
        return group != null ? group.findAllVacantCells(occupied, null) : null;
    }

    private void clearVacantCache() {
        if (this.vacantCache != null) {
            this.vacantCache.clearVacantCells();
            this.vacantCache = null;
        }
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.longClickListener = onLongClickListener;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setOnLongClickListener(onLongClickListener);
        }
    }

    private void updateWallpaperOffset() {
        if (this.wallpaperScroll && getChildCount() > 0) {
            updateWallpaperOffset(getChildAt(getChildCount() + INVALID_SCREEN).getRight() - (getRight() - getLeft()));
        }
    }

    private void centerWallpaperOffset() {
        this.wallpaperManager.setWallpaperOffsetSteps(0.5f, 0.0f);
        this.wallpaperManager.setWallpaperOffsets(getWindowToken(), 0.5f, 0.0f);
    }

    private void updateWallpaperOffset(final int scrollRange) {
        if (getScrollX() > 0 && getScrollX() < getChildAt(getChildCount() + INVALID_SCREEN).getLeft()) {
            new Thread(new Runnable() {
                public void run() {
                    Workspace.this.wallpaperManager.setWallpaperOffsetSteps(1.0f / ((float) (Workspace.this.getChildCount() + Workspace.INVALID_SCREEN)), 0.0f);
                    Workspace.this.wallpaperManager.setWallpaperOffsets(Workspace.this.getWindowToken(), ((float) Workspace.this.getScrollX()) / ((float) scrollRange), 0.0f);
                }
            }).start();
        }
    }

    void indicateCurrent() {
        if (this.launcher != null && this.launcher.getScreenIndicator() != null) {
            this.launcher.getScreenIndicator().fullIndicate(this.screenCurrent);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        int previousX = getScrollX();
        int previousY = getScrollY();
        super.scrollTo(x, y);
        if (this.launcher != null && (previousX != x || previousY != y)) {
            this.launcher.invalidateBackgroundEffects();
        }
    }

    @Override
    public void computeScroll() {
        if (this.scroller.computeScrollOffset()) {
            scrollTo(this.scroller.getCurrX(), this.scroller.getCurrY());
            if (this.liveWallpaperSupport) {
                updateWallpaperOffset();
            }
            if (this.launcher != null && this.launcher.getScreenIndicator() != null) {
                this.launcher.getScreenIndicator().indicate(((float) this.scroller.getCurrX()) / ((float) (getChildCount() * getWidth())));
            }
            postInvalidate();
        } else if (this.nextScreen != INVALID_SCREEN) {
            this.screenCurrent = Math.max(0, Math.min(this.nextScreen, getChildCount() + INVALID_SCREEN));
            Launcher.setScreen(this.screenCurrent);
            this.nextScreen = INVALID_SCREEN;
            clearChildrenCache();
            if (this.launcher != null && this.launcher.getScreenIndicator() != null) {
                indicateCurrent();
            }
            if (this.enableOvershootInterpolatorOnScrollFinish) {
                this.enableOvershootInterpolatorOnScrollFinish = false;
                setElasticScrolling(true);
            }
        }
    }

    @Override
    public boolean isOpaque() {
        return !this.liveWallpaperSupport && this.wallpaperLoaded
                && this.wallpaperDrawable != null && this.wallpaperDrawable.getOpacity() == INVALID_SCREEN;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.launcher != null) {
            this.wallpaperYOffset = h - this.launcher.getWindow().getDecorView().getHeight();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!this.liveWallpaperSupport && this.wallpaperDrawable != null) {
            float x = calculateWallpaperX();
            int y = this.wallpaperYOffset;
            if (x > 0.0f || y > 0) {
                canvas.drawColor(Color.BLACK);
            }
            canvas.drawBitmap(this.wallpaperDrawable.getBitmap(), x, (float) y, this.paint);
        }
        if (this.previews) {
            drawPreviewsMode(canvas);
        } else if (this.launcher != null && !this.launcher.isApplicationsGridLogicallyOpen() && !this.launcher.isFullScreenPreviewing()) {
            drawDesktopMode(canvas);
        }
    }

    private void drawPreviewsMode(Canvas canvas) {
        long currentTime;
        if (this.startTime == 0) {
            this.startTime = SystemClock.uptimeMillis();
            currentTime = 0;
        } else {
            currentTime = SystemClock.uptimeMillis() - this.startTime;
        }
        if (currentTime >= ((long) this.animationDuration)) {
            this.isAnimating = false;
            if (this.status == 1) {
                this.status = 3;
            } else if (this.status == 2) {
                this.status = 4;
                this.previews = false;
                unlock();
                postInvalidate();
            }
        } else {
            postInvalidate();
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            drawChild(canvas, getChildAt(i), getDrawingTime());
        }
    }

    private void drawDesktopMode(Canvas canvas) {
        if (this.touchState != 1 && this.nextScreen == INVALID_SCREEN) {
            drawChild(canvas, getChildAt(this.screenCurrent), getDrawingTime());
        } else {
            long drawingTime = getDrawingTime();
            if (this.nextScreen < 0 || this.nextScreen >= getChildCount() || Math.abs(this.screenCurrent - this.nextScreen) != 1) {
                int count2 = getChildCount();
                for (int i2 = 0; i2 < count2; i2++) {
                    drawChild(canvas, getChildAt(i2), drawingTime);
                }
            } else {
                drawChild(canvas, getChildAt(this.screenCurrent), drawingTime);
                drawChild(canvas, getChildAt(this.nextScreen), drawingTime);
            }
        }
    }

    void drawWallpaperBackdrop(Canvas canvas, Rect bounds, View target, Bitmap bitmap, Paint filterPaint) {
        if (!canDrawWallpaperBackdrop(target, bitmap)) {
            return;
        }
        Bitmap source = this.wallpaperDrawable.getBitmap();
        float x = WallpaperBackdropAlignment.offset(calculateWallpaperX(), target.getX(), getX());
        float y = WallpaperBackdropAlignment.offset(this.wallpaperYOffset, target.getY(), getY());
        canvas.save();
        canvas.clipRect(bounds);
        drawBackdropGap(canvas, bounds, x, y);
        canvas.translate(x, y);
        canvas.scale((float) source.getWidth() / bitmap.getWidth(),
                (float) source.getHeight() / bitmap.getHeight());
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, filterPaint);
        canvas.restore();
    }

    private float calculateWallpaperX() {
        float x = ((float) getScrollX()) * this.wallpaperOffset;
        if (((float) this.wallpaperWidth) + x < getWidth()) {
            x = (float) (getWidth() - this.wallpaperWidth);
        }
        if (getScrollX() < 0) {
            x = (float) getScrollX();
        }
        if (getScrollX() > getChildAt(getChildCount() + INVALID_SCREEN).getRight() - getWidth()) {
            x = (float) ((getScrollX() - this.wallpaperWidth) + getWidth());
        }
        if (!this.wallpaperScroll || getChildCount() == 1) {
            x = (float) ((getScrollX() - (this.wallpaperWidth / 2)) + (getRight() / 2));
        }
        return x;
    }

    private boolean canDrawWallpaperBackdrop(View target, Bitmap bitmap) {
        return target != null && bitmap != null && !bitmap.isRecycled()
                && this.wallpaperDrawable != null && getChildCount() > 0
                && this.wallpaperWidth > 0;
    }

    private void drawBackdropGap(Canvas canvas, Rect bounds, float x, float y) {
        if (x > bounds.left || y > bounds.top) {
            canvas.drawColor(Color.BLACK);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        if (View.MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY
                || View.MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        if (!this.liveWallpaperSupport) {
            if (this.wallpaperLoaded) {
                this.wallpaperLoaded = false;
                this.wallpaperWidth = this.wallpaperDrawable.getIntrinsicWidth();
            }
            int wpWidth = this.wallpaperWidth;
            this.wallpaperOffset = wpWidth > width
                    ? ((float) ((count * width) - wpWidth)) / (((float) (count + INVALID_SCREEN)) * ((float) width))
                    : 1.0f;
        }
        if (this.firstLayout) {
            scrollTo(this.screenCurrent * width, 0);
            this.scroller.startScroll(0, 0, this.screenCurrent * width, 0, 0);
            if (this.liveWallpaperSupport) {
                updateWallpaperOffset((getChildCount() + INVALID_SCREEN) * width);
            }
            this.firstLayout = false;
        }
        float w = (float) (getMeasuredWidth() / 3);
        this.maxPreviewWidth = (int) w;
        this.maxPreviewHeight = (int) (((float) getMeasuredHeight()) * (w / ((float) getMeasuredWidth())));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childLeft = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
        updateSystemGestureExclusionRects();
        if (!this.liveWallpaperSupport) {
            return;
        }
        if (this.wallpaperScroll) {
            updateWallpaperOffset();
        } else {
            centerWallpaperOffset();
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (this.launcher == null || this.launcher.isApplicationsGridOpen()) {
            return false;
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            return openFolder.requestFocus(direction, previouslyFocusedRect);
        }
        int focusableScreen = this.nextScreen != INVALID_SCREEN ? this.nextScreen : this.screenCurrent;
        if (focusableScreen >= 0 && focusableScreen < getChildCount()) {
            getChildAt(focusableScreen).requestFocus(direction, previouslyFocusedRect);
        }
        return false;
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (direction == View.FOCUS_LEFT) {
            if (getCurrentScreen() > 0) {
                snapToScreen(getCurrentScreen() + INVALID_SCREEN);
                return true;
            }
        } else if (direction == View.FOCUS_RIGHT && getCurrentScreen() < getChildCount() + INVALID_SCREEN) {
            snapToScreen(getCurrentScreen() + 1);
            return true;
        } else if (direction == View.FOCUS_DOWN && this.launcher != null) {
            View dock = this.launcher.getDock();
            if (dock != null && dock.getVisibility() == View.VISIBLE) {
                dock.requestFocus();
                return true;
            }
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (this.launcher != null && this.launcher.isApplicationsGridOpen()) {
            return;
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.addFocusables(views, direction);
            return;
        }
        if (this.screenCurrent >= 0 && this.screenCurrent < getChildCount()) {
            getChildAt(this.screenCurrent).addFocusables(views, direction);
        }
        if (direction == View.FOCUS_LEFT && this.screenCurrent > 0) {
            getChildAt(this.screenCurrent + INVALID_SCREEN).addFocusables(views, direction);
        } else if (direction == View.FOCUS_RIGHT && this.screenCurrent < getChildCount() + INVALID_SCREEN) {
            getChildAt(this.screenCurrent + 1).addFocusables(views, direction);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.status == 3) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                expandPreview(motionEvent.getX(), motionEvent.getY());
            }
            return true;
        }
        if (this.locked || (this.launcher != null && this.launcher.isApplicationsGridOpen())) {
            return true;
        }
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_MOVE && this.touchState != 0) {
            return true;
        }
        this.scaleGestureDetector.onTouchEvent(motionEvent);
        if (this.scaleGestureDetector.isInProgress()) {
            return false;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.lastMotionX = x;
                this.lastMotionY = y;
                this.allowLongPress = true;
                this.touchState = this.scroller.isFinished() ? 0 : 1;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (this.touchState != 1 && this.touchState != 2 && this.touchState != 3) {
                    checkWallpaperTap(motionEvent);
                }
                clearChildrenCache();
                this.touchState = 0;
                this.allowLongPress = false;
                break;
            case MotionEvent.ACTION_MOVE:
                handleInterceptMove(x, y);
                break;
        }
        return this.touchState == 0 ? this.gestureDetector.onTouchEvent(motionEvent) : true;
    }

    private void checkWallpaperTap(MotionEvent motionEvent) {
        if (this.screenCurrent >= 0 && this.screenCurrent < getChildCount()
                && !((CellLayout) getChildAt(this.screenCurrent)).lastDownOnOccupiedCell()) {
            getLocationOnScreen(this.tempCell);
            if (this.liveWallpaperSupport && this.wallpaperManager != null) {
                this.wallpaperManager.sendWallpaperCommand(getWindowToken(), "android.wallpaper.tap",
                        this.tempCell[0] + ((int) motionEvent.getX()),
                        this.tempCell[1] + ((int) motionEvent.getY()), 0, null);
            }
        }
    }

    private void handleInterceptMove(float x, float y) {
        int xDiff = (int) Math.abs(x - this.lastMotionX);
        int yDiff = (int) Math.abs(y - this.lastMotionY);
        int slop = this.touchSlop;
        boolean xMoved = xDiff > slop;
        boolean yMoved = yDiff > slop;
        if (xMoved || yMoved) {
            if (xDiff > yDiff) {
                this.touchState = 1;
                enableChildrenCache();
            } else if (getOpenFolder() == null) {
                float yDelta = y - this.lastMotionY;
                if (yDelta > 0.0f) {
                    if (Math.abs(yDelta) > (float) (slop * 4) && PreferencesUtil.getActionBindingForSwipeDown(this.launcher) != 1) {
                        this.touchState = 2;
                    }
                } else if (Math.abs(yDelta) > (float) (slop * 4) && PreferencesUtil.getActionBindingForSwipeUp(this.launcher) != 1) {
                    this.touchState = 3;
                }
            }
            if (this.allowLongPress) {
                this.allowLongPress = false;
                if (this.screenCurrent >= 0 && this.screenCurrent < getChildCount()) {
                    getChildAt(this.screenCurrent).cancelLongPress();
                }
            }
        }
    }

    void enableChildrenCache() {
        if (this.desktopCache) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (i >= this.screenCurrent + INVALID_SCREEN || i <= this.screenCurrent + 1) {
                    CellLayout layout = (CellLayout) getChildAt(i);
                    layout.setChildrenDrawnWithCacheEnabled(true);
                    layout.setChildrenDrawingCacheEnabled(true);
                }
            }
        }
    }

    void clearChildrenCache() {
        if (this.desktopCache) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                ((CellLayout) getChildAt(i)).setChildrenDrawnWithCacheEnabled(false);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.locked || (this.launcher != null && this.launcher.isApplicationsGridOpen()) || this.previews) {
            return true;
        }
        if (this.velocityTracker == null) {
            this.velocityTracker = VelocityTracker.obtain();
        }
        this.velocityTracker.addMovement(motionEvent);
        int action = motionEvent.getAction();
        float x = motionEvent.getX();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!this.scroller.isFinished()) {
                    this.scroller.abortAnimation();
                }
                this.lastMotionX = x;
                break;
            case MotionEvent.ACTION_UP:
                handleTouchUp();
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(x);
                break;
            case MotionEvent.ACTION_CANCEL:
                this.touchState = 0;
                break;
        }
        return true;
    }

    private void handleTouchUp() {
        if (this.touchState == 1) {
            VelocityTracker vt = this.velocityTracker;
            vt.computeCurrentVelocity(1000, (float) this.maximumVelocity);
            int velocityX = (int) vt.getXVelocity();
            if (velocityX > SNAP_VELOCITY && this.screenCurrent > 0) {
                snapToScreen(this.screenCurrent + INVALID_SCREEN);
            } else if (velocityX >= -SNAP_VELOCITY || this.screenCurrent >= getChildCount() + INVALID_SCREEN) {
                snapToDestination();
            } else {
                snapToScreen(this.screenCurrent + 1);
            }
            if (this.velocityTracker != null) {
                this.velocityTracker.recycle();
                this.velocityTracker = null;
            }
        } else if (this.touchState == 2 && this.launcher != null) {
            this.launcher.fireSwipeDownAction();
        } else if (this.touchState == 3 && this.launcher != null) {
            this.launcher.fireSwipeUpAction();
        }
        this.touchState = 0;
    }

    private void handleTouchMove(float x) {
        if (this.touchState != 1) {
            return;
        }
        if (this.screenCount <= 1) {
            this.touchState = 0;
            return;
        }
        boolean screenLooping = PreferencesUtil.isScreenLoopingEnabled(this.launcher);
        int deltaX = (int) (this.lastMotionX - x);
        this.lastMotionX = x;
        if (deltaX > 0) {
            handleScrollForward(deltaX, screenLooping);
        } else if (deltaX < 0) {
            handleScrollBackward(deltaX, screenLooping);
        }
    }

    private void handleScrollForward(int deltaX, boolean screenLooping) {
        int rightEdge = getChildAt(getChildCount() + INVALID_SCREEN).getRight() - getScrollX() - getWidth();
        if (rightEdge + this.scrollingBounce <= 0) {
            if (screenLooping && this.screenCurrent == this.screenCount + INVALID_SCREEN) {
                if (isElasticScrollingEnabled()) {
                    this.enableOvershootInterpolatorOnScrollFinish = true;
                    setElasticScrolling(false);
                }
                snapToScreen(0);
                this.touchState = 0;
            }
        } else {
            scrollBy(deltaX, 0);
            if (this.liveWallpaperSupport) {
                updateWallpaperOffset();
            }
            if (this.launcher != null && this.launcher.getScreenIndicator() != null) {
                this.launcher.getScreenIndicator().indicate(((float) getScrollX()) / ((float) (getChildCount() * getWidth())));
            }
        }
    }

    private void handleScrollBackward(int deltaX, boolean screenLooping) {
        if (getScrollX() <= (-this.scrollingBounce)) {
            if (screenLooping && this.screenCurrent == 0) {
                if (isElasticScrollingEnabled()) {
                    this.enableOvershootInterpolatorOnScrollFinish = true;
                    setElasticScrolling(false);
                }
                snapToScreen(this.screenCount + INVALID_SCREEN);
                this.touchState = 0;
            }
        } else {
            scrollBy(Math.min(deltaX, this.scrollingBounce), 0);
            if (this.liveWallpaperSupport) {
                updateWallpaperOffset();
            }
            if (this.launcher != null && this.launcher.getScreenIndicator() != null) {
                this.launcher.getScreenIndicator().indicate(((float) getScrollX()) / ((float) (getChildCount() * getWidth())));
            }
        }
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        if (this.launcher != null) {
            this.launcher.onDoubleTap(motionEvent);
        }
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float f1, float f2) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent1, MotionEvent motionEvent2, float f1, float f2) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        this.currentSpan = detector.getCurrentSpan();
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (detector.getTimeDelta() > 100) {
            this.scaleGestureDetector = new ScaleGestureDetector(this.launcher, this);
            return true;
        }
        if (this.currentSpan <= -1.0f) {
            return false;
        }
        if (this.currentSpan > detector.getCurrentSpan() && this.launcher != null) {
            this.launcher.showPreviews(0, this.screenCount);
            this.scaleGestureDetector = new ScaleGestureDetector(this.launcher, this);
            this.currentSpan = -1.0f;
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    private void snapToDestination() {
        int screenWidth = getWidth();
        if (screenWidth > 0) {
            snapToScreen((getScrollX() + (screenWidth / 2)) / screenWidth);
        }
    }

    private void snapToScreen(int whichScreen) {
        int whichScreen2 = Math.max(0, Math.min(whichScreen, getChildCount() + INVALID_SCREEN));
        clearVacantCache();
        enableChildrenCache();
        this.nextScreen = whichScreen2;
        View focusedChild = getFocusedChild();
        if (focusedChild != null && whichScreen2 != this.screenCurrent && focusedChild == getChildAt(this.screenCurrent)) {
            focusedChild.clearFocus();
        }
        int delta = (whichScreen2 * getWidth()) - getScrollX();
        if (!this.scroller.isFinished()) {
            this.scroller.abortAnimation();
        }
        awakenScrollBars(this.animationDuration);
        this.scroller.startScroll(getScrollX(), 0, delta, 0, this.animationDuration);
        invalidate();
    }

    void startDrag(CellLayout.CellInfo info) {
        View child = info.cell;
        if (child == null) {
            return;
        }
        if (this.previews || !(this.status == 4 || this.status == 2)) {
            ((CellLayout.LayoutParams) child.getLayoutParams()).isDragging = false;
            return;
        }
        this.dragInfo = info;
        this.dragInfo.screen = this.screenCurrent;
        if (this.screenCurrent >= 0 && this.screenCurrent < getChildCount()) {
            ((CellLayout) getChildAt(this.screenCurrent)).onDragChild(child);
        }
        if (this.dragger != null) {
            this.dragger.startDrag(child, this, child.getTag(), DragController.DRAG_ACTION_MOVE);
        }
        invalidate();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.currentScreen = this.screenCurrent;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        try {
            SavedState savedState = (SavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            if (savedState.currentScreen != INVALID_SCREEN) {
                this.screenCurrent = savedState.currentScreen;
                Launcher.setScreen(this.screenCurrent);
            }
        } catch (Exception ignored) {
            super.onRestoreInstanceState(null);
        }
    }

    void addApplicationShortcut(ApplicationItemInfo info, CellLayout.CellInfo targetCellInfo, boolean insertAtFirst) {
        CellLayout layout = (CellLayout) getChildAt(targetCellInfo.screen);
        if (layout != null) {
            int[] result = new int[2];
            layout.cellToPoint(targetCellInfo.cellX, targetCellInfo.cellY, result);
            onDropExternal(result[0], result[1], info, layout, insertAtFirst);
        }
    }

    @Override
    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragObject) {
        CellLayout cellLayout = getCurrentDropLayout();
        if (cellLayout == null) {
            return;
        }
        if (source != this) {
            onDropExternal(x - xOffset, y - yOffset, dragObject, cellLayout);
        } else if (this.dragInfo != null) {
            View cell = this.dragInfo.cell;
            int index = this.scroller.isFinished() ? this.screenCurrent : this.nextScreen;
            if (index != this.dragInfo.screen) {
                ((CellLayout) getChildAt(this.dragInfo.screen)).removeView(cell);
                cellLayout.addView(cell);
            }
            this.targetCell = estimateDropCell(x - xOffset, y - yOffset, this.dragInfo.spanX, this.dragInfo.spanY, cell, cellLayout, this.targetCell);
            cellLayout.onDropChild(cell, this.targetCell);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) cell.getLayoutParams();
            if (this.launcher != null) {
                LauncherModel.moveItemInDatabase(this.launcher, (ItemInfo) cell.getTag(), -100, index, layoutParams.cellX, layoutParams.cellY);
            }
        }
    }

    @Override
    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        clearVacantCache();
    }

    @Override
    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        clearVacantCache();
    }

    private void onDropExternal(int x, int y, Object dragInfo, CellLayout cellLayout) {
        onDropExternal(x, y, dragInfo, cellLayout, false);
    }

    private void onDropExternal(int x, int y, Object dragInfo, CellLayout cellLayout, boolean insertAtFirst) {
        View view;
        ItemInfo itemInfo = (ItemInfo) dragInfo;
        switch (itemInfo.itemType) {
            case 0:
            case 1:
                if (itemInfo.container == -1) {
                    itemInfo = new ApplicationItemInfo((ApplicationItemInfo) itemInfo);
                }
                view = this.launcher.createShortcut(R.layout.application, cellLayout, (ApplicationItemInfo) itemInfo);
                break;
            case 2:
                view = FolderIcon.fromXml(R.layout.folder_icon, this.launcher, (ViewGroup) getChildAt(this.screenCurrent), (UserFolderInfo) itemInfo);
                break;
            case 3:
                view = LiveFolderIcon.fromXml(R.layout.live_folder_icon, this.launcher, (ViewGroup) getChildAt(this.screenCurrent), (LiveFolderInfo) itemInfo);
                break;
            case 6:
                view = this.launcher.createApplicationsGridItemView((ApplicationsGridItemInfo) itemInfo);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + itemInfo.itemType);
        }
        cellLayout.addView(view, insertAtFirst ? 0 : INVALID_SCREEN);
        view.setOnLongClickListener(this.longClickListener);
        this.targetCell = estimateDropCell(x, y, 1, 1, view, cellLayout, this.targetCell);
        cellLayout.onDropChild(view, this.targetCell);
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
        Launcher.getModel().addDesktopItem(itemInfo);
        LauncherModel.addOrMoveItemInDatabase(this.launcher, itemInfo, -100, this.screenCurrent, layoutParams.cellX, layoutParams.cellY);
    }

    private CellLayout getCurrentDropLayout() {
        int index = this.scroller.isFinished() ? this.screenCurrent : this.nextScreen;
        if (index >= 0 && index < getChildCount()) {
            return (CellLayout) getChildAt(index);
        }
        return null;
    }

    @Override
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        CellLayout layout = getCurrentDropLayout();
        if (layout == null) {
            return false;
        }
        CellLayout.CellInfo info = this.dragInfo;
        int spanX = info == null ? 1 : info.spanX;
        int spanY = info == null ? 1 : info.spanY;
        if (this.vacantCache == null) {
            this.vacantCache = layout.findAllVacantCells(null, info == null ? null : info.cell);
        }
        return this.vacantCache.findCellForSpan(this.tempEstimate, spanX, spanY, false);
    }

    /**
     * Estimates drop pixel bounds for the current drag item.
     */
    public Rect estimateDropLocation(int x, int y, int xOffset, int yOffset, Rect recycle) {
        CellLayout layout = getCurrentDropLayout();
        if (layout == null) {
            return null;
        }
        CellLayout.CellInfo info = this.dragInfo;
        int spanX = info == null ? 1 : info.spanX;
        int spanY = info == null ? 1 : info.spanY;
        View ignoreView = info == null ? null : info.cell;
        Rect location = recycle != null ? recycle : new Rect();
        int[] dropCell = estimateDropCell(x - xOffset, y - yOffset, spanX, spanY, ignoreView, layout, this.tempCell);
        if (dropCell == null) {
            return null;
        }
        layout.cellToPoint(dropCell[0], dropCell[1], this.tempEstimate);
        location.left = this.tempEstimate[0];
        location.top = this.tempEstimate[1];
        layout.cellToPoint(dropCell[0] + spanX, dropCell[1] + spanY, this.tempEstimate);
        location.right = this.tempEstimate[0];
        location.bottom = this.tempEstimate[1];
        return location;
    }

    private int[] estimateDropCell(int pixelX, int pixelY, int spanX, int spanY, View ignoreView, CellLayout layout, int[] recycle) {
        if (this.vacantCache == null) {
            this.vacantCache = layout.findAllVacantCells(null, ignoreView);
        }
        return layout.findNearestVacantArea(pixelX, pixelY, spanX, spanY, this.vacantCache, recycle);
    }

    void setLauncher(Launcher launcher) {
        this.launcher = launcher;
        if (this.launcher.getScreenIndicator() != null) {
            this.launcher.getScreenIndicator().setItems(this.screenCount);
        }
    }

    /**
     * Sets the drag controller for dragging workspace items.
     */
    public void setDragger(DragController dragger) {
        this.dragger = dragger;
    }

    /**
     * Updates system gesture insets and refreshes exclusion rects.
     */
    public void setSystemGestureInsets(Rect insets) {
        this.systemGestureInsets = insets;
        updateSystemGestureExclusionRects();
    }

    void updateSystemGestureExclusionRects() {
        if (this.launcher == null || this.systemGestureInsets == null
                || getWidth() == 0 || getHeight() == 0) {
            GestureExclusionCompat.clearSystemGestureExclusionRects(this);
            return;
        }
        ArrayList<Rect> rects = new ArrayList<>();
        addSwipeDownExclusionRect(rects);
        addSwipeUpExclusionRect(rects);
        GestureExclusionCompat.setSystemGestureExclusionRects(this, rects);
    }

    private void addSwipeDownExclusionRect(ArrayList<Rect> rects) {
        if (PreferencesUtil.getActionBindingForSwipeDown(this.launcher) != ACTION_OPEN_APPLICATIONS) {
            return;
        }
        int edgeHeight = Math.min(getHeight(), this.systemGestureInsets.top);
        if (edgeHeight > 0) {
            rects.add(new Rect(0, 0, getWidth(), edgeHeight));
        }
    }

    private void addSwipeUpExclusionRect(ArrayList<Rect> rects) {
        if (PreferencesUtil.getActionBindingForSwipeUp(this.launcher) != ACTION_OPEN_APPLICATIONS) {
            return;
        }
        int edgeHeight = Math.min(getHeight(), this.systemGestureInsets.bottom);
        if (edgeHeight > 0) {
            rects.add(new Rect(0, getHeight() - edgeHeight, getWidth(), getHeight()));
        }
    }

    @Override
    public void onDropCompleted(View target, boolean success) {
        clearVacantCache();
        if (success) {
            if (target != this && this.dragInfo != null) {
                ((CellLayout) getChildAt(this.dragInfo.screen)).removeView(this.dragInfo.cell);
                Launcher.getModel().removeDesktopItem((ItemInfo) this.dragInfo.cell.getTag());
            }
        } else if (this.dragInfo != null) {
            ((CellLayout) getChildAt(this.dragInfo.screen)).onDropAborted(this.dragInfo.cell);
        }
        this.dragInfo = null;
    }

    @Override
    public void scrollLeft() {
        clearVacantCache();
        if (this.nextScreen != INVALID_SCREEN) {
            this.screenCurrent = this.nextScreen;
            this.nextScreen = INVALID_SCREEN;
        }
        if (this.nextScreen == INVALID_SCREEN && this.screenCurrent > 0) {
            snapToScreen(this.screenCurrent + INVALID_SCREEN);
        }
    }

    @Override
    public void scrollRight() {
        clearVacantCache();
        if (this.nextScreen != INVALID_SCREEN) {
            this.screenCurrent = this.nextScreen;
            this.nextScreen = INVALID_SCREEN;
        }
        if (this.nextScreen == INVALID_SCREEN && this.screenCurrent < getChildCount() + INVALID_SCREEN) {
            snapToScreen(this.screenCurrent + 1);
        }
    }

    /**
     * Resolves the screen index hosting the given child view.
     */
    public int getScreenForView(View view) {
        if (view != null) {
            ViewParent viewParent = view.getParent();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (viewParent == getChildAt(i)) {
                    return i;
                }
            }
        }
        return INVALID_SCREEN;
    }

    /**
     * Resolves the Folder matching the given FolderInfo tag across screens.
     */
    public Folder getFolderForTag(Object tag) {
        int count = getChildCount();
        for (int screen = 0; screen < count; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int childCount = currentScreen.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
                if (layoutParams.cellHSpan == this.columns && layoutParams.cellVSpan == this.rows && (child instanceof Folder)) {
                    Folder folder = (Folder) child;
                    if (folder.getInfo() == tag) {
                        return folder;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolves the child View matching the given tag across screens.
     */
    public View getViewForTag(Object tag) {
        int count = getChildCount();
        for (int screen = 0; screen < count; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int childCount = currentScreen.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = currentScreen.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Unlocks the workspace to accept user touch input.
     */
    public void unlock() {
        this.locked = false;
    }

    /**
     * Locks the workspace to ignore touch input.
     */
    public void lock() {
        this.locked = true;
    }

    /**
     * Queries whether long press gestures are allowed.
     */
    public boolean allowLongPress() {
        return this.allowLongPress;
    }

    /**
     * Configures whether long press gestures are allowed.
     */
    public void setAllowLongPress(boolean allow) {
        this.allowLongPress = allow;
    }

    void removeShortcutsForPackage(String packageName) {
        Folder folder;
        ArrayList<View> childrenToRemove = new ArrayList<>();
        LauncherModel model = Launcher.getModel();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            int childCount = cellLayout.getChildCount();
            childrenToRemove.clear();
            for (int j = 0; j < childCount; j++) {
                View view = cellLayout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ApplicationItemInfo) {
                    ApplicationItemInfo info = (ApplicationItemInfo) tag;
                    Intent intent = info.intent;
                    ComponentName name = intent != null ? intent.getComponent() : null;
                    if (intent != null && "android.intent.action.MAIN".equals(intent.getAction()) && name != null && packageName.equals(name.getPackageName())) {
                        model.removeDesktopItem(info);
                        LauncherModel.deleteItemFromDatabase(this.launcher, info);
                        childrenToRemove.add(view);
                    }
                } else if (tag instanceof UserFolderInfo) {
                    ArrayList<ApplicationItemInfo> contents = ((UserFolderInfo) tag).contents;
                    ArrayList<ApplicationItemInfo> toRemove = new ArrayList<>(1);
                    int contentsCount = contents.size();
                    boolean removedFromFolder = false;
                    for (int k = 0; k < contentsCount; k++) {
                        ApplicationItemInfo appInfo = contents.get(k);
                        Intent intent2 = appInfo.intent;
                        ComponentName name2 = intent2 != null ? intent2.getComponent() : null;
                        if (intent2 != null && "android.intent.action.MAIN".equals(intent2.getAction()) && name2 != null && packageName.equals(name2.getPackageName())) {
                            toRemove.add(appInfo);
                            LauncherModel.deleteItemFromDatabase(this.launcher, appInfo);
                            removedFromFolder = true;
                        }
                    }
                    contents.removeAll(toRemove);
                    if (removedFromFolder && (folder = getOpenFolder()) != null) {
                        folder.notifyDataSetChanged();
                    }
                }
            }
            int removeCount = childrenToRemove.size();
            for (int j2 = 0; j2 < removeCount; j2++) {
                cellLayout.removeViewInLayout(childrenToRemove.get(j2));
            }
            if (removeCount > 0) {
                cellLayout.requestLayout();
                cellLayout.invalidate();
                if (this.launcher != null) {
                    this.launcher.updateWorkspaceEmptyTip();
                }
            }
        }
    }

    void updateShortcutsForPackage(String packageName) {
        Folder folder;
        Drawable icon;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout layout = (CellLayout) getChildAt(i);
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View view = layout.getChildAt(j);
                Object itemInfo = view.getTag();
                if (itemInfo instanceof ApplicationItemInfo) {
                    ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) itemInfo;
                    Intent intent = applicationItemInfo.intent;
                    ComponentName name = intent != null ? intent.getComponent() : null;
                    if ((applicationItemInfo.itemType == 0 || applicationItemInfo.itemType == 1) && intent != null && "android.intent.action.MAIN".equals(intent.getAction()) && name != null && packageName.equals(name.getPackageName()) && (icon = Launcher.getModel().getApplicationItemInfoIconOrNull(this.launcher.getPackageManager(), applicationItemInfo)) != null && icon != applicationItemInfo.icon) {
                        applicationItemInfo.filtered = true;
                        if (applicationItemInfo.icon != null) {
                            applicationItemInfo.icon.setCallback(null);
                        }
                        applicationItemInfo.icon = Utilities.setCompoundApplicationIcon((TextView) view, icon, getContext());
                    }
                } else if (itemInfo instanceof UserFolderInfo) {
                    ArrayList<ApplicationItemInfo> applicationItemInfos = ((UserFolderInfo) itemInfo).contents;
                    int applicationItemInfoCount = applicationItemInfos.size();
                    for (int y = 0; y < applicationItemInfoCount; y++) {
                        ApplicationItemInfo applicationItemInfo2 = applicationItemInfos.get(y);
                        Intent intent2 = applicationItemInfo2.intent;
                        ComponentName name2 = intent2 != null ? intent2.getComponent() : null;
                        if ((applicationItemInfo2.itemType == 0 || applicationItemInfo2.itemType == 1) && intent2 != null && "android.intent.action.MAIN".equals(intent2.getAction()) && name2 != null && packageName.equals(name2.getPackageName())) {
                            Drawable icon2 = Launcher.getModel().getApplicationItemInfoIconOrNull(this.launcher.getPackageManager(), applicationItemInfo2);
                            boolean folderUpdated = false;
                            if (icon2 != null && icon2 != applicationItemInfo2.icon) {
                                if (applicationItemInfo2.icon != null) {
                                    applicationItemInfo2.icon.setCallback(null);
                                }
                                applicationItemInfo2.icon = Utilities.normalizeApplicationIcon(icon2, this.launcher);
                                applicationItemInfo2.filtered = true;
                                folderUpdated = true;
                            }
                            if (folderUpdated && (folder = getOpenFolder()) != null) {
                                folder.notifyDataSetChanged();
                            }
                        }
                    }
                }
            }
        }
    }

    void moveToDefaultScreen() {
        snapToScreen(this.defaultScreen);
        if (this.defaultScreen >= 0 && this.defaultScreen < getChildCount()) {
            getChildAt(this.defaultScreen).requestFocus();
        }
    }

    /**
     * Parcelable container saving current screen selection across configuration changes.
     */
    public static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int currentScreen;

        SavedState(Parcelable superState) {
            super(superState);
            this.currentScreen = Workspace.INVALID_SCREEN;
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentScreen = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.currentScreen);
        }
    }

    void applyFrostedBackgrounds(View dock, View drawer, int drawerAlpha) {
        this.blurController.apply(dock, drawer, drawerAlpha);
    }

    void destroyBackgroundEffects() {
        this.blurController.destroy();
    }

    /**
     * Refreshes wallpaper background and live wallpaper support configuration.
     */
    public void setWallpaper(boolean fromIntentReceiver) {
        if (this.wallpaperManager.getWallpaperInfo() != null || !this.wallpaperDraw) {
            this.wallpaperLoaded = false;
            this.wallpaperDrawable = null;
            this.liveWallpaperSupport = true;
        } else {
            if (fromIntentReceiver || this.wallpaperDrawable == null) {
                try {
                    this.wallpaperDrawable = (BitmapDrawable) this.wallpaperManager.getDrawable();
                    this.wallpaperLoaded = true;
                    this.liveWallpaperSupport = false;
                } catch (SecurityException e) {
                    this.wallpaperDrawable = null;
                    this.wallpaperLoaded = false;
                    this.liveWallpaperSupport = true;
                    Log.w(Launcher.LOG_TAG, "Wallpaper drawable unavailable", e);
                }
            } else {
                this.liveWallpaperSupport = false;
            }
        }
        if (this.launcher != null) {
            this.launcher.setWindowBackground(this.liveWallpaperSupport);
        }
        this.blurController.refresh();
        invalidate();
        requestLayout();
    }

    /**
     * Configures whether the wallpaper should be drawn directly by this view.
     */
    public void setDrawWallpaper(boolean drawWallpaper) {
        this.wallpaperDraw = drawWallpaper;
        this.liveWallpaperSupport = !this.wallpaperDraw || this.wallpaperManager.getWallpaperInfo() != null;
        if (this.launcher != null) {
            this.launcher.setWindowBackground(this.liveWallpaperSupport);
        }
    }

    /**
     * Configures wallpaper scrolling alongside workspace screen sliding.
     */
    public void setScrollWallpaper(boolean scrollWallpaper) {
        this.wallpaperScroll = scrollWallpaper;
        postInvalidate();
        if (this.launcher != null) {
            this.launcher.invalidateBackgroundEffects();
        }
    }

    Bitmap getBlurWallpaperSource() {
        if (Build.VERSION.SDK_INT < 31 || !PreferencesUtil.isBlurBackgroundsEnabled(getContext())) {
            return null;
        }
        if (this.liveWallpaperSupport || this.wallpaperDrawable == null) {
            return null;
        }
        Bitmap wallpaper = this.wallpaperDrawable.getBitmap();
        return wallpaper == null || wallpaper.isRecycled() ? null : wallpaper;
    }

    boolean usesLiveWallpaper() {
        return this.liveWallpaperSupport;
    }

    void applyBackgroundEffectsFromBlur() {
        if (this.launcher != null) {
            this.launcher.applyBackgroundEffects();
        }
    }

    /**
     * Enables or disables overshoot elastic scrolling interpolators.
     */
    public void setElasticScrolling(boolean enabled) {
        Context context = getContext();
        this.overshootInterpolator = new OvershootInterpolator(enabled);
        this.scroller = new Scroller(context, this.overshootInterpolator);
        this.elasticScrolling = enabled;
    }

    boolean isElasticScrollingEnabled() {
        return this.elasticScrolling;
    }

    /**
     * Initiates opening or closing transitions for workspace screen previews.
     */
    public void togglePreviews(boolean open) {
        this.scroller.abortAnimation();
        enableChildrenCache();
        this.previews = true;
        this.isAnimating = true;
        this.status = open ? 1 : 2;
        this.startTime = 0;
        if (open) {
            this.allowLongPress = true;
        }
        invalidate();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int saveCount = canvas.save();
        if (!this.previews) {
            super.drawChild(canvas, child, drawingTime);
        } else if (this.isAnimating || this.status == 3) {
            drawPreviewChild(canvas, child);
        } else {
            child.draw(canvas);
        }
        canvas.restoreToCount(saveCount);
        return true;
    }

    private void drawPreviewChild(Canvas canvas, View child) {
        long currentTime = SystemClock.uptimeMillis() - this.startTime;
        Rect rect1 = new Rect(0, 0, child.getWidth(), child.getHeight());
        RectF rect2 = getScaledChild(child);
        float x = 0.0f;
        float y = 0.0f;
        float width = 0.0f;
        float alpha = 255.0f;
        if (this.status == 1) {
            alpha = easeOut((float) currentTime, 0.0f, 100.0f, (float) this.animationDuration);
            x = easeOut((float) currentTime, (float) child.getLeft(), rect2.left, (float) this.animationDuration);
            y = easeOut((float) currentTime, (float) child.getTop(), rect2.top, (float) this.animationDuration);
            width = easeOut((float) currentTime, (float) child.getRight(), rect2.right, (float) this.animationDuration);
        } else if (this.status == 2) {
            alpha = easeOut((float) currentTime, 100.0f, 0.0f, (float) this.animationDuration);
            x = easeOut((float) currentTime, rect2.left, (float) child.getLeft(), (float) this.animationDuration);
            y = easeOut((float) currentTime, rect2.top, (float) child.getTop(), (float) this.animationDuration);
            width = easeOut((float) currentTime, rect2.right, (float) child.getRight(), (float) this.animationDuration);
        } else if (this.status == 3) {
            x = rect2.left;
            y = rect2.top;
            width = rect2.right;
            alpha = 100.0f;
        }
        float scale = (width - x) / ((float) rect1.width());
        canvas.translate(x, y);
        canvas.scale(scale, scale);
        this.paint.setAlpha((int) alpha);
        canvas.drawRoundRect(new RectF((float) (rect1.left + 5), (float) (rect1.top + 5),
                (float) (rect1.right - 5), (float) (rect1.bottom - 5)), 15.0f, 15.0f, this.paint);
        this.paint.setAlpha(255);
        child.draw(canvas);
    }

    static float easeOut(float time, float begin, float end, float duration) {
        float change = end - begin;
        float time2 = (time / duration) - 1.0f;
        float value = (((time2 * time2 * time2) + 1.0f) * change) + begin;
        if (change > 0.0f && value > end) {
            return end;
        }
        if (change < 0.0f && value < end) {
            return end;
        }
        return value;
    }

    static float easeIn(float time, float begin, float end, float duration) {
        float change = end - begin;
        float time2 = time / duration;
        float value = (change * time2 * time2 * time2) + begin;
        if (change > 0.0f && value > end) {
            return end;
        }
        if (change < 0.0f && value < end) {
            return end;
        }
        return value;
    }

    static float easeInOut(float time, float begin, float end, float duration) {
        float change = end - begin;
        float time2 = time / (duration / 2.0f);
        if (time2 < 1.0f) {
            return ((change / 2.0f) * time2 * time2 * time2) + begin;
        }
        float time3 = time2 - 2.0f;
        return ((change / 2.0f) * ((time3 * time3 * time3) + 2.0f)) + begin;
    }

    private RectF getScaledChild(View child) {
        int count = getChildCount();
        int width = getWidth();
        int height = getHeight();
        int xpos = getScrollX();
        int ypos = 0;
        int distroSet = count + INVALID_SCREEN;
        int childPos = 0;
        int maxItemsPerRow = 0;
        for (int r = 0; r < this.distro[distroSet].length; r++) {
            if (this.distro[distroSet][r] > maxItemsPerRow) {
                maxItemsPerRow = this.distro[distroSet][r];
            }
        }
        int childWidth = Math.min(width / maxItemsPerRow, this.maxPreviewWidth);
        int childHeight = Math.round(((float) this.maxPreviewHeight) * (((float) childWidth) / ((float) this.maxPreviewWidth)));
        int topMargin = (height / 2) - ((this.distro[distroSet].length * childHeight) / 2);
        for (int r2 = 0; r2 < this.distro[distroSet].length; r2++) {
            int leftMargin = (width / 2) - ((this.distro[distroSet][r2] * childWidth) / 2);
            for (int col = 0; col < this.distro[distroSet][r2] && childPos <= getChildCount() + INVALID_SCREEN; col++) {
                if (child == getChildAt(childPos)) {
                    return new RectF((float) (leftMargin + xpos), (float) (topMargin + ypos),
                            (float) (leftMargin + xpos + childWidth), (float) (topMargin + ypos + childHeight));
                }
                xpos += childWidth;
                childPos++;
            }
            xpos = getScrollX();
            ypos += childHeight;
        }
        return new RectF();
    }

    private void expandPreview(float x, float y) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getScaledChild(getChildAt(i)).contains(((float) getScrollX()) + x, ((float) getScrollY()) + y)) {
                if (this.launcher != null) {
                    this.launcher.dismissPreviews();
                }
                if (this.screenCurrent != i) {
                    if (isElasticScrollingEnabled()) {
                        setElasticScrolling(false);
                        this.enableOvershootInterpolatorOnScrollFinish = true;
                    }
                    snapToScreen(i);
                    postInvalidate();
                }
                return;
            }
        }
    }

    /**
     * Gets the associated parent Launcher activity.
     */
    public Activity getLauncherActivity() {
        return this.launcher;
    }

    /**
     * Gets the number of grid rows on current desktop.
     */
    public int getCurrentDesktopRows() {
        return this.rows;
    }

    /**
     * Gets the configured number of workspace screens.
     */
    public int getScreenCount() {
        return this.screenCount;
    }

    /**
     * Gets the number of grid columns on current desktop.
     */
    public int getCurrentDesktopColumns() {
        return this.columns;
    }
}
