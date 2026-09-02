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
 * Adapter providing direct long-press launcher actions.
 */
public class LauncherDialogAdapter extends BaseAdapter {
    public static final int ITEM_WALLPAPER = 3;
    public static final int ITEM_PREFERENCES = 4;
    private final LayoutInflater layoutInflater;
    private final ArrayList<ListItem> listItems = new ArrayList<>();

    /**
     * Describes a selectable launcher action.
     */
    public class ListItem {
        public final int actionTag;
        public final Drawable image;
        public final CharSequence text;

        /**
         * Creates an action using the supplied text, icon, and tag.
         */
        public ListItem(Resources res, int textResourceId, int imageResourceId, int actionTag) {
            this.text = res.getString(textResourceId);
            this.image = imageResourceId != -1 ? res.getDrawable(imageResourceId) : null;
            this.actionTag = actionTag;
        }
    }

    /**
     * Creates direct launcher actions for the supplied activity.
     */
    public LauncherDialogAdapter(Launcher launcher) {
        Resources resources = launcher.getResources();
        this.layoutInflater = (LayoutInflater) launcher.getSystemService("layout_inflater");
        this.listItems.add(new ListItem(resources, R.string.group_add_widgets,
                R.drawable.ic_launcher_appwidget, AddDialogAdapter.ITEM_WIDGETS));
        this.listItems.add(new ListItem(resources, R.string.group_add_shortcuts,
                R.drawable.ic_launcher_shortcut, AddDialogAdapter.ITEM_SHORTCUTS));
        this.listItems.add(new ListItem(resources, R.string.group_add_folders,
                R.drawable.ic_launcher_folder, AddDialogAdapter.ITEM_FOLDERS));
        this.listItems.add(new ListItem(resources, R.string.menu_wallpaper,
                R.drawable.ic_launcher_wallpaper, ITEM_WALLPAPER));
        this.listItems.add(new ListItem(resources, R.string.menu_preferences,
                R.drawable.ic_launcher_settings, ITEM_PREFERENCES));
    }

    /**
     * Returns the view for the action at {@code position}.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView == null
                ? this.layoutInflater.inflate(R.layout.dialog_list_item, parent, false)
                : convertView;
        TextView textView = (TextView) view;
        ListItem listItem = (ListItem) getItem(position);
        textView.setCompoundDrawablesWithIntrinsicBounds(listItem.image, (Drawable) null, (Drawable) null, (Drawable) null);
        textView.setText(listItem.text);
        textView.setTag(listItem);
        return view;
    }

    /**
     * Returns the number of available launcher actions.
     */
    @Override
    public int getCount() {
        return this.listItems.size();
    }

    /**
     * Returns the action at {@code position}.
     */
    @Override
    public Object getItem(int position) {
        return this.listItems.get(position);
    }

    /**
     * Returns the stable identifier for {@code position}.
     */
    @Override
    public long getItemId(int position) {
        return (long) position;
    }
}
