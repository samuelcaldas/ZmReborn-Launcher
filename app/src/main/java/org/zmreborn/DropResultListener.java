package org.zmreborn;

import android.view.View;

/**
 * Listener for drag-and-drop completion results.
 */
interface DropResultListener {
    /**
     * Called when a drag operation has completed.
     *
     * @param target      target view that received the drop
     * @param success     true if drop succeeded
     * @param targetFound true if a valid drop target handled the event
     */
    void onDropCompleted(View target, boolean success, boolean targetFound);
}
