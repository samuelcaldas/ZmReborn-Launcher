package org.zeam;

/** Computes drawer grid dimensions from measured bounds without allowing invalid values. */
final class DrawerLayoutMetrics {
    private final int columns;
    private final int rows;
    private final int cellWidth;
    private final int cellHeight;
    private final int availableWidth;
    private final int availableHeight;

    private DrawerLayoutMetrics(int columns, int rows, int cellWidth, int cellHeight,
            int availableWidth, int availableHeight) {
        this.columns = columns;
        this.rows = rows;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.availableWidth = availableWidth;
        this.availableHeight = availableHeight;
    }

    static DrawerLayoutMetrics calculate(int width, int height, int requestedRows,
            int requestedColumns, int horizontalPadding, int verticalPadding,
            int minimumCellWidth, int minimumCellHeight) {
        int safeWidth = Math.max(1, width - Math.max(0, horizontalPadding));
        int safeHeight = Math.max(1, height - Math.max(0, verticalPadding));
        int columns = clamp(requestedColumns, 1, Math.max(1, safeWidth / Math.max(1, minimumCellWidth)));
        int rows = clamp(requestedRows, 1, Math.max(1, safeHeight / Math.max(1, minimumCellHeight)));
        return new DrawerLayoutMetrics(columns, rows, Math.max(1, safeWidth / columns),
                Math.max(1, safeHeight / rows), safeWidth, safeHeight);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.min(Math.max(value, minimum), maximum);
    }

    int getColumns() {
        return columns;
    }

    int getRows() {
        return rows;
    }

    int getCellWidth() {
        return cellWidth;
    }

    int getCellHeight() {
        return cellHeight;
    }

    int getAvailableWidth() {
        return availableWidth;
    }

    int getAvailableHeight() {
        return availableHeight;
    }
}
