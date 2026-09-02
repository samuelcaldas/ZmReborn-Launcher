package org.zmreborn;

/**
 * Controller interface for scrollable surfaces that scroll when items are dragged to screen edges.
 */
public interface DragScroller {
    /**
     * Scrolls the underlying container left by one page or step.
     */
    void scrollLeft();

    /**
     * Scrolls the underlying container right by one page or step.
     */
    void scrollRight();
}
