package org.zmreborn.theme;

import java.util.concurrent.CancellationException;

/** Rejects cancelled wallpaper-render work before further allocation or pixel processing. */
final class BlurCancellation {
    private BlurCancellation() {
    }

    static void throwIfInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Wallpaper blur was cancelled");
        }
    }
}
