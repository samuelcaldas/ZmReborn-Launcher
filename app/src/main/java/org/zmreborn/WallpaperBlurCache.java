package org.zmreborn;

import android.graphics.Bitmap;

/** Tracks wallpaper source identity and its cached blurred representation. */
final class WallpaperBlurCache {
    private Bitmap blurred;
    private Bitmap source;

    boolean matches(Bitmap source) {
        return this.source == source;
    }

    Bitmap getBlurred() {
        return this.blurred;
    }

    void begin(Bitmap source) {
        if (source == null) {
            throw new IllegalArgumentException("Blur source must not be null");
        }
        clear();
        this.source = source;
    }

    void complete(Bitmap source, Bitmap blurred) {
        if (source == null || blurred == null) {
            throw new IllegalArgumentException("Completed blur bitmaps must not be null");
        }
        if (this.source != source) {
            throw new IllegalStateException("Completed blur source no longer matches cache");
        }
        this.blurred = blurred;
    }

    void clear() {
        this.source = null;
        this.blurred = null;
    }
}
