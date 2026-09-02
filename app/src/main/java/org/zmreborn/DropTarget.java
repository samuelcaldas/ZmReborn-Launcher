package org.zmreborn;

/**
 * Target view capable of accepting dropped items during drag operations.
 */
public interface DropTarget {
    /**
     * Queries whether this target can accept the dragged item at the specified coordinates.
     */
    boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo);

    /**
     * Called when a drag enters this target's bounds.
     */
    void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo);

    /**
     * Called when a drag exits this target's bounds.
     */
    void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo);

    /**
     * Called continuously while a drag is moved over this target.
     */
    void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo);

    /**
     * Called when an item is dropped onto this target.
     */
    void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo);
}
