package org.zmreborn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

class FastBitmapDrawable extends Drawable {
    private Bitmap mBitmap;

    FastBitmapDrawable(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, (Paint) null);
    }

    public int getOpacity() {
        return -3;
    }

    public void setAlpha(int alpha) {
    }

    public void setColorFilter(ColorFilter cf) {
    }

    public int getIntrinsicWidth() {
        return this.mBitmap.getWidth();
    }

    public int getIntrinsicHeight() {
        return this.mBitmap.getHeight();
    }

    public int getMinimumWidth() {
        return this.mBitmap.getWidth();
    }

    public int getMinimumHeight() {
        return this.mBitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }
}
