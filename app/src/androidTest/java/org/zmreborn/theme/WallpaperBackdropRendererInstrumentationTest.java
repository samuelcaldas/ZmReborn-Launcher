package org.zmreborn.theme;

import android.graphics.Bitmap;
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
}
