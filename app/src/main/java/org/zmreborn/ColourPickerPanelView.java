package org.zmreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ColourPickerPanelView extends View {
    private static final float BORDER_WIDTH_PX = 1.0f;
    private AlphaPatternDrawable mAlphaPattern;
    private int mBorderColor;
    private Paint mBorderPaint;
    private int mColor;
    private Paint mColorPaint;
    private RectF mColorRect;
    private float mDensity;
    private RectF mDrawingRect;

    public ColourPickerPanelView(Context context) {
        this(context, (AttributeSet) null);
    }

    public ColourPickerPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColourPickerPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDensity = BORDER_WIDTH_PX;
        this.mColor = -16777216;
        this.mBorderColor = -9539986;
        init();
    }

    private void init() {
        this.mBorderPaint = new Paint();
        this.mColorPaint = new Paint();
        this.mDensity = getContext().getResources().getDisplayMetrics().density;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        RectF rect = this.mColorRect;
        this.mBorderPaint.setColor(this.mBorderColor);
        canvas.drawRect(this.mDrawingRect, this.mBorderPaint);
        if (this.mAlphaPattern != null) {
            this.mAlphaPattern.draw(canvas);
        }
        this.mColorPaint.setColor(this.mColor);
        canvas.drawRect(rect, this.mColorPaint);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mDrawingRect = new RectF();
        this.mDrawingRect.left = (float) getPaddingLeft();
        this.mDrawingRect.right = (float) (w - getPaddingRight());
        this.mDrawingRect.top = (float) getPaddingTop();
        this.mDrawingRect.bottom = (float) (h - getPaddingBottom());
        setupColorRect();
    }

    private void setupColorRect() {
        RectF dRect = this.mDrawingRect;
        this.mColorRect = new RectF(dRect.left + BORDER_WIDTH_PX, dRect.top + BORDER_WIDTH_PX, dRect.right - BORDER_WIDTH_PX, dRect.bottom - BORDER_WIDTH_PX);
        this.mAlphaPattern = new AlphaPatternDrawable((int) (5.0f * this.mDensity));
        this.mAlphaPattern.setBounds(Math.round(this.mColorRect.left), Math.round(this.mColorRect.top), Math.round(this.mColorRect.right), Math.round(this.mColorRect.bottom));
    }

    public void setColor(int color) {
        this.mColor = color;
        invalidate();
    }

    public int getColor() {
        return this.mColor;
    }

    public void setBorderColor(int color) {
        this.mBorderColor = color;
        invalidate();
    }

    public int getBorderColor() {
        return this.mBorderColor;
    }
}
