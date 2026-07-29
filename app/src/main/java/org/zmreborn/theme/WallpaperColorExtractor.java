package org.zmreborn.theme;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import org.zmreborn.R;

/**
 * Resolves the current launcher palette.
 *
 * <p>API 31 and later resolve Android system dynamic color resources. API 24 through 30 derive
 * separate light and dark wallpaper palettes and cache them so cold starts do not block on
 * {@link WallpaperManager}.
 */
public final class WallpaperColorExtractor {
    private static final int CACHE_SCHEMA_VERSION = 2;
    private static final int INVALID_COLOR = 0;
    private static final String PREF_KEY_ON_PRIMARY = "on_primary";
    private static final String PREF_KEY_ON_SURFACE = "on_surface";
    private static final String PREF_KEY_OUTLINE = "outline";
    private static final String PREF_KEY_PRIMARY = "primary";
    private static final String PREF_KEY_SCHEMA = "schema";
    private static final String PREF_KEY_SURFACE = "surface";
    private static final String PREF_KEY_SURFACE_VARIANT = "surface_variant";
    private static final String PREFS_NAME = "org.zmreborn.theme.wallpaper_colors";

    private WallpaperColorExtractor() {
    }

    /** Refreshes the current pre-Android-12 wallpaper cache on a background thread. */
    public static void refresh(Context context) {
        requireContext(context);
        if (usesSystemDynamicRoles(Build.VERSION.SDK_INT)) {
            return;
        }
        boolean night = isNight(context);
        int fallbackPrimary = getColor(context, R.color.m3_primary);
        int primary = fallbackPrimary;
        if (Build.VERSION.SDK_INT >= 27) {
            primary = Api27.extractPrimary(context, fallbackPrimary);
        }
        cache(context, derive(primary, night), night);
    }

    /** Returns primary role for current brightness and SDK. */
    public static int getPrimary(Context context) {
        return palette(context).primary;
    }

    /** Returns on-primary role for current brightness and SDK. */
    public static int getOnPrimary(Context context) {
        return palette(context).onPrimary;
    }

    /** Returns surface role for current brightness and SDK. */
    public static int getSurface(Context context) {
        return palette(context).surface;
    }

    /** Returns on-surface role for current brightness and SDK. */
    public static int getOnSurface(Context context) {
        return palette(context).onSurface;
    }

    /** Returns surface-variant role for current brightness and SDK. */
    public static int getSurfaceVariant(Context context) {
        return palette(context).surfaceVariant;
    }

    /** Returns outline role for current brightness and SDK. */
    public static int getOutline(Context context) {
        return palette(context).outline;
    }

    static boolean usesSystemDynamicRoles(int sdkInt) {
        return sdkInt >= 31;
    }

    private static Palette palette(Context context) {
        requireContext(context);
        Palette fallback = fallback(context);
        if (usesSystemDynamicRoles(Build.VERSION.SDK_INT)) {
            return fallback;
        }
        Palette cached = cached(context, isNight(context));
        return cached == null ? fallback : cached;
    }

    private static Palette fallback(Context context) {
        return new Palette(
                getColor(context, R.color.m3_primary),
                getColor(context, R.color.m3_on_primary),
                getColor(context, R.color.m3_surface),
                getColor(context, R.color.m3_on_surface),
                getColor(context, R.color.m3_surface_variant),
                getColor(context, R.color.m3_outline));
    }

    private static Palette cached(Context context, boolean night) {
        SharedPreferences preferences = prefs(context);
        if (preferences.getInt(PREF_KEY_SCHEMA, 0) != CACHE_SCHEMA_VERSION) {
            return null;
        }
        String prefix = prefix(night);
        int primary = preferences.getInt(prefix + PREF_KEY_PRIMARY, INVALID_COLOR);
        int onPrimary = preferences.getInt(prefix + PREF_KEY_ON_PRIMARY, INVALID_COLOR);
        int surface = preferences.getInt(prefix + PREF_KEY_SURFACE, INVALID_COLOR);
        int onSurface = preferences.getInt(prefix + PREF_KEY_ON_SURFACE, INVALID_COLOR);
        int surfaceVariant = preferences.getInt(prefix + PREF_KEY_SURFACE_VARIANT, INVALID_COLOR);
        int outline = preferences.getInt(prefix + PREF_KEY_OUTLINE, INVALID_COLOR);
        if (!isOpaque(primary) || !isOpaque(onPrimary) || !isOpaque(surface)
                || !isOpaque(onSurface) || !isOpaque(surfaceVariant) || !isOpaque(outline)) {
            return null;
        }
        return new Palette(primary, onPrimary, surface, onSurface, surfaceVariant, outline);
    }

    private static void cache(Context context, Palette palette, boolean night) {
        String prefix = prefix(night);
        if (!prefs(context).edit()
                .putInt(PREF_KEY_SCHEMA, CACHE_SCHEMA_VERSION)
                .putInt(prefix + PREF_KEY_PRIMARY, palette.primary)
                .putInt(prefix + PREF_KEY_ON_PRIMARY, palette.onPrimary)
                .putInt(prefix + PREF_KEY_SURFACE, palette.surface)
                .putInt(prefix + PREF_KEY_ON_SURFACE, palette.onSurface)
                .putInt(prefix + PREF_KEY_SURFACE_VARIANT, palette.surfaceVariant)
                .putInt(prefix + PREF_KEY_OUTLINE, palette.outline)
                .commit()) {
            throw new IllegalStateException("Unable to cache wallpaper palette");
        }
    }

    private static Palette derive(int seedColor, boolean night) {
        float[] hsv = new float[3];
        Color.colorToHSV(seedColor, hsv);
        float[][] tones = TonalRamp.derive(hsv[0], hsv[1], night);
        return new Palette(
                Color.HSVToColor(tones[0]),
                Color.HSVToColor(tones[1]),
                Color.HSVToColor(tones[2]),
                Color.HSVToColor(tones[3]),
                Color.HSVToColor(tones[4]),
                Color.HSVToColor(tones[5]));
    }

    private static boolean isNight(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static boolean isOpaque(int color) {
        return color != INVALID_COLOR && Color.alpha(color) == 255;
    }

    private static int getColor(Context context, int resourceId) {
        return context.getColor(resourceId);
    }

    private static String prefix(boolean night) {
        return night ? "dark_" : "light_";
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static void requireContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
    }

    /** Pure HSV tonal-ramp math with separate day and night contrast targets. */
    static final class TonalRamp {
        private TonalRamp() {
        }

        static float[][] derive(float hue, float saturation, boolean night) {
            float safeSaturation = Math.max(0.0f, Math.min(1.0f, saturation));
            if (night) {
                return new float[][] {
                        {hue, safeSaturation, 0.78f},
                        {hue, Math.min(safeSaturation * 0.18f, 0.12f), 0.12f},
                        {hue, Math.min(safeSaturation * 0.25f, 0.15f), 0.08f},
                        {hue, Math.min(safeSaturation * 0.10f, 0.08f), 0.92f},
                        {hue, Math.min(safeSaturation * 0.30f, 0.18f), 0.13f},
                        {hue, Math.min(safeSaturation * 0.15f, 0.12f), 0.58f},
                };
            }
            return new float[][] {
                    {hue, safeSaturation, 0.45f},
                    {hue, Math.min(safeSaturation * 0.18f, 0.12f), 1.0f},
                    {hue, Math.min(safeSaturation * 0.08f, 0.06f), 0.98f},
                    {hue, Math.min(safeSaturation * 0.10f, 0.08f), 0.16f},
                    {hue, Math.min(safeSaturation * 0.14f, 0.12f), 0.91f},
                    {hue, Math.min(safeSaturation * 0.18f, 0.15f), 0.46f},
            };
        }
    }

    private static final class Palette {
        private final int onPrimary;
        private final int onSurface;
        private final int outline;
        private final int primary;
        private final int surface;
        private final int surfaceVariant;

        Palette(int primary, int onPrimary, int surface, int onSurface, int surfaceVariant,
                int outline) {
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.surface = surface;
            this.onSurface = onSurface;
            this.surfaceVariant = surfaceVariant;
            this.outline = outline;
        }
    }

    @TargetApi(27)
    private static final class Api27 {
        private Api27() {
        }

        static int extractPrimary(Context context, int fallbackPrimary) {
            try {
                android.app.WallpaperColors colors = WallpaperManager.getInstance(context)
                        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors == null) {
                    return fallbackPrimary;
                }
                return colors.getPrimaryColor().toArgb();
            } catch (SecurityException ignored) {
                return fallbackPrimary;
            }
        }
    }
}
