package org.zmreborn;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WallpaperBackdropAlignmentTest {
    @Test
    public void workspaceScrollIsAlreadyRepresentedByWallpaperPosition() {
        assertEquals(-120.0f,
                WallpaperBackdropAlignment.offset(-120.0f, 0.0f, 0.0f), 0.0f);
    }

    @Test
    public void siblingTargetPositionOffsetsBackdropOnce() {
        assertEquals(-168.0f,
                WallpaperBackdropAlignment.offset(-120.0f, 48.0f, 0.0f), 0.0f);
    }
}
