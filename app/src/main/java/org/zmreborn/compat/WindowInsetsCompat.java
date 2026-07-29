package org.zmreborn.compat;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/**
 * Bridges {@link WindowInsets} extraction across API 24-35 without touching classes the
 * verifier cannot resolve below their introducing API level (API 29 {@code getSystemGestureInsets},
 * API 30 {@code Type}-based {@code getInsets}); each such call is isolated behind its own
 * {@link Build.VERSION#SDK_INT} guard so the referencing method is never verified on older devices.
 */
public final class WindowInsetsCompat {

    private WindowInsetsCompat() {
    }

    public static Rect getSystemBarInsets(WindowInsets insets) {
        if (insets == null) {
            return new Rect();
        }
        return new Rect(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
    }

    public static Rect getSystemBarInsets(View view) {
        return getSystemBarInsets(rootInsets(view));
    }

    public static Rect getSystemGestureInsets(WindowInsets insets) {
        if (insets == null || Build.VERSION.SDK_INT < 29) {
            return null;
        }
        return Api29.systemGestureInsets(insets);
    }

    public static Rect getSystemGestureInsets(View view) {
        return getSystemGestureInsets(rootInsets(view));
    }

    public static Rect getStatusBarInsets(WindowInsets insets) {
        if (insets == null) {
            return new Rect();
        }
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30.statusBarInsets(insets);
        }
        return new Rect(0, insets.getSystemWindowInsetTop(), 0, 0);
    }

    public static Rect getStatusBarInsets(View view) {
        return getStatusBarInsets(rootInsets(view));
    }

    public static Rect getNavigationBarInsets(WindowInsets insets) {
        if (insets == null) {
            return new Rect();
        }
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30.navigationBarInsets(insets);
        }
        return new Rect(0, 0, 0, insets.getSystemWindowInsetBottom());
    }

    public static Rect getNavigationBarInsets(View view) {
        return getNavigationBarInsets(rootInsets(view));
    }

    private static WindowInsets rootInsets(View view) {
        return view == null ? null : view.getRootWindowInsets();
    }

    @TargetApi(29)
    private static final class Api29 {
        private Api29() {
        }

        static Rect systemGestureInsets(WindowInsets insets) {
            return fromPlatformInsets(insets.getSystemGestureInsets());
        }

        static Rect fromPlatformInsets(android.graphics.Insets insets) {
            return new Rect(insets.left, insets.top, insets.right, insets.bottom);
        }
    }

    @TargetApi(30)
    private static final class Api30 {
        private Api30() {
        }

        static Rect statusBarInsets(WindowInsets insets) {
            return Api29.fromPlatformInsets(insets.getInsets(WindowInsets.Type.statusBars()));
        }

        static Rect navigationBarInsets(WindowInsets insets) {
            return Api29.fromPlatformInsets(insets.getInsets(WindowInsets.Type.navigationBars()));
        }
    }
}
