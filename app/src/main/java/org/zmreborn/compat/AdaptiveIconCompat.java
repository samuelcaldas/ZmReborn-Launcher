package org.zmreborn.compat;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;

/** Preserves adaptive icons without verifying API 26 classes on older Android releases. */
public final class AdaptiveIconCompat {
    private AdaptiveIconCompat() {
    }

    public static boolean isAdaptiveIcon(Drawable drawable) {
        if (drawable == null || Build.VERSION.SDK_INT < 26) {
            return false;
        }
        return Api26.isAdaptiveIcon(drawable);
    }

    @TargetApi(26)
    private static final class Api26 {
        private Api26() {
        }

        static boolean isAdaptiveIcon(Drawable drawable) {
            return drawable instanceof android.graphics.drawable.AdaptiveIconDrawable;
        }
    }
}
