package org.zmreborn;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class AddDialogAdapter extends BaseAdapter {
    public static final int ITEM_FOLDERS = 2;
    public static final int ITEM_SHORTCUTS = 1;
    public static final int ITEM_WIDGETS = 0;
    private final LayoutInflater mLayoutInflater;
    private final ArrayList<ListItem> mListItems = new ArrayList<>();

    public class ListItem {
        public final int mActionTag;
        public final Drawable mImage;
        public final CharSequence mText;

        public ListItem(Resources res, int textResourceId, int imageResourceId, int actionTag) {
            this.mText = res.getString(textResourceId);
            if (imageResourceId != -1) {
                this.mImage = res.getDrawable(imageResourceId);
            } else {
                this.mImage = null;
            }
            this.mActionTag = actionTag;
        }
    }

    public AddDialogAdapter(Launcher launcher) {
        Resources res = launcher.getResources();
        this.mLayoutInflater = (LayoutInflater) launcher.getSystemService("layout_inflater");
        this.mListItems.add(new ListItem(res, R.string.group_add_widgets, R.drawable.ic_launcher_appwidget, 0));
        this.mListItems.add(new ListItem(res, R.string.group_add_shortcuts, R.drawable.ic_launcher_shortcut, 1));
        this.mListItems.add(new ListItem(res, R.string.group_add_folders, R.drawable.ic_launcher_folder, 2));
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem listItem = (ListItem) getItem(position);
        if (convertView == null) {
            convertView = this.mLayoutInflater.inflate(R.layout.dialog_list_item, parent, false);
        }
        TextView textView = (TextView) convertView;
        textView.setTag(listItem);
        textView.setText(listItem.mText);
        textView.setCompoundDrawablesWithIntrinsicBounds(listItem.mImage, (Drawable) null, (Drawable) null, (Drawable) null);
        return convertView;
    }

    public int getCount() {
        return this.mListItems.size();
    }

    public Object getItem(int position) {
        return this.mListItems.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }
}
