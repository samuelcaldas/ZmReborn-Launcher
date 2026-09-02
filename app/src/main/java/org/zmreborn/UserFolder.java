package org.zmreborn;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

/**
 * Folder view containing user-configured shortcuts with an integrated signal rail count indicator.
 */
public class UserFolder extends Folder implements DropTarget {
    private SignalRailView signalRail;
    private FrameLayout indicatorFrame;

    /**
     * Constructs a user folder with context and XML attributes.
     */
    public UserFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    static UserFolder fromXml(Context context) {
        return (UserFolder) LayoutInflater.from(context).inflate(R.layout.user_folder, null);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.indicatorFrame = (FrameLayout) findViewById(R.id.folder_indicator);
        if (this.indicatorFrame != null) {
            this.signalRail = new SignalRailView(getContext(), false);
            this.indicatorFrame.addView(this.signalRail, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    @Override
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        if (!(dragInfo instanceof ItemInfo) || this.folderInfo == null) {
            return false;
        }
        ItemInfo item = (ItemInfo) dragInfo;
        int itemType = item.itemType;
        return (itemType == 0 || itemType == 1) && item.container != this.folderInfo.id;
    }

    @Override
    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        if (this.content != null && this.content.getAdapter() instanceof ArrayAdapter) {
            ((ArrayAdapter) this.content.getAdapter()).add((ApplicationItemInfo) dragInfo);
        }
        if (this.launcher != null && this.folderInfo != null && dragInfo instanceof ApplicationItemInfo) {
            LauncherModel.addOrMoveItemInDatabase(this.launcher, (ApplicationItemInfo) dragInfo, this.folderInfo.id, 0, 0, 0);
        }
        updateSignalRail();
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

    @Override
    public void onDropCompleted(View target, boolean success) {
        if (success && this.content != null && this.content.getAdapter() instanceof ArrayAdapter) {
            ((ArrayAdapter) this.content.getAdapter()).remove(this.dragItem);
            updateSignalRail();
        }
    }

    @Override
    void bind(FolderInfo info) {
        super.bind(info);
        if (info instanceof UserFolderInfo) {
            setContentAdapter(new ApplicationsAdapter(getContext(), ((UserFolderInfo) info).contents));
        }
        updateSignalRail();
    }

    @Override
    void refreshPalette() {
        super.refreshPalette();
        if (this.signalRail != null) {
            this.signalRail.refreshPalette();
        }
    }

    private void updateSignalRail() {
        if (this.signalRail != null && this.folderInfo instanceof UserFolderInfo) {
            this.signalRail.setTotalItems(((UserFolderInfo) this.folderInfo).contents.size());
        }
    }

    @Override
    void onOpen() {
        super.onOpen();
        requestFocus();
    }
}
