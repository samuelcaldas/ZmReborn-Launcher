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
     * @throws IllegalStateException when a hardware wallpaper cannot be copied for pixel access
     * @throws CancellationException when current wallpaper worker is interrupted
     */
    public static Bitmap render(Bitmap wallpaper) {
        validateWallpaper(wallpaper);
        Bitmap readableWallpaper = createReadableWallpaper(wallpaper);
        Bitmap downsampled = null;
        try {
            BlurCancellation.throwIfInterrupted();
            int width = downsampleDimension(readableWallpaper.getWidth());
            int height = downsampleDimension(readableWallpaper.getHeight());
            downsampled = Bitmap.createScaledBitmap(readableWallpaper, width, height, true);
            return renderDownsampled(downsampled, width, height);
        } finally {
            recycleDownsampledCopy(readableWallpaper, downsampled);
            recycleReadableCopy(wallpaper, readableWallpaper);
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

    private static Bitmap createReadableWallpaper(Bitmap wallpaper) {
        Bitmap.Config config = wallpaper.getConfig();
        if (config == null || !"HARDWARE".equals(config.name())) {
            return wallpaper;
        }
        Bitmap readableWallpaper = wallpaper.copy(Bitmap.Config.ARGB_8888, false);
        if (readableWallpaper == null) {
            throw new IllegalStateException("Unable to copy hardware wallpaper for pixel access");
        }
        return readableWallpaper;
    }

    private static void recycleDownsampledCopy(Bitmap wallpaper, Bitmap downsampled) {
        if (downsampled != null && downsampled != wallpaper && !downsampled.isRecycled()) {
            downsampled.recycle();
        }
    }

    private static void recycleReadableCopy(Bitmap wallpaper, Bitmap readableWallpaper) {
        if (readableWallpaper != wallpaper && !readableWallpaper.isRecycled()) {
            readableWallpaper.recycle();
        }
    }
}
