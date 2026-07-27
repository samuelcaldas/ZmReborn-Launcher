package org.zmreborn;

class Widget extends ItemInfo {
    int layoutResource;

    Widget() {
    }

    static Widget makeSearch() {
        Widget widget = new Widget();
        widget.itemType = 1001;
        widget.layoutResource = R.layout.widget_search;
        widget.spanX = 4;
        widget.spanY = 1;
        return widget;
    }
}
