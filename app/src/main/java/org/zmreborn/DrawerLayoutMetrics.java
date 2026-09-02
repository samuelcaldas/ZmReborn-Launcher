package org.zmreborn;

/**
 * Computes drawer grid dimensions from measured bounds without allowing invalid values.
 */
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

    /**
     * Calculates layout metrics given available dimensions and constraints.
     */
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
        return this.columns;
    }

    int getRows() {
        return this.rows;
    }

    int getCellWidth() {
        return this.cellWidth;
    }

    int getCellHeight() {
        return this.cellHeight;
    }

    int getAvailableWidth() {
        return this.availableWidth;
    }

    int getAvailableHeight() {
        return this.availableHeight;
    }

    int columnLeft(int columnIndex) {
        validateColumnIndex(columnIndex);
        return columnIndex * this.availableWidth / this.columns;
    }

    int columnRight(int columnIndex) {
        validateColumnIndex(columnIndex);
        return (columnIndex + 1) * this.availableWidth / this.columns;
    }

    int rowTop(int rowIndex) {
        validateRowIndex(rowIndex);
        return rowIndex * this.availableHeight / this.rows;
    }

    int rowBottom(int rowIndex) {
        validateRowIndex(rowIndex);
        return (rowIndex + 1) * this.availableHeight / this.rows;
    }

    private void validateColumnIndex(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= this.columns) {
            throw new IllegalArgumentException(
                    "Column index " + columnIndex + " out of range [0, " + this.columns + ")");
        }
    }

    private void validateRowIndex(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= this.rows) {
            throw new IllegalArgumentException(
                    "Row index " + rowIndex + " out of range [0, " + this.rows + ")");
        }
    }
}
