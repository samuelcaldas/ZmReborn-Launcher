package org.zeam;

/** Computes bounded folder panel and scrollable content dimensions. */
final class FolderLayoutMetrics {
    private final int panelWidth;
    private final int panelHeight;
    private final int columns;
    private final int totalRows;
    private final int visibleRows;
    private final int cellWidth;
    private final int cellHeight;

    private FolderLayoutMetrics(int panelWidth, int panelHeight, int columns, int totalRows,
            int visibleRows, int cellWidth, int cellHeight) {
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.columns = columns;
        this.totalRows = totalRows;
        this.visibleRows = visibleRows;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    static FolderLayoutMetrics calculate(int viewportWidth, int viewportHeight, int safeMargin,
            int requestedWidth, int requestedHeight, int headerHeight, int requestedColumns,
            int itemCount, int minimumCellHeight) {
        int safeWidth = Math.max(1, viewportWidth - (Math.max(0, safeMargin) * 2));
        int safeHeight = Math.max(1, viewportHeight - (Math.max(0, safeMargin) * 2));
        int panelWidth = Math.min(Math.max(1, requestedWidth), safeWidth);
        int panelHeight = Math.min(Math.max(1, requestedHeight), safeHeight);
        int columns = Math.max(1, requestedColumns);
        int totalRows = Math.max(1, (Math.max(0, itemCount) + columns - 1) / columns);
        int bodyHeight = Math.max(1, panelHeight - Math.max(0, headerHeight));
        int visibleRows = Math.min(totalRows, Math.max(1, bodyHeight / Math.max(1, minimumCellHeight)));
        int contentWidth = Math.max(1, panelWidth);
        int cellWidth = Math.max(1, contentWidth / columns);
        int cellHeight = Math.max(1, bodyHeight / visibleRows);
        return new FolderLayoutMetrics(panelWidth, panelHeight, columns, totalRows, visibleRows,
                cellWidth, cellHeight);
    }

    int getPanelWidth() {
        return panelWidth;
    }

    int getPanelHeight() {
        return panelHeight;
    }

    int getColumns() {
        return columns;
    }

    int getTotalRows() {
        return totalRows;
    }

    int getVisibleRows() {
        return visibleRows;
    }

    int getCellWidth() {
        return cellWidth;
    }

    int getCellHeight() {
        return cellHeight;
    }

    boolean isScrollable() {
        return totalRows > visibleRows;
    }
}
