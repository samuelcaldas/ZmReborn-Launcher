package org.zmreborn;

import java.util.List;

/**
 * Encapsulates the drawer scroll state (anchor item and offset) across configuration and query changes.
 */
final class DrawerScrollState {
    private static final DrawerScrollState EMPTY = new DrawerScrollState(null, 0, 0);

    private final String anchorKey;
    private final int fallbackPosition;
    private final int topOffset;

    private DrawerScrollState(
            String anchorKey,
            int fallbackPosition,
            int topOffset) {
        this.anchorKey = anchorKey;
        this.fallbackPosition = Math.max(0, fallbackPosition);
        this.topOffset = topOffset;
    }

    /**
     * Returns an empty scroll state.
     */
    static DrawerScrollState empty() {
        return EMPTY;
    }

    /**
     * Captures current scroll state from the first visible anchor item.
     */
    static DrawerScrollState capture(
            ApplicationItemInfo anchor,
            int fallbackPosition,
            int topOffset) {
        if (anchor == null) {
            return new DrawerScrollState(null, fallbackPosition, topOffset);
        }
        return new DrawerScrollState(
                anchor.getStableKey(), fallbackPosition, topOffset);
    }

    boolean isEmpty() {
        return this == EMPTY;
    }

    int resolvePosition(List<ApplicationItemInfo> items) {
        if (items == null || items.isEmpty()) {
            return -1;
        }
        int anchorPosition = findAnchorPosition(items);
        if (anchorPosition >= 0) {
            return anchorPosition;
        }
        return Math.min(this.fallbackPosition, items.size() - 1);
    }

    int getTopOffset() {
        return this.topOffset;
    }

    private int findAnchorPosition(List<ApplicationItemInfo> items) {
        if (this.anchorKey == null) {
            return -1;
        }
        for (int position = 0; position < items.size(); position++) {
            ApplicationItemInfo item = items.get(position);
            if (item != null && this.anchorKey.equals(item.getStableKey())) {
                return position;
            }
        }
        return -1;
    }
}
