package org.zmreborn;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.zmreborn.theme.WallpaperColorExtractor;

/**
 * Base popup container view displaying a folder's shortcuts and providing rename and close actions.
 */
public class Folder extends LinearLayout implements DragSource, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
    protected AbsListView content;
    protected ApplicationItemInfo dragItem;
    protected DragController dragger;
    protected FolderInfo folderInfo;
    protected Launcher launcher;
    protected TextView textView;

    /**
     * Constructs a folder layout with context and XML attributes.
     */
    public Folder(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlwaysDrawnWithCacheEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setElevation(getResources().getDimension(R.dimen.elevation_folder));
        Context context = getContext();
        this.textView = (TextView) findViewById(R.id.folder_name);
        this.content = (AbsListView) findViewById(R.id.folder_content);
        refreshPalette();
        this.content.setOnItemClickListener(this);
        this.content.setOnItemLongClickListener(this);
        setupButtons(context);
        this.content.setSelector(SelectorDrawable.createSelector(context, this.content instanceof GridView));
    }

    private void setupButtons(Context context) {
        ImageButton renameButton = (ImageButton) findViewById(R.id.folder_button_rename);
        renameButton.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        renameButton.setContentDescription(context.getString(R.string.accessibility_folder_rename));
        renameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Folder.this.launcher != null) {
                    Folder.this.launcher.closeFolder(Folder.this);
                    Folder.this.launcher.showRenameDialog(Folder.this.folderInfo);
                }
            }
        });
        ImageButton closeButton = (ImageButton) findViewById(R.id.folder_button_close);
        closeButton.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        closeButton.setContentDescription(context.getString(R.string.accessibility_folder_close));
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Folder.this.launcher != null) {
                    Folder.this.launcher.closeFolder(Folder.this);
                }
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) parent.getItemAtPosition(position);
        if (view != null && applicationItemInfo != null && applicationItemInfo.intent != null) {
            Rect sourceBounds = new Rect();
            view.getGlobalVisibleRect(sourceBounds);
            applicationItemInfo.intent.setSourceBounds(sourceBounds);
        }
        if (this.launcher != null && applicationItemInfo != null) {
            this.launcher.startActivitySafely(applicationItemInfo.intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!view.isInTouchMode()) {
            return false;
        }
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) parent.getItemAtPosition(position);
        if (this.dragger != null) {
            this.dragger.startDrag(view, this, applicationItemInfo, DragController.DRAG_ACTION_COPY);
        }
        this.dragItem = applicationItemInfo;
        if (this.launcher != null) {
            this.launcher.closeFolder(this);
        }
        return true;
    }

    /**
     * Sets the drag controller used to initiate dragging folder items.
     */
    public void setDragger(DragController dragger) {
        this.dragger = dragger;
    }

    @Override
    public void onDropCompleted(View target, boolean success) {
    }

    void setContentAdapter(BaseAdapter adapter) {
        this.content.setAdapter(adapter);
    }

    void notifyDataSetChanged() {
        if (this.content != null && this.content.getAdapter() instanceof BaseAdapter) {
            ((BaseAdapter) this.content.getAdapter()).notifyDataSetChanged();
        }
    }

    void refreshPalette() {
        Context context = getContext();
        if (this.textView != null) {
            this.textView.setTextColor(WallpaperColorExtractor.getOnSurface(context));
        }
        tintBackground(WallpaperColorExtractor.getSurface(context));
        if (this.content != null && this.content.getAdapter() instanceof BaseAdapter) {
            ((BaseAdapter) this.content.getAdapter()).notifyDataSetChanged();
        }
        invalidate();
    }

    void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    FolderInfo getInfo() {
        return this.folderInfo;
    }

    void onOpen() {
        if (this.content != null) {
            this.content.requestLayout();
        }
    }

    void onClose() {
        if (this.launcher != null) {
            Workspace workspace = this.launcher.getWorkspace();
            if (workspace != null) {
                View child = workspace.getChildAt(workspace.getCurrentScreen());
                if (child != null) {
                    child.requestFocus();
                }
            }
        }
    }

    private void tintBackground(int color) {
        Drawable background = getBackground();
        if (background == null) {
            return;
        }
        background.mutate().setTint(color);
    }

    void handleFolderKeyEvent(int keyCode) {
        if (keyCode == 4) {
            if (this.launcher != null) {
                this.launcher.closeFolder(this);
            }
            return;
        }
        if (!(this.content instanceof GridView)) {
            return;
        }
        GridView gridView = (GridView) this.content;
        int selection = gridView.getSelectedItemPosition();
        int columns = gridView.getNumColumns();
        int count = this.content.getAdapter().getCount();
        int newSelection = computeNewKeySelection(keyCode, selection, columns, count);
        if (newSelection != selection) {
            gridView.setSelection(newSelection);
        }
    }

    private int computeNewKeySelection(int keyCode, int selection, int columns, int count) {
        if (keyCode == 19) {
            if (selection <= columns - 1) {
                if (this.launcher != null) {
                    this.launcher.closeFolder(this);
                }
                return selection;
            }
            return selection - columns;
        }
        if (keyCode == 20) {
            if (selection >= count - columns) {
                return selection;
            }
            return Math.min(selection + columns, count - 1);
        }
        if (keyCode == 21) {
            if ((selection % columns) == 0) {
                if (this.launcher != null) {
                    this.launcher.closeFolder(this);
                }
                return selection;
            }
            return selection - 1;
        }
        if (keyCode == 22) {
            if ((selection % columns) == (columns - 1)) {
                return selection;
            }
            return Math.min(selection + 1, count - 1);
        }
        return selection;
    }

    void bind(FolderInfo info) {
        this.folderInfo = info;
        if (this.textView != null) {
            this.textView.setText(info.title);
        }
        int itemCount = 0;
        if (info instanceof UserFolderInfo) {
            itemCount = ((UserFolderInfo) info).contents.size();
        }
        String description = getContext().getResources().getString(R.string.accessibility_folder_with_items, info.title, itemCount);
        setContentDescription(description);
    }
}
