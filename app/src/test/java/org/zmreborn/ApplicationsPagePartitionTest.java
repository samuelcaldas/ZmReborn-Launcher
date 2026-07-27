package org.zmreborn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApplicationsPagePartitionTest {
    @Test
    public void emptyApplicationListHasNoPages() {
        assertEquals(0, ApplicationsPagePartition.calculatePageCount(0, 4, 5));
    }

    @Test
    public void exactCapacityUsesOnePage() {
        assertEquals(1, ApplicationsPagePartition.calculatePageCount(20, 4, 5));
    }

    @Test
    public void overflowUsesCeilingPageCount() {
        assertEquals(2, ApplicationsPagePartition.calculatePageCount(21, 4, 5));
    }

    @Test
    public void invalidDimensionsStillProvideReachableCapacity() {
        assertEquals(1, ApplicationsPagePartition.calculateItemsPerPage(0, -1));
        assertEquals(2, ApplicationsPagePartition.calculatePageCount(2, 0, 0));
    }

    @Test
    public void pageBoundsClampToAvailableItems() {
        assertEquals(6, ApplicationsPagePartition.calculatePageStart(1, 2, 3));
        assertEquals(7, ApplicationsPagePartition.calculatePageEnd(1, 7, 2, 3));
        assertEquals(7, ApplicationsPagePartition.calculatePageEnd(4, 7, 2, 3));
    }

    @Test
    public void indexClampingPreventsNegativePageStart() {
        assertEquals(0, ApplicationsPagePartition.calculatePageStart(0, 2, 3));
        assertEquals(0, ApplicationsPagePartition.calculatePageStart(-1, 2, 3));
    }

    @Test
    public void indexClampingPreventsNegativePageEnd() {
        assertTrue(ApplicationsPagePartition.calculatePageEnd(-1, 5, 2, 3) >= 0);
        assertTrue(ApplicationsPagePartition.calculatePageEnd(0, 5, 2, 3) >= 0);
    }

    @Test
    public void indexClampingPreventsExcessivePageStart() {
        int totalItems = 100;
        int itemsPerPage = 10;
        int pageSize = 5;
        int lastPageIndex = ApplicationsPagePartition.calculatePageCount(totalItems, itemsPerPage, pageSize) - 1;
        int startIndex = ApplicationsPagePartition.calculatePageStart(lastPageIndex, itemsPerPage, pageSize);
        assertTrue("Start index must be within bounds", startIndex < totalItems);
    }

    @Test
    public void indexClampingPreventsExcessivePageEnd() {
        int totalItems = 100;
        int itemsPerPage = 10;
        int pageSize = 5;
        int lastPageIndex = ApplicationsPagePartition.calculatePageCount(totalItems, itemsPerPage, pageSize) - 1;
        int endIndex = ApplicationsPagePartition.calculatePageEnd(lastPageIndex, totalItems, itemsPerPage, pageSize);
        assertTrue("End index must not exceed total items", endIndex <= totalItems);
    }

    @Test
    public void largePageIndexClamps() {
        int result = ApplicationsPagePartition.calculatePageStart(1000, 2, 3);
        assertTrue("Must handle large page index", result >= 0);
    }
}
