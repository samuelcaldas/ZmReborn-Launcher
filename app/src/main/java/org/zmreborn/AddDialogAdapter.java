package org.zmreborn;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Adapter presenting the "Add to Home screen" dialog options.
 */
public class AddDialogAdapter extends BaseAdapter {
    public static final int ITEM_FOLDERS = 2;
    public static final int ITEM_SHORTCUTS = 1;
    public static final int ITEM_WIDGETS = 0;
    private final LayoutInflater layoutInflater;
    private final ArrayList<ListItem> listItems = new ArrayList<>();

    /**
     * Item model for add dialog entries.
     */
    public class ListItem {
        public final int actionTag;
        public final Drawable image;
        public final CharSequence text;

        /**
         * Constructs a dialog list item with resource IDs.
         */
        public ListItem(Resources res, int textResourceId, int imageResourceId, int actionTag) {
            this.text = res.getString(textResourceId);
            this.image = imageResourceId != -1 ? res.getDrawable(imageResourceId) : null;
            this.actionTag = actionTag;
        }
    }

    /**
     * Constructs an add dialog adapter for the launcher.
     */
    public AddDialogAdapter(Launcher launcher) {
        Resources res = launcher.getResources();
        this.layoutInflater = (LayoutInflater) launcher.getSystemService("layout_inflater");
        this.listItems.add(new ListItem(res, R.string.group_add_widgets, R.drawable.ic_launcher_appwidget, 0));
        this.listItems.add(new ListItem(res, R.string.group_add_shortcuts, R.drawable.ic_launcher_shortcut, 1));
        this.listItems.add(new ListItem(res, R.string.group_add_folders, R.drawable.ic_launcher_folder, 2));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView == null
                ? this.layoutInflater.inflate(R.layout.dialog_list_item, parent, false)
                : convertView;
        ListItem listItem = (ListItem) getItem(position);
        TextView textView = (TextView) view;
        textView.setTag(listItem);
        textView.setText(listItem.text);
        textView.setCompoundDrawablesWithIntrinsicBounds(listItem.image, (Drawable) null, (Drawable) null, (Drawable) null);
        return view;
    }

    @Override
    public int getCount() {
        return this.listItems.size();
    }

    @Override
    public Object getItem(int position) {
        return this.listItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }
}
