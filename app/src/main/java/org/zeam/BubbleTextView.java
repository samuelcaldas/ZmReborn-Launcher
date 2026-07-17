package org.zeam;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

public class BubbleTextView extends TextView {
    private static final float PADDING_H = 5.0f;
    private static final float PADDING_V = 1.0f;
    private Drawable mBackground;
    private boolean mBackgroundSizeChanged;
    private float mCornerRadius;
    private float mCustomCornerRadius = 8.0f;
    private float mPaddingH;
    private float mPaddingV;
    private Paint mPaint;
    private final RectF mRect = new RectF();

    public BubbleTextView(Context context) {
        super(context);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        Context context = getContext();
        Resources resources = context.getResources();
        setFocusable(true);
        this.mBackground = SelectorDrawable.createSelector(context, true);
        this.mBackground.setCallback(this);
        setBackgroundDrawable((Drawable) null);
        int color = resources.getColor(R.color.bubble_dark_background);
        this.mPaint = new Paint(1);
        this.mPaint.setColor(color);
        float scale = resources.getDisplayMetrics().density;
        this.mCornerRadius = this.mCustomCornerRadius * scale;
        this.mPaddingH = PADDING_H * scale;
        this.mPaddingV = PADDING_V * scale;
    }

    /* access modifiers changed from: protected */
    public boolean setFrame(int left, int top, int right, int bottom) {
        if (!(getLeft() == left && getRight() == right && getTop() == top && getBottom() == bottom)) {
            this.mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        return who == this.mBackground || super.verifyDrawable(who);
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        Drawable drawable = this.mBackground;
        if (drawable != null && drawable.isStateful()) {
            drawable.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    public void draw(Canvas canvas) {
        Drawable background = this.mBackground;
        if (background != null) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (this.mBackgroundSizeChanged) {
                background.setBounds(0, 0, getRight() - getLeft(), getBottom() - getTop());
                this.mBackgroundSizeChanged = false;
            }
            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
            } else {
                canvas.translate((float) scrollX, (float) scrollY);
                background.draw(canvas);
                canvas.translate((float) (-scrollX), (float) (-scrollY));
            }
        }
        if (getText().length() > 0) {
            Layout layout = getLayout();
            RectF rect = this.mRect;
            int left = getCompoundPaddingLeft();
            int top = getExtendedPaddingTop();
            rect.set((((float) left) + layout.getLineLeft(0)) - this.mPaddingH, ((float) (layout.getLineTop(0) + top)) - this.mPaddingV, Math.min(((float) left) + layout.getLineRight(0) + this.mPaddingH, (float) ((getScrollX() + getRight()) - getLeft())), ((float) (layout.getLineBottom(0) + top)) + this.mPaddingV);
            canvas.drawRoundRect(rect, this.mCornerRadius, this.mCornerRadius, this.mPaint);
        }
        super.draw(canvas);
    }
}
