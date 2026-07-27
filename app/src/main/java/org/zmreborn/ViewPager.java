package org.zmreborn;

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
    private static final int sSwipeThresholdVelocity = 500;
    /* access modifiers changed from: private */
    public int mCurrentPageIndex = 0;
    /* access modifiers changed from: private */
    public GestureDetector mGestureDetector;
    private OnPageScrollListener mOnPageScrollListener;
    /* access modifiers changed from: private */
    public LinearLayout mPageViewHolder;

    public static abstract class OnPageScrollListener {
        public abstract void onScroll();
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
        Context context = getContext();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1);
        this.mPageViewHolder = new LinearLayout(context);
        this.mPageViewHolder.setOrientation(0);
        this.mPageViewHolder.setLayoutParams(layoutParams);
        addView(this.mPageViewHolder);
    }

    public void setOnPageScrollListener(OnPageScrollListener onPageScrollListener) {
        this.mOnPageScrollListener = onPageScrollListener;
    }

    /* access modifiers changed from: protected */
    public void setPagingViews(ArrayList<View> items) {
        LinearLayout pageViewHolder = this.mPageViewHolder;
        for (int i = 0; i < items.size(); i++) {
            pageViewHolder.addView(items.get(i));
        }
        if (items.size() == 0) {
            this.mCurrentPageIndex = 0;
            scrollTo(0, 0);
        }
        setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (ViewPager.this.mGestureDetector.onTouchEvent(motionEvent)) {
                    return true;
                }
                if (motionEvent.getAction() != 1 && motionEvent.getAction() != 3) {
                    return false;
                }
                int pageCount = ViewPager.this.getPageCount();
                if (pageCount <= 0) {
                    return true;
                }
                int scrollX = ViewPager.this.getScrollX();
                int pageViewWidth = view.getMeasuredWidth();
                if (pageViewWidth <= 0) {
                    return true;
                }
                ViewPager.this.mCurrentPageIndex = ((pageViewWidth / 2) + scrollX) / pageViewWidth;
                if (ViewPager.this.mCurrentPageIndex >= pageCount) {
                    ViewPager.this.mCurrentPageIndex = pageCount - 1;
                }
                ViewPager.this.smoothScrollTo(ViewPager.this.mCurrentPageIndex * pageViewWidth, 0);
                return true;
            }
        });
        this.mGestureDetector = new GestureDetector(new PageViewGestureDetector(this, (PageViewGestureDetector) null));
    }

    /* access modifiers changed from: protected */
    public void clearPagingViews() {
        this.mPageViewHolder.removeAllViewsInLayout();
        this.mPageViewHolder.invalidate();
        this.mCurrentPageIndex = 0;
        scrollTo(0, 0);
    }

    /* access modifiers changed from: protected */
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        onScrolling();
    }

    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return true;
    }

    private class PageViewGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private PageViewGestureDetector() {
        }

        /* synthetic */ PageViewGestureDetector(ViewPager viewPager, PageViewGestureDetector pageViewGestureDetector) {
            this();
        }

        public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float velocityX, float velocityY) {
            int i;
            int pageViewCount = ViewPager.this.mPageViewHolder.getChildCount();
            if (pageViewCount <= 0) {
                return false;
            }
            if (velocityX < 0.0f && Math.abs(velocityX) > 500.0f) {
                int pageViewWidth = ViewPager.this.getMeasuredWidth();
                if (pageViewWidth <= 0) {
                    return false;
                }
                ViewPager.this.mCurrentPageIndex = ViewPager.this.mCurrentPageIndex < pageViewCount + -1 ? ViewPager.this.mCurrentPageIndex + 1 : pageViewCount - 1;
                ViewPager.this.smoothScrollTo(ViewPager.this.mCurrentPageIndex * pageViewWidth, 0);
                return true;
            } else if (velocityX <= 0.0f || Math.abs(velocityX) <= 500.0f) {
                return false;
            } else {
                int pageViewWidth2 = ViewPager.this.getMeasuredWidth();
                if (pageViewWidth2 <= 0) {
                    return false;
                }
                ViewPager viewPager = ViewPager.this;
                if (ViewPager.this.mCurrentPageIndex > 0) {
                    i = ViewPager.this.mCurrentPageIndex - 1;
                } else {
                    i = 0;
                }
                viewPager.mCurrentPageIndex = i;
                ViewPager.this.smoothScrollTo(ViewPager.this.mCurrentPageIndex * pageViewWidth2, 0);
                return true;
            }
        }
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

    /* access modifiers changed from: protected */
    public int getPageCount() {
        return this.mPageViewHolder.getChildCount();
    }

    /* access modifiers changed from: protected */
    public int getCurrentPageIndex() {
        return this.mCurrentPageIndex;
    }
}
