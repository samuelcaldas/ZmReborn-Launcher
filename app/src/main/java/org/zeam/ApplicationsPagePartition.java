package org.zeam;

final class ApplicationsPagePartition {
    private ApplicationsPagePartition() {
    }

    static int calculatePageCount(int itemCount, int requestedRows, int requestedColumns) {
        int safeItemCount = Math.max(0, itemCount);
        int itemsPerPage = calculateItemsPerPage(requestedRows, requestedColumns);
        return (safeItemCount + itemsPerPage - 1) / itemsPerPage;
    }

    static int calculateItemsPerPage(int requestedRows, int requestedColumns) {
        int rows = Math.max(1, requestedRows);
        int columns = Math.max(1, requestedColumns);
        return rows * columns;
    }

    static int calculatePageStart(int pageIndex, int requestedRows, int requestedColumns) {
        int safePageIndex = Math.max(0, pageIndex);
        return safePageIndex * calculateItemsPerPage(requestedRows, requestedColumns);
    }

    static int calculatePageEnd(int pageIndex, int itemCount, int requestedRows, int requestedColumns) {
        int safeItemCount = Math.max(0, itemCount);
        int start = calculatePageStart(pageIndex, requestedRows, requestedColumns);
        return Math.min(safeItemCount, start + calculateItemsPerPage(requestedRows, requestedColumns));
    }
}
