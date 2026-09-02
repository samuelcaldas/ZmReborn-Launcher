package org.zmreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * View showing a rectangular color preview swatch with alpha grid backing and border.
 */
public class ColourPickerPanelView extends View {
    private static final float BORDER_WIDTH_PX = 1.0f;
    private AlphaPatternDrawable alphaPattern;
    private int borderColor;
    private Paint borderPaint;
    private int color;
    private Paint colorPaint;
    private RectF colorRect;
    private float density;
    private RectF drawingRect;

    /**
     * Constructs a panel view with context.
     */
    public ColourPickerPanelView(Context context) {
        this(context, null);
    }

    /**
     * Constructs a panel view with context and XML attributes.
     */
    public ColourPickerPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a panel view with context, XML attributes, and default style.
     */
    public ColourPickerPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.density = BORDER_WIDTH_PX;
        this.color = 0xFF000000;
        this.borderColor = 0xFF6E6E6E;
        init();
    }

    private void init() {
        this.borderPaint = new Paint();
        this.colorPaint = new Paint();
        this.density = getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.borderPaint.setColor(this.borderColor);
        canvas.drawRect(this.drawingRect, this.borderPaint);
        if (this.alphaPattern != null) {
            this.alphaPattern.draw(canvas);
        }
        this.colorPaint.setColor(this.color);
        canvas.drawRect(this.colorRect, this.colorPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        this.drawingRect = new RectF();
        this.drawingRect.left = getPaddingLeft();
        this.drawingRect.right = width - getPaddingRight();
        this.drawingRect.top = getPaddingTop();
        this.drawingRect.bottom = height - getPaddingBottom();
        setupColorRect();
    }

    private void setupColorRect() {
        RectF dRect = this.drawingRect;
        this.colorRect = new RectF(dRect.left + BORDER_WIDTH_PX, dRect.top + BORDER_WIDTH_PX, dRect.right - BORDER_WIDTH_PX, dRect.bottom - BORDER_WIDTH_PX);
        this.alphaPattern = new AlphaPatternDrawable((int) (5.0f * this.density));
        this.alphaPattern.setBounds(Math.round(this.colorRect.left), Math.round(this.colorRect.top), Math.round(this.colorRect.right), Math.round(this.colorRect.bottom));
    }

    /**
     * Sets the displayed color.
     */
    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    /**
     * Returns the displayed color.
     */
    public int getColor() {
        return this.color;
    }

    /**
     * Sets the panel border color.
     */
    public void setBorderColor(int color) {
        this.borderColor = color;
        invalidate();
    }

    /**
     * Returns the panel border color.
     */
    public int getBorderColor() {
        return this.borderColor;
    }
}
