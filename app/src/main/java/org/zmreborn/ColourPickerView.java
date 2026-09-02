package org.zmreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Custom interactive view for selecting HSV hue, saturation, value, and optional alpha channels.
 */
public class ColourPickerView extends View {
    private static final float BORDER_WIDTH_PX = 1.0f;
    private static final int PANEL_SAT_VAL = 0;
    private static final int PANEL_HUE = 1;
    private static final int PANEL_ALPHA = 2;

    private float alphaPanelHeight = 20.0f;
    private float huePanelWidth = 30.0f;
    private float paletteCircleTrackerRadius = 5.0f;
    private float panelSpacing = 10.0f;
    private float rectangleTrackerOffset = 2.0f;

    private int alpha = 255;
    private Paint alphaPaint;
    private AlphaPatternDrawable alphaPattern;
    private RectF alphaRect;
    private Shader alphaShader;
    private String alphaSliderText = "";
    private Paint alphaTextPaint;
    private int borderColor = 0xFF6E6E6E;
    private Paint borderPaint;
    private float density = BORDER_WIDTH_PX;
    private float drawingOffset;
    private RectF drawingRect;
    private float hue = 360.0f;
    private Paint huePaint;
    private RectF hueRect;
    private Shader hueShader;
    private Paint hueTrackerPaint;
    private int lastTouchedPanel = PANEL_SAT_VAL;
    private OnColourChangedListener listener;
    private float saturation = 0.0f;
    private Shader saturationShader;
    private Paint saturationValuePaint;
    private RectF saturationValueRect;
    private Paint saturationValueTrackerPaint;
    private boolean showAlphaPanel;
    private int sliderTrackerColor = 0xFF1C1B1F;
    private Point startTouchPoint;
    private float value = 0.0f;
    private Shader valueShader;

    /**
     * Listener interface for color changes.
     */
    public interface OnColourChangedListener {
        void onColorChanged(int color);
    }

    /**
     * Constructs a color picker view with context.
     */
    public ColourPickerView(Context context) {
        this(context, null);
    }

    /**
     * Constructs a color picker view with context and XML attributes.
     */
    public ColourPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a color picker view with context, XML attributes, and default style.
     */
    public ColourPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.density = getContext().getResources().getDisplayMetrics().density;
        this.paletteCircleTrackerRadius *= this.density;
        this.rectangleTrackerOffset *= this.density;
        this.huePanelWidth *= this.density;
        this.alphaPanelHeight *= this.density;
        this.panelSpacing *= this.density;
        this.drawingOffset = calculateRequiredOffset();
        initPaintTools();
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void initPaintTools() {
        this.saturationValuePaint = new Paint();
        this.saturationValueTrackerPaint = new Paint();
        this.huePaint = new Paint();
        this.hueTrackerPaint = new Paint();
        this.alphaPaint = new Paint();
        this.alphaTextPaint = new Paint();
        this.borderPaint = new Paint();

        this.saturationValueTrackerPaint.setStyle(Paint.Style.STROKE);
        this.saturationValueTrackerPaint.setStrokeWidth(this.density * 2.0f);
        this.saturationValueTrackerPaint.setAntiAlias(true);

        this.hueTrackerPaint.setColor(this.sliderTrackerColor);
        this.hueTrackerPaint.setStyle(Paint.Style.STROKE);
        this.hueTrackerPaint.setStrokeWidth(this.density * 2.0f);
        this.hueTrackerPaint.setAntiAlias(true);

        this.alphaTextPaint.setColor(0xFF1C1B1F);
        this.alphaTextPaint.setTextSize(getResources().getDimension(R.dimen.text_size_body));
        this.alphaTextPaint.setAntiAlias(true);
        this.alphaTextPaint.setTextAlign(Paint.Align.CENTER);
        this.alphaTextPaint.setFakeBoldText(true);
    }

    private float calculateRequiredOffset() {
        return 1.5f * Math.max(Math.max(this.paletteCircleTrackerRadius, this.rectangleTrackerOffset), BORDER_WIDTH_PX * this.density);
    }

    private int[] buildHueColorArray() {
        int[] hues = new int[361];
        int count = 0;
        int index = hues.length - 1;
        while (index >= 0) {
            hues[count] = Color.HSVToColor(new float[]{(float) index, 1.0f, 1.0f});
            index--;
            count++;
        }
        return hues;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.drawingRect == null || this.drawingRect.width() <= 0.0f || this.drawingRect.height() <= 0.0f) {
            return;
        }
        drawSatValPanel(canvas);
        drawHuePanel(canvas);
        drawAlphaPanel(canvas);
    }

    private void drawSatValPanel(Canvas canvas) {
        RectF rect = this.saturationValueRect;
        this.borderPaint.setColor(this.borderColor);
        canvas.drawRect(this.drawingRect.left, this.drawingRect.top, BORDER_WIDTH_PX + rect.right, BORDER_WIDTH_PX + rect.bottom, this.borderPaint);
        if (this.valueShader == null) {
            this.valueShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, -1, 0xFF000000, Shader.TileMode.CLAMP);
        }
        this.saturationShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, -1, Color.HSVToColor(new float[]{this.hue, 1.0f, 1.0f}), Shader.TileMode.CLAMP);
        this.saturationValuePaint.setShader(new ComposeShader(this.valueShader, this.saturationShader, PorterDuff.Mode.MULTIPLY));
        canvas.drawRect(rect, this.saturationValuePaint);
        Point point = satValToPoint(this.saturation, this.value);
        this.saturationValueTrackerPaint.setColor(0xFF000000);
        canvas.drawCircle((float) point.x, (float) point.y, this.paletteCircleTrackerRadius - (BORDER_WIDTH_PX * this.density), this.saturationValueTrackerPaint);
        this.saturationValueTrackerPaint.setColor(0xFFDDDDDD);
        canvas.drawCircle((float) point.x, (float) point.y, this.paletteCircleTrackerRadius, this.saturationValueTrackerPaint);
    }

    private void drawHuePanel(Canvas canvas) {
        RectF rect = this.hueRect;
        this.borderPaint.setColor(this.borderColor);
        canvas.drawRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX, BORDER_WIDTH_PX + rect.bottom, this.borderPaint);
        if (this.hueShader == null) {
            this.hueShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, buildHueColorArray(), null, Shader.TileMode.CLAMP);
            this.huePaint.setShader(this.hueShader);
        }
        canvas.drawRect(rect, this.huePaint);
        float rectHeight = (4.0f * this.density) / 2.0f;
        Point point = hueToPoint(this.hue);
        RectF tracker = new RectF();
        tracker.left = rect.left - this.rectangleTrackerOffset;
        tracker.right = rect.right + this.rectangleTrackerOffset;
        tracker.top = ((float) point.y) - rectHeight;
        tracker.bottom = ((float) point.y) + rectHeight;
        canvas.drawRoundRect(tracker, 2.0f, 2.0f, this.hueTrackerPaint);
    }

    private void drawAlphaPanel(Canvas canvas) {
        if (!this.showAlphaPanel || this.alphaRect == null || this.alphaPattern == null) {
            return;
        }
        RectF rect = this.alphaRect;
        this.borderPaint.setColor(this.borderColor);
        canvas.drawRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, BORDER_WIDTH_PX + rect.right, BORDER_WIDTH_PX + rect.bottom, this.borderPaint);
        this.alphaPattern.draw(canvas);
        float[] hsv = {this.hue, this.saturation, this.value};
        this.alphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, Color.HSVToColor(hsv), Color.HSVToColor(0, hsv), Shader.TileMode.CLAMP);
        this.alphaPaint.setShader(this.alphaShader);
        canvas.drawRect(rect, this.alphaPaint);
        if (this.alphaSliderText != null && !this.alphaSliderText.isEmpty()) {
            canvas.drawText(this.alphaSliderText, rect.centerX(), rect.centerY() + (4.0f * this.density), this.alphaTextPaint);
        }
        float rectWidth = (4.0f * this.density) / 2.0f;
        Point point = alphaToPoint(this.alpha);
        RectF tracker = new RectF();
        tracker.left = ((float) point.x) - rectWidth;
        tracker.right = ((float) point.x) + rectWidth;
        tracker.top = rect.top - this.rectangleTrackerOffset;
        tracker.bottom = rect.bottom + this.rectangleTrackerOffset;
        canvas.drawRoundRect(tracker, 2.0f, 2.0f, this.hueTrackerPaint);
    }

    private Point hueToPoint(float hueValue) {
        RectF rect = this.hueRect;
        float height = rect.height();
        Point point = new Point();
        point.y = (int) ((height - ((hueValue * height) / 360.0f)) + rect.top);
        point.x = (int) rect.left;
        return point;
    }

    private Point satValToPoint(float satValue, float valValue) {
        RectF rect = this.saturationValueRect;
        float height = rect.height();
        float width = rect.width();
        Point point = new Point();
        point.x = (int) ((satValue * width) + rect.left);
        point.y = (int) (((BORDER_WIDTH_PX - valValue) * height) + rect.top);
        return point;
    }

    private Point alphaToPoint(int alphaValue) {
        RectF rect = this.alphaRect;
        float width = rect.width();
        Point point = new Point();
        point.x = (int) ((width - ((((float) alphaValue) * width) / 255.0f)) + rect.left);
        point.y = (int) rect.top;
        return point;
    }

    private float[] pointToSatVal(float x, float y) {
        RectF rect = this.saturationValueRect;
        float width = rect.width();
        float height = rect.height();
        if (width <= 0.0f || height <= 0.0f) {
            return new float[]{this.saturation, this.value};
        }
        float clampedX = Math.max(0.0f, Math.min(width, x - rect.left));
        float clampedY = Math.max(0.0f, Math.min(height, y - rect.top));
        float[] result = new float[2];
        result[0] = (BORDER_WIDTH_PX / width) * clampedX;
        result[1] = BORDER_WIDTH_PX - ((BORDER_WIDTH_PX / height) * clampedY);
        return result;
    }

    private float pointToHue(float y) {
        RectF rect = this.hueRect;
        float height = rect.height();
        if (height <= 0.0f) {
            return this.hue;
        }
        float clampedY = Math.max(0.0f, Math.min(height, y - rect.top));
        return 360.0f - ((clampedY * 360.0f) / height);
    }

    private int pointToAlpha(int x) {
        RectF rect = this.alphaRect;
        int width = (int) rect.width();
        if (width <= 0) {
            return this.alpha;
        }
        int clampedX = Math.max(0, Math.min(width, x - (int) rect.left));
        return 255 - ((clampedX * 255) / width);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_MOVE) {
            return super.onTrackballEvent(event);
        }
        boolean update = updateFromTrackball(event.getX(), event.getY());
        if (!update) {
            return super.onTrackballEvent(event);
        }
        notifyColorChanged();
        invalidate();
        return true;
    }

    private boolean updateFromTrackball(float deltaX, float deltaY) {
        if (this.lastTouchedPanel == PANEL_SAT_VAL) {
            this.saturation = Math.max(0.0f, Math.min(BORDER_WIDTH_PX, this.saturation + (deltaX / 50.0f)));
            this.value = Math.max(0.0f, Math.min(BORDER_WIDTH_PX, this.value - (deltaY / 50.0f)));
            return true;
        }
        if (this.lastTouchedPanel == PANEL_HUE) {
            this.hue = Math.max(0.0f, Math.min(360.0f, this.hue - (deltaY * 10.0f)));
            return true;
        }
        if (this.lastTouchedPanel == PANEL_ALPHA && this.showAlphaPanel && this.alphaRect != null) {
            this.alpha = Math.max(0, Math.min(255, (int) (((float) this.alpha) - (deltaX * 10.0f))));
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean update = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.startTouchPoint = new Point((int) event.getX(), (int) event.getY());
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_UP:
                this.startTouchPoint = null;
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_MOVE:
                update = moveTrackersIfNeeded(event);
                break;
            default:
                break;
        }
        if (!update) {
            return super.onTouchEvent(event);
        }
        notifyColorChanged();
        invalidate();
        return true;
    }

    private void notifyColorChanged() {
        if (this.listener != null) {
            this.listener.onColorChanged(Color.HSVToColor(this.alpha, new float[]{this.hue, this.saturation, this.value}));
        }
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (this.startTouchPoint == null) {
            return false;
        }
        int startX = this.startTouchPoint.x;
        int startY = this.startTouchPoint.y;
        if (this.hueRect.contains(startX, startY)) {
            this.lastTouchedPanel = PANEL_HUE;
            this.hue = pointToHue(event.getY());
            return true;
        }
        if (this.saturationValueRect.contains(startX, startY)) {
            this.lastTouchedPanel = PANEL_SAT_VAL;
            float[] result = pointToSatVal(event.getX(), event.getY());
            this.saturation = result[0];
            this.value = result[1];
            return true;
        }
        if (this.alphaRect != null && this.alphaRect.contains(startX, startY)) {
            this.lastTouchedPanel = PANEL_ALPHA;
            this.alpha = pointToAlpha((int) event.getX());
            return true;
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthAllowed = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightAllowed = View.MeasureSpec.getSize(heightMeasureSpec);
        int chosenWidth = chooseDimension(widthMode, widthAllowed, getPreferredWidth());
        int chosenHeight = chooseDimension(heightMode, heightAllowed, getPreferredHeight());

        int measuredWidth = chosenWidth;
        int measuredHeight = chosenHeight;
        if (!this.showAlphaPanel) {
            int calculatedHeight = (int) ((chosenWidth - this.panelSpacing) - this.huePanelWidth);
            if (chosenHeight > 0 && calculatedHeight > chosenHeight) {
                measuredHeight = chosenHeight;
                measuredWidth = (int) (chosenHeight + this.panelSpacing + this.huePanelWidth);
            } else {
                measuredHeight = calculatedHeight;
            }
        } else {
            int calculatedWidth = (int) ((chosenHeight - this.alphaPanelHeight) + this.huePanelWidth);
            if (chosenWidth > 0 && calculatedWidth > chosenWidth) {
                measuredHeight = (int) ((chosenWidth - this.huePanelWidth) + this.alphaPanelHeight);
            } else {
                measuredWidth = calculatedWidth;
            }
        }
        setMeasuredDimension(Math.max(1, measuredWidth), Math.max(1, measuredHeight));
    }

    private int chooseDimension(int mode, int size, int preferred) {
        return (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) ? size : preferred;
    }

    private int getPreferredWidth() {
        int width = getPreferredHeight();
        if (this.showAlphaPanel) {
            width = (int) (width - (this.panelSpacing + this.alphaPanelHeight));
        }
        return (int) (width + this.huePanelWidth + this.panelSpacing);
    }

    private int getPreferredHeight() {
        int height = (int) (200.0f * this.density);
        if (this.showAlphaPanel) {
            return (int) (height + this.panelSpacing + this.alphaPanelHeight);
        }
        return height;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        this.drawingRect = new RectF();
        this.drawingRect.left = this.drawingOffset + getPaddingLeft();
        this.drawingRect.right = width - this.drawingOffset - getPaddingRight();
        this.drawingRect.top = this.drawingOffset + getPaddingTop();
        this.drawingRect.bottom = height - this.drawingOffset - getPaddingBottom();
        setUpSatValRect();
        setUpHueRect();
        setUpAlphaRect();
    }

    private void setUpSatValRect() {
        RectF dRect = this.drawingRect;
        float panelSide = dRect.height() - 2.0f;
        if (this.showAlphaPanel) {
            panelSide -= this.panelSpacing + this.alphaPanelHeight;
        }
        panelSide = Math.max(1.0f, panelSide);
        float left = dRect.left + BORDER_WIDTH_PX;
        float top = dRect.top + BORDER_WIDTH_PX;
        this.saturationValueRect = new RectF(left, top, left + panelSide, top + panelSide);
    }

    private void setUpHueRect() {
        RectF dRect = this.drawingRect;
        float bottomOffset = this.showAlphaPanel ? this.panelSpacing + this.alphaPanelHeight : 0.0f;
        this.hueRect = new RectF((dRect.right - this.huePanelWidth) + BORDER_WIDTH_PX,
                dRect.top + BORDER_WIDTH_PX,
                dRect.right - BORDER_WIDTH_PX,
                (dRect.bottom - BORDER_WIDTH_PX) - bottomOffset);
    }

    private void setUpAlphaRect() {
        if (!this.showAlphaPanel) {
            return;
        }
        RectF dRect = this.drawingRect;
        this.alphaRect = new RectF(dRect.left + BORDER_WIDTH_PX, (dRect.bottom - this.alphaPanelHeight) + BORDER_WIDTH_PX, dRect.right - BORDER_WIDTH_PX, dRect.bottom - BORDER_WIDTH_PX);
        this.alphaPattern = new AlphaPatternDrawable((int) (5.0f * this.density));
        this.alphaPattern.setBounds(Math.round(this.alphaRect.left), Math.round(this.alphaRect.top), Math.round(this.alphaRect.right), Math.round(this.alphaRect.bottom));
    }

    /**
     * Registers a listener to be called when the color value changes.
     */
    public void setOnColorChangedListener(OnColourChangedListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the outline border color.
     */
    public void setBorderColor(int color) {
        this.borderColor = color;
        invalidate();
    }

    /**
     * Returns the outline border color.
     */
    public int getBorderColor() {
        return this.borderColor;
    }

    /**
     * Returns the currently selected ARGB color.
     */
    public int getColour() {
        return Color.HSVToColor(this.alpha, new float[]{this.hue, this.saturation, this.value});
    }

    /**
     * Sets the active ARGB color without invoking listener callbacks.
     */
    public void setColor(int color) {
        setColor(color, false);
    }

    /**
     * Sets the active ARGB color, optionally invoking listener callbacks.
     */
    public void setColor(int color, boolean callback) {
        int alphaValue = Color.alpha(color);
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
        this.alpha = alphaValue;
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        if (callback) {
            notifyColorChanged();
        }
        invalidate();
    }

    /**
     * Returns the drawing offset margin.
     */
    public float getDrawingOffset() {
        return this.drawingOffset;
    }

    /**
     * Sets whether the alpha transparency slider panel is visible.
     */
    public void setAlphaSliderVisible(boolean visible) {
        if (this.showAlphaPanel != visible) {
            this.showAlphaPanel = visible;
            this.valueShader = null;
            this.saturationShader = null;
            this.hueShader = null;
            this.alphaShader = null;
            requestLayout();
        }
    }

    /**
     * Sets the slider tracker indicator color.
     */
    public void setSliderTrackerColor(int color) {
        this.sliderTrackerColor = color;
        this.hueTrackerPaint.setColor(this.sliderTrackerColor);
        invalidate();
    }

    /**
     * Returns the slider tracker indicator color.
     */
    public int getSliderTrackerColor() {
        return this.sliderTrackerColor;
    }

    /**
     * Sets the alpha slider text from a string resource.
     */
    public void setAlphaSliderText(int res) {
        setAlphaSliderText(getContext().getString(res));
    }

    /**
     * Sets the alpha slider text.
     */
    public void setAlphaSliderText(String text) {
        this.alphaSliderText = text;
        invalidate();
    }

    /**
     * Returns the alpha slider text.
     */
    public String getAlphaSliderText() {
        return this.alphaSliderText;
    }
}
