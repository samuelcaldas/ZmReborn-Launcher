package org.zeam;

import android.view.View;

public interface DragController {
    public static final int DRAG_ACTION_COPY = 1;
    public static final int DRAG_ACTION_MOVE = 0;

    public interface DragListener {
        void onDragEnd();

        void onDragStart(View view, DragSource dragSource, Object obj, int i);
    }

    void startDrag(View view, DragSource dragSource, Object obj, int i);
}
