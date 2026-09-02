package org.zmreborn;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Pager dots indicator displaying transitions between active and inactive dot states.
 */
public class DotsIndicator extends ViewGroup {
    private static final int DOT_TRANSITION_SHORT_MS = 50;
    private static final int DOT_TRANSITION_LONG_MS = 200;

    private int currentItem;
    private int dotDrawableId;
    private int totalItems;

    /**
     * Constructs a DotsIndicator with the given context and XML attributes.
     */
    public DotsIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructs a DotsIndicator with the given context.
     */
    public DotsIndicator(Context context) {
        super(context);
        init();
    }

    private void init() {
        setFocusable(false);
        setWillNotDraw(false);
        this.dotDrawableId = R.drawable.pager_dots;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.totalItems > 0) {
            createLayout();
        }
    }

    private void updateLayout() {
        int childCount = getChildCount();
        for (int index = 0; index < childCount; index++) {
            ImageView dotView = (ImageView) getChildAt(index);
            TransitionDrawable transitionDrawable = (TransitionDrawable) dotView.getDrawable();
            if (index == this.currentItem) {
                transitionDrawable.startTransition(DOT_TRANSITION_SHORT_MS);
            } else {
                transitionDrawable.resetTransition();
            }
        }
    }

    private void createLayout() {
        detachAllViewsFromParent();
        Drawable templateDrawable = getContext().getDrawable(this.dotDrawableId);
        if (templateDrawable == null) {
            return;
        }
        int dotWidth = templateDrawable.getIntrinsicWidth();
        int separation = dotWidth;
        int totalDotsWidth = (this.totalItems * dotWidth) + ((this.totalItems - 1) * separation);
        int marginLeft = (getWidth() - totalDotsWidth) / 2;
        int marginTop = (getHeight() - dotWidth) / 2;

        for (int index = 0; index < this.totalItems; index++) {
            ImageView dotImageView = new ImageView(getContext());
            Drawable drawable = getContext().getDrawable(this.dotDrawableId);
            if (drawable instanceof TransitionDrawable) {
                TransitionDrawable transitionDrawable = (TransitionDrawable) drawable;
                transitionDrawable.setCrossFadeEnabled(true);
                dotImageView.setImageDrawable(transitionDrawable);
            }
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dotImageView.setLayoutParams(layoutParams);
            int widthSpec = View.MeasureSpec.makeMeasureSpec(dotWidth, MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(dotWidth, MeasureSpec.EXACTLY);
            dotImageView.measure(widthSpec, heightSpec);
            int left = marginLeft + ((dotWidth + separation) * index);
            dotImageView.layout(left, marginTop, left + dotWidth, marginTop + dotWidth);
            addViewInLayout(dotImageView, getChildCount(), layoutParams, true);
            if (index == this.currentItem && dotImageView.getDrawable() instanceof TransitionDrawable) {
                ((TransitionDrawable) dotImageView.getDrawable()).startTransition(DOT_TRANSITION_LONG_MS);
            }
        }
        postInvalidate();
    }

    /**
     * Returns the total number of page items.
     */
    protected int getTotalItems() {
        return this.totalItems;
    }

    /**
     * Updates the total number of page items and rebuilds dot views.
     */
    protected void setTotalItems(int totalItems) {
        if (totalItems != this.totalItems) {
            this.totalItems = totalItems;
            createLayout();
        }
    }

    /**
     * Returns the currently active item index.
     */
    protected int getCurrentItem() {
        return this.currentItem;
    }

    /**
     * Updates the currently active item index and triggers transition animations.
     */
    protected void setCurrentItem(int currentItem) {
        if (currentItem != this.currentItem) {
            this.currentItem = currentItem;
            updateLayout();
        }
    }
}
