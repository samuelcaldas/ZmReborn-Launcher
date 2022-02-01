package org.zeam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class ScreenIndicator extends ViewGroup implements Animation.AnimationListener {
    private static final int DEFAULT_COLOR = -1711276033;
    public static final int TYPE_DOTS = 1;
    public static final int TYPE_SLIDER_BOTTOM = 3;
    public static final int TYPE_SLIDER_TOP = 2;
    /* access modifiers changed from: private */
    public Animation mAnimation;
    private Runnable mAutoHide = new Runnable() {
        public void run() {
            if (ScreenIndicator.this.mAnimation == null) {
                ScreenIndicator.this.mAnimation = AnimationUtils.loadAnimation(ScreenIndicator.this.getContext(), C0041R.anim.screen_indicator_fade_out);
                ScreenIndicator.this.mAnimation.setAnimationListener(ScreenIndicator.this);
            } else {
                try {
                    if (!ScreenIndicator.this.mAnimation.hasEnded()) {
                        ScreenIndicator.this.mAnimation.reset();
                    }
                } catch (NoSuchMethodError e) {
                }
            }
            ScreenIndicator.this.startAnimation(ScreenIndicator.this.mAnimation);
        }
    };
    /* access modifiers changed from: private */
    public int mColor = DEFAULT_COLOR;
    private int mCurrent = 0;
    private Handler mHandler = new Handler();
    private View mIndicator;
    private int mItems = 5;
    private int mType = 1;
    private int mVisibleTime = 300;

    public ScreenIndicator(Context context) {
        super(context);
        initIndicator(context);
    }

    public ScreenIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        initIndicator(context);
    }

    public ScreenIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initIndicator(context);
    }

    private void initIndicator(Context context) {
        switch (this.mType) {
            case 1:
                this.mIndicator = new DotsIndicator(context);
                DotsIndicator dotsIndicator = (DotsIndicator) this.mIndicator;
                dotsIndicator.setCurrentItem(this.mCurrent);
                dotsIndicator.setTotalItems(this.mItems);
                break;
            case 2:
            case 3:
                this.mIndicator = new SliderIndicator(context);
                break;
        }
        addView(this.mIndicator);
        setEnabled(true);
    }

    public void setItems(int items) {
        this.mItems = items;
        switch (this.mType) {
            case 1:
                DotsIndicator dotsIndicator = (DotsIndicator) this.mIndicator;
                dotsIndicator.setCurrentItem(this.mCurrent);
                dotsIndicator.setTotalItems(this.mItems);
                return;
            case 2:
            case 3:
                ((SliderIndicator) this.mIndicator).setTotalItems(this.mItems);
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int realHeight = 4;
        switch (this.mType) {
            case 1:
                realHeight = 20;
                this.mIndicator.measure(getWidth(), 20);
                break;
            case 2:
            case 3:
                this.mIndicator.measure(getWidth(), 4);
                break;
        }
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(realHeight, 1073741824));
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        switch (this.mType) {
            case 1:
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                this.mIndicator.measure(getWidth(), 20);
                this.mIndicator.setLayoutParams(params);
                this.mIndicator.layout(0, 0, getWidth(), 20);
                return;
            case 2:
            case 3:
                LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(-1, -2);
                this.mIndicator.measure(getWidth(), 4);
                this.mIndicator.setLayoutParams(params2);
                this.mIndicator.layout(0, 0, getWidth(), 4);
                return;
            default:
                return;
        }
    }

    public void indicate(float percent) {
        setVisibility(0);
        int position = Math.round(((float) this.mItems) * percent);
        switch (this.mType) {
            case 1:
                ((DotsIndicator) this.mIndicator).setCurrentItem(position);
                break;
            case 2:
            case 3:
                ((SliderIndicator) this.mIndicator).setOffset(((int) (((float) getWidth()) * percent)) - this.mIndicator.getLeft());
                this.mIndicator.invalidate();
                break;
        }
        this.mHandler.removeCallbacks(this.mAutoHide);
        if (this.mVisibleTime > 0) {
            this.mHandler.postDelayed(this.mAutoHide, (long) this.mVisibleTime);
        }
        this.mCurrent = position;
    }

    public void fullIndicate(int position) {
        setVisibility(0);
        switch (this.mType) {
            case 1:
                ((DotsIndicator) this.mIndicator).setCurrentItem(position);
                break;
        }
        this.mHandler.removeCallbacks(this.mAutoHide);
        if (this.mVisibleTime > 0) {
            this.mHandler.postDelayed(this.mAutoHide, (long) this.mVisibleTime);
        }
        this.mCurrent = position;
    }

    /* access modifiers changed from: package-private */
    public void setType(int type) {
        if (type != this.mType) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(getLayoutParams());
            if (type == 3) {
                layoutParams.gravity = 80;
            } else {
                layoutParams.gravity = 48;
            }
            setLayoutParams(layoutParams);
            this.mType = type;
            removeView(this.mIndicator);
            initIndicator(getContext());
        }
    }

    public void setAutoHide(boolean autohide) {
        if (autohide) {
            this.mVisibleTime = 300;
            setVisibility(4);
            return;
        }
        this.mVisibleTime = -1;
        setVisibility(0);
    }

    private class SliderIndicator extends View {
        private static final int INDICATOR_HEIGHT = 4;
        private Paint mPaint = new Paint();
        private RectF mRect;
        private int mTotalItems = 5;

        public SliderIndicator(Context context) {
            super(context);
            this.mPaint.setColor(ScreenIndicator.this.mColor);
            this.mRect = new RectF(0.0f, 0.0f, 5.0f, 4.0f);
        }

        public void draw(Canvas canvas) {
            canvas.drawRoundRect(this.mRect, 2.0f, 2.0f, this.mPaint);
        }

        public void setTotalItems(int items) {
            this.mTotalItems = items;
        }

        public void setOffset(int offset) {
            int width = getWidth() / this.mTotalItems;
            this.mRect.left = (float) offset;
            this.mRect.right = (float) (offset + width);
        }
    }

    public void onAnimationEnd(Animation animation) {
        setVisibility(4);
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void onAnimationStart(Animation animation) {
    }

    public void hide() {
        setVisibility(4);
    }

    public void show() {
        if (this.mVisibleTime < 0) {
            setVisibility(0);
        }
    }
}
