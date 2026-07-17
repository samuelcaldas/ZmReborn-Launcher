package org.zeam;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import org.zeam.CellLayout;

public class Workspace extends ViewGroup implements DropTarget, DragSource, DragScroller, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnDoubleTapListener {
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
    private boolean isAnimating;
    protected boolean mAllowLongPress;
    private int mAnimationDuration;
    final Rect mClipBounds;
    private int mColumns;
    private float mCurrentSpan;
    private int mDefaultScreen;
    private boolean mDesktopCache;
    private int[][] mDistro;
    private CellLayout.CellInfo mDragInfo;
    private DragController mDragger;
    final Rect mDrawerBounds;
    int mDrawerContentHeight;
    int mDrawerContentWidth;
    private boolean mElasticScrolling;
    private boolean mEnableOvershootInterpolatorOnScrollFinish;
    private boolean mFirstLayout;
    private GestureDetector mGestureDetector;
    private float mLastMotionX;
    private float mLastMotionY;
    private Launcher mLauncher;
    private boolean mLiveWallpaperSupport;
    private boolean mLocked;
    private View.OnLongClickListener mLongClickListener;
    private int mMaximumVelocity;
    private int mNextScreen;
    private OvershootInterpolator mOvershootInterpolator;
    private Paint mPaint;
    private boolean mPreviews;
    private int mRows;
    private ScaleGestureDetector mScaleGestureDetector;
    int mScreenCount;
    protected int mScreenCurrent;
    int mScreensLoaded;
    private Scroller mScroller;
    private int mScrollingBounce;
    private long mStartTime;
    private int mStatus;
    private int[] mTargetCell;
    private int[] mTempCell;
    private int[] mTempEstimate;
    private int mTouchSlop;
    private int mTouchState;
    private CellLayout.CellInfo mVacantCache;
    private VelocityTracker mVelocityTracker;
    private boolean mWallpaperDraw;
    private BitmapDrawable mWallpaperDrawable;
    private boolean mWallpaperLoaded;
    /* access modifiers changed from: private */
    public final WallpaperManager mWallpaperManager;
    private float mWallpaperOffset;
    private boolean mWallpaperScroll;
    private int mWallpaperWidth;
    private int mWallpaperYOffset;
    private int maxPreviewHeight;
    private int maxPreviewWidth;

    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFirstLayout = true;
        this.mNextScreen = INVALID_SCREEN;
        this.mTargetCell = null;
        this.mTouchState = 0;
        this.mCurrentSpan = -1.0f;
        this.mVacantCache = null;
        this.mTempCell = new int[2];
        this.mTempEstimate = new int[2];
        this.mDrawerBounds = new Rect();
        this.mClipBounds = new Rect();
        this.mScreenCount = 0;
        this.mScreensLoaded = 0;
        this.mWallpaperDraw = true;
        this.mWallpaperScroll = true;
        this.mLiveWallpaperSupport = true;
        this.mScrollingBounce = 50;
        this.isAnimating = false;
        this.mPreviews = false;
        this.mStatus = 4;
        this.mAnimationDuration = 330;
        this.mDistro = new int[][]{new int[]{1}, new int[]{2}, new int[]{1, 2}, new int[]{2, 2}, new int[]{2, 1, 2}, new int[]{2, 2, 2}, new int[]{2, 3, 2}};
        this.mDesktopCache = true;
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        this.mScreenCount = PreferencesUtil.getNumberOfScreens(context);
        this.mDefaultScreen = PreferencesUtil.getDefaultScreen(context);
        if (this.mDefaultScreen > this.mScreenCount + INVALID_SCREEN) {
            this.mDefaultScreen = 0;
        }
        initWorkspace();
    }

    private void initWorkspace() {
        Context context = getContext();
        this.mOvershootInterpolator = new OvershootInterpolator();
        this.mScroller = new Scroller(context, this.mOvershootInterpolator);
        this.mScreenCurrent = this.mDefaultScreen;
        Launcher.setScreen(this.mScreenCurrent);
        this.mPaint = new Paint();
        this.mPaint.setDither(false);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mRows = PreferencesUtil.getContentGridRows(context);
        this.mColumns = PreferencesUtil.getContentGridColumns(context);
        this.mDesktopCache = true;
        this.mGestureDetector = new GestureDetector(this);
        this.mGestureDetector.setOnDoubleTapListener(this);
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    private static class OvershootInterpolator implements Interpolator {
        private float mTension;

        public OvershootInterpolator() {
            this(true);
        }

        public OvershootInterpolator(boolean enable) {
            if (enable) {
                this.mTension = 1.5f;
            } else {
                this.mTension = 0.0f;
            }
        }

        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * (((this.mTension + 1.0f) * t2) + this.mTension)) + 1.0f;
        }
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        } else if (this.mScreensLoaded < this.mScreenCount) {
            this.mScreensLoaded++;
            super.addView(child, index, params);
        }
    }

    public void addView(View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child);
    }

    public void addView(View child, int index) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, index);
    }

    public void addView(View child, int width, int height) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, width, height);
    }

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

    public Search findSearchWidgetOnCurrentScreen() {
        return findSearchWidget((CellLayout) getChildAt(this.mScreenCurrent));
    }

    /* access modifiers changed from: package-private */
    public Folder getOpenFolder() {
        CellLayout currentScreen = (CellLayout) getChildAt(this.mScreenCurrent);
        int count = currentScreen.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = currentScreen.getChildAt(i);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
            if (layoutParams.cellHSpan == this.mColumns && layoutParams.cellVSpan == this.mRows && (child instanceof Folder)) {
                return (Folder) child;
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public ArrayList<Folder> getOpenFolders() {
        int screens = getChildCount();
        ArrayList<Folder> folders = new ArrayList<>(screens);
        for (int screen = 0; screen < screens; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int count = currentScreen.getChildCount();
            int i = 0;
            while (true) {
                if (i < count) {
                    View child = currentScreen.getChildAt(i);
                    CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
                    if (layoutParams.cellHSpan == this.mColumns && layoutParams.cellVSpan == this.mRows && (child instanceof Folder)) {
                        folders.add((Folder) child);
                        break;
                    }
                    i++;
                } else {
                    break;
                }
            }
        }
        return folders;
    }

    /* access modifiers changed from: package-private */
    public boolean isDefaultScreenShowing() {
        return this.mScreenCurrent == this.mDefaultScreen;
    }

    /* access modifiers changed from: package-private */
    public int getCurrentScreen() {
        return this.mScreenCurrent;
    }

    /* access modifiers changed from: package-private */
    public void setCurrentScreen(int currentScreen) {
        clearVacantCache();
        this.mScreenCurrent = Math.max(0, Math.min(currentScreen, getChildCount() + INVALID_SCREEN));
        scrollTo(this.mScreenCurrent * getWidth(), 0);
        invalidate();
    }

    /* access modifiers changed from: package-private */
    public void addInCurrentScreen(View child, int x, int y, int spanX, int spanY) {
        addInScreen(child, this.mScreenCurrent, x, y, spanX, spanY, false);
    }

    /* access modifiers changed from: package-private */
    public void addInCurrentScreen(View child, int x, int y, int spanX, int spanY, boolean insert) {
        addInScreen(child, this.mScreenCurrent, x, y, spanX, spanY, insert);
    }

    /* access modifiers changed from: package-private */
    public void addInScreen(View child, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, screen, x, y, spanX, spanY, false);
    }

    /* access modifiers changed from: package-private */
    public void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert) {
        if (screen >= 0 && screen < getChildCount() && x < this.mColumns && y < this.mRows) {
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
                child.setOnLongClickListener(this.mLongClickListener);
            }
            this.mLauncher.updateWorkspaceEmptyTip();
        }
    }

    /* access modifiers changed from: package-private */
    public void addWidget(View view, Widget widget, boolean insert) {
        addInScreen(view, widget.screen, widget.cellX, widget.cellY, widget.spanX, widget.spanY, insert);
    }

    /* access modifiers changed from: package-private */
    public CellLayout.CellInfo findAllVacantCells(boolean[] occupied) {
        CellLayout group = (CellLayout) getChildAt(this.mScreenCurrent);
        if (group != null) {
            return group.findAllVacantCells(occupied, (View) null);
        }
        return null;
    }

    private void clearVacantCache() {
        if (this.mVacantCache != null) {
            this.mVacantCache.clearVacantCells();
            this.mVacantCache = null;
        }
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.mLongClickListener = onLongClickListener;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setOnLongClickListener(onLongClickListener);
        }
    }

    private void updateWallpaperOffset() {
        if (this.mWallpaperScroll) {
            updateWallpaperOffset(getChildAt(getChildCount() + INVALID_SCREEN).getRight() - (getRight() - getLeft()));
        }
    }

    private void centerWallpaperOffset() {
        this.mWallpaperManager.setWallpaperOffsetSteps(0.5f, 0.0f);
        this.mWallpaperManager.setWallpaperOffsets(getWindowToken(), 0.5f, 0.0f);
    }

    private void updateWallpaperOffset(final int scrollRange) {
        if (getScrollX() > 0 && getScrollX() < getChildAt(getChildCount() + INVALID_SCREEN).getLeft()) {
            new Thread(new Runnable() {
                public void run() {
                    Workspace.this.mWallpaperManager.setWallpaperOffsetSteps(1.0f / ((float) (Workspace.this.getChildCount() + Workspace.INVALID_SCREEN)), 0.0f);
                    Workspace.this.mWallpaperManager.setWallpaperOffsets(Workspace.this.getWindowToken(), ((float) Workspace.this.getScrollX()) / ((float) scrollRange), 0.0f);
                }
            }).start();
        }
    }

    /* access modifiers changed from: package-private */
    public void indicateCurrent() {
        this.mLauncher.getScreenIndicator().fullIndicate(this.mScreenCurrent);
    }

    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            scrollTo(this.mScroller.getCurrX(), this.mScroller.getCurrY());
            if (this.mLiveWallpaperSupport) {
                updateWallpaperOffset();
            }
            if (this.mLauncher.getScreenIndicator() != null) {
                this.mLauncher.getScreenIndicator().indicate(((float) this.mScroller.getCurrX()) / ((float) (getChildCount() * getWidth())));
            }
            postInvalidate();
        } else if (this.mNextScreen != INVALID_SCREEN) {
            this.mScreenCurrent = Math.max(0, Math.min(this.mNextScreen, getChildCount() + INVALID_SCREEN));
            Launcher.setScreen(this.mScreenCurrent);
            this.mNextScreen = INVALID_SCREEN;
            clearChildrenCache();
            if (this.mLauncher.getScreenIndicator() != null) {
                indicateCurrent();
            }
            if (this.mEnableOvershootInterpolatorOnScrollFinish) {
                this.mEnableOvershootInterpolatorOnScrollFinish = false;
                setElasticScrolling(true);
            }
        }
    }

    public boolean isOpaque() {
        if (this.mLiveWallpaperSupport || !this.mWallpaperLoaded || this.mWallpaperDrawable.getOpacity() != INVALID_SCREEN) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mLauncher != null) {
            this.mWallpaperYOffset = h - this.mLauncher.getWindow().getDecorView().getHeight();
        }
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        long currentTime;
        if (!this.mLiveWallpaperSupport && this.mWallpaperDrawable != null) {
            float x = ((float) getScrollX()) * this.mWallpaperOffset;
            if (((float) this.mWallpaperWidth) + x < ((float) (getRight() - getLeft()))) {
                x = (float) ((getRight() - getLeft()) - this.mWallpaperWidth);
            }
            if (getScrollX() < 0) {
                x = (float) getScrollX();
            }
            if (getScrollX() > getChildAt(getChildCount() + INVALID_SCREEN).getRight() - (getRight() - getLeft())) {
                x = (float) ((getScrollX() - this.mWallpaperWidth) + (getRight() - getLeft()));
            }
            if (!this.mWallpaperScroll || getChildCount() == 1) {
                x = (float) ((getScrollX() - (this.mWallpaperWidth / 2)) + (getRight() / 2));
            }
            int y = this.mWallpaperYOffset;
            if (x > 0.0f || y > 0) {
                canvas.drawColor(-16777216);
            }
            canvas.drawBitmap(this.mWallpaperDrawable.getBitmap(), x, (float) y, this.mPaint);
        }
        if (this.mPreviews) {
            if (this.mStartTime == 0) {
                this.mStartTime = SystemClock.uptimeMillis();
                currentTime = 0;
            } else {
                currentTime = SystemClock.uptimeMillis() - this.mStartTime;
            }
            if (currentTime >= ((long) this.mAnimationDuration)) {
                this.isAnimating = false;
                if (this.mStatus == 1) {
                    this.mStatus = 3;
                } else if (this.mStatus == 2) {
                    this.mStatus = 4;
                    this.mPreviews = false;
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
        } else if (!this.mLauncher.isApplicationsGridOpen() && !this.mLauncher.isFullScreenPreviewing()) {
            if (this.mTouchState != 1 && this.mNextScreen == INVALID_SCREEN) {
                drawChild(canvas, getChildAt(this.mScreenCurrent), getDrawingTime());
            } else {
                long drawingTime = getDrawingTime();
                if (this.mNextScreen < 0 || this.mNextScreen >= getChildCount() || Math.abs(this.mScreenCurrent - this.mNextScreen) != 1) {
                    int count2 = getChildCount();
                    for (int i2 = 0; i2 < count2; i2++) {
                        drawChild(canvas, getChildAt(i2), drawingTime);
                    }
                } else {
                    drawChild(canvas, getChildAt(this.mScreenCurrent), drawingTime);
                    drawChild(canvas, getChildAt(this.mNextScreen), drawingTime);
                }
            }
        } else {
            return;
        }
        if (0 != 0) {
            canvas.restore();
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        if (View.MeasureSpec.getMode(widthMeasureSpec) != 1073741824) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        } else if (View.MeasureSpec.getMode(heightMeasureSpec) != 1073741824) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        } else {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
            }
            if (!this.mLiveWallpaperSupport) {
                if (this.mWallpaperLoaded) {
                    this.mWallpaperLoaded = false;
                    this.mWallpaperWidth = this.mWallpaperDrawable.getIntrinsicWidth();
                }
                int wallpaperWidth = this.mWallpaperWidth;
                this.mWallpaperOffset = wallpaperWidth > width ? ((float) ((count * width) - wallpaperWidth)) / (((float) (count + INVALID_SCREEN)) * ((float) width)) : 1.0f;
            }
            if (this.mFirstLayout) {
                scrollTo(this.mScreenCurrent * width, 0);
                this.mScroller.startScroll(0, 0, this.mScreenCurrent * width, 0, 0);
                if (this.mLiveWallpaperSupport) {
                    updateWallpaperOffset((getChildCount() + INVALID_SCREEN) * width);
                }
                this.mFirstLayout = false;
            }
            float w = (float) (getMeasuredWidth() / 3);
            this.maxPreviewWidth = (int) w;
            this.maxPreviewHeight = (int) (((float) getMeasuredHeight()) * (w / ((float) getMeasuredWidth())));
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childLeft = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
        if (!this.mLiveWallpaperSupport) {
            return;
        }
        if (this.mWallpaperScroll) {
            updateWallpaperOffset();
        } else {
            centerWallpaperOffset();
        }
    }

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int focusableScreen;
        if (!this.mLauncher.isApplicationsGridOpen()) {
            Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                return openFolder.requestFocus(direction, previouslyFocusedRect);
            }
            if (this.mNextScreen != INVALID_SCREEN) {
                focusableScreen = this.mNextScreen;
            } else {
                focusableScreen = this.mScreenCurrent;
            }
            getChildAt(focusableScreen).requestFocus(direction, previouslyFocusedRect);
        }
        return false;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (direction == 17) {
            if (getCurrentScreen() > 0) {
                snapToScreen(getCurrentScreen() + INVALID_SCREEN);
                return true;
            }
        } else if (direction == 66 && getCurrentScreen() < getChildCount() + INVALID_SCREEN) {
            snapToScreen(getCurrentScreen() + 1);
            return true;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!this.mLauncher.isApplicationsGridOpen()) {
            Folder openFolder = getOpenFolder();
            if (openFolder == null) {
                getChildAt(this.mScreenCurrent).addFocusables(views, direction);
                if (direction == 17) {
                    if (this.mScreenCurrent > 0) {
                        getChildAt(this.mScreenCurrent + INVALID_SCREEN).addFocusables(views, direction);
                    }
                } else if (direction == 66 && this.mScreenCurrent < getChildCount() + INVALID_SCREEN) {
                    getChildAt(this.mScreenCurrent + 1).addFocusables(views, direction);
                }
            } else {
                openFolder.addFocusables(views, direction);
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mStatus == 3) {
            if (motionEvent.getAction() == 0) {
                expandPreview(motionEvent.getX(), motionEvent.getY());
            }
            return true;
        } else if (this.mLocked || this.mLauncher.isApplicationsGridOpen()) {
            return true;
        } else {
            int action = motionEvent.getAction();
            if (action == 2 && this.mTouchState != 0) {
                return true;
            }
            this.mScaleGestureDetector.onTouchEvent(motionEvent);
            if (this.mScaleGestureDetector.isInProgress()) {
                return false;
            }
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            switch (action) {
                case 0:
                    this.mLastMotionX = x;
                    this.mLastMotionY = y;
                    this.mAllowLongPress = true;
                    this.mTouchState = this.mScroller.isFinished() ? 0 : 1;
                    break;
                case 1:
                case 3:
                    if (!(this.mTouchState == 1 || this.mTouchState == 2 || this.mTouchState == 3)) {
                        if (!((CellLayout) getChildAt(this.mScreenCurrent)).lastDownOnOccupiedCell()) {
                            getLocationOnScreen(this.mTempCell);
                            if (this.mLiveWallpaperSupport) {
                                this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.wallpaper.tap", this.mTempCell[0] + ((int) motionEvent.getX()), this.mTempCell[1] + ((int) motionEvent.getY()), 0, (Bundle) null);
                            }
                        }
                    }
                    clearChildrenCache();
                    this.mTouchState = 0;
                    this.mAllowLongPress = false;
                    break;
                case 2:
                    int xDiff = (int) Math.abs(x - this.mLastMotionX);
                    int yDiff = (int) Math.abs(y - this.mLastMotionY);
                    int touchSlop = this.mTouchSlop;
                    boolean xMoved = xDiff > touchSlop;
                    boolean yMoved = yDiff > touchSlop;
                    if (xMoved || yMoved) {
                        if (xDiff > yDiff) {
                            this.mTouchState = 1;
                            enableChildrenCache();
                        } else if (getOpenFolder() == null) {
                            if (y - this.mLastMotionY > 0.0f) {
                                if (Math.abs(y - this.mLastMotionY) > ((float) (touchSlop * 4)) && PreferencesUtil.getActionBindingForSwipeDown(this.mLauncher) != 1) {
                                    this.mTouchState = 2;
                                }
                            } else if (Math.abs(y - this.mLastMotionY) > ((float) (touchSlop * 4)) && PreferencesUtil.getActionBindingForSwipeUp(this.mLauncher) != 1) {
                                this.mTouchState = 3;
                            }
                        }
                        if (this.mAllowLongPress) {
                            this.mAllowLongPress = false;
                            getChildAt(this.mScreenCurrent).cancelLongPress();
                            break;
                        }
                    }
                    break;
            }
            if (this.mTouchState == 0) {
                return this.mGestureDetector.onTouchEvent(motionEvent);
            }
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public void enableChildrenCache() {
        if (this.mDesktopCache) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (i >= this.mScreenCurrent + INVALID_SCREEN || i <= this.mScreenCurrent + 1) {
                    CellLayout layout = (CellLayout) getChildAt(i);
                    layout.setChildrenDrawnWithCacheEnabled(true);
                    layout.setChildrenDrawingCacheEnabled(true);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void clearChildrenCache() {
        if (this.mDesktopCache) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                ((CellLayout) getChildAt(i)).setChildrenDrawnWithCacheEnabled(false);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mLocked && !this.mLauncher.isApplicationsGridOpen() && !this.mPreviews) {
            if (this.mVelocityTracker == null) {
                this.mVelocityTracker = VelocityTracker.obtain();
            }
            this.mVelocityTracker.addMovement(motionEvent);
            int action = motionEvent.getAction();
            float x = motionEvent.getX();
            switch (action) {
                case 0:
                    if (!this.mScroller.isFinished()) {
                        this.mScroller.abortAnimation();
                    }
                    this.mLastMotionX = x;
                    break;
                case 1:
                    if (this.mTouchState == 1) {
                        VelocityTracker velocityTracker = this.mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                        int velocityX = (int) velocityTracker.getXVelocity();
                        if (velocityX > SNAP_VELOCITY && this.mScreenCurrent > 0) {
                            snapToScreen(this.mScreenCurrent + INVALID_SCREEN);
                        } else if (velocityX >= -500 || this.mScreenCurrent >= getChildCount() + INVALID_SCREEN) {
                            snapToDestination();
                        } else {
                            snapToScreen(this.mScreenCurrent + 1);
                        }
                        if (this.mVelocityTracker != null) {
                            this.mVelocityTracker.recycle();
                            this.mVelocityTracker = null;
                        }
                    } else if (this.mTouchState == 2) {
                        this.mLauncher.fireSwipeDownAction();
                    } else if (this.mTouchState == 3) {
                        this.mLauncher.fireSwipeUpAction();
                    }
                    this.mTouchState = 0;
                    break;
                case 2:
                    if (this.mTouchState == 1) {
                        if (this.mScreenCount != 1) {
                            boolean screenLooping = PreferencesUtil.isScreenLoopingEnabled(this.mLauncher);
                            int deltaX = (int) (this.mLastMotionX - x);
                            this.mLastMotionX = x;
                            if (deltaX >= 0) {
                                if (deltaX > 0) {
                                    if (((getChildAt(getChildCount() + INVALID_SCREEN).getRight() - getScrollX()) - getWidth()) + this.mScrollingBounce <= 0) {
                                        if (screenLooping && this.mScreenCurrent == this.mScreenCount + INVALID_SCREEN) {
                                            if (isElasticScrollingEnabled()) {
                                                this.mEnableOvershootInterpolatorOnScrollFinish = true;
                                                setElasticScrolling(false);
                                            }
                                            snapToScreen(0);
                                            this.mTouchState = 0;
                                            break;
                                        }
                                    } else {
                                        scrollBy(deltaX, 0);
                                        if (this.mLiveWallpaperSupport) {
                                            updateWallpaperOffset();
                                        }
                                        ScreenIndicator screenIndicator = this.mLauncher.getScreenIndicator();
                                        if (screenIndicator != null) {
                                            screenIndicator.indicate(((float) getScrollX()) / ((float) (getChildCount() * getWidth())));
                                            break;
                                        }
                                    }
                                }
                            } else if (getScrollX() <= (-this.mScrollingBounce)) {
                                if (screenLooping && this.mScreenCurrent == 0) {
                                    if (isElasticScrollingEnabled()) {
                                        this.mEnableOvershootInterpolatorOnScrollFinish = true;
                                        setElasticScrolling(false);
                                    }
                                    snapToScreen(this.mScreenCount + INVALID_SCREEN);
                                    this.mTouchState = 0;
                                    break;
                                }
                            } else {
                                scrollBy(Math.min(deltaX, this.mScrollingBounce), 0);
                                if (this.mLiveWallpaperSupport) {
                                    updateWallpaperOffset();
                                }
                                ScreenIndicator screenIndicator2 = this.mLauncher.getScreenIndicator();
                                if (screenIndicator2 != null) {
                                    screenIndicator2.indicate(((float) getScrollX()) / ((float) (getChildCount() * getWidth())));
                                    break;
                                }
                            }
                        } else {
                            this.mTouchState = 0;
                            break;
                        }
                    }
                    break;
                case 3:
                    this.mTouchState = 0;
                    break;
            }
        }
        return true;
    }

    public boolean onDoubleTap(MotionEvent motionEvent) {
        this.mLauncher.onDoubleTap(motionEvent);
        return true;
    }

    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float f1, float f2) {
        return false;
    }

    public void onLongPress(MotionEvent motionEvent) {
    }

    public boolean onScroll(MotionEvent motionEvent1, MotionEvent motionEvent2, float f1, float f2) {
        return false;
    }

    public void onShowPress(MotionEvent motionEvent) {
    }

    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        this.mCurrentSpan = scaleGestureDetector.getCurrentSpan();
        return true;
    }

    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        if (scaleGestureDetector.getTimeDelta() > 100) {
            this.mScaleGestureDetector = new ScaleGestureDetector(this.mLauncher, this);
            return true;
        } else if (this.mCurrentSpan <= -1.0f) {
            return false;
        } else {
            if (this.mCurrentSpan <= scaleGestureDetector.getCurrentSpan()) {
                return false;
            }
            this.mLauncher.showPreviews(0, this.mScreenCount);
            this.mScaleGestureDetector = new ScaleGestureDetector(this.mLauncher, this);
            this.mCurrentSpan = -1.0f;
            return false;
        }
    }

    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
    }

    private void snapToDestination() {
        int screenWidth = getWidth();
        snapToScreen((getScrollX() + (screenWidth / 2)) / screenWidth);
    }

    private void snapToScreen(int whichScreen) {
        int whichScreen2 = Math.max(0, Math.min(whichScreen, getChildCount() + INVALID_SCREEN));
        clearVacantCache();
        enableChildrenCache();
        this.mNextScreen = whichScreen2;
        View focusedChild = getFocusedChild();
        if (!(focusedChild == null || whichScreen2 == this.mScreenCurrent || focusedChild != getChildAt(this.mScreenCurrent))) {
            focusedChild.clearFocus();
        }
        int delta = (whichScreen2 * getWidth()) - getScrollX();
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        awakenScrollBars(this.mAnimationDuration);
        this.mScroller.startScroll(getScrollX(), 0, delta, 0, this.mAnimationDuration);
        invalidate();
    }

    /* access modifiers changed from: package-private */
    public void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;
        if (this.mPreviews || !(this.mStatus == 4 || this.mStatus == 2)) {
            ((CellLayout.LayoutParams) child.getLayoutParams()).isDragging = false;
            return;
        }
        this.mDragInfo = cellInfo;
        this.mDragInfo.screen = this.mScreenCurrent;
        ((CellLayout) getChildAt(this.mScreenCurrent)).onDragChild(child);
        this.mDragger.startDrag(child, this, child.getTag(), 0);
        invalidate();
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.currentScreen = this.mScreenCurrent;
        return state;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        try {
            SavedState savedState = (SavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            if (savedState.currentScreen != INVALID_SCREEN) {
                this.mScreenCurrent = savedState.currentScreen;
                Launcher.setScreen(this.mScreenCurrent);
            }
        } catch (Exception e) {
            super.onRestoreInstanceState((Parcelable) null);
        }
    }

    /* access modifiers changed from: package-private */
    public void addApplicationShortcut(ApplicationItemInfo info, CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
        CellLayout layout = (CellLayout) getChildAt(cellInfo.screen);
        int[] result = new int[2];
        layout.cellToPoint(cellInfo.cellX, cellInfo.cellY, result);
        onDropExternal(result[0], result[1], info, layout, insertAtFirst);
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        CellLayout cellLayout = getCurrentDropLayout();
        if (source != this) {
            onDropExternal(x - xOffset, y - yOffset, dragInfo, cellLayout);
        } else if (this.mDragInfo != null) {
            View cell = this.mDragInfo.cell;
            int index = this.mScroller.isFinished() ? this.mScreenCurrent : this.mNextScreen;
            if (index != this.mDragInfo.screen) {
                ((CellLayout) getChildAt(this.mDragInfo.screen)).removeView(cell);
                cellLayout.addView(cell);
            }
            this.mTargetCell = estimateDropCell(x - xOffset, y - yOffset, this.mDragInfo.spanX, this.mDragInfo.spanY, cell, cellLayout, this.mTargetCell);
            cellLayout.onDropChild(cell, this.mTargetCell);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) cell.getLayoutParams();
            LauncherModel.moveItemInDatabase(this.mLauncher, (ItemInfo) cell.getTag(), -100, index, layoutParams.cellX, layoutParams.cellY);
        }
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        clearVacantCache();
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

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
                view = this.mLauncher.createShortcut(R.layout.application, cellLayout, (ApplicationItemInfo) itemInfo);
                break;
            case 2:
                view = FolderIcon.fromXml(R.layout.folder_icon, this.mLauncher, (ViewGroup) getChildAt(this.mScreenCurrent), (UserFolderInfo) itemInfo);
                break;
            case 3:
                view = LiveFolderIcon.fromXml(R.layout.live_folder_icon, this.mLauncher, (ViewGroup) getChildAt(this.mScreenCurrent), (LiveFolderInfo) itemInfo);
                break;
            case 6:
                view = this.mLauncher.createApplicationsGridItemView((ApplicationsGridItemInfo) itemInfo);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + itemInfo.itemType);
        }
        cellLayout.addView(view, insertAtFirst ? 0 : INVALID_SCREEN);
        view.setOnLongClickListener(this.mLongClickListener);
        this.mTargetCell = estimateDropCell(x, y, 1, 1, view, cellLayout, this.mTargetCell);
        cellLayout.onDropChild(view, this.mTargetCell);
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
        Launcher.getModel().addDesktopItem(itemInfo);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, itemInfo, -100, this.mScreenCurrent, layoutParams.cellX, layoutParams.cellY);
    }

    private CellLayout getCurrentDropLayout() {
        return (CellLayout) getChildAt(this.mScroller.isFinished() ? this.mScreenCurrent : this.mNextScreen);
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        int spanY = 1;
        CellLayout layout = getCurrentDropLayout();
        CellLayout.CellInfo cellInfo = this.mDragInfo;
        int spanX = cellInfo == null ? 1 : cellInfo.spanX;
        if (cellInfo != null) {
            spanY = cellInfo.spanY;
        }
        if (this.mVacantCache == null) {
            this.mVacantCache = layout.findAllVacantCells((boolean[]) null, cellInfo == null ? null : cellInfo.cell);
        }
        return this.mVacantCache.findCellForSpan(this.mTempEstimate, spanX, spanY, false);
    }

    public Rect estimateDropLocation(int x, int y, int xOffset, int yOffset, Rect recycle) {
        CellLayout layout = getCurrentDropLayout();
        CellLayout.CellInfo cellInfo = this.mDragInfo;
        int spanX = cellInfo == null ? 1 : cellInfo.spanX;
        int spanY = cellInfo == null ? 1 : cellInfo.spanY;
        View ignoreView = cellInfo == null ? null : cellInfo.cell;
        Rect location = recycle != null ? recycle : new Rect();
        int[] dropCell = estimateDropCell(x - xOffset, y - yOffset, spanX, spanY, ignoreView, layout, this.mTempCell);
        if (dropCell == null) {
            return null;
        }
        layout.cellToPoint(dropCell[0], dropCell[1], this.mTempEstimate);
        location.left = this.mTempEstimate[0];
        location.top = this.mTempEstimate[1];
        layout.cellToPoint(dropCell[0] + spanX, dropCell[1] + spanY, this.mTempEstimate);
        location.right = this.mTempEstimate[0];
        location.bottom = this.mTempEstimate[1];
        return location;
    }

    private int[] estimateDropCell(int pixelX, int pixelY, int spanX, int spanY, View ignoreView, CellLayout layout, int[] recycle) {
        if (this.mVacantCache == null) {
            this.mVacantCache = layout.findAllVacantCells((boolean[]) null, ignoreView);
        }
        return layout.findNearestVacantArea(pixelX, pixelY, spanX, spanY, this.mVacantCache, recycle);
    }

    /* access modifiers changed from: package-private */
    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
        if (this.mLauncher.getScreenIndicator() != null) {
            this.mLauncher.getScreenIndicator().setItems(this.mScreenCount);
        }
    }

    public void setDragger(DragController dragger) {
        this.mDragger = dragger;
    }

    public void onDropCompleted(View target, boolean success) {
        clearVacantCache();
        if (success) {
            if (!(target == this || this.mDragInfo == null)) {
                ((CellLayout) getChildAt(this.mDragInfo.screen)).removeView(this.mDragInfo.cell);
                Launcher.getModel().removeDesktopItem((ItemInfo) this.mDragInfo.cell.getTag());
            }
        } else if (this.mDragInfo != null) {
            ((CellLayout) getChildAt(this.mDragInfo.screen)).onDropAborted(this.mDragInfo.cell);
        }
        this.mDragInfo = null;
    }

    public void scrollLeft() {
        clearVacantCache();
        if (this.mNextScreen != INVALID_SCREEN) {
            this.mScreenCurrent = this.mNextScreen;
            this.mNextScreen = INVALID_SCREEN;
        }
        if (this.mNextScreen == INVALID_SCREEN && this.mScreenCurrent > 0) {
            snapToScreen(this.mScreenCurrent + INVALID_SCREEN);
        }
    }

    public void scrollRight() {
        clearVacantCache();
        if (this.mNextScreen != INVALID_SCREEN) {
            this.mScreenCurrent = this.mNextScreen;
            this.mNextScreen = INVALID_SCREEN;
        }
        if (this.mNextScreen == INVALID_SCREEN && this.mScreenCurrent < getChildCount() + INVALID_SCREEN) {
            snapToScreen(this.mScreenCurrent + 1);
        }
    }

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

    public Folder getFolderForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
                if (layoutParams.cellHSpan == this.mColumns && layoutParams.cellVSpan == this.mRows && (child instanceof Folder)) {
                    Folder folder = (Folder) child;
                    if (folder.getInfo() == tag) {
                        return folder;
                    }
                }
            }
        }
        return null;
    }

    public View getViewForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    public void unlock() {
        this.mLocked = false;
    }

    public void lock() {
        this.mLocked = true;
    }

    public boolean allowLongPress() {
        return this.mAllowLongPress;
    }

    public void setAllowLongPress(boolean allowLongPress) {
        this.mAllowLongPress = allowLongPress;
    }

    /* access modifiers changed from: package-private */
    public void removeShortcutsForPackage(String packageName) {
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
                    ComponentName name = intent.getComponent();
                    if ("android.intent.action.MAIN".equals(intent.getAction()) && name != null && packageName.equals(name.getPackageName())) {
                        model.removeDesktopItem(info);
                        LauncherModel.deleteItemFromDatabase(this.mLauncher, info);
                        childrenToRemove.add(view);
                    }
                } else if (tag instanceof UserFolderInfo) {
                    ArrayList<ApplicationItemInfo> contents = ((UserFolderInfo) tag).contents;
                    ArrayList arrayList = new ArrayList(1);
                    int contentsCount = contents.size();
                    boolean removedFromFolder = false;
                    for (int k = 0; k < contentsCount; k++) {
                        ApplicationItemInfo appInfo = contents.get(k);
                        Intent intent2 = appInfo.intent;
                        ComponentName name2 = intent2.getComponent();
                        if ("android.intent.action.MAIN".equals(intent2.getAction()) && name2 != null && packageName.equals(name2.getPackageName())) {
                            arrayList.add(appInfo);
                            LauncherModel.deleteItemFromDatabase(this.mLauncher, appInfo);
                            removedFromFolder = true;
                        }
                    }
                    contents.removeAll(arrayList);
                    if (removedFromFolder && (folder = getOpenFolder()) != null) {
                        folder.notifyDataSetChanged();
                    }
                }
            }
            int childCount2 = childrenToRemove.size();
            for (int j2 = 0; j2 < childCount2; j2++) {
                cellLayout.removeViewInLayout(childrenToRemove.get(j2));
            }
            if (childCount2 > 0) {
                cellLayout.requestLayout();
                cellLayout.invalidate();
                this.mLauncher.updateWorkspaceEmptyTip();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void updateShortcutsForPackage(String packageName) {
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
                    ComponentName name = intent.getComponent();
                    if ((applicationItemInfo.itemType == 0 || applicationItemInfo.itemType == 1) && "android.intent.action.MAIN".equals(intent.getAction()) && name != null && packageName.equals(name.getPackageName()) && (icon = Launcher.getModel().getApplicationItemInfoIconOrNull(this.mLauncher.getPackageManager(), applicationItemInfo)) != null && icon != applicationItemInfo.icon) {
                        applicationItemInfo.filtered = true;
                        applicationItemInfo.icon.setCallback((Drawable.Callback) null);
                        applicationItemInfo.icon = Utilities.createIconThumbnail(icon, getContext());
                        ((TextView) view).setCompoundDrawablesWithIntrinsicBounds((Drawable) null, applicationItemInfo.icon, (Drawable) null, (Drawable) null);
                    }
                } else if (itemInfo instanceof UserFolderInfo) {
                    ArrayList<ApplicationItemInfo> applicationItemInfos = ((UserFolderInfo) itemInfo).contents;
                    int applicationItemInfoCount = applicationItemInfos.size();
                    for (int y = 0; y < applicationItemInfoCount; y++) {
                        ApplicationItemInfo applicationItemInfo2 = applicationItemInfos.get(y);
                        Intent intent2 = applicationItemInfo2.intent;
                        ComponentName name2 = intent2.getComponent();
                        if ((applicationItemInfo2.itemType == 0 || applicationItemInfo2.itemType == 1) && "android.intent.action.MAIN".equals(intent2.getAction()) && name2 != null && packageName.equals(name2.getPackageName())) {
                            Drawable icon2 = Launcher.getModel().getApplicationItemInfoIconOrNull(this.mLauncher.getPackageManager(), applicationItemInfo2);
                            boolean folderUpdated = false;
                            if (!(icon2 == null || icon2 == applicationItemInfo2.icon)) {
                                applicationItemInfo2.icon.setCallback((Drawable.Callback) null);
                                applicationItemInfo2.icon = Utilities.createIconThumbnail(icon2, this.mLauncher);
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

    /* access modifiers changed from: package-private */
    public void moveToDefaultScreen() {
        snapToScreen(this.mDefaultScreen);
        getChildAt(this.mDefaultScreen).requestFocus();
    }

    public static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, (SavedState) null);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int currentScreen;

        SavedState(Parcelable superState) {
            super(superState);
            this.currentScreen = Workspace.INVALID_SCREEN;
        }

        /* synthetic */ SavedState(Parcel parcel, SavedState savedState) {
            this(parcel);
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentScreen = Workspace.INVALID_SCREEN;
            this.currentScreen = in.readInt();
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.currentScreen);
        }
    }

    public void setWallpaper(boolean fromIntentReceiver) {
        if (this.mWallpaperManager.getWallpaperInfo() != null || !this.mWallpaperDraw) {
            this.mWallpaperLoaded = false;
            this.mWallpaperDrawable = null;
            this.mLiveWallpaperSupport = true;
        } else {
            if (fromIntentReceiver || this.mWallpaperDrawable == null) {
                try {
                    this.mWallpaperDrawable = (BitmapDrawable) this.mWallpaperManager.getDrawable();
                    this.mWallpaperLoaded = true;
                    this.mLiveWallpaperSupport = false;
                } catch (SecurityException e) {
                    this.mWallpaperDrawable = null;
                    this.mWallpaperLoaded = false;
                    this.mLiveWallpaperSupport = true;
                    Log.w(Launcher.LOG_TAG, "Wallpaper drawable unavailable", e);
                }
            } else {
                this.mLiveWallpaperSupport = false;
            }
        }
        this.mLauncher.setWindowBackground(this.mLiveWallpaperSupport);
        invalidate();
        requestLayout();
    }

    public void setDrawWallpaper(boolean drawWallpaper) {
        this.mWallpaperDraw = drawWallpaper;
        if (!this.mWallpaperDraw || this.mWallpaperManager.getWallpaperInfo() != null) {
            this.mLiveWallpaperSupport = true;
        } else {
            this.mLiveWallpaperSupport = false;
        }
        this.mLauncher.setWindowBackground(this.mLiveWallpaperSupport);
    }

    public void setScrollWallpaper(boolean scrollWallpaper) {
        this.mWallpaperScroll = scrollWallpaper;
        postInvalidate();
    }

    public void setElasticScrolling(boolean enabled) {
        Context context = getContext();
        this.mOvershootInterpolator = new OvershootInterpolator(enabled);
        this.mScroller = new Scroller(context, this.mOvershootInterpolator);
        this.mElasticScrolling = enabled;
    }

    /* access modifiers changed from: package-private */
    public boolean isElasticScrollingEnabled() {
        return this.mElasticScrolling;
    }

    public void togglePreviews(boolean open) {
        this.mScroller.abortAnimation();
        enableChildrenCache();
        if (open) {
            this.mPreviews = true;
            this.isAnimating = true;
            this.mAllowLongPress = true;
            this.mStatus = 1;
            this.mStartTime = 0;
        } else {
            this.mPreviews = true;
            this.isAnimating = true;
            this.mStatus = 2;
            this.mStartTime = 0;
        }
        invalidate();
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int saveCount = canvas.save();
        if (!this.mPreviews) {
            super.drawChild(canvas, child, drawingTime);
        } else if (this.isAnimating || this.mStatus == 3) {
            long currentTime = SystemClock.uptimeMillis() - this.mStartTime;
            Rect rect1 = new Rect(0, 0, child.getWidth(), child.getHeight());
            RectF rect2 = getScaledChild(child);
            float x = 0.0f;
            float y = 0.0f;
            float width = 0.0f;
            float alpha = 255.0f;
            if (this.mStatus == 1) {
                alpha = easeOut((float) currentTime, 0.0f, 100.0f, (float) this.mAnimationDuration);
                x = easeOut((float) currentTime, (float) child.getLeft(), rect2.left, (float) this.mAnimationDuration);
                y = easeOut((float) currentTime, (float) child.getTop(), rect2.top, (float) this.mAnimationDuration);
                width = easeOut((float) currentTime, (float) child.getRight(), rect2.right, (float) this.mAnimationDuration);
                float height = easeOut((float) currentTime, (float) child.getBottom(), rect2.bottom, (float) this.mAnimationDuration);
            } else if (this.mStatus == 2) {
                alpha = easeOut((float) currentTime, 100.0f, 0.0f, (float) this.mAnimationDuration);
                x = easeOut((float) currentTime, rect2.left, (float) child.getLeft(), (float) this.mAnimationDuration);
                y = easeOut((float) currentTime, rect2.top, (float) child.getTop(), (float) this.mAnimationDuration);
                width = easeOut((float) currentTime, rect2.right, (float) child.getRight(), (float) this.mAnimationDuration);
                float height2 = easeOut((float) currentTime, rect2.bottom, (float) child.getBottom(), (float) this.mAnimationDuration);
            } else if (this.mStatus == 3) {
                x = rect2.left;
                y = rect2.top;
                width = rect2.right;
                float height3 = rect2.bottom;
                alpha = 100.0f;
            }
            float scale = (width - x) / ((float) rect1.width());
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            this.mPaint.setAlpha((int) alpha);
            canvas.drawRoundRect(new RectF((float) (rect1.left + 5), (float) (rect1.top + 5), (float) (rect1.right - 5), (float) (rect1.bottom - 5)), 15.0f, 15.0f, this.mPaint);
            this.mPaint.setAlpha(255);
            child.draw(canvas);
        } else {
            child.draw(canvas);
        }
        canvas.restoreToCount(saveCount);
        return true;
    }

    static float easeOut(float time, float begin, float end, float duration) {
        float change = end - begin;
        float time2 = (time / duration) - 1.0f;
        float value = (((time2 * time2 * time2) + 1.0f) * change) + begin;
        if (change > 0.0f && value > end) {
            value = end;
        }
        if (change >= 0.0f || value >= end) {
            return value;
        }
        return end;
    }

    static float easeIn(float time, float begin, float end, float duration) {
        float change = end - begin;
        float time2 = time / duration;
        float value = (change * time2 * time2 * time2) + begin;
        if (change > 0.0f && value > end) {
            value = end;
        }
        if (change >= 0.0f || value >= end) {
            return value;
        }
        return end;
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
        for (int rows = 0; rows < this.mDistro[distroSet].length; rows++) {
            if (this.mDistro[distroSet][rows] > maxItemsPerRow) {
                maxItemsPerRow = this.mDistro[distroSet][rows];
            }
        }
        int childWidth = width / maxItemsPerRow;
        if (childWidth > this.maxPreviewWidth) {
            childWidth = this.maxPreviewWidth;
        }
        int childHeight = Math.round(((float) this.maxPreviewHeight) * (((float) childWidth) / ((float) this.maxPreviewWidth)));
        int topMargin = (height / 2) - ((this.mDistro[distroSet].length * childHeight) / 2);
        for (int rows2 = 0; rows2 < this.mDistro[distroSet].length; rows2++) {
            int leftMargin = (width / 2) - ((this.mDistro[distroSet][rows2] * childWidth) / 2);
            for (int columns = 0; columns < this.mDistro[distroSet][rows2] && childPos <= getChildCount() + INVALID_SCREEN; columns++) {
                if (child == getChildAt(childPos)) {
                    return new RectF((float) (leftMargin + xpos), (float) (topMargin + ypos), (float) (leftMargin + xpos + childWidth), (float) (topMargin + ypos + childHeight));
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
                if (this.mScreenCurrent != i) {
                    this.mLauncher.dismissPreviews();
                    if (isElasticScrollingEnabled()) {
                        setElasticScrolling(false);
                        this.mEnableOvershootInterpolatorOnScrollFinish = true;
                    }
                    snapToScreen(i);
                    postInvalidate();
                } else {
                    this.mLauncher.dismissPreviews();
                }
            }
        }
    }

    public Activity getLauncherActivity() {
        return this.mLauncher;
    }

    public int getCurrentDesktopRows() {
        return this.mRows;
    }

    public int getCurrentDesktopColumns() {
        return this.mColumns;
    }
}
