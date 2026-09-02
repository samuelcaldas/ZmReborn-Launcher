package org.zmreborn;

import android.view.View;

/**
 * Source originating a drag operation that receives notifications when the drag concludes.
 */
public interface DragSource {
    /**
     * Called when a drag operation starting from this source is completed.
     *
     * @param target the drop target view that accepted the item, or null if drop failed
     * @param success true if the drop was accepted and processed successfully
     */
    void onDropCompleted(View target, boolean success);
}
