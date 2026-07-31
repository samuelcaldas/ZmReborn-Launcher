package org.zmreborn;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WallpaperBlurGenerationTest {
    @Test
    public void advancingGenerationRejectsQueuedObsoleteWork() {
        WallpaperBlurGeneration generation = new WallpaperBlurGeneration();
        int obsolete = generation.advance();
        int current = generation.advance();

        assertFalse(generation.isCurrent(obsolete));
        assertTrue(generation.isCurrent(current));
    }
}
