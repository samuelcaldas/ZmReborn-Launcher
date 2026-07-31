package org.zmreborn;

/** Calculates wallpaper position relative to a sibling backdrop target. */
final class WallpaperBackdropAlignment {
    private WallpaperBackdropAlignment() {
    }

    static float offset(float wallpaperPosition, float targetPosition,
            float workspacePosition) {
        return wallpaperPosition - (targetPosition - workspacePosition);
    }
}
