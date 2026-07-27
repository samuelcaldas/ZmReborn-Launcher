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
import org.zmreborn.LauncherSettings;

class LiveFolderAdapter extends CursorAdapter {
    private final HashMap<Long, SoftReference<Drawable>> mCustomIcons = new HashMap<>();
    private final HashMap<String, Drawable> mIcons = new HashMap<>();
    private LayoutInflater mInflater;
    private boolean mIsList;
    private final Launcher mLauncher;

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    LiveFolderAdapter(Launcher launcher, LiveFolderInfo info, Cursor cursor) {
        super(launcher, cursor, true);
        boolean z = true;
        this.mLauncher = launcher;
        this.mInflater = LayoutInflater.from(launcher);
        this.mLauncher.startManagingCursor(getCursor());
        this.mIsList = info.displayMode != 2 ? false : z;
    }

    static Cursor query(Context context, LiveFolderInfo info) {
        return context.getContentResolver().query(info.uri, (String[]) null, (String) null, (String[]) null, "name ASC");
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view;
        ViewHolder viewHolder = new ViewHolder();
        if (!this.mIsList) {
            view = this.mInflater.inflate(R.layout.application_boxed_grid, parent, false);
        } else {
            view = this.mInflater.inflate(R.layout.application_list, parent, false);
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

    public void bindView(View view, Context context, Cursor cursor) {
        boolean hasIcon;
        int i;
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.f4id = cursor.getLong(viewHolder.idIndex);
        Drawable icon = loadIcon(context, cursor, viewHolder);
        viewHolder.name.setText(cursor.getString(viewHolder.nameIndex));
        if (!this.mIsList) {
            viewHolder.name.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, icon, (Drawable) null, (Drawable) null);
        } else {
            if (icon != null) {
                hasIcon = true;
            } else {
                hasIcon = false;
            }
            ImageView imageView = viewHolder.icon;
            if (hasIcon) {
                i = 0;
            } else {
                i = 8;
            }
            imageView.setVisibility(i);
            if (hasIcon) {
                viewHolder.icon.setImageDrawable(icon);
            }
            if (viewHolder.descriptionIndex != -1) {
                String description = cursor.getString(viewHolder.descriptionIndex);
                if (description != null) {
                    viewHolder.description.setText(description);
                    viewHolder.description.setVisibility(0);
                } else {
                    viewHolder.description.setVisibility(8);
                }
            } else {
                viewHolder.description.setVisibility(8);
            }
        }
        if (viewHolder.intentIndex != -1) {
            try {
                viewHolder.intent = Intent.parseUri(cursor.getString(viewHolder.intentIndex), 0);
            } catch (URISyntaxException e) {
            }
        } else {
            viewHolder.useBaseIntent = true;
        }
    }

    private Drawable loadIcon(Context context, Cursor cursor, ViewHolder holder) {
        Drawable icon = null;
        byte[] data = null;
        if (holder.iconBitmapIndex != -1) {
            data = cursor.getBlob(holder.iconBitmapIndex);
        }
        if (data != null) {
            SoftReference<Drawable> reference = this.mCustomIcons.get(Long.valueOf(holder.f4id));
            if (reference != null) {
                icon = reference.get();
            }
            if (icon != null) {
                return icon;
            }
            Drawable icon2 = new FastBitmapDrawable(Utilities.createBitmapThumbnail(BitmapFactory.decodeByteArray(data, 0, data.length), context));
            this.mCustomIcons.put(Long.valueOf(holder.f4id), new SoftReference(icon2));
            return icon2;
        } else if (holder.iconResourceIndex == -1 || holder.iconPackageIndex == -1) {
            return null;
        } else {
            String resource = cursor.getString(holder.iconResourceIndex);
            Drawable icon3 = this.mIcons.get(resource);
            if (icon3 != null) {
                return icon3;
            }
            try {
                Resources resources = context.getPackageManager().getResourcesForApplication(cursor.getString(holder.iconPackageIndex));
                icon3 = Utilities.createIconThumbnail(resources.getDrawable(resources.getIdentifier(resource, (String) null, (String) null)), context);
                this.mIcons.put(resource, icon3);
                return icon3;
            } catch (Exception e) {
                return icon3;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void cleanup() {
        for (Drawable icon : this.mIcons.values()) {
            icon.setCallback((Drawable.Callback) null);
        }
        this.mIcons.clear();
        for (SoftReference<Drawable> icon2 : this.mCustomIcons.values()) {
            Drawable drawable = icon2.get();
            if (drawable != null) {
                drawable.setCallback((Drawable.Callback) null);
            }
        }
        this.mCustomIcons.clear();
        Cursor cursor = getCursor();
        if (cursor != null) {
            try {
                cursor.close();
            } finally {
                this.mLauncher.stopManagingCursor(cursor);
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

        /* renamed from: id */
        long f4id;
        int idIndex;
        Intent intent;
        int intentIndex = -1;
        TextView name;
        int nameIndex;
        boolean useBaseIntent;

        ViewHolder() {
        }
    }
}
