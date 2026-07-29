package org.zmreborn;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void endEdgeResizePreservesOriginAndGrowsToPointerCell() {
        CellLayout.ResizeCandidate candidate = resizeCandidate(
                1, 1, 2, 2, 3, 1,
                CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_NONE,
                1, 1, 5, 4);

        assertCandidate(candidate, 1, 1, 3, 2);
    }

    @Test
    public void startEdgeResizePreservesFarEdge() {
        CellLayout.ResizeCandidate candidate = resizeCandidate(
                1, 1, 2, 2, 0, 1,
                CellLayout.RESIZE_EDGE_START, CellLayout.RESIZE_EDGE_NONE,
                1, 1, 5, 4);

        assertCandidate(candidate, 0, 1, 3, 2);
    }

    @Test
    public void startEdgeCanShrinkWithoutMovingFarEdge() {
        CellLayout.ResizeCandidate candidate = resizeCandidate(
                0, 1, 3, 2, 1, 1,
                CellLayout.RESIZE_EDGE_START, CellLayout.RESIZE_EDGE_NONE,
                1, 1, 5, 4);

        assertCandidate(candidate, 1, 1, 2, 2);
    }

    @Test
    public void cornerResizeChangesBothSupportedAxes() {
        CellLayout.ResizeCandidate candidate = resizeCandidate(
                1, 1, 2, 2, 3, 3,
                CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_END,
                1, 1, 5, 4);

        assertCandidate(candidate, 1, 1, 3, 3);
    }

    @Test
    public void minimumSpanClampsStartAndEndEdges() {
        CellLayout.ResizeCandidate startCandidate = resizeCandidate(
                0, 0, 4, 2, 3, 0,
                CellLayout.RESIZE_EDGE_START, CellLayout.RESIZE_EDGE_NONE,
                2, 1, 5, 4);
        CellLayout.ResizeCandidate endCandidate = resizeCandidate(
                1, 0, 1, 2, 1, 0,
                CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_NONE,
                2, 1, 5, 4);

        assertCandidate(startCandidate, 2, 0, 2, 2);
        assertCandidate(endCandidate, 1, 0, 2, 2);
    }

    @Test
    public void pointerOutsideGridClampsToWorkspaceBoundary() {
        CellLayout.ResizeCandidate candidate = resizeCandidate(
                1, 1, 2, 2, Integer.MAX_VALUE, Integer.MAX_VALUE,
                CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_END,
                1, 1, 4, 3);

        assertCandidate(candidate, 1, 1, 3, 2);
        assertTrue(candidate.isWithinBounds(4, 3));
    }

    @Test
    public void unsupportedAxisRemainsUnchanged() {
        CellLayout.ResizeCandidate candidate = resizeCandidate(
                1, 1, 2, 2, 0, 3,
                CellLayout.RESIZE_EDGE_NONE, CellLayout.RESIZE_EDGE_END,
                2, 1, 5, 4);

        assertCandidate(candidate, 1, 1, 2, 3);
    }

    @Test
    public void invalidOriginalPlacementCannotProduceCandidate() {
        assertNull(resizeCandidate(
                4, 0, 2, 1, 4, 0,
                CellLayout.RESIZE_EDGE_END, CellLayout.RESIZE_EDGE_NONE,
                1, 1, 5, 4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidResizeEdgeFailsFast() {
        resizeCandidate(0, 0, 1, 1, 0, 0,
                99, CellLayout.RESIZE_EDGE_NONE, 1, 1, 4, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidResizeGridFailsFast() {
        resizeCandidate(0, 0, 1, 1, 0, 0,
                CellLayout.RESIZE_EDGE_NONE, CellLayout.RESIZE_EDGE_NONE,
                1, 1, 0, 4);
    }

    @Test
    public void exactRegionOccupancyRejectsAnyNeighborCell() {
        boolean[][] occupied = new boolean[4][4];
        occupied[2][1] = true;

        assertFalse(CellLayout.isEmpty(1, 2, 1, 2, occupied));
        assertTrue(CellLayout.isEmpty(0, 1, 2, 3, occupied));
    }

    private static CellLayout.ResizeCandidate resizeCandidate(
            int cellX, int cellY, int spanX, int spanY,
            int pointerCellX, int pointerCellY,
            int horizontalEdge, int verticalEdge,
            int minimumSpanX, int minimumSpanY,
            int columnCount, int rowCount) {
        return CellLayout.calculateResizeCandidate(cellX, cellY, spanX, spanY,
                pointerCellX, pointerCellY, horizontalEdge, verticalEdge,
                minimumSpanX, minimumSpanY, columnCount, rowCount);
    }

    private static void assertCandidate(CellLayout.ResizeCandidate candidate,
            int cellX, int cellY, int spanX, int spanY) {
        assertEquals(cellX, candidate.cellX);
        assertEquals(cellY, candidate.cellY);
        assertEquals(spanX, candidate.spanX);
        assertEquals(spanY, candidate.spanY);
    }
}
