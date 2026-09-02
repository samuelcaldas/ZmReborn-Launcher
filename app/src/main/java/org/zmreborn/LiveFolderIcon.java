package org.zmreborn;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Desktop icon representing a live folder that queries and displays dynamic provider content.
 */
public class LiveFolderIcon extends FolderIcon {

    /**
     * Constructs a live folder icon with context and XML attributes.
     */
    public LiveFolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructs a live folder icon with context.
     */
    public LiveFolderIcon(Context context) {
        super(context);
    }

    static LiveFolderIcon fromXml(int resId, Launcher launcher, ViewGroup group, LiveFolderInfo folderInfo) {
        LiveFolderIcon icon = (LiveFolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        Resources resources = launcher.getResources();
        Drawable drawable = folderInfo.icon;
        if (drawable == null) {
            drawable = Utilities.createIconThumbnail(resources.getDrawable(R.drawable.ic_launcher_folder), launcher);
            folderInfo.filtered = true;
        }
        icon.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        if (PreferencesUtil.isShowShortcutTitlesEnabled(launcher)) {
            icon.setText(folderInfo.title);
        }
        icon.setContentDescription(folderInfo.title);
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        return icon;
    }

    @Override
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        return false;
    }

    @Override
    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }
}
