package org.zmreborn.theme;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests pure brightness-specific wallpaper role derivation. */
public class WallpaperColorExtractorTest {
    @Test
    public void dynamicRolesStartOnlyOnAndroid12() {
        assertFalse(WallpaperColorExtractor.usesSystemDynamicRoles(30));
        assertTrue(WallpaperColorExtractor.usesSystemDynamicRoles(31));
    }

    @Test
    public void tonalRampsReturnAllOpaqueRoleShapes() {
        assertRoleShape(WallpaperColorExtractor.TonalRamp.derive(38f, 0.88f, false));
        assertRoleShape(WallpaperColorExtractor.TonalRamp.derive(38f, 0.88f, true));
    }

    @Test
    public void tonalRampsPreserveInputHue() {
        float hue = 210f;
        assertHue(hue, WallpaperColorExtractor.TonalRamp.derive(hue, 0.5f, false));
        assertHue(hue, WallpaperColorExtractor.TonalRamp.derive(hue, 0.5f, true));
    }

    @Test
    public void darkRampUsesDarkSurfacesAndBrightText() {
        float[][] tones = WallpaperColorExtractor.TonalRamp.derive(38f, 0.88f, true);
        assertTrue("dark surface must remain dark", tones[2][2] < 0.15f);
        assertTrue("dark on-surface must remain bright", tones[3][2] > 0.8f);
        assertTrue("dark surface variant must exceed surface", tones[4][2] > tones[2][2]);
        assertTrue("dark outline must be a midtone", tones[5][2] >= 0.4f
                && tones[5][2] <= 0.7f);
    }

    @Test
    public void lightRampUsesLightSurfacesAndDarkText() {
        float[][] tones = WallpaperColorExtractor.TonalRamp.derive(38f, 0.88f, false);
        assertTrue("light surface must remain bright", tones[2][2] > 0.95f);
        assertTrue("light on-surface must remain dark", tones[3][2] < 0.2f);
        assertTrue("light surface variant must be darker than surface", tones[4][2] < tones[2][2]);
        assertTrue("light outline must be a midtone", tones[5][2] >= 0.4f
                && tones[5][2] <= 0.7f);
    }

    @Test
    public void graySeedsKeepZeroSaturation() {
        float[][] tones = WallpaperColorExtractor.TonalRamp.derive(0f, 0.0f, false);
        for (int index = 0; index < tones.length; index++) {
            assertEquals("role " + index + " saturation must remain zero", 0f,
                    tones[index][1], 0.001f);
        }
    }

    private static void assertRoleShape(float[][] tones) {
        assertEquals("expect primary plus five supporting roles", 6, tones.length);
        for (int index = 0; index < tones.length; index++) {
            assertEquals("role " + index + " must have three HSV components", 3,
                    tones[index].length);
            assertTrue("role " + index + " saturation must be bounded", tones[index][1] >= 0f
                    && tones[index][1] <= 1f);
        }
    }

    private static void assertHue(float hue, float[][] tones) {
        for (int index = 0; index < tones.length; index++) {
            assertEquals("role " + index + " must preserve hue", hue, tones[index][0],
                    0.001f);
        }
    }
}
