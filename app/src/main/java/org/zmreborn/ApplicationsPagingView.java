package org.zmreborn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.zmreborn.ViewPager;
import org.zmreborn.theme.WallpaperColorExtractor;

/**
 * Paging view implementation for horizontally paginated app drawer.
 */
public class ApplicationsPagingView extends FrameLayout implements ApplicationsView,
        View.OnClickListener, View.OnLongClickListener, DragSource {
    private static final int CLOSE_DRAG_THRESHOLD_DP = 72;
    private static int staticColumns;
    private static int staticRows;
    private ArrayList<ApplicationItemInfo> applicationItemInfos;
    private ArrayList<ApplicationItemInfo> sourceItems = new ArrayList<>();
    private String query = "";
    private boolean actionsEnabled = true;
    private boolean closing;
    private DragController dragController;
    private boolean destroyed;
    private Launcher launcher;
    public int mode = 0;
    private boolean resetMode;
    private ScreenIndicator screenIndicator;
    private int indicatorType = ScreenIndicator.TYPE_DOTS;
    private boolean indicatorEnabled = true;
    private ViewPager viewPager;
    private FrameLayout searchContainer;
    private EditText searchInput;
    private ImageButton clearSearch;
    private TextView noResults;
    private boolean interceptingClose;
    private float closeStartX;
    private float closeStartY;
    private int basePaddingBottom;
    private int basePaddingLeft;
    private int basePaddingRight;
    private int basePaddingTop;
    private Rect systemGestureInsets;
    private boolean builtWithFallbackDimensions;

    /**
     * Constructs applications paging view with style.
     */
    public ApplicationsPagingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Constructs applications paging view with XML attributes.
     */
    public ApplicationsPagingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructs applications paging view with context.
     */
    public ApplicationsPagingView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setElevation(getResources().getDimension(R.dimen.elevation_drawer_header));
        this.basePaddingLeft = getPaddingLeft();
        this.basePaddingTop = getPaddingTop();
        this.basePaddingRight = getPaddingRight();
        this.basePaddingBottom = getPaddingBottom();
        this.resetMode = true;
        this.viewPager = (ViewPager) findViewById(R.id.view_pager);
        this.viewPager.setDrawingCacheEnabled(false);
        setDrawingCacheEnabled(false);
        this.viewPager.setOnPageScrollListener(new ViewPager.OnPageScrollListener() {
            @Override
            public void onScroll() {
                ApplicationsPagingView.this.indicate();
            }
        });
        this.viewPager.setOnViewportChangedListener(new ViewPager.OnViewportChangedListener() {
            @Override
            public void onViewportChanged(int width, int height) {
                ApplicationsPagingView.this.onPagerViewportChanged();
            }
        });
        this.searchContainer = (FrameLayout) findViewById(R.id.drawer_search_container);
        this.searchInput = (EditText) findViewById(R.id.drawer_search_input);
        this.clearSearch = (ImageButton) findViewById(R.id.drawer_search_clear);
        this.noResults = (TextView) findViewById(R.id.drawer_search_empty);
        if (this.searchInput != null) {
            bindSearch();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.closeStartX = event.getX();
                this.closeStartY = event.getY();
                this.interceptingClose = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - this.closeStartX);
                float dy = event.getY() - this.closeStartY;
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (dy > slop && dy > dx) {
                    this.interceptingClose = true;
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                this.interceptingClose = false;
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.interceptingClose) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                float dy = event.getY() - this.closeStartY;
                this.interceptingClose = false;
                performClick();
                if (dy >= closeDragThresholdPx() && this.launcher != null) {
                    this.launcher.closeAllApplications();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                this.interceptingClose = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int closeDragThresholdPx() {
        return (int) (CLOSE_DRAG_THRESHOLD_DP * getResources().getDisplayMetrics().density);
    }

    private void bindSearch() {
        this.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateQuery(s != null ? s.toString() : "");
            }
        });
        if (this.clearSearch != null) {
            this.clearSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (searchInput != null) {
                        searchInput.setText("");
                    }
                }
            });
        }
    }

    private void updateQuery(String newQuery) {
        this.query = newQuery;
        boolean hasQuery = !DrawerSearchFilter.isEmptyQuery(newQuery);
        if (this.clearSearch != null) {
            this.clearSearch.setVisibility(hasQuery ? VISIBLE : GONE);
        }
        ArrayList<ApplicationItemInfo> filtered = DrawerSearchFilter.filter(this.sourceItems, newQuery);
        this.applicationItemInfos = filtered;
        buildPages();
        initIndicator();
        if (this.noResults != null) {
            this.noResults.setVisibility(hasQuery && filtered.isEmpty() ? VISIBLE : GONE);
        }
    }

    private void clearSearchOnClose() {
        if (DrawerSearchFilter.isEmptyQuery(this.query)) {
            return;
        }
        this.query = "";
        if (this.searchInput != null) {
            this.searchInput.setText("");
            this.searchInput.clearFocus();
        }
        if (this.clearSearch != null) {
            this.clearSearch.setVisibility(GONE);
        }
        if (this.noResults != null) {
            this.noResults.setVisibility(GONE);
        }
        this.applicationItemInfos = new ArrayList<>(this.sourceItems);
        buildPages();
        initIndicator();
        hideSearchKeyboard();
    }

    private void hideSearchKeyboard() {
        if (this.searchInput == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    private void applySearchPalette() {
        if (this.searchInput == null) {
            return;
        }
        int onSurface = WallpaperColorExtractor.getOnSurface(getContext());
        this.searchInput.setTextColor(onSurface);
        this.searchInput.setHintTextColor(WallpaperColorExtractor.getOutline(getContext()));
        this.searchInput.setBackground(createSearchBackground());
        if (this.clearSearch != null) {
            this.clearSearch.setColorFilter(onSurface);
        }
        if (this.noResults != null) {
            this.noResults.setTextColor(onSurface);
        }
    }

    private GradientDrawable createSearchBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(WallpaperColorExtractor.getSurfaceVariant(getContext()));
        background.setCornerRadius(getResources().getDimension(R.dimen.shape_corner_extra_large));
        int strokeWidth = Math.max(1, Math.round(getResources().getDisplayMetrics().density));
        background.setStroke(strokeWidth, WallpaperColorExtractor.getOutline(getContext()));
        return background;
    }

    /**
     * Configures the external ScreenIndicator provided by Launcher from DragLayer.
     */
    public void configureIndicator(ScreenIndicator indicator, boolean enabled, int type) {
        this.screenIndicator = indicator;
        this.indicatorEnabled = enabled;
        this.indicatorType = type;
    }

    @Override
    public void setNumColumns(int columns) {
        staticColumns = columns;
    }

    /**
     * Configures rows per page.
     */
    public void setNumRows(int rows) {
        staticRows = rows;
    }

    @Override
    public void setSystemBarInsets(int left, int top, int right, int bottom) {
        setPadding(this.basePaddingLeft + Math.max(0, left),
                this.basePaddingTop + Math.max(0, top),
                this.basePaddingRight + Math.max(0, right),
                this.basePaddingBottom + Math.max(0, bottom));
        requestLayout();
    }

    @Override
    public void setSystemGestureInsets(Rect insets) {
        this.systemGestureInsets = insets;
    }

    @Override
    public void setBackgroundAlpha(int alpha) {
        int surface = WallpaperColorExtractor.getSurface(getContext());
        int background = Color.argb(alpha, Color.red(surface),
                Color.green(surface), Color.blue(surface));
        setBackgroundColor(background);
        invalidate();
    }

    @Override
    public void refreshPalette() {
        if (this.destroyed || this.viewPager == null) {
            return;
        }
        View pageHolder = this.viewPager.getChildAt(0);
        if (pageHolder instanceof ViewGroup) {
            ViewGroup holder = (ViewGroup) pageHolder;
            for (int index = 0; index < holder.getChildCount(); index++) {
                View page = holder.getChildAt(index);
                if (page instanceof ApplicationsPageView) {
                    ((ApplicationsPageView) page).refreshPalette();
                }
            }
        }
        if (this.screenIndicator != null) {
            this.screenIndicator.refreshPalette();
        }
        if (this.searchContainer != null) {
            applySearchPalette();
        }
        invalidate();
    }

    @Override
    public void setLoading() {
        if (this.destroyed) {
            return;
        }
        this.actionsEnabled = false;
        setEnabled(false);
        if (this.launcher != null) {
            this.launcher.onApplicationsLoading();
        }
    }

    @Override
    public void setApplications(ArrayList<ApplicationItemInfo> infos) {
        if (this.destroyed) {
            return;
        }
        this.sourceItems = infos == null ? new ArrayList<ApplicationItemInfo>() : new ArrayList<>(infos);
        this.applicationItemInfos = DrawerSearchFilter.filter(this.sourceItems, this.query);
        buildPages();
        initIndicator();
    }

    @Override
    public void setEmpty() {
        if (this.destroyed) {
            return;
        }
        this.actionsEnabled = false;
        setEnabled(false);
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
        setEnabled(false);
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
        setEnabled(true);
        if (this.launcher != null) {
            this.launcher.onApplicationsReady();
        }
    }

    private void buildPages() {
        if (this.viewPager == null) {
            return;
        }
        int priorFirstOrdinal = captureFirstVisibleOrdinal();
        ArrayList<ApplicationItemInfo> items = this.applicationItemInfos;
        int viewportWidth = this.viewPager.getWidth();
        int viewportHeight = this.viewPager.getHeight();
        this.builtWithFallbackDimensions = (viewportWidth <= 0 || viewportHeight <= 0);
        DrawerLayoutMetrics metrics = calculatePageMetrics();
        LinkedHashMap<Integer, List<ApplicationItemInfo>> pageContents = loadPageContents(
                metrics.getRows(), metrics.getColumns(), items);
        boolean uninstalling = this.mode == 1;
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        ArrayList<View> pageViews = new ArrayList<>();
        for (Integer intValue : pageContents.keySet()) {
            int page = intValue.intValue();
            ApplicationsPageView applicationsPageView = (ApplicationsPageView) layoutInflater.inflate(
                    R.layout.apps_page_view, this.viewPager, false);
            applicationsPageView.populatePage(uninstalling, metrics.getRows(), metrics.getColumns(),
                    pageContents.get(Integer.valueOf(page)), this, this);
            pageViews.add(applicationsPageView);
        }
        this.viewPager.clearPagingViews();
        this.viewPager.setPagingViews(pageViews);
        clampCurrentPageIndex(priorFirstOrdinal, metrics.getRows(), metrics.getColumns());
    }

    private int captureFirstVisibleOrdinal() {
        if (this.viewPager == null) {
            return 0;
        }
        int currentPage = this.viewPager.getCurrentPageIndex();
        return ApplicationsPagePartition.calculatePageStart(currentPage, staticRows, staticColumns);
    }

    private void onPagerViewportChanged() {
        if (this.builtWithFallbackDimensions && this.applicationItemInfos != null
                && !this.applicationItemInfos.isEmpty()) {
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
        int minimumCellWidth = getResources().getDimensionPixelSize(R.dimen.drawer_cell_min_width);
        int minimumCellHeight = getResources().getDimensionPixelSize(R.dimen.drawer_cell_min_height);
        return DrawerLayoutMetrics.calculate(width, height, staticRows, staticColumns,
                getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom(),
                minimumCellWidth, minimumCellHeight);
    }

    private static LinkedHashMap<Integer, List<ApplicationItemInfo>> loadPageContents(int rows, int columns,
            ArrayList<ApplicationItemInfo> applicationItemInfos) {
        LinkedHashMap<Integer, List<ApplicationItemInfo>> pageContents = new LinkedHashMap<>();
        if (applicationItemInfos == null || applicationItemInfos.isEmpty()) {
            return pageContents;
        }
        int pageCount = ApplicationsPagePartition.calculatePageCount(applicationItemInfos.size(), rows, columns);
        for (int page = 0; page < pageCount; page++) {
            int start = ApplicationsPagePartition.calculatePageStart(page, rows, columns);
            int end = ApplicationsPagePartition.calculatePageEnd(page, applicationItemInfos.size(), rows, columns);
            List<ApplicationItemInfo> pageList = applicationItemInfos.subList(start, end);
            if (!pageList.isEmpty()) {
                pageContents.put(Integer.valueOf(page + 1), pageList);
            }
        }
        return pageContents;
    }

    @Override
    public void onDestroy() {
        this.destroyed = true;
        this.actionsEnabled = false;
        setEnabled(false);
        this.applicationItemInfos = null;
    }

    private void initIndicator() {
        if (this.screenIndicator == null) {
            return;
        }
        int pageCount = this.viewPager.getPageCount();
        this.screenIndicator.setItems(pageCount);
        if (!this.indicatorEnabled) {
            this.screenIndicator.hide();
            return;
        }
        this.screenIndicator.setType(this.indicatorType);
        this.screenIndicator.setAutoHide(false);
        if (pageCount <= 0) {
            this.screenIndicator.fullIndicate(0);
            this.viewPager.resetScroll();
            return;
        }
        this.screenIndicator.fullIndicate(this.viewPager.getCurrentPageIndex());
    }

    @Override
    public void open(boolean animated) {
        this.closing = false;
        resetVisualState();
        buildPages();
        initIndicator();
        setVisibility(VISIBLE);
        if (animated) {
            startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.apps_scale_in));
        }
        invalidate();
    }

    @Override
    public boolean close(boolean animated) {
        if (this.mode != 0) {
            if (this.resetMode) {
                setMode(0);
            }
            this.resetMode = true;
            return false;
        }
        clearSearchOnClose();
        this.closing = true;
        resetVisualState();
        if (!animated) {
            setVisibility(INVISIBLE);
            return true;
        }
        startAnimation(createCloseAnimation());
        return true;
    }

    @Override
    public void setDragController(DragController controller) {
        this.dragController = controller;
    }

    @Override
    public void setLauncher(Launcher launcherInstance) {
        this.launcher = launcherInstance;
    }

    @Override
    public View getImplementingView() {
        return this;
    }

    @Override
    public void setMode(int newMode) {
        if (this.mode == newMode) {
            if (newMode != MODE_DEFAULT) {
                setMode(MODE_DEFAULT);
            }
            return;
        }
        this.mode = newMode;
        buildPages();
    }

    @Override
    public Launcher getLauncher() {
        return this.launcher;
    }

    @Override
    public int getMode() {
        return this.mode;
    }

    @Override
    public void onDropCompleted(View target, boolean success) {
    }

    @Override
    public boolean onLongClick(View view) {
        if (!this.actionsEnabled || this.closing || this.mode != 0 || !view.isInTouchMode()) {
            return false;
        }
        ApplicationItemInfo item = (ApplicationItemInfo) view.getTag();
        if (item instanceof AppListFolderInfo) {
            if (this.launcher != null) {
                this.launcher.showAppListFolderActions((AppListFolderInfo) item);
            }
            return true;
        }
        ApplicationItemInfo copiedItem = new ApplicationItemInfo(item);
        if (this.dragController != null) {
            this.dragController.startDrag(view, this, copiedItem, DragController.DRAG_ACTION_COPY);
        }
        if (this.launcher != null) {
            this.launcher.closeAllApplications();
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if (!this.actionsEnabled || this.closing) {
            return;
        }
        ApplicationItemInfo item = (ApplicationItemInfo) view.getTag();
        if (item instanceof AppListFolderInfo) {
            if (this.mode == 0 && this.launcher != null) {
                this.launcher.openAppListFolder((AppListFolderInfo) item);
            }
            return;
        }
        if (this.mode == 0 && this.launcher != null) {
            this.resetMode = true;
            this.launcher.startActivitySafely(item.intent);
            return;
        }
        if (this.mode == 1 && Utilities.canUninstallApplication(getContext(), item) && this.launcher != null) {
            this.resetMode = false;
            this.launcher.uninstallApplication(item);
        }
    }

    private void indicate() {
        if (this.screenIndicator == null || this.viewPager == null) {
            return;
        }
        int pageCount = this.viewPager.getPageCount();
        int pageWidth = this.viewPager.getPageWidth();
        if (pageCount <= 0 || pageWidth <= 0) {
            return;
        }
        float progress = ((float) this.viewPager.getScrollX()) / ((float) (pageCount * pageWidth));
        this.screenIndicator.indicate(progress);
    }

    private void clampCurrentPageIndex(int priorFirstOrdinal, int rows, int columns) {
        int pageCount = this.viewPager.getPageCount();
        if (pageCount <= 0) {
            this.viewPager.resetScroll();
            return;
        }
        if (!PreferencesUtil.rememberApplicationsPosition(getContext())) {
            this.viewPager.resetScroll();
            return;
        }
        int restoredPage = ApplicationsPagePartition.pageIndexForItemOrdinal(
                priorFirstOrdinal, rows, columns);
        int clampedPage = Math.min(restoredPage, pageCount - 1);
        if (clampedPage != this.viewPager.getCurrentPageIndex()) {
            this.viewPager.moveToPageForced(clampedPage);
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
            @Override
            public void onAnimationStart(Animation ignored) {
            }

            @Override
            public void onAnimationRepeat(Animation ignored) {
            }

            @Override
            public void onAnimationEnd(Animation ignored) {
                setVisibility(INVISIBLE);
                resetVisualState();
            }
        });
        return animation;
    }
}
