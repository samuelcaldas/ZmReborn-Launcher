package org.zmreborn;

import org.zmreborn.LauncherSettings;

/**
 * Item model for built-in launcher widgets (such as the quick search widget).
 */
class Widget extends ItemInfo {
    int layoutResource;

    Widget() {
    }

    /**
     * Factory method creating a quick search widget item.
     */
    static Widget makeSearch() {
        Widget widget = new Widget();
        widget.itemType = LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH;
        widget.layoutResource = R.layout.widget_search;
        widget.spanX = 4;
        widget.spanY = 1;
        return widget;
    }
}
