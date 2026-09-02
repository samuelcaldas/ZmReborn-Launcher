package org.zmreborn;

/**
 * Calculates page counts, page boundaries, and item index distributions for paged drawer grids.
 */
final class ApplicationsPagePartition {
    private ApplicationsPagePartition() {
    }

    /**
     * Calculates total pages required for the specified item count and grid dimensions.
     */
    static int calculatePageCount(int itemCount, int requestedRows, int requestedColumns) {
        int safeItemCount = Math.max(0, itemCount);
        int itemsPerPage = calculateItemsPerPage(requestedRows, requestedColumns);
        return (safeItemCount + itemsPerPage - 1) / itemsPerPage;
    }

    /**
     * Calculates the capacity of a single page given rows and columns.
     */
    static int calculateItemsPerPage(int requestedRows, int requestedColumns) {
        int rows = Math.max(1, requestedRows);
        int columns = Math.max(1, requestedColumns);
        return rows * columns;
    }

    /**
     * Calculates the starting item index for the given page.
     */
    static int calculatePageStart(int pageIndex, int requestedRows, int requestedColumns) {
        int safePageIndex = Math.max(0, pageIndex);
        return safePageIndex * calculateItemsPerPage(requestedRows, requestedColumns);
    }

    /**
     * Calculates the exclusive ending item index for the given page.
     */
    static int calculatePageEnd(int pageIndex, int itemCount, int requestedRows, int requestedColumns) {
        int safeItemCount = Math.max(0, itemCount);
        int start = calculatePageStart(pageIndex, requestedRows, requestedColumns);
        return Math.min(safeItemCount, start + calculateItemsPerPage(requestedRows, requestedColumns));
    }

    /**
     * Resolves which page an item ordinal belongs to.
     */
    static int pageIndexForItemOrdinal(int ordinal, int requestedRows, int requestedColumns) {
        int safeOrdinal = Math.max(0, ordinal);
        int itemsPerPage = calculateItemsPerPage(requestedRows, requestedColumns);
        return safeOrdinal / itemsPerPage;
    }
}
