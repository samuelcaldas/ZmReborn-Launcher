package org.zmreborn;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CellLayoutSpanTest {
    @Test
    public void exactCellSizeUsesOneCell() {
        assertEquals(1, CellLayout.calculateSpan(80, 80, 0));
    }

    @Test
    public void sizePastCellBoundaryUsesNextCell() {
        assertEquals(2, CellLayout.calculateSpan(81, 80, 0));
    }

    @Test
    public void gapContributesToMultiCellWidgetSize() {
        assertEquals(2, CellLayout.calculateSpan(170, 80, 10));
        assertEquals(3, CellLayout.calculateSpan(171, 80, 10));
    }

    @Test
    public void negativeGapUsesSignedWidgetGeometry() {
        assertEquals(2, CellLayout.calculateSpan(120, 80, -40));
        assertEquals(3, CellLayout.calculateSpan(121, 80, -40));
    }

    @Test
    public void emptyProviderDimensionStillUsesOneCell() {
        assertEquals(1, CellLayout.calculateSpan(0, 80, 10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCellSizeFailsFast() {
        CellLayout.calculateSpan(80, 0, 0);
    }

    @Test
    public void oversizedHorizontalSpanClampsToAvailableColumns() {
        assertEquals(4, CellLayout.clampSpan(5, 4));
    }

    @Test
    public void oversizedVerticalSpanClampsToAvailableRows() {
        assertEquals(3, CellLayout.clampSpan(4, 3));
    }

    @Test
    public void spanClampingRetainsOneCellMinimumAndAvailableBoundary() {
        assertEquals(1, CellLayout.clampSpan(0, 4));
        assertEquals(1, CellLayout.clampSpan(1, 0));
        assertEquals(4, CellLayout.clampSpan(4, 4));
    }

    @Test
    public void oversizedProviderDimensionsClampToAvailableGrid() {
        assertArrayEquals(new int[]{4, 3}, CellLayout.calculateClampedSpans(
                Integer.MAX_VALUE, Integer.MAX_VALUE, 80, 80, 10, 10, 4, 3));
    }

    @Test
    public void signedGapsDetermineClampedWidgetSpans() {
        assertArrayEquals(new int[]{3, 2}, CellLayout.calculateClampedSpans(
                121, 120, 80, 80, -40, -40, 4, 4));
    }

    @Test
    public void spanToPixelsIncludesOnlyInternalGaps() {
        assertEquals(260, CellLayout.calculateSpanPixels(3, 80, 10));
    }

    @Test
    public void spanToPixelsUsesSignedInternalGaps() {
        assertEquals(120, CellLayout.calculateSpanPixels(2, 80, -40));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonpositiveStrideFailsFast() {
        CellLayout.calculateSpan(120, 80, -80);
    }

    @Test
    public void spanPixelOverflowClampsToIntegerRange() {
        assertEquals(Integer.MAX_VALUE,
                CellLayout.calculateSpanPixels(Integer.MAX_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

}
