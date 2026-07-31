package org.zmreborn;

import java.util.concurrent.atomic.AtomicInteger;

/** Rejects obsolete queued wallpaper-blur work across lifecycle generations. */
final class WallpaperBlurGeneration {
    private final AtomicInteger value = new AtomicInteger();

    int advance() {
        return this.value.incrementAndGet();
    }

    boolean isCurrent(int generation) {
        return this.value.get() == generation;
    }
}
