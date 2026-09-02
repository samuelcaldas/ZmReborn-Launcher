package org.zmreborn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * Lightweight, non-scaling bitmap drawable wrapper for launcher thumbnails.
 */
class FastBitmapDrawable extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint;

    FastBitmapDrawable(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap must not be null");
        }
        this.bitmap = bitmap;
        this.paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!this.bitmap.isRecycled()) {
            canvas.drawBitmap(this.bitmap, 0.0f, 0.0f, this.paint);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.bitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this.bitmap.getHeight();
    }

    @Override
    public int getMinimumWidth() {
        return this.bitmap.getWidth();
    }

    @Override
    public int getMinimumHeight() {
        return this.bitmap.getHeight();
    }

    /**
     * Returns the underlying wrapped bitmap.
     */
    public Bitmap getBitmap() {
        return this.bitmap;
    }
}
