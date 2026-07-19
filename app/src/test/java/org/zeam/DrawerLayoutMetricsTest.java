package org.zeam;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DrawerLayoutMetricsTest {
    @Test
    public void clampsInvalidGridValuesAndKeepsPositiveCells() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(240, 320, 0, 0,
                20, 20, 80, 80);

        assertEquals(1, metrics.getRows());
        assertEquals(1, metrics.getColumns());
        assertTrue(metrics.getCellWidth() > 0);
        assertTrue(metrics.getCellHeight() > 0);
    }

    @Test
    public void reducesRequestedGridToMeasuredBounds() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(240, 320, 6, 8,
                20, 20, 80, 80);

        assertEquals(2, metrics.getColumns());
        assertEquals(3, metrics.getRows());
        assertTrue(metrics.getCellWidth() * metrics.getColumns() <= metrics.getAvailableWidth());
        assertTrue(metrics.getCellHeight() * metrics.getRows() <= metrics.getAvailableHeight());
    }

    @Test
    public void tinyViewportStillProducesUsableOneByOneCell() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(1, 1, 99, 99,
                40, 40, 100, 100);

        assertEquals(1, metrics.getRows());
        assertEquals(1, metrics.getColumns());
        assertEquals(1, metrics.getCellWidth());
        assertEquals(1, metrics.getCellHeight());
    }
}
