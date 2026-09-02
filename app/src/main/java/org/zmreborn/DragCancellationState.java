package org.zmreborn;

/**
 * Tracks drop target active state during drag operations to ensure clean exit event dispatch on cancellation.
 */
final class DragCancellationState {
    private boolean dropTargetActive;

    void reset() {
        this.dropTargetActive = false;
    }

    void setDropTargetActive(boolean active) {
        this.dropTargetActive = active;
    }

    boolean consumeDropTargetExit() {
        if (!this.dropTargetActive) {
            return false;
        }
        this.dropTargetActive = false;
        return true;
    }
}
