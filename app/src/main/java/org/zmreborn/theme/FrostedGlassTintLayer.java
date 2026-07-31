package org.zmreborn.theme;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;

/** Caches gradient paint for one frosted-glass drawable. */
final class FrostedGlassTintLayer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final FrostedGlassStyle style;

    FrostedGlassTintLayer(FrostedGlassStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("Frosted glass style must not be null");
        }
        this.style = style;
    }

    void updateBounds(Rect bounds) {
        if (bounds.isEmpty()) {
            this.paint.setShader(null);
            return;
        }
        this.paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                this.style.topColor(), this.style.bottomColor(), Shader.TileMode.CLAMP));
    }

    void draw(Canvas canvas, Rect bounds) {
        canvas.drawRect(bounds, this.paint);
    }

    int grainAlpha() {
        return this.style.grainAlpha();
    }

    void setDrawableAlpha(int alpha) {
        this.style.setDrawableAlpha(alpha);
    }
}
