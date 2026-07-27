package org.zmreborn;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

public class ScreenIndicator extends ViewGroup implements Animation.AnimationListener {
    public static final int TYPE_DOTS = 1;
    public static final int TYPE_SLIDER_TOP = 2;
    public static final int TYPE_SLIDER_BOTTOM = 3;

    private final Handler mHandler = new Handler();
    private final Runnable mAutoHide = new Runnable() {
        public void run() {
            startHideAnimation();
        }
    };
    private Animation mAnimation;
    private View mIndicator;
    private int mCurrent;
    private int mItems = 5;
    private int mType = TYPE_DOTS;
    private int mVisibleTime = -1;

    public ScreenIndicator(Context context) {
        super(context);
        initIndicator();
    }

    public ScreenIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        initIndicator();
    }

    public ScreenIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initIndicator();
    }

    public void setItems(int items) {
        this.mItems = Math.max(1, items);
        this.mCurrent = clampIndex(this.mCurrent);
        updateIndicator(getCurrentProgress());
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveDimension(getDesiredWidth(widthMeasureSpec), widthMeasureSpec);
        int height = resolveDimension(getDesiredHeight(heightMeasureSpec), heightMeasureSpec);
        setMeasuredDimension(width, height);
        this.mIndicator.measure(exactly(width), exactly(height));
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mIndicator.layout(0, 0, getWidth(), getHeight());
    }

    public void indicate(float percent) {
        float progress = clampProgress(percent);
        this.mCurrent = clampIndex(Math.round(this.mItems * progress));
        updateIndicator(progress);
        revealAndScheduleHide();
    }

    public void fullIndicate(int position) {
        this.mCurrent = clampIndex(position);
        updateIndicator(getCurrentProgress());
        revealAndScheduleHide();
    }

    public void setType(int type) {
        validateType(type);
        if (type == this.mType) {
            applyLayoutParams();
            return;
        }
        removeView(this.mIndicator);
        this.mType = type;
        initIndicator();
    }

    public void setAutoHide(boolean autoHide) {
        this.mVisibleTime = autoHide ? getResources().getInteger(R.integer.duration_long) : -1;
        setVisibility(autoHide ? INVISIBLE : VISIBLE);
    }

    public void onAnimationEnd(Animation animation) {
        setVisibility(INVISIBLE);
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void onAnimationStart(Animation animation) {
    }

    public void hide() {
        this.mHandler.removeCallbacks(this.mAutoHide);
        setVisibility(INVISIBLE);
    }

    public void show() {
        revealAndScheduleHide();
    }

    private void initIndicator() {
        this.mIndicator = createIndicator();
        this.mIndicator.setClickable(false);
        this.mIndicator.setFocusable(false);
        addView(this.mIndicator);
        setClickable(false);
        setFocusable(false);
        applyLayoutParams();
        updateIndicator(getCurrentProgress());
    }

    private View createIndicator() {
        if (this.mType == TYPE_DOTS) {
            return new DotsIndicator(getContext());
        }
        return new SignalRailView(getContext(), isLandscape());
    }

    private void updateIndicator(float progress) {
        if (this.mIndicator instanceof DotsIndicator) {
            updateDots((DotsIndicator) this.mIndicator);
        } else {
            updateRail((SignalRailView) this.mIndicator, progress);
        }
        updateContentDescription();
    }

    private void updateDots(DotsIndicator dots) {
        dots.setTotalItems(this.mItems);
        dots.setCurrentItem(this.mCurrent);
    }

    private void updateRail(SignalRailView rail, float progress) {
        rail.setTotalItems(this.mItems);
        rail.setProgress(progress);
    }

    private void revealAndScheduleHide() {
        clearAnimation();
        setVisibility(VISIBLE);
        this.mHandler.removeCallbacks(this.mAutoHide);
        if (this.mVisibleTime > 0) {
            this.mHandler.postDelayed(this.mAutoHide, this.mVisibleTime);
        }
    }

    private void startHideAnimation() {
        if (this.mAnimation == null) {
            this.mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.screen_indicator_fade_out);
            this.mAnimation.setAnimationListener(this);
        }
        this.mAnimation.reset();
        startAnimation(this.mAnimation);
    }

    private void updateContentDescription() {
        setContentDescription(getResources().getString(
                R.string.accessibility_page_indicator,
                this.mCurrent + 1,
                this.mItems));
    }

    private void applyLayoutParams() {
        FrameLayout.LayoutParams params = createLayoutParams();
        clearMargins(params);
        if (isRailType()) {
            applyRailLayout(params);
        } else {
            applyDotsLayout(params);
        }
        setLayoutParams(params);
    }

    private void applyRailLayout(FrameLayout.LayoutParams params) {
        int activeThickness = dimension(R.dimen.rail_active_thickness);
        int inset = dimension(R.dimen.rail_inset);
        int navigationSize = dimension(R.dimen.navigation_strip_size);
        if (isLandscape()) {
            params.width = activeThickness;
            params.height = LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.END;
            params.rightMargin = navigationSize + inset;
            return;
        }
        params.width = LayoutParams.MATCH_PARENT;
        params.height = activeThickness;
        params.gravity = this.mType == TYPE_SLIDER_TOP ? Gravity.TOP : Gravity.BOTTOM;
        params.topMargin = this.mType == TYPE_SLIDER_TOP ? inset : 0;
        params.bottomMargin = this.mType == TYPE_SLIDER_BOTTOM ? navigationSize + inset : 0;
    }

    private void applyDotsLayout(FrameLayout.LayoutParams params) {
        params.width = LayoutParams.MATCH_PARENT;
        params.height = dimension(R.dimen.screen_indicator_dots_height);
        params.gravity = Gravity.TOP;
    }

    private FrameLayout.LayoutParams createLayoutParams() {
        ViewGroup.LayoutParams current = getLayoutParams();
        if (current == null) {
            return new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        return new FrameLayout.LayoutParams(current);
    }

    private void clearMargins(FrameLayout.LayoutParams params) {
        params.leftMargin = 0;
        params.topMargin = 0;
        params.rightMargin = 0;
        params.bottomMargin = 0;
    }

    private int getDesiredWidth(int measureSpec) {
        if (isRailType() && isLandscape()) {
            return dimension(R.dimen.rail_active_thickness);
        }
        return MeasureSpec.getSize(measureSpec);
    }

    private int getDesiredHeight(int measureSpec) {
        if (this.mType == TYPE_DOTS) {
            return dimension(R.dimen.screen_indicator_dots_height);
        }
        if (isLandscape()) {
            return MeasureSpec.getSize(measureSpec);
        }
        return dimension(R.dimen.rail_active_thickness);
    }

    private int resolveDimension(int desired, int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            return size;
        }
        if (mode == MeasureSpec.AT_MOST) {
            return Math.min(desired, size);
        }
        return desired;
    }

    private int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    private int dimension(int resourceId) {
        return getResources().getDimensionPixelSize(resourceId);
    }

    private boolean isRailType() {
        return this.mType == TYPE_SLIDER_TOP || this.mType == TYPE_SLIDER_BOTTOM;
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(index, this.mItems - 1));
    }

    private float clampProgress(float progress) {
        if (Float.isNaN(progress) || progress < 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, progress);
    }

    private float getCurrentProgress() {
        return ((float) this.mCurrent) / this.mItems;
    }

    private void validateType(int type) {
        if (type != TYPE_DOTS && type != TYPE_SLIDER_TOP && type != TYPE_SLIDER_BOTTOM) {
            throw new IllegalArgumentException("Unknown screen indicator type: " + type);
        }
    }
}
