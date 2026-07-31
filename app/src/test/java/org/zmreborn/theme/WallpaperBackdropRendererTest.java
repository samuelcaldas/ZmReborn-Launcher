package org.zmreborn.theme;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WallpaperBackdropRendererTest {
    @Test
    public void downsampleDimensionUsesOneEighthScale() {
        assertEquals(1, WallpaperBackdropRenderer.downsampleDimension(1));
        assertEquals(1, WallpaperBackdropRenderer.downsampleDimension(7));
        assertEquals(2, WallpaperBackdropRenderer.downsampleDimension(16));
        assertEquals(240, WallpaperBackdropRenderer.downsampleDimension(1920));
    }

    @Test
    public void blurRadiusFitsSmallAndLargeBitmaps() {
        assertEquals(1, WallpaperBackdropRenderer.blurRadius(1, 1));
        assertTrue(WallpaperBackdropRenderer.blurRadius(240, 135) > 1);
        assertTrue(WallpaperBackdropRenderer.blurRadius(240, 135) <= 12);
    }

    @Test
    public void rejectsInvalidGeometryBeforeRendering() {
        assertInvalidDimension(0);
        assertInvalidDimension(-1);
        try {
            WallpaperBackdropRenderer.blurRadius(0, 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }

    private static void assertInvalidDimension(int dimension) {
        try {
            WallpaperBackdropRenderer.downsampleDimension(dimension);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }
}
