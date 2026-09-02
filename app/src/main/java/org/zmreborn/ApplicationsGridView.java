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

/**
 * Grid view displaying applications within the app drawer.
 */
public class ApplicationsGridView extends GridView implements ApplicationsView,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DragSource {
    private DragController dragController;
    private Launcher launcher;
    private boolean actionsEnabled = true;
    private boolean closing;
    private boolean destroyed;
    public int mode;
    private boolean resetMode;
    private int basePaddingBottom;
    private int basePaddingLeft;
    private int basePaddingRight;
    private int basePaddingTop;
    private int fastScrollInsetEnd;
    private boolean fastScrollVisible;
    private int systemBarInsetBottom;
    private int systemBarInsetLeft;
    private int systemBarInsetRight;
    private int systemBarInsetTop;
    private Rect systemGestureInsets;

    /**
     * Constructs an applications grid view with context.
     */
    public ApplicationsGridView(Context context) {
        super(context);
        this.mode = 0;
        configureResponsiveColumns();
    }

    /**
     * Constructs an applications grid view with context and attributes.
     */
    public ApplicationsGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842865);
    }

    /**
     * Constructs an applications grid view with context, attributes, and default style.
     */
    public ApplicationsGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mode = 0;
        this.resetMode = true;
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

    @Override
    public void setSystemBarInsets(int left, int top, int right, int bottom) {
        this.systemBarInsetLeft = Math.max(0, left);
        this.systemBarInsetTop = Math.max(0, top);
        this.systemBarInsetRight = Math.max(0, right);
        this.systemBarInsetBottom = Math.max(0, bottom);
        updatePadding();
    }

    void setFastScrollVisible(boolean visible) {
        this.fastScrollVisible = visible;
        this.fastScrollInsetEnd = visible ? getResources().getDimensionPixelSize(
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
        int railId = this.fastScrollVisible ? R.id.drawer_fast_scroll : gridId;
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
        int fastScrollInsetLeft = isLayoutDirectionRtl() ? this.fastScrollInsetEnd : 0;
        int fastScrollInsetRight = isLayoutDirectionRtl() ? 0 : this.fastScrollInsetEnd;
        setPadding(this.basePaddingLeft + this.systemBarInsetLeft + fastScrollInsetLeft,
                this.basePaddingTop + this.systemBarInsetTop,
                this.basePaddingRight + this.systemBarInsetRight + fastScrollInsetRight,
                this.basePaddingBottom + this.systemBarInsetBottom);
        requestLayout();
    }

    @Override
    public void setSystemGestureInsets(Rect insets) {
        this.systemGestureInsets = insets;
    }

    @Override
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
        if (this.destroyed) {
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

    @Override
    public void setMode(int newMode) {
        if (this.mode == newMode) {
            if (newMode != MODE_DEFAULT) {
                setMode(MODE_DEFAULT);
            }
            return;
        }
        Context context = getContext();
        ApplicationsAdapter applicationsAdapter = (ApplicationsAdapter) getAdapter();
        if (applicationsAdapter == null) {
            this.mode = newMode;
            return;
        }
        if (newMode == MODE_DEFAULT) {
            setSelector(SelectorDrawable.createSelector(context, true));
            applicationsAdapter.setUninstalling(false);
            applicationsAdapter.notifyDataSetChanged();
        } else if (newMode == MODE_UNINSTALL) {
            setSelector(17170445);
            applicationsAdapter.setUninstalling(true);
            applicationsAdapter.notifyDataSetChanged();
        }
        this.mode = newMode;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.basePaddingLeft = getPaddingLeft();
        this.basePaddingTop = getPaddingTop();
        this.basePaddingRight = getPaddingRight();
        this.basePaddingBottom = getPaddingBottom();
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        if (!this.actionsEnabled || this.closing) {
            return;
        }
        ApplicationItemInfo item = (ApplicationItemInfo) parent.getItemAtPosition(position);
        if (item instanceof AppListFolderInfo) {
            if (this.mode == MODE_DEFAULT && this.launcher != null) {
                this.launcher.openAppListFolder((AppListFolderInfo) item);
            }
            return;
        }
        if (this.mode == MODE_DEFAULT && this.launcher != null) {
            this.resetMode = true;
            this.launcher.startActivitySafely(item.intent);
            return;
        }
        if (this.mode == MODE_UNINSTALL && Utilities.canUninstallApplication(getContext(), item) && this.launcher != null) {
            this.resetMode = false;
            this.launcher.uninstallApplication(item);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!this.actionsEnabled || this.closing || this.mode != MODE_DEFAULT || !view.isInTouchMode()) {
            return false;
        }
        ApplicationItemInfo item = (ApplicationItemInfo) parent.getItemAtPosition(position);
        if (item instanceof AppListFolderInfo) {
            if (this.launcher != null) {
                this.launcher.showAppListFolderActions((AppListFolderInfo) item);
            }
            return true;
        }
        if (this.dragController != null) {
            this.dragController.startDrag(view, this, new ApplicationItemInfo(item), DragController.DRAG_ACTION_COPY);
        }
        if (this.launcher != null) {
            this.launcher.closeAllApplications();
        }
        return true;
    }

    @Override
    public void setDragController(DragController controller) {
        this.dragController = controller;
    }

    @Override
    public void onDropCompleted(View target, boolean success) {
    }

    @Override
    public void setLauncher(Launcher launcherInstance) {
        this.launcher = launcherInstance;
    }

    @Override
    public void open(boolean animated) {
        prepareOpen();
        if (animated) {
            startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.apps_scale_in));
        }
        invalidate();
    }

    void prepareOpen() {
        this.closing = false;
        updateInputEnabled();
        resetVisualState();
        if (!PreferencesUtil.rememberApplicationsPosition(getContext())) {
            setSelection(0);
        }
        setVisibility(VISIBLE);
    }

    @Override
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
        if (this.mode != MODE_DEFAULT) {
            if (this.resetMode) {
                setMode(MODE_DEFAULT);
            }
            this.resetMode = true;
            return false;
        }
        this.closing = true;
        updateInputEnabled();
        resetVisualState();
        return true;
    }

    void finishClose() {
        setVisibility(INVISIBLE);
        resetVisualState();
    }

    @Override
    public void setLoading() {
        if (this.destroyed) {
            return;
        }
        this.actionsEnabled = false;
        updateInputEnabled();
        if (this.launcher != null) {
            this.launcher.onApplicationsLoading();
        }
    }

    @Override
    public void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos) {
        if (this.destroyed) {
            return;
        }
        ArrayList<ApplicationItemInfo> items = applicationItemInfos == null
                ? new ArrayList<ApplicationItemInfo>() : applicationItemInfos;
        ApplicationsAdapter applicationsAdapter = new ApplicationsAdapter(getContext(), items);
        applicationsAdapter.setUninstalling(this.mode == MODE_UNINSTALL);
        setAdapter(applicationsAdapter);
        resetPositionIfNeeded();
    }

    @Override
    public void setEmpty() {
        if (this.destroyed) {
            return;
        }
        this.actionsEnabled = false;
        updateInputEnabled();
        if (this.launcher != null) {
            this.launcher.onApplicationsEmpty();
        }
    }

    @Override
    public void setError() {
        if (this.destroyed) {
            return;
        }
        this.actionsEnabled = false;
        updateInputEnabled();
        if (this.launcher != null) {
            this.launcher.onApplicationsError();
        }
    }

    @Override
    public void clearState() {
        if (this.destroyed) {
            return;
        }
        this.actionsEnabled = true;
        updateInputEnabled();
        if (this.launcher != null) {
            this.launcher.onApplicationsReady();
        }
    }

    private void updateInputEnabled() {
        setEnabled(this.actionsEnabled && !this.closing);
    }

    @Override
    public void onDestroy() {
        this.destroyed = true;
        this.actionsEnabled = false;
        setEnabled(false);
        clearTextFilter();
        setAdapter((ListAdapter) null);
    }

    @Override
    public View getImplementingView() {
        return this;
    }

    @Override
    public Launcher getLauncher() {
        return this.launcher;
    }

    @Override
    public int getMode() {
        return this.mode;
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
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.apps_scale_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation ignored) {
            }

            @Override
            public void onAnimationRepeat(Animation ignored) {
            }

            @Override
            public void onAnimationEnd(Animation ignored) {
                finishClose();
            }
        });
        return animation;
    }
}
