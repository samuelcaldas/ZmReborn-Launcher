package org.zmreborn;

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
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import org.zmreborn.DragController;

/**
 * Dock bar container providing quick-launch application, folder, and drawer icons.
 */
public class Dock extends LinearLayout implements View.OnLongClickListener, DropTarget, DragSource,
        DropResultListener, DragController.DragListener {
    private static final String POSITION_CENTER = "CENTER";
    private static final String POSITION_END = "END";
    private static final String POSITION_START = "START";
    static final int WIDTH_LARGE = 2;
    static final int WIDTH_MEDIUM = 1;
    static final int WIDTH_SMALL = 0;
    private int cellHeight = 20;
    private int cellWidth = 20;
    private DragController dragController;
    private LinearLayout itemHolder;
    private Launcher launcher;
    private int orientation = 1;
    private View scrollView;
    private View selectedView;
    private DockDragTransaction dragTransaction;
    private Rect systemBarInsets;

    /**
     * Constructs a dock view with context.
     */
    public Dock(Context context) {
        super(context);
    }

    /**
     * Constructs a dock view with context and XML attributes.
     */
    public Dock(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Dock, 0, 0);
        this.cellHeight = typedArray.getDimensionPixelSize(1, this.cellHeight);
        this.cellWidth = typedArray.getDimensionPixelSize(0, this.cellWidth);
        this.orientation = typedArray.getInt(2, this.orientation);
    }

    /**
     * Sets the drag controller for initiating item movement.
     */
    public void setDragger(DragController controller) {
        this.dragController = controller;
    }

    /**
     * Sets system bar insets.
     */
    public void setSystemBarInsets(Rect insets) {
        this.systemBarInsets = insets;
    }

    @Override
    protected void onFinishInflate() {
        this.itemHolder = (LinearLayout) findViewById(R.id.dock_item_holder);
        this.scrollView = findViewById(R.id.dock_scroll_view);
        this.scrollView.setBackgroundColor(0);
        setElevation(getResources().getDimension(R.dimen.elevation_dock));
        super.onFinishInflate();
    }

    @Override
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        switch (((ItemInfo) dragInfo).itemType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
                this.scrollView.setBackgroundResource(R.drawable.dock_bg_glow);
                return true;
            default:
                this.scrollView.setBackgroundDrawable((Drawable) null);
                return false;
        }
    }

    /**
     * Dispatches a drop to the end of the dock.
     */
    public void sendDrop(ItemInfo itemInfo) {
        sendDrop(itemInfo, -1);
    }

    /**
     * Dispatches a drop to a specific dock position.
     */
    public void sendDrop(ItemInfo itemInfo, int position) {
        onDrop((DragSource) null, position, -1, -1, -1, itemInfo);
    }

    @Override
    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        int insertAfter = findDropIndex(x, y);
        this.scrollView.setBackgroundDrawable((Drawable) null);
        if (source == this) {
            if (this.dragTransaction != null) {
                this.dragTransaction.stageDrop(insertAfter);
            }
            return;
        }
        addExternalItem((ItemInfo) dragInfo, insertAfter);
    }

    private int findDropIndex(int x, int y) {
        int childCount = this.itemHolder.getChildCount();
        if (x == -1 && y == -1) {
            return childCount;
        }
        for (int index = 0; index < childCount; index++) {
            if (isBeforeChild(this.itemHolder.getChildAt(index), x, y)) {
                return index;
            }
        }
        return childCount;
    }

    private boolean isBeforeChild(View childView, int x, int y) {
        int[] childLocation = new int[2];
        childView.getLocationOnScreen(childLocation);
        int childPosition = this.orientation == 1
                ? childLocation[0] + (childView.getWidth() / 2)
                : childLocation[1] + (childView.getHeight() / 2);
        return this.orientation == 1 ? x <= childPosition : y <= childPosition;
    }

    private void addExternalItem(ItemInfo itemInfo, int position) {
        itemInfo.cellX = position;
        LauncherModel launcherModel = Launcher.getModel();
        launcherModel.addDesktopItem(itemInfo);
        LauncherModel.addOrMoveItemInDatabase(getContext(), itemInfo, -200, -1, position, -1);
        addItemViewAt(itemInfo, position);
        updateItemsInDatabase();
    }

    @Override
    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        this.scrollView.setBackgroundDrawable((Drawable) null);
    }

    @Override
    public void onDragStart(View v, DragSource source, Object info, int dragAction) {
    }

    @Override
    public void onDragEnd() {
        if (this.dragTransaction != null) {
            completeDrop((View) null, false, false);
        }
    }

    /**
     * Adds items to the dock sorted by horizontal cell position.
     */
    public void addItemViews(ArrayList<ItemInfo> itemInfos) {
        Collections.sort(itemInfos, ItemInfo.createCellXComparator());
        for (int i = 0; i < itemInfos.size(); i++) {
            ItemInfo itemInfo = itemInfos.get(i);
            if (itemInfo.cellX <= this.itemHolder.getChildCount()) {
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
                view = this.launcher.createSmallShortcut(R.layout.small_application, this, (ApplicationItemInfo) itemInfo);
                break;
            case 2:
                view = this.launcher.createSmallFolder(R.layout.small_application, this, (UserFolderInfo) itemInfo);
                break;
            case 3:
                view = this.launcher.createSmallLiveFolder(R.layout.small_application, this, (LiveFolderInfo) itemInfo);
                break;
            case 6:
                view = this.launcher.createSmallApplicationsGridItem(R.layout.small_application, this, (ApplicationsGridItemInfo) itemInfo);
                break;
        }
        Drawable selectorDrawable = itemInfo.itemType == 6
                ? SelectorDrawable.createOblongSelector(getContext())
                : SelectorDrawable.createSelector(getContext(), true);
        ImageView imageView = (ImageView) view;
        imageView.setMinimumHeight(this.cellHeight);
        imageView.setMinimumWidth(this.cellWidth);
        view.setLongClickable(true);
        view.setFocusable(true);
        view.setOnLongClickListener(this);
        view.setBackgroundDrawable(selectorDrawable);
        if (position == -1) {
            this.itemHolder.addView(view);
        } else {
            this.itemHolder.addView(view, position);
        }
        this.itemHolder.invalidate();
        invalidate();
    }

    @Override
    public boolean onLongClick(View longClickedView) {
        if (this.dragTransaction != null) {
            return false;
        }
        if (this.launcher.isApplicationsGridOpen()) {
            this.launcher.closeAllApplications();
        }
        int originalIndex = this.itemHolder.indexOfChild(longClickedView);
        if (originalIndex < 0) {
            return false;
        }
        this.selectedView = longClickedView;
        this.dragTransaction = new DockDragTransaction(originalIndex);
        this.dragController.startDrag(longClickedView, this, (ItemInfo) longClickedView.getTag(), 0);
        return true;
    }

    @Override
    public void removeAllViewsInLayout() {
        this.itemHolder.removeAllViewsInLayout();
    }

    void removeShortcutsForPackage(String packageName) {
        Intent intent;
        ComponentName componentName;
        String pkg;
        if (packageName == null) {
            return;
        }
        cancelActiveDragForPackageRemoval();
        for (int i = 0; i < this.itemHolder.getChildCount(); i++) {
            View child = this.itemHolder.getChildAt(i);
            ItemInfo itemInfo = (ItemInfo) child.getTag();
            if (itemInfo instanceof ApplicationItemInfo) {
                intent = ((ApplicationItemInfo) itemInfo).intent;
                if (intent != null) {
                    componentName = intent.getComponent();
                    if (componentName != null) {
                        pkg = componentName.getPackageName();
                        if (pkg != null && pkg.equals(packageName)) {
                            this.selectedView = child;
                            removeSelectedItem(true);
                        }
                    }
                }
            }
        }
    }

    private void cancelActiveDragForPackageRemoval() {
        if (this.dragTransaction == null) {
            return;
        }
        this.dragController.cancelDrag();
        if (this.dragTransaction != null) {
            completeDrop((View) null, false, false);
        }
    }

    void updateShortcutsForPackage(String packageName) {
        Folder folder;
        Drawable icon;
        if (packageName == null) {
            return;
        }
        for (int i = 0; i < this.itemHolder.getChildCount(); i++) {
            View itemView = this.itemHolder.getChildAt(i);
            ItemInfo itemInfo = (ItemInfo) itemView.getTag();
            if (itemInfo instanceof ApplicationItemInfo) {
                ApplicationItemInfo info = (ApplicationItemInfo) itemInfo;
                Intent intent = info.intent;
                ComponentName name = intent.getComponent();
                if ((info.itemType == 0 || info.itemType == 1) && "android.intent.action.MAIN".equals(intent.getAction()) && name != null) {
                    if (packageName.equals(name.getPackageName())) {
                        icon = Launcher.getModel().getApplicationItemInfoIconOrNull(this.launcher.getPackageManager(), info);
                        if (icon != null && icon != info.icon) {
                            info.filtered = true;
                            info.icon.setCallback((Drawable.Callback) null);
                            info.icon = Utilities.createIconThumbnail(icon, getContext());
                            ((ImageView) itemView).setImageDrawable(Utilities.createDockIconThumbnail(info.icon, getContext()));
                        }
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
                            Drawable icon2 = Launcher.getModel().getApplicationItemInfoIconOrNull(this.launcher.getPackageManager(), applicationItemInfo);
                            boolean folderUpdated = false;
                            if (icon2 != null && icon2 != applicationItemInfo.icon) {
                                applicationItemInfo.icon.setCallback((Drawable.Callback) null);
                                applicationItemInfo.icon = Utilities.createIconThumbnail(icon2, this.launcher);
                                applicationItemInfo.filtered = true;
                                folderUpdated = true;
                            }
                            if (folderUpdated) {
                                folder = this.launcher.getWorkspace().getOpenFolder();
                                if (folder != null) {
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
        this.itemHolder.forceLayout();
        this.itemHolder.invalidate();
        int count = this.itemHolder.getChildCount();
        for (int i = 0; i < count; i++) {
            ItemInfo itemInfo = (ItemInfo) this.itemHolder.getChildAt(i).getTag();
            itemInfo.cellX = i;
            LauncherModel.moveItemInDatabase(this.launcher, itemInfo, -200, -1, itemInfo.cellX, -1);
        }
    }

    private void removeSelectedItem(boolean deleteItemFromDatabase) {
        if (this.selectedView == null) {
            return;
        }
        ItemInfo itemInfo = (ItemInfo) this.selectedView.getTag();
        LauncherModel launcherModel = Launcher.getModel();
        if (deleteItemFromDatabase) {
            if (itemInfo instanceof UserFolderInfo) {
                UserFolderInfo userFolderInfo = (UserFolderInfo) itemInfo;
                LauncherModel.deleteUserFolderContentsFromDatabase(this.launcher, userFolderInfo);
                launcherModel.removeUserFolder(userFolderInfo);
            }
            LauncherModel.deleteItemFromDatabase(this.launcher, itemInfo);
        }
        launcherModel.removeDesktopItem(itemInfo);
        this.itemHolder.removeView(this.selectedView);
        int count = this.itemHolder.getChildCount();
        for (int i = 0; i < count; i++) {
            View cell = this.itemHolder.getChildAt(i);
            ItemInfo info = (ItemInfo) cell.getTag();
            if (info.cellX > itemInfo.cellX) {
                info.cellX--;
                cell.setTag(info);
                LauncherModel.moveItemInDatabase(this.launcher, info, -200, -1, info.cellX, -1);
            }
        }
        requestLayout();
        this.selectedView = null;
    }

    void setLauncher(Launcher launcherInstance) {
        this.launcher = launcherInstance;
    }

    void setItemWidth(int width) {
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
        int size = (int) (((double) this.launcher.getDisplayMetrics().density) * spacing);
        if (this.orientation == 1) {
            this.cellWidth = size;
        } else {
            this.cellHeight = size;
        }
        invalidate();
        requestLayout();
    }

    void scrollReset() {
        String resetTo = PreferencesUtil.getDockResetTo(getContext());
        if (this.scrollView instanceof HorizontalScrollView) {
            HorizontalScrollView hScrollView = (HorizontalScrollView) this.scrollView;
            if (resetTo.equalsIgnoreCase(POSITION_START)) {
                hScrollView.fullScroll(17);
            } else if (resetTo.equalsIgnoreCase(POSITION_CENTER)) {
                hScrollView.smoothScrollTo((this.itemHolder.getMeasuredWidth() - getMeasuredWidth()) / 2, 0);
            } else if (resetTo.equalsIgnoreCase(POSITION_END)) {
                hScrollView.fullScroll(66);
            } else {
                hScrollView.fullScroll(17);
            }
        } else if (this.scrollView instanceof ScrollView) {
            ScrollView scrView = (ScrollView) this.scrollView;
            if (resetTo.equalsIgnoreCase(POSITION_START)) {
                scrView.fullScroll(33);
            } else if (resetTo.equalsIgnoreCase(POSITION_CENTER)) {
                scrView.smoothScrollTo(0, (this.itemHolder.getMeasuredHeight() - getMeasuredHeight()) / 2);
            } else if (resetTo.equalsIgnoreCase(POSITION_END)) {
                scrView.fullScroll(130);
            } else {
                scrView.fullScroll(33);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int itemHolderSize;
        int i = 3;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int dockSize = this.orientation == 1 ? getMeasuredWidth() : getMeasuredHeight();
        if (this.orientation == 1) {
            itemHolderSize = this.itemHolder.getMeasuredWidth();
        } else {
            itemHolderSize = this.itemHolder.getMeasuredHeight();
        }
        if (itemHolderSize != 0) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.itemHolder.getLayoutParams();
            if (itemHolderSize <= dockSize) {
                String itemAlignment = PreferencesUtil.getDockItemAlignment(getContext());
                if (this.orientation == 1) {
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
                if (this.orientation != 1) {
                    i = 48;
                }
                layoutParams.gravity = i;
            }
            this.itemHolder.setLayoutParams(layoutParams);
            postInvalidate();
            requestLayout();
        }
    }

    @Override
    public void onDropCompleted(View target, boolean success) {
        completeDrop(target, success, target != null);
    }

    @Override
    public void onDropCompleted(View target, boolean success, boolean targetFound) {
        completeDrop(target, success, targetFound);
    }

    private void completeDrop(View target, boolean success, boolean targetFound) {
        DockDragTransaction transaction = this.dragTransaction;
        if (transaction == null) {
            return;
        }
        boolean completed = success ? transaction.finish(true) : transaction.cancel();
        if (!completed) {
            return;
        }
        try {
            if (transaction.isSuccessful() && commitDrop(target, transaction)) {
                return;
            }
            restoreSelectedView(transaction);
            showMoveResult(transaction.isSuccessful(), targetFound);
        } finally {
            clearDragState();
        }
    }

    private boolean commitDrop(View target, DockDragTransaction transaction) {
        if (this.selectedView == null || this.itemHolder.indexOfChild(this.selectedView) < 0) {
            return false;
        }
        if (target == this) {
            return commitDockMove(transaction);
        }
        Launcher.getModel().removeDesktopItem((ItemInfo) this.selectedView.getTag());
        this.itemHolder.removeView(this.selectedView);
        updateItemsInDatabase();
        return true;
    }

    private boolean commitDockMove(DockDragTransaction transaction) {
        int insertionIndex = transaction.getInsertionIndex(this.itemHolder.getChildCount());
        this.itemHolder.removeView(this.selectedView);
        this.itemHolder.addView(this.selectedView, insertionIndex);
        this.selectedView.setVisibility(0);
        updateItemsInDatabase();
        return true;
    }

    private void restoreSelectedView(DockDragTransaction transaction) {
        if (this.selectedView == null) {
            return;
        }
        int currentIndex = this.itemHolder.indexOfChild(this.selectedView);
        int originalIndex = Math.min(transaction.getOriginalIndex(), this.itemHolder.getChildCount());
        if (currentIndex < 0) {
            this.itemHolder.addView(this.selectedView, originalIndex);
        } else if (currentIndex != originalIndex) {
            this.itemHolder.removeView(this.selectedView);
            this.itemHolder.addView(this.selectedView, originalIndex);
        }
        this.selectedView.setVisibility(0);
        requestLayout();
    }

    private void showMoveResult(boolean accepted, boolean targetFound) {
        boolean couldNotMove = DockDragTransaction.shouldShowCouldNotMove(accepted, targetFound);
        int message = couldNotMove ? R.string.could_not_move_item : R.string.move_canceled;
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void clearDragState() {
        this.dragTransaction = null;
        this.selectedView = null;
    }

    void hide(boolean animated) {
        if (animated) {
            setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.dock_fade_out));
        }
        setVisibility(View.INVISIBLE);
    }

    void show(boolean animated) {
        if (animated) {
            setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.dock_fade_in));
        }
        setVisibility(View.VISIBLE);
    }

    boolean isEmpty() {
        return this.itemHolder.getChildCount() == 0;
    }
}
