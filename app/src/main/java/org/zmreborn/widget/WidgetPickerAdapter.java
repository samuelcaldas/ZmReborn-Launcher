package org.zmreborn.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.zmreborn.R;

/** Renders widget entries as accessible preview cards. */
public final class WidgetPickerAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final ArrayList<WidgetPickerEntry> entries;

    /** Creates adapter from immutable picker entry snapshot. */
    public WidgetPickerAdapter(Context context, List<WidgetPickerEntry> entries) {
        if (context == null || entries == null) {
            throw new IllegalArgumentException("Widget picker adapter requires context and entries");
        }
        this.inflater = LayoutInflater.from(context);
        this.entries = new ArrayList<>(entries);
    }

    /** Returns number of preview cards. */
    public int getCount() {
        return this.entries.size();
    }

    /** Returns entry at {@code position}. */
    public WidgetPickerEntry getItem(int position) {
        return this.entries.get(position);
    }

    /** Returns stable list position identifier. */
    public long getItemId(int position) {
        return position;
    }

    /** Returns bound preview-card view. */
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = this.inflater.inflate(R.layout.widget_picker_item, parent, false);
        }
        WidgetPickerEntry entry = getItem(position);
        ImageView preview = (ImageView) row.findViewById(R.id.widget_preview);
        TextView label = (TextView) row.findViewById(R.id.widget_label);
        TextView detail = (TextView) row.findViewById(R.id.widget_detail);
        preview.setImageDrawable(entry.getPreview());
        preview.setContentDescription(null);
        label.setText(entry.getLabel());
        detail.setText(entry.getDetail());
        row.setTag(entry);
        row.setContentDescription(row.getResources().getString(
                R.string.widget_picker_accessibility_item,
                entry.getLabel(), entry.getDetail()));
        return row;
    }
}
