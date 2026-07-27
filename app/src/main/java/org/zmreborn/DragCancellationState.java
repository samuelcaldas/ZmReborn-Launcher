package org.zmreborn;

final class DragCancellationState {
    private boolean mDropTargetActive;

    void reset() {
        this.mDropTargetActive = false;
    }

    void setDropTargetActive(boolean active) {
        this.mDropTargetActive = active;
    }

    boolean consumeDropTargetExit() {
        if (!this.mDropTargetActive) {
            return false;
        }
        this.mDropTargetActive = false;
        return true;
    }
}
