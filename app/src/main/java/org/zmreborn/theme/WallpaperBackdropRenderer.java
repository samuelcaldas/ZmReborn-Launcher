package org.zmreborn.theme;

import android.graphics.Bitmap;
import java.util.concurrent.CancellationException;

/** Builds a compact blurred copy of an existing static wallpaper bitmap. */
public final class WallpaperBackdropRenderer {
    private static final int BLUR_PASSES = 3;
    private static final int DOWNSAMPLE_FACTOR = 8;
    private static final int MAXIMUM_BLUR_RADIUS = 12;

    private WallpaperBackdropRenderer() {
    }

    /**
     * Returns a downsampled, blurred wallpaper bitmap.
     *
     * @throws IllegalArgumentException when wallpaper is null, recycled, or has invalid geometry
     * @throws CancellationException when current wallpaper worker is interrupted
     */
    public static Bitmap render(Bitmap wallpaper) {
        validateWallpaper(wallpaper);
        BlurCancellation.throwIfInterrupted();
        int width = downsampleDimension(wallpaper.getWidth());
        int height = downsampleDimension(wallpaper.getHeight());
        Bitmap downsampled = Bitmap.createScaledBitmap(wallpaper, width, height, true);
        try {
            return renderDownsampled(downsampled, width, height);
        } finally {
            recycleDownsampledCopy(wallpaper, downsampled);
        }
    }

    private static Bitmap renderDownsampled(Bitmap bitmap, int width, int height) {
        BlurCancellation.throwIfInterrupted();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        BlurCancellation.throwIfInterrupted();
        int radius = blurRadius(width, height);
        int[] blurred = ArgbBoxBlur.blur(pixels, width, height, radius, BLUR_PASSES);
        BlurCancellation.throwIfInterrupted();
        return Bitmap.createBitmap(blurred, width, height, Bitmap.Config.ARGB_8888);
    }

    static int downsampleDimension(int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("Wallpaper dimension must be positive");
        }
        return Math.max(1, dimension / DOWNSAMPLE_FACTOR);
    }

    static int blurRadius(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Blur dimensions must be positive");
        }
        int shortestDimension = Math.min(width, height);
        return Math.min(MAXIMUM_BLUR_RADIUS, Math.max(1, shortestDimension / 24));
    }

    private static void validateWallpaper(Bitmap wallpaper) {
        if (wallpaper == null) {
            throw new IllegalArgumentException("Wallpaper bitmap must not be null");
        }
        if (wallpaper.isRecycled()) {
            throw new IllegalArgumentException("Wallpaper bitmap must not be recycled");
        }
        downsampleDimension(wallpaper.getWidth());
        downsampleDimension(wallpaper.getHeight());
    }

    private static void recycleDownsampledCopy(Bitmap wallpaper, Bitmap downsampled) {
        if (downsampled != wallpaper && !downsampled.isRecycled()) {
            downsampled.recycle();
        }
    }
}
