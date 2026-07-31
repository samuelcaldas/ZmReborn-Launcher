package org.zmreborn;

import android.graphics.Bitmap;

/** Owns cached blur data plus generation checks for queued work. */
final class WallpaperBlurState {
    private final WallpaperBlurCache cache = new WallpaperBlurCache();
    private final WallpaperBlurGeneration generation = new WallpaperBlurGeneration();

    int begin(Bitmap source) {
        int current = this.generation.advance();
        this.cache.begin(source);
        return current;
    }

    void clear() {
        this.generation.advance();
        this.cache.clear();
    }

    boolean isCurrent(int value, Bitmap source) {
        return this.generation.isCurrent(value) && this.cache.matches(source);
    }

    boolean matches(Bitmap source) {
        return this.cache.matches(source);
    }

    Bitmap getBlurred() {
        return this.cache.getBlurred();
    }

    void complete(Bitmap source, Bitmap blurred) {
        this.cache.complete(source, blurred);
    }
}
