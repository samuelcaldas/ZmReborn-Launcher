package org.zmreborn;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

/**
 * Screen indicator container supporting pager dots and signal rail slider representations.
 */
public class ScreenIndicator extends ViewGroup implements Animation.AnimationListener {
    public static final int TYPE_DOTS = 1;
    public static final int TYPE_SLIDER_TOP = 2;
    public static final int TYPE_SLIDER_BOTTOM = 3;

    private static final int DEFAULT_ITEM_COUNT = 5;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoHideRunnable = new Runnable() {
        @Override
        public void run() {
            startHideAnimation();
        }
    };
    private Animation fadeAnimation;
    private View indicatorView;
    private int mCurrent;
    private int mItems = DEFAULT_ITEM_COUNT;
    private int indicatorType = TYPE_DOTS;
    private int visibleDurationMs = -1;

    /**
     * Constructs a ScreenIndicator with the provided context.
     */
    public ScreenIndicator(Context context) {
        super(context);
        initIndicator();
    }

    /**
     * Constructs a ScreenIndicator with the provided context and XML attributes.
     */
    public ScreenIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        initIndicator();
    }

    /**
     * Constructs a ScreenIndicator with the provided context, attributes, and style.
     */
    public ScreenIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initIndicator();
    }

    /**
     * Sets the total number of workspace screens/pages.
     */
    public void setItems(int items) {
        this.mItems = Math.max(1, items);
        this.mCurrent = clampIndex(this.mCurrent);
        updateIndicator(getCurrentProgress());
        if (!hasMultipleItems()) {
            hide();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveDimension(getDesiredWidth(widthMeasureSpec), widthMeasureSpec);
        int height = resolveDimension(getDesiredHeight(heightMeasureSpec), heightMeasureSpec);
        setMeasuredDimension(width, height);
        this.indicatorView.measure(exactly(width), exactly(height));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.indicatorView.layout(0, 0, getWidth(), getHeight());
    }

    /**
     * Updates the indicator position based on scrolling percentage [0.0, 1.0].
     */
    public void indicate(float percent) {
        float progress = clampProgress(percent);
        this.mCurrent = clampIndex(Math.round(this.mItems * progress));
        updateIndicator(progress);
        revealAndScheduleHide();
    }

    /**
     * Updates the indicator directly to an exact page position.
     */
    public void fullIndicate(int position) {
        this.mCurrent = clampIndex(position);
        updateIndicator(getCurrentProgress());
        revealAndScheduleHide();
    }

    /**
     * Sets the indicator style type (dots or signal rail).
     */
    public void setType(int type) {
        validateType(type);
        if (type == this.indicatorType) {
            applyLayoutParams();
            return;
        }
        removeView(this.indicatorView);
        this.indicatorType = type;
        initIndicator();
    }

    /**
     * Configures whether the indicator automatically fades out after user inactivity.
     */
    public void setAutoHide(boolean autoHide) {
        this.visibleDurationMs = autoHide ? getResources().getInteger(R.integer.duration_long) : -1;
        setVisibility(autoHide || !hasMultipleItems() ? INVISIBLE : VISIBLE);
    }

    /**
     * Refreshes dynamic colors from the theme palette.
     */
    void refreshPalette() {
        if (this.indicatorView instanceof SignalRailView) {
            ((SignalRailView) this.indicatorView).refreshPalette();
            return;
        }
        this.indicatorView.invalidate();
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        setVisibility(INVISIBLE);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    /**
     * Immediately hides the indicator and cancels pending auto-hide callbacks.
     */
    public void hide() {
        this.handler.removeCallbacks(this.autoHideRunnable);
        setVisibility(INVISIBLE);
    }

    /**
     * Shows the indicator and schedules auto-hide if configured.
     */
    public void show() {
        revealAndScheduleHide();
    }

    private void initIndicator() {
        this.indicatorView = createIndicator();
        this.indicatorView.setClickable(false);
        this.indicatorView.setFocusable(false);
        addView(this.indicatorView);
        setClickable(false);
        setFocusable(false);
        applyLayoutParams();
        updateIndicator(getCurrentProgress());
    }

    private View createIndicator() {
        if (this.indicatorType == TYPE_DOTS) {
            return new DotsIndicator(getContext());
        }
        return new SignalRailView(getContext(), isLandscape());
    }

    private void updateIndicator(float progress) {
        if (this.indicatorView instanceof DotsIndicator) {
            updateDots((DotsIndicator) this.indicatorView);
        } else {
            updateRail((SignalRailView) this.indicatorView, progress);
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
        if (!hasMultipleItems()) {
            hide();
            return;
        }
        clearAnimation();
        setVisibility(VISIBLE);
        this.handler.removeCallbacks(this.autoHideRunnable);
        if (this.visibleDurationMs > 0) {
            this.handler.postDelayed(this.autoHideRunnable, this.visibleDurationMs);
        }
    }

    private void startHideAnimation() {
        if (!hasMultipleItems()) {
            hide();
            return;
        }
        if (this.fadeAnimation == null) {
            this.fadeAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.screen_indicator_fade_out);
            this.fadeAnimation.setAnimationListener(this);
        }
        this.fadeAnimation.reset();
        startAnimation(this.fadeAnimation);
    }

    private void updateContentDescription() {
        if (!hasMultipleItems()) {
            setContentDescription(null);
            return;
        }
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
        params.gravity = this.indicatorType == TYPE_SLIDER_TOP ? Gravity.TOP : Gravity.BOTTOM;
        params.topMargin = this.indicatorType == TYPE_SLIDER_TOP ? inset : 0;
        params.bottomMargin = this.indicatorType == TYPE_SLIDER_BOTTOM ? navigationSize + inset : 0;
    }

    private void applyDotsLayout(FrameLayout.LayoutParams params) {
        params.width = LayoutParams.MATCH_PARENT;
        params.height = dimension(R.dimen.screen_indicator_dots_height);
        params.gravity = Gravity.BOTTOM;
        params.bottomMargin = dimension(R.dimen.navigation_strip_size) + dimension(R.dimen.rail_inset);
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
        if (this.indicatorType == TYPE_DOTS) {
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
        return this.indicatorType == TYPE_SLIDER_TOP || this.indicatorType == TYPE_SLIDER_BOTTOM;
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
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

    private boolean hasMultipleItems() {
        return this.mItems > 1;
    }

    private void validateType(int type) {
        if (type != TYPE_DOTS && type != TYPE_SLIDER_TOP && type != TYPE_SLIDER_BOTTOM) {
            throw new IllegalArgumentException("Unknown screen indicator type: " + type);
        }
    }
}
