package org.zmreborn;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import java.util.ArrayList;

public class ViewPager extends HorizontalScrollView {
    private static final int SWIPE_THRESHOLD_VELOCITY = 500;
    private int mCurrentPageIndex;
    private GestureDetector mGestureDetector;
    private OnPageScrollListener mOnPageScrollListener;
    private OnViewportChangedListener mOnViewportChangedListener;
    private LinearLayout mPageViewHolder;

    public static abstract class OnPageScrollListener {
        public abstract void onScroll();
    }

    public interface OnViewportChangedListener {
        void onViewportChanged(int width, int height);
    }

    public ViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ViewPager(Context context) {
        super(context);
        init();
    }

    private void init() {
        setFillViewport(true);
        this.mPageViewHolder = new LinearLayout(getContext());
        this.mPageViewHolder.setOrientation(LinearLayout.HORIZONTAL);
        addView(this.mPageViewHolder, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT));
        this.mGestureDetector = new GestureDetector(getContext(), new PageViewGestureDetector());
        setOnTouchListener(new PageTouchListener());
    }

    public void setOnPageScrollListener(OnPageScrollListener onPageScrollListener) {
        this.mOnPageScrollListener = onPageScrollListener;
    }

    public void setOnViewportChangedListener(OnViewportChangedListener listener) {
        this.mOnViewportChangedListener = listener;
    }

    protected void setPagingViews(ArrayList<View> items) {
        int pageWidth = resolvePageWidth();
        for (View item : items) {
            addPage(item, pageWidth);
        }
        if (items.isEmpty()) {
            resetScroll();
        }
        requestLayout();
    }

    private void addPage(View page, int pageWidth) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                pageWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        this.mPageViewHolder.addView(page, layoutParams);
    }

    private int resolvePageWidth() {
        int pageWidth = getWidth();
        if (pageWidth > 0) {
            return pageWidth;
        }
        return getResources().getDisplayMetrics().widthPixels;
    }

    protected void clearPagingViews() {
        this.mPageViewHolder.removeAllViewsInLayout();
        this.mPageViewHolder.invalidate();
        resetScroll();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int pageWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        if (pageWidth > 0) {
            updatePageWidths(pageWidth);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width <= 0 || (width == oldWidth && height == oldHeight)) {
            return;
        }
        resizePages(width);
        alignCurrentPage();
        if (this.mOnViewportChangedListener != null) {
            this.mOnViewportChangedListener.onViewportChanged(width, height);
        }
    }

    private void resizePages(int pageWidth) {
        if (updatePageWidths(pageWidth)) {
            this.mPageViewHolder.requestLayout();
        }
    }

    private boolean updatePageWidths(int pageWidth) {
        boolean changed = false;
        int pageCount = this.mPageViewHolder.getChildCount();
        for (int index = 0; index < pageCount; index++) {
            View page = this.mPageViewHolder.getChildAt(index);
            changed = updatePageWidth(page, pageWidth) || changed;
        }
        return changed;
    }

    private static boolean updatePageWidth(View page, int pageWidth) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) page.getLayoutParams();
        if (params.width == pageWidth) {
            return false;
        }
        params.width = pageWidth;
        params.height = LinearLayout.LayoutParams.MATCH_PARENT;
        return true;
    }

    private void alignCurrentPage() {
        post(new Runnable() {
            public void run() {
                int pageWidth = getPageWidth();
                if (pageWidth > 0) {
                    scrollTo(mCurrentPageIndex * pageWidth, 0);
                }
            }
        });
    }

    @Override
    protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
        super.onScrollChanged(left, top, oldLeft, oldTop);
        onScrolling();
    }

    @Override
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return true;
    }

    private final class PageTouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (mGestureDetector.onTouchEvent(motionEvent)) {
                return true;
            }
            if (!isRelease(motionEvent)) {
                return false;
            }
            snapToNearestPage();
            return true;
        }
    }

    private static boolean isRelease(MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        return action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
    }

    private void snapToNearestPage() {
        int pageCount = getPageCount();
        int pageWidth = getPageWidth();
        if (pageCount <= 0 || pageWidth <= 0) {
            return;
        }
        int nearestPage = ((pageWidth / 2) + getScrollX()) / pageWidth;
        moveToPage(Math.min(nearestPage, pageCount - 1));
    }

    private final class PageViewGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent firstEvent, MotionEvent secondEvent,
                float velocityX, float velocityY) {
            if (Math.abs(velocityX) <= SWIPE_THRESHOLD_VELOCITY) {
                return false;
            }
            int pageOffset = velocityX < 0.0f ? 1 : -1;
            return moveToPage(mCurrentPageIndex + pageOffset);
        }
    }

    boolean moveToPageForced(int requestedPage) {
        return moveToPage(requestedPage);
    }

    private boolean moveToPage(int requestedPage) {
        int pageCount = getPageCount();
        int pageWidth = getPageWidth();
        if (pageCount <= 0 || pageWidth <= 0) {
            return false;
        }
        this.mCurrentPageIndex = Math.max(0, Math.min(requestedPage, pageCount - 1));
        smoothScrollTo(this.mCurrentPageIndex * pageWidth, 0);
        return true;
    }

    private void onScrolling() {
        if (this.mOnPageScrollListener != null) {
            this.mOnPageScrollListener.onScroll();
        }
    }

    public void resetScroll() {
        this.mCurrentPageIndex = 0;
        scrollTo(0, 0);
    }

    protected int getPageCount() {
        return this.mPageViewHolder.getChildCount();
    }

    protected int getCurrentPageIndex() {
        return this.mCurrentPageIndex;
    }

    protected int getPageWidth() {
        return getWidth();
    }
}
