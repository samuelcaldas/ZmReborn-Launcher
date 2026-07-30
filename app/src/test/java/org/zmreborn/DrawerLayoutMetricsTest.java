package org.zmreborn;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    public void reservesStatusAndNavigationInsetsBeforeSizingRows() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(320, 640, 8, 4,
                0, 64, 48, 48);

        assertEquals(576, metrics.getAvailableHeight());
        assertTrue(metrics.getCellHeight() * metrics.getRows() <= 576);
    }

    @Test
    public void columnBoundariesAreContiguousAndCoverFullWidth() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(301, 400, 4, 3,
                0, 0, 1, 1);
        int cols = metrics.getColumns();

        for (int i = 0; i < cols - 1; i++) {
            assertEquals("col " + i + " right == col " + (i + 1) + " left",
                    metrics.columnRight(i), metrics.columnLeft(i + 1));
        }
        assertEquals(0, metrics.columnLeft(0));
        assertEquals(metrics.getAvailableWidth(), metrics.columnRight(cols - 1));
    }

    @Test
    public void rowBoundariesAreContiguousAndCoverFullHeight() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(300, 401, 4, 3,
                0, 0, 1, 1);
        int rows = metrics.getRows();

        for (int i = 0; i < rows - 1; i++) {
            assertEquals("row " + i + " bottom == row " + (i + 1) + " top",
                    metrics.rowBottom(i), metrics.rowTop(i + 1));
        }
        assertEquals(0, metrics.rowTop(0));
        assertEquals(metrics.getAvailableHeight(), metrics.rowBottom(rows - 1));
    }

    @Test
    public void slotWidthsAndHeightsArePositive() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(100, 100, 3, 3,
                0, 0, 1, 1);

        for (int c = 0; c < metrics.getColumns(); c++) {
            assertTrue(metrics.columnRight(c) > metrics.columnLeft(c));
        }
        for (int r = 0; r < metrics.getRows(); r++) {
            assertTrue(metrics.rowBottom(r) > metrics.rowTop(r));
        }
    }

    @Test
    public void invalidColumnIndexThrowsIllegalArgument() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(300, 400, 4, 3,
                0, 0, 1, 1);
        try {
            metrics.columnLeft(-1);
            fail("expected IllegalArgumentException for negative column index");
        } catch (IllegalArgumentException expected) {
            // pass
        }
        try {
            metrics.columnLeft(metrics.getColumns());
            fail("expected IllegalArgumentException for out-of-range column index");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    @Test
    public void invalidRowIndexThrowsIllegalArgument() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(300, 400, 4, 3,
                0, 0, 1, 1);
        try {
            metrics.rowTop(-1);
            fail("expected IllegalArgumentException for negative row index");
        } catch (IllegalArgumentException expected) {
            // pass
        }
        try {
            metrics.rowTop(metrics.getRows());
            fail("expected IllegalArgumentException for out-of-range row index");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    @Test
    public void tinyViewportSlotBoundariesRemainValid() {
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(1, 1, 99, 99,
                40, 40, 100, 100);

        assertEquals(0, metrics.columnLeft(0));
        assertEquals(1, metrics.columnRight(0));
        assertEquals(0, metrics.rowTop(0));
        assertEquals(1, metrics.rowBottom(0));
    }
}
