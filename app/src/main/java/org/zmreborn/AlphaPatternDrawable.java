package org.zmreborn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Checkerboard alpha pattern drawable for visualizing color transparency.
 */
public class AlphaPatternDrawable extends Drawable {
    private static final int DEFAULT_RECTANGLE_SIZE = 10;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFCBCBCB;

    private Bitmap bitmap;
    private final Paint paint = new Paint();
    private final Paint paintGray = new Paint();
    private final Paint paintWhite = new Paint();
    private final int rectangleSize;
    private int numRectanglesHorizontal;
    private int numRectanglesVertical;

    /**
     * Constructs a checkerboard drawable with default square size.
     */
    public AlphaPatternDrawable() {
        this(DEFAULT_RECTANGLE_SIZE);
    }

    /**
     * Constructs a checkerboard drawable with the specified square size.
     */
    public AlphaPatternDrawable(int rectangleSize) {
        this.rectangleSize = Math.max(1, rectangleSize);
        this.paintWhite.setColor(COLOR_WHITE);
        this.paintGray.setColor(COLOR_GRAY);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.bitmap != null && !this.bitmap.isRecycled()) {
            canvas.drawBitmap(this.bitmap, null, getBounds(), this.paint);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        throw new UnsupportedOperationException("Alpha is not supported by this drawable.");
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        throw new UnsupportedOperationException("ColorFilter is not supported by this drawable.");
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        int width = bounds.width();
        int height = bounds.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        this.numRectanglesHorizontal = (int) Math.ceil((double) width / this.rectangleSize);
        this.numRectanglesVertical = (int) Math.ceil((double) height / this.rectangleSize);
        generatePatternBitmap(width, height);
    }

    private void generatePatternBitmap(int width, int height) {
        this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.bitmap);
        Rect rectangle = new Rect();
        boolean verticalStartWhite = true;
        for (int verticalIndex = 0; verticalIndex <= this.numRectanglesVertical; verticalIndex++) {
            boolean isWhite = verticalStartWhite;
            for (int horizontalIndex = 0; horizontalIndex <= this.numRectanglesHorizontal; horizontalIndex++) {
                rectangle.top = this.rectangleSize * verticalIndex;
                rectangle.left = this.rectangleSize * horizontalIndex;
                rectangle.bottom = rectangle.top + this.rectangleSize;
                rectangle.right = rectangle.left + this.rectangleSize;
                canvas.drawRect(rectangle, isWhite ? this.paintWhite : this.paintGray);
                isWhite = !isWhite;
            }
            verticalStartWhite = !verticalStartWhite;
        }
    }
}
