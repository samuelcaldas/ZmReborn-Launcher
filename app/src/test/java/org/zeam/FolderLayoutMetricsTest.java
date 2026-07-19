package org.zeam;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FolderLayoutMetricsTest {
    @Test
    public void boundsPanelInsideShortNarrowViewport() {
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(220, 260, 12,
                480, 600, 58, 4, 16, 48);

        assertTrue(metrics.getPanelWidth() <= 196);
        assertTrue(metrics.getPanelHeight() <= 236);
        assertTrue(metrics.getCellWidth() > 0);
        assertTrue(metrics.getCellHeight() > 0);
        assertTrue(metrics.isScrollable());
    }

    @Test
    public void emptyFolderKeepsOneVisibleRowWithoutScrolling() {
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(480, 800, 16,
                440, 640, 58, 5, 0, 48);

        assertEquals(1, metrics.getTotalRows());
        assertEquals(1, metrics.getVisibleRows());
        assertFalse(metrics.isScrollable());
    }

    @Test
    public void invalidInputsRemainPositive() {
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(0, 0, -1,
                0, 0, -1, 0, -1, 0);

        assertTrue(metrics.getPanelWidth() > 0);
        assertTrue(metrics.getPanelHeight() > 0);
        assertTrue(metrics.getCellWidth() > 0);
        assertTrue(metrics.getCellHeight() > 0);
    }
}
