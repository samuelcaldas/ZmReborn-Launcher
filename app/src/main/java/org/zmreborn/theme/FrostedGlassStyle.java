package org.zmreborn.theme;

import android.graphics.Color;

/** Stores tint and drawable opacity for a frosted-glass surface. */
final class FrostedGlassStyle {
    private final int tintColor;
    private int drawableAlpha = 255;

    FrostedGlassStyle(int tintColor, int tintAlpha) {
        this.tintColor = Color.argb(tintAlpha, Color.red(tintColor),
                Color.green(tintColor), Color.blue(tintColor));
    }

    int topColor() {
        int alpha = Math.round(Color.alpha(this.tintColor) * 0.72f);
        return applyDrawableAlpha(this.tintColor, alpha);
    }

    int bottomColor() {
        return applyDrawableAlpha(this.tintColor, Color.alpha(this.tintColor));
    }

    int grainAlpha() {
        return Math.round(18.0f * this.drawableAlpha / 255.0f);
    }

    void setDrawableAlpha(int alpha) {
        this.drawableAlpha = Math.max(0, Math.min(255, alpha));
    }

    private int applyDrawableAlpha(int color, int alpha) {
        int combined = Math.round(alpha * this.drawableAlpha / 255.0f);
        return Color.argb(combined, Color.red(color), Color.green(color), Color.blue(color));
    }
}
