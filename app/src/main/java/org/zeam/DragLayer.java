package org.zeam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import java.util.ArrayList;
import org.zeam.DragController;

public class DragLayer extends FrameLayout implements DragController {
    private static final int ANIMATION_STATE_DONE = 3;
    private static final int ANIMATION_STATE_RUNNING = 2;
    private static final int ANIMATION_STATE_STARTING = 1;
    private static final int ANIMATION_TYPE_SCALE = 1;
    private static final int COLOR_NORMAL = 1727987712;
    private static final int COLOR_TRASH = -1426128896;
    private static final int SCROLL_LEFT = 0;
    private static final int SCROLL_OUTSIDE_ZONE = 0;
    private static final int SCROLL_RIGHT = 1;
    private static final int SCROLL_WAITING_IN_ZONE = 1;
    private static final int sAnimationScaleUpDuration = 110;
    private static final float sDragScale = 24.0f;
    private static final boolean sProfileDrawingDuringDrag = false;
    private static final int sScrollDelay = 600;
    private static final int sScrollZone = 20;
    private int mAnimationDuration;
    private float mAnimationFrom;
    private long mAnimationStartTime;
    private int mAnimationState = 3;
    private float mAnimationTo;
    private int mAnimationType;
    private int mBitmapOffsetX;
    private int mBitmapOffsetY;
    private Bitmap mDragBitmap = null;
    private Object mDragInfo;
    private ArrayList<DragController.DragListener> mDragListeners = new ArrayList<>();
    private Paint mDragPaint;
    private Rect mDragRect = new Rect();
    private RectF mDragRegion;
    /* access modifiers changed from: private */
    public DragScroller mDragScroller;
    private DragSource mDragSource;
    private boolean mDragging = sProfileDrawingDuringDrag;
    private int mDrawHeight;
    private boolean mDrawModeBitmap = true;
    private int mDrawWidth;
    private final int[] mDropCoordinates = new int[2];
    private boolean mEnteredRegion;
    private View mIgnoredDropTarget;
    private InputMethodManager mInputMethodManager;
    private DropTarget mLastDropTarget;
    private float mLastMotionX;
    private float mLastMotionY;
    private int mOrientation;
    private View mOriginator;
    private final Rect mRect = new Rect();
    private Paint mRectPaint;
    private ScrollRunnable mScrollRunnable = new ScrollRunnable();
    /* access modifiers changed from: private */
    public int mScrollState = 0;
    private boolean mShouldDrop;
    private float mTouchOffsetX;
    private float mTouchOffsetY;
    private final Paint mTrashPaint = new Paint();

    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTrashPaint.setColorFilter(new PorterDuffColorFilter(context.getResources().getColor(C0041R.color.delete_color_filter), PorterDuff.Mode.SRC_ATOP));
        int snagColor = context.getResources().getColor(C0041R.color.snag_callout_color);
        Paint estimatedPaint = new Paint();
        estimatedPaint.setColor(snagColor);
        estimatedPaint.setStrokeWidth(3.0f);
        estimatedPaint.setAntiAlias(true);
        this.mRectPaint = new Paint();
        this.mRectPaint.setColor(COLOR_NORMAL);
        this.mOrientation = ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getOrientation();
    }

    public void startDrag(View view, DragSource source, Object dragInfo, int dragAction) {
        if (this.mInputMethodManager == null) {
            this.mInputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        }
        this.mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        for (int i = 0; i < this.mDragListeners.size(); i++) {
            this.mDragListeners.get(i).onDragStart(view, source, dragInfo, dragAction);
        }
        Rect rect = this.mDragRect;
        rect.set(view.getScrollX(), view.getScrollY(), 0, 0);
        offsetDescendantRectToMyCoords(view, rect);
        this.mTouchOffsetX = this.mLastMotionX - ((float) rect.left);
        this.mTouchOffsetY = this.mLastMotionY - ((float) rect.top);
        view.clearFocus();
        view.setPressed(sProfileDrawingDuringDrag);
        boolean willNotCache = view.willNotCacheDrawing();
        view.setWillNotCacheDrawing(sProfileDrawingDuringDrag);
        int color = view.getDrawingCacheBackgroundColor();
        view.setDrawingCacheBackgroundColor(0);
        if (color != 0) {
            view.destroyDrawingCache();
        }
        view.buildDrawingCache();
        Bitmap viewBitmap = view.getDrawingCache();
        if (viewBitmap != null) {
            this.mDrawModeBitmap = true;
            int width = viewBitmap.getWidth();
            int height = viewBitmap.getHeight();
            Matrix scale = new Matrix();
            float scaleFactor = (float) view.getWidth();
            float scaleFactor2 = (sDragScale + scaleFactor) / scaleFactor;
            scale.setScale(scaleFactor2, scaleFactor2);
            this.mAnimationTo = 1.0f;
            this.mAnimationFrom = 1.0f / scaleFactor2;
            this.mAnimationDuration = sAnimationScaleUpDuration;
            this.mAnimationState = 1;
            this.mAnimationType = 1;
            this.mDragBitmap = Bitmap.createBitmap(viewBitmap, 0, 0, width, height, scale, true);
            view.destroyDrawingCache();
            view.setWillNotCacheDrawing(willNotCache);
            view.setDrawingCacheBackgroundColor(color);
            Bitmap dragBitmap = this.mDragBitmap;
            this.mBitmapOffsetX = (dragBitmap.getWidth() - width) / 2;
            this.mBitmapOffsetY = (dragBitmap.getHeight() - height) / 2;
        } else {
            this.mDrawModeBitmap = sProfileDrawingDuringDrag;
            int width2 = view.getWidth();
            int height2 = view.getHeight();
            float scaleFactor3 = (float) view.getWidth();
            float scaleFactor4 = (sDragScale + scaleFactor3) / scaleFactor3;
            this.mDrawWidth = (int) (((float) view.getWidth()) * scaleFactor4);
            this.mDrawHeight = (int) (((float) view.getHeight()) * scaleFactor4);
            this.mAnimationTo = 1.0f;
            this.mAnimationFrom = 1.0f / scaleFactor4;
            this.mAnimationDuration = sAnimationScaleUpDuration;
            this.mAnimationState = 1;
            this.mAnimationType = 1;
            this.mBitmapOffsetX = (this.mDrawWidth - width2) / 2;
            this.mBitmapOffsetY = (this.mDrawHeight - height2) / 2;
        }
        if (dragAction == 0) {
            view.setVisibility(8);
        }
        this.mDragPaint = null;
        this.mDragging = true;
        this.mShouldDrop = true;
        this.mOriginator = view;
        this.mDragSource = source;
        this.mDragInfo = dragInfo;
        this.mEnteredRegion = sProfileDrawingDuringDrag;
        invalidate();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mDragging || super.dispatchKeyEvent(event)) {
            return true;
        }
        return sProfileDrawingDuringDrag;
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mDragging) {
            if (this.mAnimationState == 1) {
                this.mAnimationStartTime = SystemClock.uptimeMillis();
                this.mAnimationState = 2;
            }
            if (this.mAnimationState == 2) {
                float normalized = ((float) (SystemClock.uptimeMillis() - this.mAnimationStartTime)) / ((float) this.mAnimationDuration);
                if (normalized >= 1.0f) {
                    this.mAnimationState = 3;
                }
                float value = this.mAnimationFrom + ((this.mAnimationTo - this.mAnimationFrom) * Math.min(normalized, 1.0f));
                switch (this.mAnimationType) {
                    case 1:
                        if (!this.mDrawModeBitmap || this.mDragBitmap == null) {
                            canvas.save();
                            canvas.translate(((((float) getScrollX()) + this.mLastMotionX) - this.mTouchOffsetX) - ((float) this.mBitmapOffsetX), ((((float) getScrollY()) + this.mLastMotionY) - this.mTouchOffsetY) - ((float) this.mBitmapOffsetY));
                            canvas.translate((((float) this.mDrawWidth) * (1.0f - value)) / 2.0f, (((float) this.mDrawHeight) * (1.0f - value)) / 2.0f);
                            canvas.drawRoundRect(new RectF(0.0f, 0.0f, (float) this.mDrawWidth, (float) this.mDrawHeight), 8.0f, 8.0f, this.mRectPaint);
                            canvas.restore();
                            return;
                        }
                        Bitmap dragBitmap = this.mDragBitmap;
                        canvas.save();
                        canvas.translate(((((float) getScrollX()) + this.mLastMotionX) - this.mTouchOffsetX) - ((float) this.mBitmapOffsetX), ((((float) getScrollY()) + this.mLastMotionY) - this.mTouchOffsetY) - ((float) this.mBitmapOffsetY));
                        canvas.translate((((float) dragBitmap.getWidth()) * (1.0f - value)) / 2.0f, (((float) dragBitmap.getHeight()) * (1.0f - value)) / 2.0f);
                        canvas.scale(value, value);
                        canvas.drawBitmap(dragBitmap, 0.0f, 0.0f, this.mDragPaint);
                        canvas.restore();
                        return;
                    default:
                        return;
                }
            } else if (!this.mDrawModeBitmap || this.mDragBitmap == null) {
                canvas.save();
                canvas.translate(((((float) getScrollX()) + this.mLastMotionX) - this.mTouchOffsetX) - ((float) this.mBitmapOffsetX), ((((float) getScrollY()) + this.mLastMotionY) - this.mTouchOffsetY) - ((float) this.mBitmapOffsetY));
                canvas.drawRoundRect(new RectF(0.0f, 0.0f, (float) this.mDrawWidth, (float) this.mDrawHeight), 8.0f, 8.0f, this.mRectPaint);
                canvas.restore();
            } else {
                canvas.drawBitmap(this.mDragBitmap, ((((float) getScrollX()) + this.mLastMotionX) - this.mTouchOffsetX) - ((float) this.mBitmapOffsetX), ((((float) getScrollY()) + this.mLastMotionY) - this.mTouchOffsetY) - ((float) this.mBitmapOffsetY), this.mDragPaint);
            }
        }
    }

    private void endDrag() {
        if (this.mDragging) {
            this.mDragging = sProfileDrawingDuringDrag;
            if (this.mDragBitmap != null) {
                this.mDragBitmap.recycle();
            }
            if (this.mOriginator != null) {
                this.mOriginator.setVisibility(0);
            }
            for (int i = 0; i < this.mDragListeners.size(); i++) {
                this.mDragListeners.get(i).onDragEnd();
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();
        switch (action) {
            case 0:
                this.mLastMotionX = x;
                this.mLastMotionY = y;
                this.mLastDropTarget = null;
                break;
            case 1:
            case 3:
                if (this.mShouldDrop && drop(x, y)) {
                    this.mShouldDrop = sProfileDrawingDuringDrag;
                }
                endDrag();
                break;
        }
        return this.mDragging;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int width;
        int height;
        if (!this.mDragging) {
            return sProfileDrawingDuringDrag;
        }
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();
        switch (action) {
            case 0:
                this.mLastMotionX = x;
                this.mLastMotionY = y;
                if (x >= 20.0f && x <= ((float) (getWidth() - 20))) {
                    this.mScrollState = 0;
                    break;
                } else {
                    this.mScrollState = 1;
                    postDelayed(this.mScrollRunnable, 600);
                    break;
                }
            case 1:
                removeCallbacks(this.mScrollRunnable);
                if (this.mShouldDrop) {
                    drop(x, y);
                    this.mShouldDrop = sProfileDrawingDuringDrag;
                }
                endDrag();
                break;
            case 2:
                int scrollX = getScrollX();
                int scrollY = getScrollY();
                float touchX = this.mTouchOffsetX;
                float touchY = this.mTouchOffsetY;
                int offsetX = this.mBitmapOffsetX;
                int offsetY = this.mBitmapOffsetY;
                int left = (int) (((((float) scrollX) + this.mLastMotionX) - touchX) - ((float) offsetX));
                int top = (int) (((((float) scrollY) + this.mLastMotionY) - touchY) - ((float) offsetY));
                if (!this.mDrawModeBitmap || this.mDragBitmap == null) {
                    width = this.mDrawWidth;
                    height = this.mDrawHeight;
                } else {
                    Bitmap dragBitmap = this.mDragBitmap;
                    width = dragBitmap.getWidth();
                    height = dragBitmap.getHeight();
                }
                Rect rect = this.mRect;
                rect.set(left - 1, top - 1, left + width + 1, top + height + 1);
                this.mLastMotionX = x;
                this.mLastMotionY = y;
                int left2 = (int) (((((float) scrollX) + x) - touchX) - ((float) offsetX));
                int top2 = (int) (((((float) scrollY) + y) - touchY) - ((float) offsetY));
                rect.union(left2 - 1, top2 - 1, left2 + width + 1, top2 + height + 1);
                int[] coordinates = this.mDropCoordinates;
                DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates);
                if (dropTarget != null) {
                    if (this.mLastDropTarget == dropTarget) {
                        dropTarget.onDragOver(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragInfo);
                    } else {
                        if (this.mLastDropTarget != null) {
                            this.mLastDropTarget.onDragExit(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragInfo);
                        }
                        dropTarget.onDragEnter(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragInfo);
                    }
                } else if (this.mLastDropTarget != null) {
                    this.mLastDropTarget.onDragExit(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragInfo);
                }
                invalidate(rect);
                this.mLastDropTarget = dropTarget;
                boolean inDragRegion = sProfileDrawingDuringDrag;
                if (this.mDragRegion != null) {
                    boolean inRegion = this.mDragRegion.contains(ev.getRawX(), ev.getRawY());
                    if (!this.mEnteredRegion && inRegion) {
                        this.mDragPaint = this.mTrashPaint;
                        this.mRectPaint.setColor(COLOR_TRASH);
                        this.mEnteredRegion = true;
                        inDragRegion = true;
                    } else if (this.mEnteredRegion && !inRegion) {
                        this.mDragPaint = null;
                        this.mRectPaint.setColor(COLOR_NORMAL);
                        this.mEnteredRegion = sProfileDrawingDuringDrag;
                    }
                }
                if (!inDragRegion && x < 20.0f) {
                    if (this.mScrollState == 0 && (this.mOrientation != 0 || y <= ((float) getDockY()))) {
                        this.mScrollState = 1;
                        this.mScrollRunnable.setDirection(0);
                        postDelayed(this.mScrollRunnable, 600);
                        break;
                    }
                } else if (!inDragRegion && x > ((float) (getWidth() - 20))) {
                    if (this.mScrollState == 0 && (this.mOrientation != 0 || y <= ((float) getDockY()))) {
                        this.mScrollState = 1;
                        this.mScrollRunnable.setDirection(1);
                        postDelayed(this.mScrollRunnable, 600);
                        break;
                    }
                } else if (this.mScrollState == 1 && (this.mOrientation != 0 || y <= ((float) getDockY()))) {
                    this.mScrollState = 0;
                    this.mScrollRunnable.setDirection(1);
                    removeCallbacks(this.mScrollRunnable);
                    break;
                }
                break;
            case 3:
                endDrag();
                break;
        }
        return true;
    }

    private int getDockY() {
        int[] dockLocation = new int[2];
        findViewById(C0041R.C0042id.dock).getLocationOnScreen(dockLocation);
        return dockLocation[1];
    }

    private boolean drop(float x, float y) {
        invalidate();
        int[] coordinates = this.mDropCoordinates;
        DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates);
        if (dropTarget == null) {
            return sProfileDrawingDuringDrag;
        }
        dropTarget.onDragExit(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragInfo);
        if (dropTarget.acceptDrop(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragInfo)) {
            dropTarget.onDrop(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragInfo);
            this.mDragSource.onDropCompleted((View) dropTarget, true);
            return true;
        }
        this.mDragSource.onDropCompleted((View) dropTarget, sProfileDrawingDuringDrag);
        return true;
    }

    /* access modifiers changed from: package-private */
    public DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
        return findDropTarget(this, x, y, dropCoordinates);
    }

    private DropTarget findDropTarget(ViewGroup container, int x, int y, int[] dropCoordinates) {
        Rect rect = this.mDragRect;
        int count = container.getChildCount();
        int scrolledX = x + container.getScrollX();
        int scrolledY = y + container.getScrollY();
        View ignoredDropTarget = this.mIgnoredDropTarget;
        for (int i = count - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if (child.getVisibility() == 0 && child != ignoredDropTarget) {
                child.getHitRect(rect);
                if (rect.contains(scrolledX, scrolledY)) {
                    DropTarget target = null;
                    if (child instanceof ViewGroup) {
                        x = scrolledX - child.getLeft();
                        y = scrolledY - child.getTop();
                        target = findDropTarget((ViewGroup) child, x, y, dropCoordinates);
                    }
                    if (target != null) {
                        return target;
                    }
                    if (child instanceof DropTarget) {
                        if (!((DropTarget) child).acceptDrop(this.mDragSource, x, y, 0, 0, this.mDragInfo)) {
                            return null;
                        }
                        dropCoordinates[0] = x;
                        dropCoordinates[1] = y;
                        return (DropTarget) child;
                    }
                } else {
                    continue;
                }
            }
        }
        return null;
    }

    public void setDragScoller(DragScroller scroller) {
        this.mDragScroller = scroller;
    }

    public void addDragListener(DragController.DragListener l) {
        this.mDragListeners.add(l);
    }

    public void removeDragListener(DragController.DragListener l) {
        this.mDragListeners.remove(l);
    }

    /* access modifiers changed from: package-private */
    public void setIgnoredDropTarget(View view) {
        this.mIgnoredDropTarget = view;
    }

    /* access modifiers changed from: package-private */
    public void setDeleteRegion(RectF region) {
        this.mDragRegion = region;
    }

    private class ScrollRunnable implements Runnable {
        private int mDirection;

        ScrollRunnable() {
        }

        public void run() {
            if (DragLayer.this.mDragScroller != null) {
                if (this.mDirection == 0) {
                    DragLayer.this.mDragScroller.scrollLeft();
                } else {
                    DragLayer.this.mDragScroller.scrollRight();
                }
                DragLayer.this.mScrollState = 0;
            }
        }

        /* access modifiers changed from: package-private */
        public void setDirection(int direction) {
            this.mDirection = direction;
        }
    }
}
