package org.zmreborn;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * TextView with a rounded bubble background rendered behind item text.
 */
public class BubbleTextView extends TextView {
    private static final float DEFAULT_PADDING_HORIZONTAL = 5.0f;
    private static final float DEFAULT_PADDING_VERTICAL = 1.0f;
    private static final float DEFAULT_CORNER_RADIUS = 8.0f;

    private Drawable bubbleBackground;
    private boolean backgroundSizeChanged;
    private float cornerRadius;
    private float paddingHorizontal;
    private float paddingVertical;
    private Paint bubblePaint;
    private final RectF bubbleRect = new RectF();

    /**
     * Constructs a BubbleTextView with the provided context.
     */
    public BubbleTextView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructs a BubbleTextView with the provided context and attributes.
     */
    public BubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructs a BubbleTextView with the provided context, attributes, and default style.
     */
    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        Context context = getContext();
        Resources resources = context.getResources();
        setFocusable(true);
        this.bubbleBackground = SelectorDrawable.createSelector(context, true);
        this.bubbleBackground.setCallback(this);
        setBackground(null);
        int color = context.getColor(R.color.bubble_dark_background);
        this.bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.bubblePaint.setColor(color);
        float density = resources.getDisplayMetrics().density;
        this.cornerRadius = DEFAULT_CORNER_RADIUS * density;
        this.paddingHorizontal = DEFAULT_PADDING_HORIZONTAL * density;
        this.paddingVertical = DEFAULT_PADDING_VERTICAL * density;
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            this.backgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == this.bubbleBackground || super.verifyDrawable(who);
    }

    @Override
    protected void drawableStateChanged() {
        Drawable drawable = this.bubbleBackground;
        if (drawable != null && drawable.isStateful()) {
            drawable.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    @Override
    public void draw(Canvas canvas) {
        drawBackgroundSelector(canvas);
        drawTextBubble(canvas);
        super.draw(canvas);
    }

    private void drawBackgroundSelector(Canvas canvas) {
        Drawable background = this.bubbleBackground;
        if (background == null) {
            return;
        }
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        if (this.backgroundSizeChanged) {
            background.setBounds(0, 0, getRight() - getLeft(), getBottom() - getTop());
            this.backgroundSizeChanged = false;
        }
        if ((scrollX | scrollY) == 0) {
            background.draw(canvas);
            return;
        }
        canvas.translate((float) scrollX, (float) scrollY);
        background.draw(canvas);
        canvas.translate((float) (-scrollX), (float) (-scrollY));
    }

    private void drawTextBubble(Canvas canvas) {
        if (getText().length() == 0) {
            return;
        }
        Layout layout = getLayout();
        if (layout == null) {
            return;
        }
        int compoundLeft = getCompoundPaddingLeft();
        int extendedTop = getExtendedPaddingTop();
        float left = compoundLeft + layout.getLineLeft(0) - this.paddingHorizontal;
        float top = layout.getLineTop(0) + extendedTop - this.paddingVertical;
        float rightBound = (getScrollX() + getRight()) - getLeft();
        float right = Math.min(compoundLeft + layout.getLineRight(0) + this.paddingHorizontal, rightBound);
        float bottom = layout.getLineBottom(0) + extendedTop + this.paddingVertical;
        this.bubbleRect.set(left, top, right, bottom);
        canvas.drawRoundRect(this.bubbleRect, this.cornerRadius, this.cornerRadius, this.bubblePaint);
    }
}
