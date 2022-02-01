package org.zeam;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class FolderIcon extends BubbleTextView implements DropTarget {
    private Drawable mCloseIcon;
    private UserFolderInfo mInfo;
    private Launcher mLauncher;
    private Drawable mOpenIcon;

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderIcon(Context context) {
        super(context);
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group, UserFolderInfo folderInfo) {
        Resources resources = launcher.getResources();
        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        Drawable drawableOpen = resources.getDrawable(C0041R.drawable.ic_launcher_folder_open);
        Drawable drawableClosed = resources.getDrawable(C0041R.drawable.ic_launcher_folder);
        icon.mCloseIcon = Utilities.createIconThumbnail(drawableClosed, launcher);
        icon.mOpenIcon = drawableOpen;
        icon.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, drawableClosed, (Drawable) null, (Drawable) null);
        if (PreferencesUtil.isShowShortcutTitlesEnabled(launcher)) {
            icon.setText(folderInfo.title);
        }
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        return icon;
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        ItemInfo itemInfo = (ItemInfo) dragInfo;
        int itemType = itemInfo.itemType;
        if ((itemType == 0 || itemType == 1) && itemInfo.container != this.mInfo.f3id) {
            return true;
        }
        return false;
    }

    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo, Rect recycle) {
        return null;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        ApplicationItemInfo item = (ApplicationItemInfo) dragInfo;
        this.mInfo.add(item);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, item, this.mInfo.f3id, 0, 0, 0);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.mOpenIcon, (Drawable) null, (Drawable) null);
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.mCloseIcon, (Drawable) null, (Drawable) null);
    }
}
