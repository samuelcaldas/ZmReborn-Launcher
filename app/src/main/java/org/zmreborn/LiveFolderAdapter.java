package org.zmreborn;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Cursor adapter for populating live folder views from external content providers.
 */
class LiveFolderAdapter extends CursorAdapter {
    private final HashMap<Long, SoftReference<Drawable>> customIcons = new HashMap<>();
    private final HashMap<String, Drawable> icons = new HashMap<>();
    private final LayoutInflater inflater;
    private final boolean isList;
    private final Launcher launcher;

    LiveFolderAdapter(Launcher launcher, LiveFolderInfo info, Cursor cursor) {
        super(launcher, cursor, true);
        this.launcher = launcher;
        this.inflater = LayoutInflater.from(launcher);
        this.launcher.startManagingCursor(getCursor());
        this.isList = info.displayMode == 2;
    }

    static Cursor query(Context context, LiveFolderInfo info) {
        return context.getContentResolver().query(info.uri, null, null, null, "name ASC");
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view;
        ViewHolder viewHolder = new ViewHolder();
        if (!this.isList) {
            view = this.inflater.inflate(R.layout.application_boxed_grid, parent, false);
        } else {
            view = this.inflater.inflate(R.layout.application_list, parent, false);
            viewHolder.description = (TextView) view.findViewById(R.id.description);
            viewHolder.icon = (ImageView) view.findViewById(R.id.icon);
        }
        viewHolder.name = (TextView) view.findViewById(R.id.name);
        viewHolder.idIndex = cursor.getColumnIndexOrThrow("_id");
        viewHolder.nameIndex = cursor.getColumnIndexOrThrow("name");
        viewHolder.descriptionIndex = cursor.getColumnIndex("description");
        viewHolder.intentIndex = cursor.getColumnIndex(LauncherSettings.BaseLauncherColumns.INTENT);
        viewHolder.iconBitmapIndex = cursor.getColumnIndex("icon_bitmap");
        viewHolder.iconResourceIndex = cursor.getColumnIndex("icon_resource");
        viewHolder.iconPackageIndex = cursor.getColumnIndex("icon_package");
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.id = cursor.getLong(viewHolder.idIndex);
        Drawable icon = loadIcon(context, cursor, viewHolder);
        viewHolder.name.setText(cursor.getString(viewHolder.nameIndex));
        if (!this.isList) {
            viewHolder.name.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
        } else {
            bindListView(viewHolder, icon, cursor);
        }
        if (viewHolder.intentIndex != -1) {
            try {
                viewHolder.intent = Intent.parseUri(cursor.getString(viewHolder.intentIndex), 0);
            } catch (URISyntaxException ignored) {
            }
        } else {
            viewHolder.useBaseIntent = true;
        }
    }

    private void bindListView(ViewHolder viewHolder, Drawable icon, Cursor cursor) {
        boolean hasIcon = icon != null;
        viewHolder.icon.setVisibility(hasIcon ? View.VISIBLE : View.GONE);
        if (hasIcon) {
            viewHolder.icon.setImageDrawable(icon);
        }
        if (viewHolder.descriptionIndex != -1) {
            String description = cursor.getString(viewHolder.descriptionIndex);
            if (description != null) {
                viewHolder.description.setText(description);
                viewHolder.description.setVisibility(View.VISIBLE);
            } else {
                viewHolder.description.setVisibility(View.GONE);
            }
        } else {
            viewHolder.description.setVisibility(View.GONE);
        }
    }

    private Drawable loadIcon(Context context, Cursor cursor, ViewHolder holder) {
        byte[] data = null;
        if (holder.iconBitmapIndex != -1) {
            data = cursor.getBlob(holder.iconBitmapIndex);
        }
        if (data != null) {
            return loadBitmapIcon(context, holder.id, data);
        }
        if (holder.iconResourceIndex == -1 || holder.iconPackageIndex == -1) {
            return null;
        }
        return loadResourceIcon(context, cursor, holder);
    }

    private Drawable loadBitmapIcon(Context context, long iconId, byte[] data) {
        SoftReference<Drawable> reference = this.customIcons.get(iconId);
        if (reference != null) {
            Drawable cached = reference.get();
            if (cached != null) {
                return cached;
            }
        }
        Drawable icon = new FastBitmapDrawable(Utilities.createBitmapThumbnail(
                BitmapFactory.decodeByteArray(data, 0, data.length), context));
        this.customIcons.put(iconId, new SoftReference<>(icon));
        return icon;
    }

    private Drawable loadResourceIcon(Context context, Cursor cursor, ViewHolder holder) {
        String resource = cursor.getString(holder.iconResourceIndex);
        Drawable cached = this.icons.get(resource);
        if (cached != null) {
            return cached;
        }
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(
                    cursor.getString(holder.iconPackageIndex));
            Drawable icon = Utilities.createIconThumbnail(resources.getDrawable(
                    resources.getIdentifier(resource, null, null)), context);
            this.icons.put(resource, icon);
            return icon;
        } catch (Exception ignored) {
            return null;
        }
    }

    void cleanup() {
        for (Drawable icon : this.icons.values()) {
            if (icon != null) {
                icon.setCallback(null);
            }
        }
        this.icons.clear();
        for (SoftReference<Drawable> ref : this.customIcons.values()) {
            Drawable drawable = ref != null ? ref.get() : null;
            if (drawable != null) {
                drawable.setCallback(null);
            }
        }
        this.customIcons.clear();
        Cursor cursor = getCursor();
        if (cursor != null) {
            try {
                cursor.close();
            } finally {
                this.launcher.stopManagingCursor(cursor);
            }
        }
    }

    static class ViewHolder {
        TextView description;
        int descriptionIndex = -1;
        ImageView icon;
        int iconBitmapIndex = -1;
        int iconPackageIndex = -1;
        int iconResourceIndex = -1;
        long id;
        int idIndex;
        Intent intent;
        int intentIndex = -1;
        TextView name;
        int nameIndex;
        boolean useBaseIntent;
    }
}
