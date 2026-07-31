package org.zmreborn.theme;

import java.util.concurrent.CancellationException;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArgbBoxBlurTest {
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    @Test
    public void zeroRadiusPreservesPixels() {
        int[] pixels = {BLACK, WHITE, 0x80402010};

        assertArrayEquals(pixels, ArgbBoxBlur.blur(pixels, 3, 1, 0, 3));
    }

    @Test
    public void uniformInputRemainsUniform() {
        int[] pixels = {0x80402010, 0x80402010, 0x80402010, 0x80402010};

        int[] blurred = ArgbBoxBlur.blur(pixels, 2, 2, 1, 3);

        assertArrayEquals(pixels, blurred);
    }

    @Test
    public void centerPixelDiffusesSymmetrically() {
        int[] pixels = new int[25];
        pixels[12] = WHITE;

        int[] blurred = ArgbBoxBlur.blur(pixels, 5, 5, 1, 1);

        assertTrue(channel(blurred[12]) < 255);
        assertTrue(channel(blurred[11]) > 0);
        assertEquals(blurred[11], blurred[13]);
        assertEquals(blurred[7], blurred[17]);
    }

    @Test
    public void alphaChannelRemainsBounded() {
        int[] pixels = {0x00000000, 0xFFFFFFFF, 0x40000000};

        int[] blurred = ArgbBoxBlur.blur(pixels, 3, 1, 1, 3);

        for (int pixel : blurred) {
            int alpha = pixel >>> 24;
            assertTrue(alpha >= 0 && alpha <= 255);
        }
    }

    @Test
    public void supportsSingleRowAndSingleColumn() {
        int[] pixels = {BLACK, WHITE, BLACK};

        assertEquals(3, ArgbBoxBlur.blur(pixels, 3, 1, 1, 1).length);
        assertEquals(3, ArgbBoxBlur.blur(pixels, 1, 3, 1, 1).length);
    }

    @Test
    public void rejectsInvalidBoundaries() {
        assertInvalid(null, 1, 1, 1, 1);
        assertInvalid(new int[1], 0, 1, 1, 1);
        assertInvalid(new int[1], 1, 0, 1, 1);
        assertInvalid(new int[2], 1, 1, 1, 1);
        assertInvalid(new int[1], 1, 1, -1, 1);
        assertInvalid(new int[1], 1, 1, 1, 0);
    }

    @Test
    public void rejectsInterruptedWork() {
        Thread.currentThread().interrupt();
        try {
            ArgbBoxBlur.blur(new int[9], 3, 3, 1, 3);
            fail("Expected CancellationException");
        } catch (CancellationException expected) {
            assertTrue(expected.getMessage().length() > 0);
        } finally {
            Thread.interrupted();
        }
    }

    private static int channel(int pixel) {
        return pixel & 0xFF;
    }

    private static void assertInvalid(int[] pixels, int width, int height, int radius,
            int passes) {
        try {
            ArgbBoxBlur.blur(pixels, width, height, radius, passes);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }
}
