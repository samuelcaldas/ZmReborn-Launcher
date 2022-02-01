package org.zeam;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class UserFolder extends Folder implements DropTarget {
    public UserFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    static UserFolder fromXml(Context context) {
        return (UserFolder) LayoutInflater.from(context).inflate(C0041R.layout.user_folder, (ViewGroup) null);
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        ItemInfo item = (ItemInfo) dragInfo;
        int itemType = item.itemType;
        if ((itemType == 0 || itemType == 1) && item.container != this.mFolderInfo.f3id) {
            return true;
        }
        return false;
    }

    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo, Rect recycle) {
        return null;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        ((ArrayAdapter) this.mContent.getAdapter()).add((ApplicationItemInfo) dragInfo);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, (ApplicationItemInfo) dragInfo, this.mFolderInfo.f3id, 0, 0, 0);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    public void onDropCompleted(View target, boolean success) {
        if (success) {
            ((ArrayAdapter) this.mContent.getAdapter()).remove(this.mDragItem);
        }
    }

    /* access modifiers changed from: package-private */
    public void bind(FolderInfo info) {
        super.bind(info);
        setContentAdapter(new ApplicationsAdapter(getContext(), ((UserFolderInfo) info).contents));
    }

    /* access modifiers changed from: package-private */
    public void onOpen() {
        super.onOpen();
        requestFocus();
    }
}
