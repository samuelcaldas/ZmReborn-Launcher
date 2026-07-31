package org.zmreborn.theme;

import java.util.Arrays;

/** Applies deterministic multi-pass box blur to packed ARGB pixels. */
public final class ArgbBoxBlur {
    private ArgbBoxBlur() {
    }

    /**
     * Returns a blurred copy of the supplied pixels.
     *
     * @throws IllegalArgumentException when geometry, radius, passes, or pixel count is invalid
     */
    public static int[] blur(int[] pixels, int width, int height, int radius, int passes) {
        validate(pixels, width, height, radius, passes);
        BlurCancellation.throwIfInterrupted();
        int[] result = Arrays.copyOf(pixels, pixels.length);
        for (int pass = 0; pass < passes && radius > 0; pass++) {
            result = blurVertical(blurHorizontal(result, width, height, radius),
                    width, height, radius);
        }
        return result;
    }

    private static void validate(int[] pixels, int width, int height, int radius, int passes) {
        if (pixels == null) {
            throw new IllegalArgumentException("Pixels must not be null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Blur dimensions must be positive");
        }
        if ((long) width * height != pixels.length) {
            throw new IllegalArgumentException("Pixel count must match blur dimensions");
        }
        if (radius < 0 || radius > Math.max(width, height)) {
            throw new IllegalArgumentException("Blur radius must fit image dimensions");
        }
        if (passes <= 0) {
            throw new IllegalArgumentException("Blur passes must be positive");
        }
    }

    private static int[] blurHorizontal(int[] source, int width, int height, int radius) {
        BlurCancellation.throwIfInterrupted();
        int[] target = new int[source.length];
        for (int y = 0; y < height; y++) {
            BlurCancellation.throwIfInterrupted();
            blurRow(source, target, width, y, radius);
        }
        return target;
    }

    private static void blurRow(int[] source, int[] target, int width, int y, int radius) {
        ArgbChannelSums sums = ArgbChannelSums.horizontal(source, width, y, radius);
        int rowOffset = y * width;
        for (int x = 0; x < width; x++) {
            checkPixelCancellation(x);
            target[rowOffset + x] = sums.average();
            int outgoing = source[rowOffset + clamp(x - radius, width)];
            int incoming = source[rowOffset + clamp(x + radius + 1, width)];
            sums.slide(outgoing, incoming);
        }
    }

    private static int[] blurVertical(int[] source, int width, int height, int radius) {
        BlurCancellation.throwIfInterrupted();
        int[] target = new int[source.length];
        for (int x = 0; x < width; x++) {
            BlurCancellation.throwIfInterrupted();
            blurColumn(source, target, width, height, x, radius);
        }
        return target;
    }

    private static void blurColumn(int[] source, int[] target, int width, int height, int x,
            int radius) {
        ArgbChannelSums sums = ArgbChannelSums.vertical(source, width, height, x, radius);
        for (int y = 0; y < height; y++) {
            checkPixelCancellation(y);
            target[(y * width) + x] = sums.average();
            int outgoing = source[(clamp(y - radius, height) * width) + x];
            int incoming = source[(clamp(y + radius + 1, height) * width) + x];
            sums.slide(outgoing, incoming);
        }
    }

    private static void checkPixelCancellation(int coordinate) {
        if ((coordinate & 63) == 0) {
            BlurCancellation.throwIfInterrupted();
        }
    }

    static int clamp(int coordinate, int size) {
        return Math.max(0, Math.min(size - 1, coordinate));
    }
}
