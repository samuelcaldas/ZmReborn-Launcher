package org.zmreborn;

import android.view.View;

/**
 * Controller interface coordinating drag operations, drop targets, and drag listeners.
 */
public interface DragController {
    int DRAG_ACTION_MOVE = 0;
    int DRAG_ACTION_COPY = 1;

    /**
     * Listener receiving notifications when drag operations start and finish.
     */
    public interface DragListener {
        /**
         * Called when a drag operation has ended.
         */
        void onDragEnd();

        /**
         * Called when a drag operation has started from a view and source.
         */
        void onDragStart(View view, DragSource source, Object info, int dragAction);
    }

    /**
     * Initiates a drag operation for the specified view and item info.
     */
    void startDrag(View view, DragSource source, Object info, int dragAction);

    /**
     * Cancels an ongoing drag operation.
     */
    void cancelDrag();
}
