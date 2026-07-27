package org.zmreborn;

public interface DropTarget {
    boolean acceptDrop(DragSource dragSource, int i, int i2, int i3, int i4, Object obj);

    void onDragEnter(DragSource dragSource, int i, int i2, int i3, int i4, Object obj);

    void onDragExit(DragSource dragSource, int i, int i2, int i3, int i4, Object obj);

    void onDragOver(DragSource dragSource, int i, int i2, int i3, int i4, Object obj);

    void onDrop(DragSource dragSource, int i, int i2, int i3, int i4, Object obj);
}
