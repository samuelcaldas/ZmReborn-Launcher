package org.zmreborn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import java.util.ArrayList;
import org.zmreborn.theme.WallpaperColorExtractor;

public class ApplicationsGridView extends GridView implements ApplicationsView, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DragSource {
    private DragController mDragController;
    private Launcher mLauncher;
    private boolean mActionsEnabled = true;
    private boolean mClosing;
    private boolean mDestroyed;
    public int mMode;
    private boolean mResetMode;
    private int mBasePaddingBottom;
    private int mBasePaddingLeft;
    private int mBasePaddingRight;
    private int mBasePaddingTop;
    private int mFastScrollInsetEnd;
    private boolean mFastScrollVisible;
    private int mSystemBarInsetBottom;
    private int mSystemBarInsetLeft;
    private int mSystemBarInsetRight;
    private int mSystemBarInsetTop;
    private Rect mSystemGestureInsets;

    public ApplicationsGridView(Context context) {
        super(context);
        this.mMode = 0;
        configureResponsiveColumns();
    }

    public ApplicationsGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842865);
    }

    public ApplicationsGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mMode = 0;
        this.mResetMode = true;
        setSelector(SelectorDrawable.createSelector(context, true));
        setTextFilterEnabled(false);
        setScrollingCacheEnabled(false);
        setDrawingCacheEnabled(false);
        configureResponsiveColumns();
    }

    private void configureResponsiveColumns() {
        super.setNumColumns(AUTO_FIT);
        setPreferredColumnWidth(getResources().getDimensionPixelSize(
                R.dimen.drawer_cell_preferred_width));
        setStretchMode(STRETCH_COLUMN_WIDTH);
    }

    void setPreferredColumnWidth(int columnWidth) {
        if (columnWidth <= 0) {
            throw new IllegalArgumentException("columnWidth must be positive");
        }
        super.setNumColumns(AUTO_FIT);
        setColumnWidth(columnWidth);
        requestLayout();
    }

    public void setSystemBarInsets(int left, int top, int right, int bottom) {
        this.mSystemBarInsetLeft = Math.max(0, left);
        this.mSystemBarInsetTop = Math.max(0, top);
        this.mSystemBarInsetRight = Math.max(0, right);
        this.mSystemBarInsetBottom = Math.max(0, bottom);
        updatePadding();
    }

    void setFastScrollVisible(boolean visible) {
        this.mFastScrollVisible = visible;
        this.mFastScrollInsetEnd = visible ? getResources().getDimensionPixelSize(
                R.dimen.drawer_fast_scroll_width) : 0;
        updateFastScrollFocus();
        updatePadding();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateFastScrollFocus();
        updatePadding();
    }

    private void updateFastScrollFocus() {
        int gridId = R.id.apps_grid_content;
        int railId = this.mFastScrollVisible ? R.id.drawer_fast_scroll : gridId;
        if (isLayoutDirectionRtl()) {
            setNextFocusLeftId(railId);
            setNextFocusRightId(gridId);
            return;
        }
        setNextFocusLeftId(gridId);
        setNextFocusRightId(railId);
    }

    private boolean isLayoutDirectionRtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    private void updatePadding() {
        int fastScrollInsetLeft = isLayoutDirectionRtl() ? this.mFastScrollInsetEnd : 0;
        int fastScrollInsetRight = isLayoutDirectionRtl() ? 0 : this.mFastScrollInsetEnd;
        setPadding(this.mBasePaddingLeft + this.mSystemBarInsetLeft + fastScrollInsetLeft,
                this.mBasePaddingTop + this.mSystemBarInsetTop,
                this.mBasePaddingRight + this.mSystemBarInsetRight + fastScrollInsetRight,
                this.mBasePaddingBottom + this.mSystemBarInsetBottom);
        requestLayout();
    }

    public void setSystemGestureInsets(Rect insets) {
        this.mSystemGestureInsets = insets;
    }

    public void setBackgroundAlpha(int alpha) {
        int surface = WallpaperColorExtractor.getSurface(getContext());
        int background = Color.argb(alpha, Color.red(surface), Color.green(surface),
                Color.blue(surface));
        setBackgroundColor(background);
        setCacheColorHint(alpha == 255 ? background : Color.TRANSPARENT);
        invalidate();
    }

    @Override
    public void refreshPalette() {
        if (this.mDestroyed) {
            return;
        }
        ApplicationsAdapter applicationsAdapter = (ApplicationsAdapter) getAdapter();
        if (applicationsAdapter != null) {
            applicationsAdapter.notifyDataSetChanged();
        }
        for (int index = 0; index < getChildCount(); index++) {
            getChildAt(index).invalidate();
        }
        invalidate();
    }

    public void setMode(int mode) {
        if (this.mMode == mode) {
            if (mode != MODE_DEFAULT) {
                setMode(MODE_DEFAULT);
            }
            return;
        }
        Context context = getContext();
        ApplicationsAdapter applicationsAdapter = (ApplicationsAdapter) getAdapter();
        if (applicationsAdapter == null) {
            this.mMode = mode;
            return;
        }
        switch (mode) {
            case 0:
                setSelector(SelectorDrawable.createSelector(context, true));
                applicationsAdapter.setUninstalling(false);
                applicationsAdapter.notifyDataSetChanged();
                break;
            case 1:
                setSelector(17170445);
                applicationsAdapter.setUninstalling(true);
                applicationsAdapter.notifyDataSetChanged();
                break;
        }
        this.mMode = mode;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBasePaddingLeft = getPaddingLeft();
        this.mBasePaddingTop = getPaddingTop();
        this.mBasePaddingRight = getPaddingRight();
        this.mBasePaddingBottom = getPaddingBottom();
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        if (!this.mActionsEnabled || this.mClosing) {
            return;
        }
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) parent.getItemAtPosition(position);
        if (applicationItemInfo instanceof AppListFolderInfo) {
            if (this.mMode == 0) {
                this.mLauncher.openAppListFolder((AppListFolderInfo) applicationItemInfo);
            }
            return;
        }
        switch (this.mMode) {
            case 0:
                this.mResetMode = true;
                this.mLauncher.startActivitySafely(applicationItemInfo.intent);
                return;
            case 1:
                if (Utilities.canUninstallApplication(getContext(), applicationItemInfo)) {
                    this.mResetMode = false;
                    this.mLauncher.uninstallApplication(applicationItemInfo);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!this.mActionsEnabled || this.mClosing
                || this.mMode != 0 || !view.isInTouchMode()) {
            return false;
        }
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) parent.getItemAtPosition(position);
        if (applicationItemInfo instanceof AppListFolderInfo) {
            this.mLauncher.showAppListFolderActions((AppListFolderInfo) applicationItemInfo);
            return true;
        }
        this.mDragController.startDrag(view, this, new ApplicationItemInfo(applicationItemInfo), 1);
        this.mLauncher.closeAllApplications();
        return true;
    }

    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
    }

    public void onDropCompleted(View target, boolean success) {
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public void open(boolean animated) {
        prepareOpen();
        if (animated) {
            startAnimation(AnimationUtils.loadAnimation(
                    getContext(), R.anim.apps_scale_in));
        }
        invalidate();
    }

    void prepareOpen() {
        this.mClosing = false;
        updateInputEnabled();
        resetVisualState();
        if (!PreferencesUtil.rememberApplicationsPosition(getContext())) {
            setSelection(0);
        }
        setVisibility(VISIBLE);
    }

    public boolean close(boolean animated) {
        if (!prepareClose()) {
            return false;
        }
        if (!animated) {
            finishClose();
            return true;
        }
        startAnimation(createCloseAnimation());
        return true;
    }

    boolean prepareClose() {
        if (this.mMode != MODE_DEFAULT) {
            if (this.mResetMode) {
                setMode(MODE_DEFAULT);
            }
            this.mResetMode = true;
            return false;
        }
        this.mClosing = true;
        updateInputEnabled();
        resetVisualState();
        return true;
    }

    void finishClose() {
        setVisibility(INVISIBLE);
        resetVisualState();
    }

    public void setLoading() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = false;
        updateInputEnabled();
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsLoading();
        }
    }

    public void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos) {
        if (this.mDestroyed) {
            return;
        }
        ArrayList<ApplicationItemInfo> items = applicationItemInfos;
        if (items == null) {
            items = new ArrayList<>();
        }
        ApplicationsAdapter applicationsAdapter = new ApplicationsAdapter(getContext(), items);
        applicationsAdapter.setUninstalling(this.mMode == MODE_UNINSTALL);
        setAdapter(applicationsAdapter);
        resetPositionIfNeeded();
    }

    public void setEmpty() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = false;
        updateInputEnabled();
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsEmpty();
        }
    }

    public void setError() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = false;
        updateInputEnabled();
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsError();
        }
    }

    public void clearState() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = true;
        updateInputEnabled();
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsReady();
        }
    }

    private void updateInputEnabled() {
        setEnabled(this.mActionsEnabled && !this.mClosing);
    }

    public void onDestroy() {
        this.mDestroyed = true;
        this.mActionsEnabled = false;
        setEnabled(false);
        clearTextFilter();
        setAdapter((ListAdapter) null);
    }

    public View getImplementingView() {
        return this;
    }

    public Launcher getLauncher() {
        return this.mLauncher;
    }

    @Override
    public int getMode() {
        return this.mMode;
    }

    private void resetPositionIfNeeded() {
        if (!PreferencesUtil.rememberApplicationsPosition(getContext())) {
            setSelection(0);
        }
    }

    private void resetVisualState() {
        clearAnimation();
        setAlpha(1.0f);
        setScaleX(1.0f);
        setScaleY(1.0f);
        setTranslationX(0.0f);
        setTranslationY(0.0f);
    }

    private Animation createCloseAnimation() {
        Animation animation = AnimationUtils.loadAnimation(
                getContext(), R.anim.apps_scale_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation ignored) {
            }

            public void onAnimationRepeat(Animation ignored) {
            }

            public void onAnimationEnd(Animation ignored) {
                finishClose();
            }
        });
        return animation;
    }
}
