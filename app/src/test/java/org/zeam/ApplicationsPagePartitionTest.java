package org.zeam;

import static org.junit.Assert.assertEquals;

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
}
