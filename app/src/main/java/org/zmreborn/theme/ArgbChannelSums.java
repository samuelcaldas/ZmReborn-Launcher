package org.zmreborn.theme;

/** Accumulates ARGB channels for a moving blur window. */
final class ArgbChannelSums {
    private long alpha;
    private long red;
    private long green;
    private long blue;
    private final int count;

    private ArgbChannelSums(int count) {
        this.count = count;
    }

    static ArgbChannelSums horizontal(int[] pixels, int width, int y, int radius) {
        ArgbChannelSums sums = new ArgbChannelSums((radius * 2) + 1);
        int rowOffset = y * width;
        for (int x = -radius; x <= radius; x++) {
            sums.add(pixels[rowOffset + ArgbBoxBlur.clamp(x, width)]);
        }
        return sums;
    }

    static ArgbChannelSums vertical(int[] pixels, int width, int height, int x, int radius) {
        ArgbChannelSums sums = new ArgbChannelSums((radius * 2) + 1);
        for (int y = -radius; y <= radius; y++) {
            int row = ArgbBoxBlur.clamp(y, height) * width;
            sums.add(pixels[row + x]);
        }
        return sums;
    }

    void slide(int outgoing, int incoming) {
        remove(outgoing);
        add(incoming);
    }

    int average() {
        return ((int) (alpha / count) << 24) | ((int) (red / count) << 16)
                | ((int) (green / count) << 8) | (int) (blue / count);
    }

    private void add(int pixel) {
        alpha += pixel >>> 24;
        red += (pixel >>> 16) & 0xFF;
        green += (pixel >>> 8) & 0xFF;
        blue += pixel & 0xFF;
    }

    private void remove(int pixel) {
        alpha -= pixel >>> 24;
        red -= (pixel >>> 16) & 0xFF;
        green -= (pixel >>> 8) & 0xFF;
        blue -= pixel & 0xFF;
    }
}
