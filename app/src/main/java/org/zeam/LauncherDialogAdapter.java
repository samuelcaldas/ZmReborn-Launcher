package org.zeam;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class LauncherDialogAdapter extends BaseAdapter {
    public static final int ITEM_ADD = 0;
    public static final int ITEM_PREFERENCES = 2;
    public static final int ITEM_WALLPAPER = 1;
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

    public LauncherDialogAdapter(Launcher launcher) {
        Resources resources = launcher.getResources();
        this.mLayoutInflater = (LayoutInflater) launcher.getSystemService("layout_inflater");
        this.mListItems.add(new ListItem(resources, C0041R.string.add, C0041R.drawable.ic_launcher_shortcut, 0));
        this.mListItems.add(new ListItem(resources, C0041R.string.menu_wallpaper, C0041R.drawable.ic_launcher_wallpaper, 1));
        this.mListItems.add(new ListItem(resources, C0041R.string.menu_preferences, C0041R.drawable.ic_launcher_settings, 2));
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.mLayoutInflater.inflate(C0041R.layout.dialog_list_item, parent, false);
        }
        TextView textView = (TextView) convertView;
        ListItem listItem = (ListItem) getItem(position);
        textView.setCompoundDrawablesWithIntrinsicBounds(listItem.mImage, (Drawable) null, (Drawable) null, (Drawable) null);
        textView.setText(listItem.mText);
        textView.setTag(listItem);
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
