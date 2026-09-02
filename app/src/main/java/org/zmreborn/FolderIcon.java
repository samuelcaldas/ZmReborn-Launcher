package org.zmreborn;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Desktop and dock icon representing a user folder of grouped shortcuts.
 */
public class FolderIcon extends BubbleTextView implements DropTarget {
    private Drawable closeIcon;
    private UserFolderInfo info;
    private Launcher launcher;
    private Drawable openIcon;

    /**
     * Constructs a FolderIcon with context and XML attributes.
     */
    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructs a FolderIcon with context.
     */
    public FolderIcon(Context context) {
        super(context);
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group, UserFolderInfo folderInfo) {
        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        Drawable drawableOpen = launcher.getDrawable(R.drawable.ic_launcher_folder_open);
        Drawable drawableClosed = launcher.getDrawable(R.drawable.ic_launcher_folder);
        icon.closeIcon = Utilities.createIconThumbnail(drawableClosed, launcher);
        icon.openIcon = drawableOpen;
        icon.setCompoundDrawablesWithIntrinsicBounds(null, drawableClosed, null, null);
        if (PreferencesUtil.isShowShortcutTitlesEnabled(launcher)) {
            icon.setText(folderInfo.title);
        }
        int itemCount = folderInfo.contents.size();
        String description = launcher.getResources().getString(
                R.string.accessibility_folder_with_items, folderInfo.title, itemCount);
        icon.setContentDescription(description);
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.info = folderInfo;
        icon.launcher = launcher;
        return icon;
    }

    @Override
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        if (!(dragInfo instanceof ItemInfo) || this.info == null) {
            return false;
        }
        ItemInfo itemInfo = (ItemInfo) dragInfo;
        int itemType = itemInfo.itemType;
        return (itemType == 0 || itemType == 1) && itemInfo.container != this.info.id;
    }

    @Override
    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        if (dragInfo instanceof ApplicationItemInfo && this.info != null) {
            ApplicationItemInfo item = (ApplicationItemInfo) dragInfo;
            this.info.add(item);
            if (this.launcher != null) {
                LauncherModel.addOrMoveItemInDatabase(this.launcher, item, this.info.id, 0, 0, 0);
            }
        }
    }

    @Override
    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        setCompoundDrawablesWithIntrinsicBounds(null, this.openIcon, null, null);
    }

    @Override
    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        setCompoundDrawablesWithIntrinsicBounds(null, this.closeIcon, null, null);
    }
}
