package org.zmreborn.compat;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import java.util.Collections;
import java.util.List;

/**
 * Bridges {@link View#setSystemGestureExclusionRects(List)} (API 29+); a no-op below that
 * level, where the OS has no competing edge-swipe-back gesture to exclude zones from.
 */
public final class GestureExclusionCompat {

    private GestureExclusionCompat() {
    }

    public static void setSystemGestureExclusionRects(View view, List<Rect> rects) {
        if (view == null || Build.VERSION.SDK_INT < 29) {
            return;
        }
        Api29.setExclusionRects(view, rects == null ? Collections.<Rect>emptyList() : rects);
    }

    public static void clearSystemGestureExclusionRects(View view) {
        setSystemGestureExclusionRects(view, Collections.<Rect>emptyList());
    }

    @TargetApi(29)
    private static final class Api29 {
        private Api29() {
        }

        static void setExclusionRects(View view, List<Rect> rects) {
            view.setSystemGestureExclusionRects(rects);
        }
    }
}
