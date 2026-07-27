package org.zmreborn;

final class DockDragTransaction {
    private final int mOriginalIndex;
    private int mDropIndex;
    private boolean mFinished;
    private boolean mSuccessful;

    DockDragTransaction(int originalIndex) {
        if (originalIndex < 0) {
            throw new IllegalArgumentException("originalIndex must be non-negative");
        }
        this.mOriginalIndex = originalIndex;
        this.mDropIndex = originalIndex;
    }

    int getOriginalIndex() {
        return this.mOriginalIndex;
    }

    void stageDrop(int dropIndex) {
        if (this.mFinished) {
            return;
        }
        if (dropIndex < 0) {
            throw new IllegalArgumentException("dropIndex must be non-negative");
        }
        this.mDropIndex = dropIndex;
    }

    int getInsertionIndex(int childCount) {
        int targetIndex = this.mDropIndex;
        if (targetIndex > this.mOriginalIndex) {
            targetIndex--;
        }
        return Math.max(0, Math.min(targetIndex, Math.max(0, childCount - 1)));
    }

    boolean finish(boolean successful) {
        if (this.mFinished) {
            return false;
        }
        this.mFinished = true;
        this.mSuccessful = successful;
        return true;
    }

    boolean cancel() {
        return finish(false);
    }

    boolean isSuccessful() {
        return this.mSuccessful;
    }

    static boolean shouldShowCouldNotMove(boolean successful, boolean targetFound) {
        return successful || targetFound;
    }
}
