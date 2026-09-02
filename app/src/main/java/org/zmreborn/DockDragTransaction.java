package org.zmreborn;

/**
 * Manages atomic drag-and-drop state transactions when moving items within the dock bar.
 */
final class DockDragTransaction {
    private final int originalIndex;
    private int dropIndex;
    private boolean finished;
    private boolean successful;

    DockDragTransaction(int originalIndex) {
        if (originalIndex < 0) {
            throw new IllegalArgumentException("originalIndex must be non-negative");
        }
        this.originalIndex = originalIndex;
        this.dropIndex = originalIndex;
    }

    int getOriginalIndex() {
        return this.originalIndex;
    }

    void stageDrop(int dropIndex) {
        if (this.finished) {
            return;
        }
        if (dropIndex < 0) {
            throw new IllegalArgumentException("dropIndex must be non-negative");
        }
        this.dropIndex = dropIndex;
    }

    int getInsertionIndex(int childCount) {
        int targetIndex = this.dropIndex;
        if (targetIndex > this.originalIndex) {
            targetIndex--;
        }
        return Math.max(0, Math.min(targetIndex, Math.max(0, childCount - 1)));
    }

    boolean finish(boolean successful) {
        if (this.finished) {
            return false;
        }
        this.finished = true;
        this.successful = successful;
        return true;
    }

    boolean cancel() {
        return finish(false);
    }

    boolean isSuccessful() {
        return this.successful;
    }

    static boolean shouldShowCouldNotMove(boolean successful, boolean targetFound) {
        return successful || targetFound;
    }
}
