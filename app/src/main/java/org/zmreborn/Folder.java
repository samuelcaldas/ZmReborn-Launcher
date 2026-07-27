package org.zmreborn;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Folder extends LinearLayout implements DragSource, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
    protected AbsListView mContent;
    protected ApplicationItemInfo mDragItem;
    protected DragController mDragger;
    protected FolderInfo mFolderInfo;
    protected Launcher mLauncher;
    protected TextView mTextView;

    public Folder(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlwaysDrawnWithCacheEnabled(false);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        Context context = getContext();
        this.mTextView = (TextView) findViewById(R.id.folder_name);
        this.mContent = (AbsListView) findViewById(R.id.folder_content);
        this.mContent.setOnItemClickListener(this);
        this.mContent.setOnItemLongClickListener(this);
        ImageButton renameButton = (ImageButton) findViewById(R.id.folder_button_rename);
        renameButton.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        renameButton.setContentDescription(context.getString(R.string.accessibility_folder_rename));
        renameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Folder.this.mLauncher.closeFolder(Folder.this);
                Folder.this.mLauncher.showRenameDialog(Folder.this.mFolderInfo);
            }
        });
        ImageButton closeButton = (ImageButton) findViewById(R.id.folder_button_close);
        closeButton.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        closeButton.setContentDescription(context.getString(R.string.accessibility_folder_close));
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Folder.this.mLauncher.closeFolder(Folder.this);
            }
        });
        this.mContent.setSelector(SelectorDrawable.createSelector(context, this.mContent instanceof GridView));
    }

    public void onItemClick(AdapterView parent, View view, int position, long id) {
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) parent.getItemAtPosition(position);
        if (view != null) {
            Rect sourceBounds = new Rect();
            view.getGlobalVisibleRect(sourceBounds);
            try {
                applicationItemInfo.intent.setSourceBounds(sourceBounds);
            } catch (NoSuchMethodError e) {
            }
        }
        this.mLauncher.startActivitySafely(applicationItemInfo.intent);
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!view.isInTouchMode()) {
            return false;
        }
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) parent.getItemAtPosition(position);
        this.mDragger.startDrag(view, this, applicationItemInfo, 1);
        this.mDragItem = applicationItemInfo;
        this.mLauncher.closeFolder(this);
        return true;
    }

    public void setDragger(DragController dragger) {
        this.mDragger = dragger;
    }

    public void onDropCompleted(View target, boolean success) {
    }

    /* access modifiers changed from: package-private */
    public void setContentAdapter(BaseAdapter adapter) {
        this.mContent.setAdapter(adapter);
    }

    /* access modifiers changed from: package-private */
    public void notifyDataSetChanged() {
        ((BaseAdapter) this.mContent.getAdapter()).notifyDataSetChanged();
    }

    /* access modifiers changed from: package-private */
    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    /* access modifiers changed from: package-private */
    public FolderInfo getInfo() {
        return this.mFolderInfo;
    }

    /* access modifiers changed from: package-private */
    public void onOpen() {
        this.mContent.requestLayout();
    }

    /* access modifiers changed from: package-private */
    public void onClose() {
        Workspace workspace = this.mLauncher.getWorkspace();
        workspace.getChildAt(workspace.getCurrentScreen()).requestFocus();
    }

    private int getGridViewColumns(GridView gridView) {
        return gridView.getNumColumns();
    }

    /* access modifiers changed from: package-private */
    public void handleFolderKeyEvent(int keyCode) {
        if (keyCode == 4) {
            this.mLauncher.closeFolder(this);
        } else if (this.mContent instanceof GridView) {
            GridView gridView = (GridView) this.mContent;
            int selection = gridView.getSelectedItemPosition();
            int newSelection = selection;
            int columns = getGridViewColumns(gridView);
            int count = this.mContent.getAdapter().getCount();
            switch (keyCode) {
                case 19:
                    if (selection <= columns - 1) {
                        this.mLauncher.closeFolder(this);
                    } else {
                        newSelection = selection - columns;
                    }
                    break;
                case 20:
                    if (selection >= count - columns) {
                        return;
                    }
                    newSelection = Math.min(selection + columns, count - 1);
                    break;
                case 21:
                    if ((selection % columns) == 0) {
                        this.mLauncher.closeFolder(this);
                    } else {
                        newSelection = selection - 1;
                    }
                    break;
                case 22:
                    if ((selection % columns) == (columns - 1)) {
                        return;
                    }
                    newSelection = Math.min(selection + 1, count - 1);
                    break;
            }
            if (newSelection != selection) {
                gridView.setSelection(newSelection);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void bind(FolderInfo info) {
        this.mFolderInfo = info;
        this.mTextView.setText(info.title);
        int itemCount = 0;
        if (info instanceof UserFolderInfo) {
            itemCount = ((UserFolderInfo) info).contents.size();
        }
        String description = getContext().getResources().getString(R.string.accessibility_folder_with_items, info.title, itemCount);
        setContentDescription(description);
    }
}
