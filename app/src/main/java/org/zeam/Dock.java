package org.zeam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.Collections;
import org.zeam.DragController;

public class Dock extends LinearLayout implements View.OnLongClickListener, DropTarget, DragSource, DragController.DragListener {
    private static final String POSITION_CENTER = "CENTER";
    private static final String POSITION_END = "END";
    private static final String POSITION_START = "START";
    static final int WIDTH_LARGE = 2;
    static final int WIDTH_MEDIUM = 1;
    static final int WIDTH_SMALL = 0;
    private int mCellHeight = 20;
    private int mCellWidth = 20;
    private DragController mDragController;
    private LinearLayout mItemHolder;
    private Launcher mLauncher;
    private int mOrientation = 1;
    private View mScrollView;
    private View mSelectedView;

    public Dock(Context context) {
        super(context);
    }

    public Dock(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Dock, 0, 0);
        this.mCellHeight = typedArray.getDimensionPixelSize(1, this.mCellHeight);
        this.mCellWidth = typedArray.getDimensionPixelSize(0, this.mCellWidth);
        this.mOrientation = typedArray.getInt(2, this.mOrientation);
    }

    public void setDragger(DragController dragController) {
        this.mDragController = dragController;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mItemHolder = (LinearLayout) findViewById(R.id.dock_item_holder);
        this.mScrollView = findViewById(R.id.dock_scroll_view);
        this.mScrollView.setBackgroundColor(0);
        super.onFinishInflate();
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        switch (((ItemInfo) dragInfo).itemType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                this.mScrollView.setBackgroundResource(R.drawable.dock_bg_glow);
                return true;
            default:
                this.mScrollView.setBackgroundDrawable((Drawable) null);
                return false;
        }
    }

    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo, Rect recycle) {
        return null;
    }

    public void sendDrop(ItemInfo itemInfo) {
        sendDrop(itemInfo, -1);
    }

    public void sendDrop(ItemInfo itemInfo, int position) {
        onDrop((DragSource) null, position, -1, -1, -1, itemInfo);
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        int insertAfter = this.mItemHolder.getChildCount();
        if (x != -1 || y != -1) {
            int i = 0;
            while (true) {
                if (i >= this.mItemHolder.getChildCount()) {
                    break;
                }
                View childView = this.mItemHolder.getChildAt(i);
                int[] childLocation = new int[2];
                childView.getLocationOnScreen(childLocation);
                int childPosition = this.mOrientation == 1 ? childLocation[0] + (childView.getWidth() / 2) : childLocation[1] + (childView.getHeight() / 2);
                if (this.mOrientation == 1) {
                    if (x <= childPosition) {
                        insertAfter = i;
                        break;
                    }
                } else if (y <= childPosition) {
                    insertAfter = i;
                    break;
                }
                i++;
            }
        }
        this.mScrollView.setBackgroundDrawable((Drawable) null);
        LauncherModel launcherModel = Launcher.getModel();
        ItemInfo itemInfo = (ItemInfo) dragInfo;
        itemInfo.cellX = insertAfter;
        launcherModel.addDesktopItem(itemInfo);
        LauncherModel.addOrMoveItemInDatabase(getContext(), itemInfo, -200, -1, insertAfter, -1);
        addItemViewAt(itemInfo, insertAfter);
        updateItemsInDatabase();
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        this.mScrollView.setBackgroundDrawable((Drawable) null);
    }

    public void onDragStart(View v, DragSource source, Object info, int dragAction) {
    }

    public void onDragEnd() {
    }

    public void addItemViews(ArrayList<ItemInfo> itemInfos) {
        Collections.sort(itemInfos, ItemInfo.createCellXComparator());
        for (int i = 0; i < itemInfos.size(); i++) {
            ItemInfo itemInfo = itemInfos.get(i);
            if (itemInfo.cellX <= this.mItemHolder.getChildCount()) {
                addItemViewAt(itemInfo, itemInfo.cellX);
            } else {
                addItemView(itemInfo);
            }
        }
    }

    private synchronized void addItemView(ItemInfo itemInfo) {
        addItemViewAt(itemInfo, -1);
    }

    private synchronized void addItemViewAt(ItemInfo itemInfo, int position) {
        View view = null;
        switch (itemInfo.itemType) {
            case 0:
            case 1:
                if (itemInfo.container == -1) {
                    itemInfo = new ApplicationItemInfo((ApplicationItemInfo) itemInfo);
                }
                view = this.mLauncher.createSmallShortcut(R.layout.small_application, this, (ApplicationItemInfo) itemInfo);
                break;
            case 2:
                view = this.mLauncher.createSmallFolder(R.layout.small_application, this, (UserFolderInfo) itemInfo);
                break;
            case 3:
                view = this.mLauncher.createSmallLiveFolder(R.layout.small_application, this, (LiveFolderInfo) itemInfo);
                break;
            case 6:
                view = this.mLauncher.createSmallApplicationsGridItem(R.layout.small_application, this, (ApplicationsGridItemInfo) itemInfo);
                break;
        }
        Drawable selectorDrawable = SelectorDrawable.createSelector(getContext(), true);
        ImageView imageView = (ImageView) view;
        imageView.setMinimumHeight(this.mCellHeight);
        imageView.setMinimumWidth(this.mCellWidth);
        view.setLongClickable(true);
        view.setOnLongClickListener(this);
        view.setBackgroundDrawable(selectorDrawable);
        if (position == -1) {
            this.mItemHolder.addView(view);
        } else {
            this.mItemHolder.addView(view, position);
        }
        this.mItemHolder.invalidate();
        invalidate();
    }

    public boolean onLongClick(View selectedView) {
        if (this.mLauncher.isApplicationsGridOpen()) {
            this.mLauncher.closeAllApplications();
        }
        this.mSelectedView = selectedView;
        this.mDragController.startDrag(selectedView, this, (ItemInfo) selectedView.getTag(), 0);
        removeSelectedItem(false);
        return true;
    }

    public void removeAllViewsInLayout() {
        this.mItemHolder.removeAllViewsInLayout();
    }

    /* access modifiers changed from: package-private */
    public void removeShortcutsForPackage(String packageName) {
        Intent intent;
        ComponentName componentName;
        String packageName1;
        if (packageName != null) {
            for (int i = 0; i < this.mItemHolder.getChildCount(); i++) {
                View child = this.mItemHolder.getChildAt(i);
                ItemInfo itemInfo = (ItemInfo) child.getTag();
                if (!(itemInfo == null || !(itemInfo instanceof ApplicationItemInfo) || (intent = ((ApplicationItemInfo) itemInfo).intent) == null || (componentName = intent.getComponent()) == null || (packageName1 = componentName.getPackageName()) == null || !packageName1.equals(packageName))) {
                    this.mSelectedView = child;
                    removeSelectedItem(true);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void updateShortcutsForPackage(String packageName) {
        Folder folder;
        Drawable icon;
        if (packageName != null) {
            for (int i = 0; i < this.mItemHolder.getChildCount(); i++) {
                View itemView = this.mItemHolder.getChildAt(i);
                ItemInfo itemInfo = (ItemInfo) itemView.getTag();
                if (itemInfo instanceof ApplicationItemInfo) {
                    ApplicationItemInfo info = (ApplicationItemInfo) itemInfo;
                    Intent intent = info.intent;
                    ComponentName name = intent.getComponent();
                    if ((info.itemType == 0 || info.itemType == 1) && "android.intent.action.MAIN".equals(intent.getAction()) && name != null) {
                        if (!(!packageName.equals(name.getPackageName()) || (icon = Launcher.getModel().getApplicationItemInfoIconOrNull(this.mLauncher.getPackageManager(), info)) == null || icon == info.icon)) {
                            info.filtered = true;
                            info.icon.setCallback((Drawable.Callback) null);
                            info.icon = Utilities.createIconThumbnail(icon, getContext());
                            ((ImageView) itemView).setImageDrawable(Utilities.createDockIconThumbnail(info.icon, getContext()));
                        }
                    }
                } else if (itemInfo instanceof UserFolderInfo) {
                    ArrayList<ApplicationItemInfo> applicationItemInfos = ((UserFolderInfo) itemInfo).contents;
                    int applicationItemInfoCount = applicationItemInfos.size();
                    for (int y = 0; y < applicationItemInfoCount; y++) {
                        ApplicationItemInfo applicationItemInfo = applicationItemInfos.get(y);
                        Intent intent2 = applicationItemInfo.intent;
                        ComponentName name2 = intent2.getComponent();
                        if ((applicationItemInfo.itemType == 0 || applicationItemInfo.itemType == 1) && "android.intent.action.MAIN".equals(intent2.getAction()) && name2 != null) {
                            if (packageName.equals(name2.getPackageName())) {
                                Drawable icon2 = Launcher.getModel().getApplicationItemInfoIconOrNull(this.mLauncher.getPackageManager(), applicationItemInfo);
                                boolean folderUpdated = false;
                                if (!(icon2 == null || icon2 == applicationItemInfo.icon)) {
                                    applicationItemInfo.icon.setCallback((Drawable.Callback) null);
                                    applicationItemInfo.icon = Utilities.createIconThumbnail(icon2, this.mLauncher);
                                    applicationItemInfo.filtered = true;
                                    folderUpdated = true;
                                }
                                if (folderUpdated && (folder = this.mLauncher.getWorkspace().getOpenFolder()) != null) {
                                    folder.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateItemsInDatabase() {
        this.mItemHolder.forceLayout();
        this.mItemHolder.invalidate();
        int count = this.mItemHolder.getChildCount();
        for (int i = 0; i < count; i++) {
            ItemInfo itemInfo = (ItemInfo) this.mItemHolder.getChildAt(i).getTag();
            itemInfo.cellX = i;
            LauncherModel.moveItemInDatabase(this.mLauncher, itemInfo, -200, -1, itemInfo.cellX, -1);
        }
    }

    private void removeSelectedItem(boolean deleteItemFromDatabase) {
        if (this.mSelectedView != null) {
            ItemInfo itemInfo = (ItemInfo) this.mSelectedView.getTag();
            LauncherModel launcherModel = Launcher.getModel();
            if (deleteItemFromDatabase) {
                if (itemInfo instanceof UserFolderInfo) {
                    UserFolderInfo userFolderInfo = (UserFolderInfo) itemInfo;
                    LauncherModel.deleteUserFolderContentsFromDatabase(this.mLauncher, userFolderInfo);
                    launcherModel.removeUserFolder(userFolderInfo);
                }
                LauncherModel.deleteItemFromDatabase(this.mLauncher, itemInfo);
            }
            launcherModel.removeDesktopItem(itemInfo);
            this.mItemHolder.removeView(this.mSelectedView);
            int count = this.mItemHolder.getChildCount();
            for (int i = 0; i < count; i++) {
                View cell = this.mItemHolder.getChildAt(i);
                ItemInfo info = (ItemInfo) cell.getTag();
                if (info.cellX > itemInfo.cellX) {
                    info.cellX--;
                    cell.setTag(info);
                    LauncherModel.moveItemInDatabase(this.mLauncher, info, -200, -1, info.cellX, -1);
                }
            }
            requestLayout();
            this.mSelectedView = null;
        }
    }

    /* access modifiers changed from: package-private */
    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    /* access modifiers changed from: package-private */
    public void setItemWidth(int width) {
        double spacing;
        switch (width) {
            case 0:
                spacing = 45.95d;
                break;
            case 2:
                spacing = 64.0d;
                break;
            default:
                return;
        }
        int size = (int) (((double) this.mLauncher.getDisplayMetrics().density) * spacing);
        if (this.mOrientation == 1) {
            this.mCellWidth = size;
        } else {
            this.mCellHeight = size;
        }
        invalidate();
        requestLayout();
    }

    /* access modifiers changed from: package-private */
    public void scrollReset() {
        String resetTo = PreferencesUtil.getDockResetTo(getContext());
        if (this.mScrollView instanceof HorizontalScrollView) {
            HorizontalScrollView hScrollView = (HorizontalScrollView) this.mScrollView;
            if (resetTo.equalsIgnoreCase(POSITION_START)) {
                hScrollView.fullScroll(17);
            } else if (resetTo.equalsIgnoreCase(POSITION_CENTER)) {
                hScrollView.smoothScrollTo((this.mItemHolder.getMeasuredWidth() - getMeasuredWidth()) / 2, 0);
            } else if (resetTo.equalsIgnoreCase(POSITION_END)) {
                hScrollView.fullScroll(66);
            } else {
                hScrollView.fullScroll(17);
            }
        } else if (this.mScrollView instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) this.mScrollView;
            if (resetTo.equalsIgnoreCase(POSITION_START)) {
                scrollView.fullScroll(33);
            } else if (resetTo.equalsIgnoreCase(POSITION_CENTER)) {
                scrollView.smoothScrollTo(0, (this.mItemHolder.getMeasuredHeight() - getMeasuredHeight()) / 2);
            } else if (resetTo.equalsIgnoreCase(POSITION_END)) {
                scrollView.fullScroll(130);
            } else {
                scrollView.fullScroll(33);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int itemHolderSize;
        int i = 3;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int dockSize = this.mOrientation == 1 ? getMeasuredWidth() : getMeasuredHeight();
        if (this.mOrientation == 1) {
            itemHolderSize = this.mItemHolder.getMeasuredWidth();
        } else {
            itemHolderSize = this.mItemHolder.getMeasuredHeight();
        }
        if (itemHolderSize != 0) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mItemHolder.getLayoutParams();
            if (itemHolderSize <= dockSize) {
                String itemAlignment = PreferencesUtil.getDockItemAlignment(getContext());
                if (this.mOrientation == 1) {
                    if (itemAlignment.equalsIgnoreCase(POSITION_START)) {
                        layoutParams.gravity = 3;
                    } else if (itemAlignment.equalsIgnoreCase(POSITION_CENTER)) {
                        layoutParams.gravity = 1;
                    } else if (itemAlignment.equalsIgnoreCase(POSITION_END)) {
                        layoutParams.gravity = 5;
                    } else {
                        layoutParams.gravity = 1;
                    }
                } else if (itemAlignment.equalsIgnoreCase(POSITION_START)) {
                    layoutParams.gravity = 48;
                } else if (itemAlignment.equalsIgnoreCase(POSITION_CENTER)) {
                    layoutParams.gravity = 16;
                } else if (itemAlignment.equalsIgnoreCase(POSITION_END)) {
                    layoutParams.gravity = 80;
                } else {
                    layoutParams.gravity = 1;
                }
            } else {
                if (this.mOrientation != 1) {
                    i = 48;
                }
                layoutParams.gravity = i;
            }
            this.mItemHolder.setLayoutParams(layoutParams);
            postInvalidate();
            requestLayout();
        }
    }

    public void onDropCompleted(View target, boolean success) {
    }

    /* access modifiers changed from: package-private */
    public void hide(boolean animated) {
        if (animated) {
            setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.dock_fade_out));
        }
        setVisibility(4);
    }

    /* access modifiers changed from: package-private */
    public void show(boolean animated) {
        if (animated) {
            setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.dock_fade_in));
        }
        setVisibility(0);
    }

    /* access modifiers changed from: package-private */
    public boolean isEmpty() {
        return this.mItemHolder.getChildCount() == 0;
    }
}
