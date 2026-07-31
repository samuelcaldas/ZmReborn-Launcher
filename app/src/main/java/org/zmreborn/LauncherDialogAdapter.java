package org.zmreborn;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

/** Provides direct long-press launcher actions. */
public class LauncherDialogAdapter extends BaseAdapter {
    public static final int ITEM_WALLPAPER = 3;
    public static final int ITEM_PREFERENCES = 4;
    private final LayoutInflater mLayoutInflater;
    private final ArrayList<ListItem> mListItems = new ArrayList<>();

    /** Describes a selectable launcher action. */
    public class ListItem {
        public final int mActionTag;
        public final Drawable mImage;
        public final CharSequence mText;

        /** Creates an action using the supplied text, icon, and tag. */
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

    /** Creates direct launcher actions for the supplied activity. */
    public LauncherDialogAdapter(Launcher launcher) {
        Resources resources = launcher.getResources();
        this.mLayoutInflater = (LayoutInflater) launcher.getSystemService("layout_inflater");
        this.mListItems.add(new ListItem(resources, R.string.group_add_widgets,
                R.drawable.ic_launcher_appwidget, AddDialogAdapter.ITEM_WIDGETS));
        this.mListItems.add(new ListItem(resources, R.string.group_add_shortcuts,
                R.drawable.ic_launcher_shortcut, AddDialogAdapter.ITEM_SHORTCUTS));
        this.mListItems.add(new ListItem(resources, R.string.group_add_folders,
                R.drawable.ic_launcher_folder, AddDialogAdapter.ITEM_FOLDERS));
        this.mListItems.add(new ListItem(resources, R.string.menu_wallpaper,
                R.drawable.ic_launcher_wallpaper, ITEM_WALLPAPER));
        this.mListItems.add(new ListItem(resources, R.string.menu_preferences,
                R.drawable.ic_launcher_settings, ITEM_PREFERENCES));
    }

    /** Returns the view for the action at {@code position}. */
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.mLayoutInflater.inflate(R.layout.dialog_list_item, parent, false);
        }
        TextView textView = (TextView) convertView;
        ListItem listItem = (ListItem) getItem(position);
        textView.setCompoundDrawablesWithIntrinsicBounds(listItem.mImage, (Drawable) null, (Drawable) null, (Drawable) null);
        textView.setText(listItem.mText);
        textView.setTag(listItem);
        return convertView;
    }

    /** Returns the number of available launcher actions. */
    public int getCount() {
        return this.mListItems.size();
    }

    /** Returns the action at {@code position}. */
    public Object getItem(int position) {
        return this.mListItems.get(position);
    }

    /** Returns the stable identifier for {@code position}. */
    public long getItemId(int position) {
        return (long) position;
    }
}
