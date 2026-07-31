package org.zmreborn.theme;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import java.util.Random;

/** Draws optional blurred wallpaper beneath a tinted, subtly grained glass surface. */
public final class FrostedGlassDrawable extends Drawable {
    private static final Bitmap GRAIN = createGrain();
    private static final int GRAIN_SIZE = 32;
    private static final Paint GRAIN_PAINT = createGrainPaint();
    private final Drawable backdrop;
    private final FrostedGlassTintLayer tintLayer;

    private FrostedGlassDrawable(Drawable backdrop, FrostedGlassStyle style) {
        this.backdrop = backdrop;
        this.tintLayer = new FrostedGlassTintLayer(style);
    }

    /** Creates dock glass with a fixed translucent tint. */
    public static FrostedGlassDrawable forDock(Drawable backdrop, int tintColor) {
        return new FrostedGlassDrawable(backdrop, new FrostedGlassStyle(tintColor, 104));
    }

    /** Creates drawer glass whose tint strength follows configured background opacity. */
    public static FrostedGlassDrawable forDrawer(Drawable backdrop, int tintColor,
            int configuredAlpha) {
        int boundedAlpha = Math.max(0, Math.min(255, configuredAlpha));
        int glassAlpha = 48 + Math.round(boundedAlpha * 112.0f / 255.0f);
        return new FrostedGlassDrawable(backdrop, new FrostedGlassStyle(tintColor, glassAlpha));
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        drawBackdrop(canvas, bounds);
        drawTint(canvas, bounds);
        drawGrain(canvas, bounds);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.tintLayer.updateBounds(bounds);
    }

    @Override
    public void setAlpha(int alpha) {
        this.tintLayer.setDrawableAlpha(alpha);
        this.tintLayer.updateBounds(getBounds());
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private void drawBackdrop(Canvas canvas, Rect bounds) {
        if (this.backdrop == null) {
            return;
        }
        this.backdrop.setBounds(bounds);
        this.backdrop.draw(canvas);
    }

    private void drawTint(Canvas canvas, Rect bounds) {
        this.tintLayer.draw(canvas, bounds);
    }

    private void drawGrain(Canvas canvas, Rect bounds) {
        GRAIN_PAINT.setAlpha(this.tintLayer.grainAlpha());
        canvas.drawRect(bounds, GRAIN_PAINT);
    }

    private static Paint createGrainPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(GRAIN, Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT));
        return paint;
    }

    private static Bitmap createGrain() {
        int[] pixels = new int[GRAIN_SIZE * GRAIN_SIZE];
        Random random = new Random(0x5A4D5242L);
        for (int index = 0; index < pixels.length; index++) {
            int tone = random.nextBoolean() ? 255 : 0;
            pixels[index] = Color.argb(255, tone, tone, tone);
        }
        return Bitmap.createBitmap(pixels, GRAIN_SIZE, GRAIN_SIZE, Bitmap.Config.ARGB_8888);
    }
}
