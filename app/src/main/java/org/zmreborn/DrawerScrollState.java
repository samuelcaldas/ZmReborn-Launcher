package org.zmreborn;

import java.util.List;

final class DrawerScrollState {
    private static final DrawerScrollState EMPTY = new DrawerScrollState(null, 0, 0);

    private final String mAnchorKey;
    private final int mFallbackPosition;
    private final int mTopOffset;

    private DrawerScrollState(
            String anchorKey,
            int fallbackPosition,
            int topOffset) {
        this.mAnchorKey = anchorKey;
        this.mFallbackPosition = Math.max(0, fallbackPosition);
        this.mTopOffset = topOffset;
    }

    static DrawerScrollState empty() {
        return EMPTY;
    }

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
        return Math.min(this.mFallbackPosition, items.size() - 1);
    }

    int getTopOffset() {
        return this.mTopOffset;
    }

    private int findAnchorPosition(List<ApplicationItemInfo> items) {
        if (this.mAnchorKey == null) {
            return -1;
        }
        for (int position = 0; position < items.size(); position++) {
            ApplicationItemInfo item = items.get(position);
            if (item != null && this.mAnchorKey.equals(item.getStableKey())) {
                return position;
            }
        }
        return -1;
    }
}
