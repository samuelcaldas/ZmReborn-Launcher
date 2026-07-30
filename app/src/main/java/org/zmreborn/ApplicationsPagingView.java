package org.zmreborn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.zmreborn.ViewPager;
import org.zmreborn.theme.WallpaperColorExtractor;

public class ApplicationsPagingView extends FrameLayout implements ApplicationsView, View.OnClickListener, View.OnLongClickListener, DragSource {
    private static int sColumns;
    private static int sRows;
    private ArrayList<ApplicationItemInfo> mApplicationItemInfos;
    private boolean mActionsEnabled = true;
    private boolean mClosing;
    private DragController mDragController;
    private boolean mDestroyed;
    private Launcher mLauncher;
    public int mMode = 0;
    private boolean mResetMode;
    private ScreenIndicator mScreenIndicator;
    private ViewPager mViewPager;
    private int mBasePaddingBottom;
    private int mBasePaddingLeft;
    private int mBasePaddingRight;
    private int mBasePaddingTop;
    private Rect mSystemGestureInsets;
    private boolean mBuiltWithFallbackDimensions;

    public ApplicationsPagingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ApplicationsPagingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ApplicationsPagingView(Context context) {
        super(context);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        setElevation(getResources().getDimension(R.dimen.elevation_drawer_header));
        this.mBasePaddingLeft = getPaddingLeft();
        this.mBasePaddingTop = getPaddingTop();
        this.mBasePaddingRight = getPaddingRight();
        this.mBasePaddingBottom = getPaddingBottom();
        this.mResetMode = true;
        this.mViewPager = (ViewPager) findViewById(R.id.view_pager);
        this.mViewPager.setDrawingCacheEnabled(false);
        setDrawingCacheEnabled(false);
        this.mViewPager.setOnPageScrollListener(new ViewPager.OnPageScrollListener() {
            public void onScroll() {
                ApplicationsPagingView.this.indicate();
            }
        });
        this.mViewPager.setOnViewportChangedListener(new ViewPager.OnViewportChangedListener() {
            public void onViewportChanged(int width, int height) {
                ApplicationsPagingView.this.onPagerViewportChanged();
            }
        });
        this.mScreenIndicator = (ScreenIndicator) findViewById(R.id.apps_paging_screen_indicator);
    }

    public void setNumColumns(int columns) {
        sColumns = columns;
    }

    public void setNumRows(int rows) {
        sRows = rows;
    }

    public void setSystemBarInsets(int left, int top, int right, int bottom) {
        setPadding(this.mBasePaddingLeft + Math.max(0, left),
                this.mBasePaddingTop + Math.max(0, top),
                this.mBasePaddingRight + Math.max(0, right),
                this.mBasePaddingBottom + Math.max(0, bottom));
        requestLayout();
    }

    public void setSystemGestureInsets(Rect insets) {
        this.mSystemGestureInsets = insets;
    }

    public void setBackgroundAlpha(int alpha) {
        int surface = WallpaperColorExtractor.getSurface(getContext());
        int background = Color.argb(alpha, Color.red(surface),
                Color.green(surface), Color.blue(surface));
        setBackgroundColor(background);
        invalidate();
    }

    @Override
    public void refreshPalette() {
        if (this.mDestroyed || this.mViewPager == null) {
            return;
        }
        View pageHolder = this.mViewPager.getChildAt(0);
        if (pageHolder instanceof ViewGroup) {
            ViewGroup holder = (ViewGroup) pageHolder;
            for (int index = 0; index < holder.getChildCount(); index++) {
                View page = holder.getChildAt(index);
                if (page instanceof ApplicationsPageView) {
                    ((ApplicationsPageView) page).refreshPalette();
                }
            }
        }
        if (this.mScreenIndicator != null) {
            this.mScreenIndicator.refreshPalette();
        }
        invalidate();
    }

    public void setLoading() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = false;
        setEnabled(false);
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsLoading();
        }
    }

    public void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos) {
        if (this.mDestroyed) {
            return;
        }
        this.mApplicationItemInfos = applicationItemInfos == null
                ? new ArrayList<ApplicationItemInfo>() : applicationItemInfos;
        buildPages();
        initIndicator();
    }

    public void setEmpty() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = false;
        setEnabled(false);
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsEmpty();
        }
    }

    public void setError() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = false;
        setEnabled(false);
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsError();
        }
    }

    public void clearState() {
        if (this.mDestroyed) {
            return;
        }
        this.mActionsEnabled = true;
        setEnabled(true);
        if (this.mLauncher != null) {
            this.mLauncher.onApplicationsReady();
        }
    }

    private void buildPages() {
        if (this.mViewPager == null) {
            return;
        }
        int priorFirstOrdinal = captureFirstVisibleOrdinal();
        ArrayList<ApplicationItemInfo> applicationItemInfos = this.mApplicationItemInfos;
        int viewportWidth = this.mViewPager.getWidth();
        int viewportHeight = this.mViewPager.getHeight();
        this.mBuiltWithFallbackDimensions = (viewportWidth <= 0 || viewportHeight <= 0);
        DrawerLayoutMetrics metrics = calculatePageMetrics();
        LinkedHashMap<Integer, List<ApplicationItemInfo>> pageContents = loadPageContents(
                metrics.getRows(), metrics.getColumns(), applicationItemInfos);
        boolean uninstalling = this.mMode == 1;
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        ArrayList<View> pageViews = new ArrayList<>();
        for (Integer intValue : pageContents.keySet()) {
            int page = intValue.intValue();
            ApplicationsPageView applicationsPageView = (ApplicationsPageView) layoutInflater.inflate(
                    R.layout.apps_page_view, (ViewGroup) null);
            applicationsPageView.populatePage(uninstalling, metrics.getRows(), metrics.getColumns(),
                    pageContents.get(Integer.valueOf(page)), this, this);
            pageViews.add(applicationsPageView);
        }
        this.mViewPager.clearPagingViews();
        this.mViewPager.setPagingViews(pageViews);
        clampCurrentPageIndex(priorFirstOrdinal, metrics.getRows(), metrics.getColumns());
    }

    private int captureFirstVisibleOrdinal() {
        if (this.mViewPager == null) {
            return 0;
        }
        int currentPage = this.mViewPager.getCurrentPageIndex();
        return ApplicationsPagePartition.calculatePageStart(currentPage, sRows, sColumns);
    }

    private void onPagerViewportChanged() {
        if (this.mBuiltWithFallbackDimensions && this.mApplicationItemInfos != null
                && !this.mApplicationItemInfos.isEmpty()) {
            buildPages();
            initIndicator();
        }
    }

    private DrawerLayoutMetrics calculatePageMetrics() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
            height = getResources().getDisplayMetrics().heightPixels;
        }
        int minimumCellWidth = getResources().getDimensionPixelSize(
                R.dimen.drawer_cell_min_width);
        int minimumCellHeight = getResources().getDimensionPixelSize(
                R.dimen.drawer_cell_min_height);
        return DrawerLayoutMetrics.calculate(width, height, sRows, sColumns,
                getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom(),
                minimumCellWidth, minimumCellHeight);
    }

    private static LinkedHashMap<Integer, List<ApplicationItemInfo>> loadPageContents(int rows, int columns,
            ArrayList<ApplicationItemInfo> applicationItemInfos) {
        LinkedHashMap<Integer, List<ApplicationItemInfo>> pageContents = new LinkedHashMap<>();
        if (applicationItemInfos == null || applicationItemInfos.size() == 0) {
            return pageContents;
        }
        int pageCount = ApplicationsPagePartition.calculatePageCount(applicationItemInfos.size(), rows, columns);
        for (int page = 0; page < pageCount; page++) {
            int start = ApplicationsPagePartition.calculatePageStart(page, rows, columns);
            int end = ApplicationsPagePartition.calculatePageEnd(page, applicationItemInfos.size(), rows, columns);
            List<ApplicationItemInfo> pageList = applicationItemInfos.subList(start, end);
            if (pageList.size() > 0) {
                pageContents.put(Integer.valueOf(page + 1), pageList);
            }
        }
        return pageContents;
    }

    public void onDestroy() {
        this.mDestroyed = true;
        this.mActionsEnabled = false;
        setEnabled(false);
        this.mApplicationItemInfos = null;
    }

    private void initIndicator() {
        if (this.mScreenIndicator != null) {
            int pageCount = this.mViewPager.getPageCount();
            this.mScreenIndicator.setItems(pageCount);
            this.mScreenIndicator.setType(1);
            this.mScreenIndicator.setAutoHide(false);
            if (pageCount <= 0) {
                this.mScreenIndicator.fullIndicate(0);
                this.mViewPager.resetScroll();
                return;
            }
            this.mScreenIndicator.fullIndicate(this.mViewPager.getCurrentPageIndex());
        }
    }

    public void open(boolean animated) {
        this.mClosing = false;
        resetVisualState();
        buildPages();
        initIndicator();
        setVisibility(VISIBLE);
        if (animated) {
            startAnimation(AnimationUtils.loadAnimation(
                    getContext(), R.anim.apps_scale_in));
        }
        invalidate();
    }

    public boolean close(boolean animated) {
        if (this.mMode != 0) {
            if (this.mResetMode) {
                setMode(0);
            }
            this.mResetMode = true;
            return false;
        }
        this.mClosing = true;
        resetVisualState();
        if (!animated) {
            setVisibility(INVISIBLE);
            return true;
        }
        startAnimation(createCloseAnimation());
        return true;
    }

    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public View getImplementingView() {
        return this;
    }

    public void setMode(int mode) {
        if (this.mMode == mode) {
            if (mode != MODE_DEFAULT) {
                setMode(MODE_DEFAULT);
            }
            return;
        }
        this.mMode = mode;
        buildPages();
    }

    public Launcher getLauncher() {
        return this.mLauncher;
    }

    @Override
    public int getMode() {
        return this.mMode;
    }

    public void onDropCompleted(View target, boolean success) {
    }

    public boolean onLongClick(View view) {
        if (!this.mActionsEnabled || this.mClosing
                || this.mMode != 0 || !view.isInTouchMode()) {
            return false;
        }
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) view.getTag();
        if (applicationItemInfo instanceof AppListFolderInfo) {
            this.mLauncher.showAppListFolderActions((AppListFolderInfo) applicationItemInfo);
            return true;
        }
        ApplicationItemInfo copiedItem = new ApplicationItemInfo(applicationItemInfo);
        this.mDragController.startDrag(view, this, copiedItem, 1);
        this.mLauncher.closeAllApplications();
        return true;
    }

    public void onClick(View view) {
        if (!this.mActionsEnabled || this.mClosing) {
            return;
        }
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) view.getTag();
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

    /* access modifiers changed from: private */
    public void indicate() {
        int pageCount = this.mViewPager.getPageCount();
        int pageWidth = this.mViewPager.getPageWidth();
        if (pageCount <= 0 || pageWidth <= 0) {
            return;
        }
        float progress = ((float) this.mViewPager.getScrollX()) / ((float) (pageCount * pageWidth));
        this.mScreenIndicator.indicate(progress);
    }

    private void clampCurrentPageIndex(int priorFirstOrdinal, int rows, int columns) {
        int pageCount = this.mViewPager.getPageCount();
        if (pageCount <= 0) {
            this.mViewPager.resetScroll();
            return;
        }
        if (!PreferencesUtil.rememberApplicationsPosition(getContext())) {
            this.mViewPager.resetScroll();
            return;
        }
        int restoredPage = ApplicationsPagePartition.pageIndexForItemOrdinal(
                priorFirstOrdinal, rows, columns);
        int clampedPage = Math.min(restoredPage, pageCount - 1);
        if (clampedPage != this.mViewPager.getCurrentPageIndex()) {
            this.mViewPager.moveToPageForced(clampedPage);
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
                setVisibility(INVISIBLE);
                resetVisualState();
            }
        });
        return animation;
    }
}
