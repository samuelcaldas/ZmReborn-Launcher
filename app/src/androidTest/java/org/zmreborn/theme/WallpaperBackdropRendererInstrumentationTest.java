package org.zmreborn.theme;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.test.InstrumentationTestCase;

public class WallpaperBackdropRendererInstrumentationTest extends InstrumentationTestCase {
    public void testRendererProducesCompactBlurredBitmapWithoutRecyclingSource() {
        Bitmap source = createSplitWallpaper(64, 64);
        Bitmap blurred = null;
        try {
            blurred = WallpaperBackdropRenderer.render(source);

            assertEquals("Blurred width must use one-eighth scale", 8, blurred.getWidth());
            assertEquals("Blurred height must use one-eighth scale", 8, blurred.getHeight());
            int boundaryChannel = blurred.getPixel(3, 4) & 0xFF;
            assertTrue("Blur must diffuse across source boundary", boundaryChannel > 0);
            assertTrue("Blurred boundary must remain below white", boundaryChannel < 255);
            assertFalse("Renderer must retain caller-owned source", source.isRecycled());
        } finally {
            recycle(blurred);
            recycle(source);
        }
    }

    public void testRendererCopiesHardwareWallpaperBeforePixelAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        Bitmap softwareSeed = createSplitWallpaper(64, 64);
        Bitmap hardwareSource = Api26.createHardwareCopy(softwareSeed);
        Bitmap blurred = null;
        try {
            blurred = WallpaperBackdropRenderer.render(hardwareSource);

            assertNotNull("Renderer must produce a bitmap from hardware wallpaper", blurred);
            assertEquals("Blurred output must use ARGB_8888", Bitmap.Config.ARGB_8888, blurred.getConfig());
            assertEquals("Blurred width must use one-eighth scale", 8, blurred.getWidth());
            assertEquals("Blurred height must use one-eighth scale", 8, blurred.getHeight());
            assertFalse("Renderer must retain caller-owned hardware source", hardwareSource.isRecycled());
        } finally {
            recycle(blurred);
            recycle(hardwareSource);
            recycle(softwareSeed);
        }
    }

    private static Bitmap createSplitWallpaper(int width, int height) {
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = width / 2; x < width; x++) {
                pixels[(y * width) + x] = 0xFFFFFFFF;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    @TargetApi(26)
    private static final class Api26 {
        private Api26() {
        }

        static Bitmap createHardwareCopy(Bitmap source) {
            return source.copy(Bitmap.Config.HARDWARE, false);
        }
    }
}
