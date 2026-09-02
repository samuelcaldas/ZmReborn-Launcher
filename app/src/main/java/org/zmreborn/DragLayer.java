package org.zmreborn;

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

/**
 * Top-level container view providing animated drag-and-drop overlays, drop target resolution, and edge scrolling.
 */
public class DragLayer extends FrameLayout implements DragController {
    private static final int ANIMATION_STATE_STARTING = 1;
    private static final int ANIMATION_STATE_RUNNING = 2;
    private static final int ANIMATION_STATE_DONE = 3;
    private static final int ANIMATION_TYPE_SCALE = 1;
    private static final int COLOR_NORMAL = 0x67000000;
    private static final int COLOR_TRASH = 0xAA000000;
    private static final int SCROLL_OUTSIDE_ZONE = 0;
    private static final int SCROLL_WAITING_IN_ZONE = 1;
    private static final int SCROLL_LEFT = 0;
    private static final int SCROLL_RIGHT = 1;
    private static final int ANIMATION_SCALE_UP_DURATION = 110;
    private static final float DRAG_SCALE = 24.0f;
    private static final int SCROLL_DELAY = 600;
    private static final int SCROLL_ZONE = 20;

    private int animationDuration;
    private float animationFrom;
    private long animationStartTime;
    private int animationState = ANIMATION_STATE_DONE;
    private float animationTo;
    private int animationType;
    private int bitmapOffsetX;
    private int bitmapOffsetY;
    private Bitmap dragBitmap;
    private Object dragInfo;
    private final ArrayList<DragController.DragListener> dragListeners = new ArrayList<>();
    private Paint dragPaint;
    private final Rect dragRect = new Rect();
    private RectF dragRegion;
    private DragScroller dragScroller;
    private DragSource dragSource;
    private boolean dragging;
    private int drawHeight;
    private boolean drawModeBitmap = true;
    private int drawWidth;
    private final int[] dropCoordinates = new int[2];
    private final DragCancellationState dragCancellationState = new DragCancellationState();
    private boolean enteredRegion;
    private View ignoredDropTarget;
    private InputMethodManager inputMethodManager;
    private DropTarget lastDropTarget;
    private float lastMotionX;
    private float lastMotionY;
    private int orientation;
    private View originator;
    private DropTarget rejectedDropTarget;
    private final Rect rect = new Rect();
    private Paint rectPaint;
    private final ScrollRunnable scrollRunnable = new ScrollRunnable();
    private int scrollState = SCROLL_OUTSIDE_ZONE;
    private boolean shouldDrop;
    private Rect systemGestureInsets;
    private float touchOffsetX;
    private float touchOffsetY;
    private final Paint trashPaint = new Paint();

    /**
     * Constructs a drag layer with context and XML attributes.
     */
    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.trashPaint.setColorFilter(new PorterDuffColorFilter(
                context.getResources().getColor(R.color.delete_color_filter), PorterDuff.Mode.SRC_ATOP));
        int snagColor = context.getResources().getColor(R.color.snag_callout_color);
        Paint estimatedPaint = new Paint();
        estimatedPaint.setColor(snagColor);
        estimatedPaint.setStrokeWidth(3.0f);
        estimatedPaint.setAntiAlias(true);
        this.rectPaint = new Paint();
        this.rectPaint.setColor(COLOR_NORMAL);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            this.orientation = windowManager.getDefaultDisplay().getOrientation();
        }
    }

    /**
     * Sets system gesture insets to exclude edge areas from conflicts.
     */
    public void setSystemGestureInsets(Rect insets) {
        this.systemGestureInsets = insets;
    }

    @Override
    public void startDrag(View view, DragSource source, Object dragInfo, int dragAction) {
        if (this.inputMethodManager == null) {
            this.inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        if (this.inputMethodManager != null) {
            this.inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
        for (int i = 0; i < this.dragListeners.size(); i++) {
            this.dragListeners.get(i).onDragStart(view, source, dragInfo, dragAction);
        }
        Rect dRect = this.dragRect;
        dRect.set(view.getScrollX(), view.getScrollY(), 0, 0);
        offsetDescendantRectToMyCoords(view, dRect);
        this.touchOffsetX = this.lastMotionX - dRect.left;
        this.touchOffsetY = this.lastMotionY - dRect.top;
        view.clearFocus();
        view.setPressed(false);
        boolean willNotCache = view.willNotCacheDrawing();
        view.setWillNotCacheDrawing(false);
        int color = view.getDrawingCacheBackgroundColor();
        view.setDrawingCacheBackgroundColor(0);
        if (color != 0) {
            view.destroyDrawingCache();
        }
        this.rejectedDropTarget = null;
        this.dragCancellationState.reset();
        view.buildDrawingCache();
        Bitmap viewBitmap = view.getDrawingCache();
        if (viewBitmap != null) {
            setupBitmapDragMode(view, viewBitmap, willNotCache, color);
        } else {
            setupOutlineDragMode(view);
        }
        if (dragAction == DRAG_ACTION_MOVE) {
            view.setVisibility(View.GONE);
        }
        this.dragPaint = null;
        this.dragging = true;
        this.shouldDrop = true;
        this.originator = view;
        this.dragSource = source;
        this.dragInfo = dragInfo;
        this.enteredRegion = false;
        invalidate();
    }

    private void setupBitmapDragMode(View view, Bitmap viewBitmap, boolean willNotCache, int color) {
        this.drawModeBitmap = true;
        int width = viewBitmap.getWidth();
        int height = viewBitmap.getHeight();
        Matrix scale = new Matrix();
        float scaleFactor = view.getWidth();
        float scaleRatio = (DRAG_SCALE + scaleFactor) / scaleFactor;
        scale.setScale(scaleRatio, scaleRatio);
        this.animationTo = 1.0f;
        this.animationFrom = 1.0f / scaleRatio;
        this.animationDuration = ANIMATION_SCALE_UP_DURATION;
        this.animationState = ANIMATION_STATE_STARTING;
        this.animationType = ANIMATION_TYPE_SCALE;
        this.dragBitmap = Bitmap.createBitmap(viewBitmap, 0, 0, width, height, scale, true);
        view.destroyDrawingCache();
        view.setWillNotCacheDrawing(willNotCache);
        view.setDrawingCacheBackgroundColor(color);
        Bitmap bitmap = this.dragBitmap;
        this.bitmapOffsetX = (bitmap.getWidth() - width) / 2;
        this.bitmapOffsetY = (bitmap.getHeight() - height) / 2;
    }

    private void setupOutlineDragMode(View view) {
        this.drawModeBitmap = false;
        int width = view.getWidth();
        int height = view.getHeight();
        float scaleFactor = view.getWidth();
        float scaleRatio = (DRAG_SCALE + scaleFactor) / scaleFactor;
        this.drawWidth = (int) (view.getWidth() * scaleRatio);
        this.drawHeight = (int) (view.getHeight() * scaleRatio);
        this.animationTo = 1.0f;
        this.animationFrom = 1.0f / scaleRatio;
        this.animationDuration = ANIMATION_SCALE_UP_DURATION;
        this.animationState = ANIMATION_STATE_STARTING;
        this.animationType = ANIMATION_TYPE_SCALE;
        this.bitmapOffsetX = (this.drawWidth - width) / 2;
        this.bitmapOffsetY = (this.drawHeight - height) / 2;
    }

    @Override
    public void cancelDrag() {
        this.shouldDrop = false;
        removeCallbacks(this.scrollRunnable);
        this.scrollState = SCROLL_OUTSIDE_ZONE;
        exitLastDropTarget();
        endDrag();
    }

    private void exitLastDropTarget() {
        DropTarget target = this.lastDropTarget;
        if (target == null || !this.dragCancellationState.consumeDropTargetExit()) {
            this.lastDropTarget = null;
            this.dragCancellationState.reset();
            return;
        }
        try {
            target.onDragExit(this.dragSource, this.dropCoordinates[0], this.dropCoordinates[1],
                    (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo);
        } finally {
            this.lastDropTarget = null;
            this.dragCancellationState.reset();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return this.dragging || super.dispatchKeyEvent(event);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!this.dragging) {
            return;
        }
        if (this.animationState == ANIMATION_STATE_STARTING) {
            this.animationStartTime = SystemClock.uptimeMillis();
            this.animationState = ANIMATION_STATE_RUNNING;
        }
        if (this.animationState == ANIMATION_STATE_RUNNING) {
            float normalized = ((float) (SystemClock.uptimeMillis() - this.animationStartTime)) / this.animationDuration;
            if (normalized >= 1.0f) {
                this.animationState = ANIMATION_STATE_DONE;
            }
            float value = this.animationFrom + ((this.animationTo - this.animationFrom) * Math.min(normalized, 1.0f));
            drawAnimatedDrag(canvas, value);
            return;
        }
        drawStaticDrag(canvas);
    }

    private void drawAnimatedDrag(Canvas canvas, float value) {
        if (this.animationType != ANIMATION_TYPE_SCALE) {
            return;
        }
        if (!this.drawModeBitmap || this.dragBitmap == null) {
            canvas.save();
            canvas.translate(((getScrollX() + this.lastMotionX) - this.touchOffsetX) - this.bitmapOffsetX,
                    ((getScrollY() + this.lastMotionY) - this.touchOffsetY) - this.bitmapOffsetY);
            canvas.translate((this.drawWidth * (1.0f - value)) / 2.0f, (this.drawHeight * (1.0f - value)) / 2.0f);
            canvas.drawRoundRect(new RectF(0.0f, 0.0f, this.drawWidth, this.drawHeight), 8.0f, 8.0f, this.rectPaint);
            canvas.restore();
            return;
        }
        Bitmap bitmap = this.dragBitmap;
        canvas.save();
        canvas.translate(((getScrollX() + this.lastMotionX) - this.touchOffsetX) - this.bitmapOffsetX,
                ((getScrollY() + this.lastMotionY) - this.touchOffsetY) - this.bitmapOffsetY);
        canvas.translate((bitmap.getWidth() * (1.0f - value)) / 2.0f, (bitmap.getHeight() * (1.0f - value)) / 2.0f);
        canvas.scale(value, value);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, this.dragPaint);
        canvas.restore();
    }

    private void drawStaticDrag(Canvas canvas) {
        if (!this.drawModeBitmap || this.dragBitmap == null) {
            canvas.save();
            canvas.translate(((getScrollX() + this.lastMotionX) - this.touchOffsetX) - this.bitmapOffsetX,
                    ((getScrollY() + this.lastMotionY) - this.touchOffsetY) - this.bitmapOffsetY);
            canvas.drawRoundRect(new RectF(0.0f, 0.0f, this.drawWidth, this.drawHeight), 8.0f, 8.0f, this.rectPaint);
            canvas.restore();
            return;
        }
        canvas.drawBitmap(this.dragBitmap, ((getScrollX() + this.lastMotionX) - this.touchOffsetX) - this.bitmapOffsetX,
                ((getScrollY() + this.lastMotionY) - this.touchOffsetY) - this.bitmapOffsetY, this.dragPaint);
    }

    private void endDrag() {
        if (!this.dragging) {
            return;
        }
        this.dragging = false;
        if (this.dragBitmap != null) {
            this.dragBitmap.recycle();
        }
        if (this.originator != null) {
            this.originator.setVisibility(View.VISIBLE);
        }
        this.rejectedDropTarget = null;
        for (int i = 0; i < this.dragListeners.size(); i++) {
            this.dragListeners.get(i).onDragEnd();
        }
        this.lastDropTarget = null;
        this.dragCancellationState.reset();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.lastMotionX = x;
                this.lastMotionY = y;
                this.lastDropTarget = null;
                this.dragCancellationState.reset();
                break;
            case MotionEvent.ACTION_UP:
                if (this.shouldDrop) {
                    drop(x, y);
                    this.shouldDrop = false;
                }
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelDrag();
                break;
            default:
                break;
        }
        return this.dragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.dragging) {
            return false;
        }
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(x, y);
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(this.scrollRunnable);
                if (this.shouldDrop) {
                    drop(x, y);
                    this.shouldDrop = false;
                }
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelDrag();
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(ev, x, y);
                break;
            default:
                break;
        }
        return true;
    }

    private void handleTouchDown(float x, float y) {
        this.lastMotionX = x;
        this.lastMotionY = y;
        if (x >= SCROLL_ZONE && x <= (getWidth() - SCROLL_ZONE)) {
            this.scrollState = SCROLL_OUTSIDE_ZONE;
            return;
        }
        this.scrollState = SCROLL_WAITING_IN_ZONE;
        postDelayed(this.scrollRunnable, SCROLL_DELAY);
    }

    private void handleTouchMove(MotionEvent ev, float x, float y) {
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        float touchX = this.touchOffsetX;
        float touchY = this.touchOffsetY;
        int offsetX = this.bitmapOffsetX;
        int offsetY = this.bitmapOffsetY;
        int left = (int) ((((scrollX + this.lastMotionX) - touchX) - offsetX));
        int top = (int) ((((scrollY + this.lastMotionY) - touchY) - offsetY));
        int width = (!this.drawModeBitmap || this.dragBitmap == null) ? this.drawWidth : this.dragBitmap.getWidth();
        int height = (!this.drawModeBitmap || this.dragBitmap == null) ? this.drawHeight : this.dragBitmap.getHeight();
        Rect r = this.rect;
        r.set(left - 1, top - 1, left + width + 1, top + height + 1);
        this.lastMotionX = x;
        this.lastMotionY = y;
        int left2 = (int) ((((scrollX + x) - touchX) - offsetX));
        int top2 = (int) ((((scrollY + y) - touchY) - offsetY));
        r.union(left2 - 1, top2 - 1, left2 + width + 1, top2 + height + 1);
        int[] coordinates = this.dropCoordinates;
        DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates);
        routeDragEvents(dropTarget, coordinates);
        invalidate(r);
        this.lastDropTarget = dropTarget;
        this.dragCancellationState.setDropTargetActive(dropTarget != null);
        boolean inDragRegion = checkDragRegion(ev);
        handleEdgeScrolling(inDragRegion, x, y);
    }

    private void routeDragEvents(DropTarget dropTarget, int[] coordinates) {
        if (dropTarget != null) {
            if (this.lastDropTarget == dropTarget) {
                dropTarget.onDragOver(this.dragSource, coordinates[0], coordinates[1],
                        (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo);
            } else {
                if (this.lastDropTarget != null) {
                    this.lastDropTarget.onDragExit(this.dragSource, coordinates[0], coordinates[1],
                            (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo);
                }
                dropTarget.onDragEnter(this.dragSource, coordinates[0], coordinates[1],
                        (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo);
            }
        } else if (this.lastDropTarget != null) {
            this.lastDropTarget.onDragExit(this.dragSource, coordinates[0], coordinates[1],
                    (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo);
        }
    }

    private boolean checkDragRegion(MotionEvent ev) {
        boolean inDragRegion = false;
        if (this.dragRegion != null) {
            boolean inRegion = this.dragRegion.contains(ev.getRawX(), ev.getRawY());
            if (!this.enteredRegion && inRegion) {
                this.dragPaint = this.trashPaint;
                this.rectPaint.setColor(COLOR_TRASH);
                this.enteredRegion = true;
                inDragRegion = true;
            } else if (this.enteredRegion && !inRegion) {
                this.dragPaint = null;
                this.rectPaint.setColor(COLOR_NORMAL);
                this.enteredRegion = false;
            }
        }
        return inDragRegion;
    }

    private void handleEdgeScrolling(boolean inDragRegion, float x, float y) {
        if (!inDragRegion && x < SCROLL_ZONE) {
            if (this.scrollState == SCROLL_OUTSIDE_ZONE && (this.orientation != 0 || y <= getDockY())) {
                this.scrollState = SCROLL_WAITING_IN_ZONE;
                this.scrollRunnable.setDirection(SCROLL_LEFT);
                postDelayed(this.scrollRunnable, SCROLL_DELAY);
            }
        } else if (!inDragRegion && x > (getWidth() - SCROLL_ZONE)) {
            if (this.scrollState == SCROLL_OUTSIDE_ZONE && (this.orientation != 0 || y <= getDockY())) {
                this.scrollState = SCROLL_WAITING_IN_ZONE;
                this.scrollRunnable.setDirection(SCROLL_RIGHT);
                postDelayed(this.scrollRunnable, SCROLL_DELAY);
            }
        } else if (this.scrollState == SCROLL_WAITING_IN_ZONE && (this.orientation != 0 || y <= getDockY())) {
            this.scrollState = SCROLL_OUTSIDE_ZONE;
            this.scrollRunnable.setDirection(SCROLL_RIGHT);
            removeCallbacks(this.scrollRunnable);
        }
    }

    private int getDockY() {
        View dock = findViewById(R.id.dock);
        if (dock == null) {
            return getHeight();
        }
        int[] dockLocation = new int[2];
        dock.getLocationOnScreen(dockLocation);
        return dockLocation[1];
    }

    private boolean drop(float x, float y) {
        invalidate();
        int[] coordinates = this.dropCoordinates;
        this.rejectedDropTarget = null;
        DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates);
        if (dropTarget == null) {
            exitLastDropTarget();
            boolean targetFound = this.rejectedDropTarget != null;
            notifyDropCompleted((View) this.rejectedDropTarget, false, targetFound);
            return false;
        }
        if (this.lastDropTarget != null && this.lastDropTarget != dropTarget) {
            exitLastDropTarget();
        }
        dropTarget.onDragExit(this.dragSource, coordinates[0], coordinates[1],
                (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo);
        this.lastDropTarget = null;
        this.dragCancellationState.reset();
        if (dropTarget.acceptDrop(this.dragSource, coordinates[0], coordinates[1],
                (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo)) {
            dropTarget.onDrop(this.dragSource, coordinates[0], coordinates[1],
                    (int) this.touchOffsetX, (int) this.touchOffsetY, this.dragInfo);
            notifyDropCompleted((View) dropTarget, true, true);
            return true;
        }
        notifyDropCompleted((View) dropTarget, false, true);
        return true;
    }

    private void notifyDropCompleted(View target, boolean success, boolean targetFound) {
        if (this.dragSource instanceof DropResultListener) {
            ((DropResultListener) this.dragSource).onDropCompleted(target, success, targetFound);
            return;
        }
        if (this.dragSource != null) {
            this.dragSource.onDropCompleted(target, success);
        }
    }

    /**
     * Finds the drop target under the specified layer coordinates.
     */
    public DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
        return findDropTarget(this, x, y, dropCoordinates);
    }

    private DropTarget findDropTarget(ViewGroup container, int x, int y, int[] dropCoordinates) {
        Rect hitRect = this.dragRect;
        int count = container.getChildCount();
        int scrolledX = x + container.getScrollX();
        int scrolledY = y + container.getScrollY();
        View ignored = this.ignoredDropTarget;
        for (int i = count - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child != ignored) {
                child.getHitRect(hitRect);
                if (hitRect.contains(scrolledX, scrolledY)) {
                    DropTarget target = null;
                    if (child instanceof ViewGroup) {
                        int childX = scrolledX - child.getLeft();
                        int childY = scrolledY - child.getTop();
                        target = findDropTarget((ViewGroup) child, childX, childY, dropCoordinates);
                    }
                    if (target != null) {
                        return target;
                    }
                    if (child instanceof DropTarget) {
                        if (!((DropTarget) child).acceptDrop(this.dragSource, scrolledX - child.getLeft(),
                                scrolledY - child.getTop(), 0, 0, this.dragInfo)) {
                            this.rejectedDropTarget = (DropTarget) child;
                            return null;
                        }
                        dropCoordinates[0] = scrolledX - child.getLeft();
                        dropCoordinates[1] = scrolledY - child.getTop();
                        return (DropTarget) child;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the drag scroller component to receive scroll left/right triggers.
     */
    public void setDragScoller(DragScroller scroller) {
        this.dragScroller = scroller;
    }

    /**
     * Registers a drag listener.
     */
    public void addDragListener(DragController.DragListener listener) {
        this.dragListeners.add(listener);
    }

    /**
     * Unregisters a drag listener.
     */
    public void removeDragListener(DragController.DragListener listener) {
        this.dragListeners.remove(listener);
    }

    void setIgnoredDropTarget(View view) {
        this.ignoredDropTarget = view;
    }

    void setDeleteRegion(RectF region) {
        this.dragRegion = region;
    }

    private class ScrollRunnable implements Runnable {
        private int direction;

        ScrollRunnable() {
        }

        @Override
        public void run() {
            if (DragLayer.this.dragScroller != null) {
                if (this.direction == SCROLL_LEFT) {
                    DragLayer.this.dragScroller.scrollLeft();
                } else {
                    DragLayer.this.dragScroller.scrollRight();
                }
                DragLayer.this.scrollState = SCROLL_OUTSIDE_ZONE;
            }
        }

        void setDirection(int direction) {
            this.direction = direction;
        }
    }
}
