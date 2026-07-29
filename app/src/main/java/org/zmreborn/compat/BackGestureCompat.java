package org.zmreborn.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

/**
 * Facade over predictive back (API 33+ {@code OnBackInvokedCallback}, API 34+ adds animated
 * progress via {@code OnBackAnimationCallback}). Below API 33 this registers nothing; the
 * caller must instead route its legacy {@code Activity#onBackPressed()} override to the same
 * {@link BackHandler#onBackInvoked()} call so both paths share one close-priority decision.
 *
 * <p>Each API-version-specific class reference lives in its own nested static class (Api33,
 * Api34) so those classes are only loaded, and therefore only verified, when actually invoked
 * behind the matching {@link Build.VERSION#SDK_INT} guard.
 */
public final class BackGestureCompat {

    public interface BackHandler {
        void onBackInvoked();

        void onBackProgressed(float progress);

        void onBackCancelled();
    }

    private BackGestureCompat() {
    }

    public static Object registerBackHandler(Activity activity, BackHandler handler) {
        if (activity == null || handler == null || Build.VERSION.SDK_INT < 33) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34.register(activity, handler);
        }
        return Api33.register(activity, handler);
    }

    public static void unregisterBackHandler(Activity activity, Object registration) {
        if (activity == null || registration == null || Build.VERSION.SDK_INT < 33) {
            return;
        }
        Api33.unregister(activity, registration);
    }

    @TargetApi(33)
    private static final class Api33 {
        private Api33() {
        }

        static Object register(Activity activity, BackHandler handler) {
            return registerCallback(activity, createCallback(handler));
        }

        static Object registerCallback(Activity activity,
                android.window.OnBackInvokedCallback callback) {
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }

        static void unregister(Activity activity, Object registration) {
            if (registration instanceof android.window.OnBackInvokedCallback) {
                activity.getOnBackInvokedDispatcher()
                        .unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback) registration);
            }
        }

        static android.window.OnBackInvokedCallback createCallback(final BackHandler handler) {
            return new android.window.OnBackInvokedCallback() {
                public void onBackInvoked() {
                    handler.onBackInvoked();
                }
            };
        }
    }

    @TargetApi(34)
    private static final class Api34 {
        private Api34() {
        }

        static Object register(Activity activity, BackHandler handler) {
            return Api33.registerCallback(activity, createCallback(handler));
        }

        static android.window.OnBackInvokedCallback createCallback(final BackHandler handler) {
            return new android.window.OnBackAnimationCallback() {
                public void onBackStarted(android.window.BackEvent backEvent) {
                }

                public void onBackProgressed(android.window.BackEvent backEvent) {
                    handler.onBackProgressed(backEvent.getProgress());
                }

                public void onBackInvoked() {
                    handler.onBackInvoked();
                }

                public void onBackCancelled() {
                    handler.onBackCancelled();
                }
            };
        }
    }
}
