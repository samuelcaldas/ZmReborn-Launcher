package org.zmreborn;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class DotsIndicator extends ViewGroup {
    private int mCurrentItem;
    private int mDotDrawableId;
    private int mTotalItems;

    public DotsIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotsIndicator(Context context) {
        super(context);
        init();
    }

    private void init() {
        setFocusable(false);
        setWillNotDraw(false);
        this.mDotDrawableId = R.drawable.pager_dots;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mTotalItems > 0) {
            createLayout();
        }
    }

    private void updateLayout() {
        for (int i = 0; i < getChildCount(); i++) {
            TransitionDrawable transitionDrawable = (TransitionDrawable) ((ImageView) getChildAt(i)).getDrawable();
            if (i == this.mCurrentItem) {
                transitionDrawable.startTransition(50);
            } else {
                transitionDrawable.resetTransition();
            }
        }
    }

    private void createLayout() {
        detachAllViewsFromParent();
        int dotWidth = getResources().getDrawable(this.mDotDrawableId).getIntrinsicWidth();
        int separation = dotWidth;
        int marginLeft = (getWidth() / 2) - (((this.mTotalItems * dotWidth) / 2) + (((this.mTotalItems - 1) * separation) / 2));
        int marginTop = (getHeight() / 2) - (dotWidth / 2);
        for (int i = 0; i < this.mTotalItems; i++) {
            ImageView dotImageView = new ImageView(getContext());
            TransitionDrawable transitionDrawable = (TransitionDrawable) getResources().getDrawable(this.mDotDrawableId);
            transitionDrawable.setCrossFadeEnabled(true);
            dotImageView.setImageDrawable(transitionDrawable);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, -1);
            dotImageView.setLayoutParams(layoutParams);
            dotImageView.measure(getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(dotWidth, 1073741824), 0, layoutParams.width), getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(dotWidth, 0), 0, layoutParams.height));
            int left = marginLeft + ((dotWidth + separation) * i);
            dotImageView.layout(left, marginTop, left + dotWidth, marginTop + dotWidth);
            addViewInLayout(dotImageView, getChildCount(), layoutParams, true);
            if (i == this.mCurrentItem) {
                ((TransitionDrawable) dotImageView.getDrawable()).startTransition(200);
            }
        }
        postInvalidate();
    }

    /* access modifiers changed from: protected */
    public int getTotalItems() {
        return this.mTotalItems;
    }

    /* access modifiers changed from: protected */
    public void setTotalItems(int totalItems) {
        if (totalItems != this.mTotalItems) {
            this.mTotalItems = totalItems;
            createLayout();
        }
    }

    /* access modifiers changed from: protected */
    public int getCurrentItem() {
        return this.mCurrentItem;
    }

    /* access modifiers changed from: protected */
    public void setCurrentItem(int currentItem) {
        if (currentItem != this.mCurrentItem) {
            this.mCurrentItem = currentItem;
            updateLayout();
        }
    }
}
