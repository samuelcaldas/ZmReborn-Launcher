package org.zmreborn;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import org.zmreborn.theme.WallpaperColorExtractor;

/**
 * Vertical app drawer containing a collapsible search bar, alphabet fast-scroll rail, and applications grid.
 */
public class ApplicationsDrawerView extends LinearLayout implements ApplicationsView {
    private static final int FAST_SCROLL_HIDE_DELAY_MS = 1000;
    private static final int FAST_SCROLL_ANIM_DURATION_MS = 150;
    private static final int SEARCH_REVEAL_ANIM_DURATION_MS = 150;
    private static final int CLOSE_DRAG_THRESHOLD_DP = 72;

    private ApplicationsGridView gridView;
    private EditText searchInput;
    private ImageButton clearSearch;
    private TextView noResults;
    private DrawerFastScrollView fastScroll;
    private FrameLayout searchContainer;
    private ArrayList<ApplicationItemInfo> sourceItems = new ArrayList<>();
    private ArrayList<ApplicationItemInfo> displayedItems = new ArrayList<>();
    private DrawerScrollState searchOriginState = DrawerScrollState.empty();
    private String query = "";
    private boolean destroyed;
    private boolean closing;
    private boolean searchControlsEnabled = true;
    private int submissionGeneration;
    private int closeGeneration;
    private int backgroundAlpha = 255;
    private int basePaddingLeft;
    private int basePaddingTop;
    private int basePaddingRight;
    private int basePaddingBottom;

    private boolean fastScrollEnabled;
    private boolean fastScrollVisible;
    private Handler fastScrollHandler;
    private Runnable fastScrollHideRunnable;

    private int searchBarMaxHeight;
    private boolean searchRevealed;
    private float pullStartY;
    private int pullCurrentHeight;
    private boolean interceptingPull;
    private boolean interceptingClose;

    /** Creates drawer without XML attributes. */
    public ApplicationsDrawerView(Context context) {
        super(context);
    }

    /** Creates drawer from XML attributes. */
    public ApplicationsDrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Creates drawer from XML attributes and style. */
    public ApplicationsDrawerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        bindViews();
        captureBasePadding();
        bindSearch();
        bindFastScroll();
        updateSearchFocus();
        applyDensity();
        refreshPalette();
        updateSearchControls();
        initFastScrollHandler();
        collapseSearchBar();
    }

    private void bindViews() {
        this.gridView = (ApplicationsGridView) findViewById(R.id.apps_grid_content);
        this.searchInput = (EditText) findViewById(R.id.drawer_search_input);
        this.clearSearch = (ImageButton) findViewById(R.id.drawer_search_clear);
        this.noResults = (TextView) findViewById(R.id.drawer_search_empty);
        this.fastScroll = (DrawerFastScrollView) findViewById(R.id.drawer_fast_scroll);
        this.searchContainer = (FrameLayout) findViewById(R.id.drawer_search_container);
        if (this.gridView == null || this.searchInput == null
                || this.clearSearch == null || this.noResults == null
                || this.fastScroll == null || this.searchContainer == null) {
            throw new IllegalStateException("Applications drawer layout is incomplete");
        }
    }

    private void captureBasePadding() {
        this.basePaddingLeft = getPaddingLeft();
        this.basePaddingTop = getPaddingTop();
        this.basePaddingRight = getPaddingRight();
        this.basePaddingBottom = getPaddingBottom();
    }

    private void bindSearch() {
        this.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                updateQuery(value);
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        this.clearSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSearch();
            }
        });
    }

    private void bindFastScroll() {
        this.fastScroll.setOnSectionSelectedListener(
                new DrawerFastScrollView.OnSectionSelectedListener() {
            @Override
            public void onSectionSelected(int position) {
                scrollToFastScrollPosition(position);
            }
        });
        this.gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                onGridScrollStateChanged(scrollState);
            }

            @Override
            public void onScroll(AbsListView view, int first, int visible, int total) {
            }
        });
    }

    private void initFastScrollHandler() {
        this.fastScrollHandler = new Handler(Looper.getMainLooper());
        this.fastScrollHideRunnable = new Runnable() {
            @Override
            public void run() {
                hideFastScrollAnimated();
            }
        };
    }

    private void onGridScrollStateChanged(int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            this.fastScrollHandler.removeCallbacks(this.fastScrollHideRunnable);
            this.fastScroll.clearSelection();
            if (this.fastScrollEnabled && !this.fastScrollVisible) {
                this.fastScrollVisible = true;
                this.fastScroll.setVisibility(VISIBLE);
                this.fastScroll.setAlpha(0.0f);
                this.fastScroll.animate()
                        .alpha(1.0f)
                        .setDuration(FAST_SCROLL_ANIM_DURATION_MS)
                        .start();
            }
        } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            this.fastScrollHandler.postDelayed(
                    this.fastScrollHideRunnable, FAST_SCROLL_HIDE_DELAY_MS);
        }
    }

    private void hideFastScrollAnimated() {
        if (!this.fastScrollVisible) {
            return;
        }
        this.fastScrollVisible = false;
        this.fastScroll.animate()
                .alpha(0.0f)
                .setDuration(FAST_SCROLL_ANIM_DURATION_MS)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!fastScrollVisible) {
                            fastScroll.setVisibility(GONE);
                        }
                    }
                })
                .start();
    }

    private void hideFastScrollImmediately() {
        if (this.fastScrollHandler != null) {
            this.fastScrollHandler.removeCallbacks(this.fastScrollHideRunnable);
        }
        this.fastScrollVisible = false;
        this.fastScroll.animate().cancel();
        this.fastScroll.setAlpha(1.0f);
        this.fastScroll.setVisibility(GONE);
    }

    private void scrollToFastScrollPosition(int position) {
        if (this.destroyed || this.closing || position < 0) {
            return;
        }
        this.gridView.setSelectionFromTop(position, this.gridView.getPaddingTop());
    }

    private void updateQuery(CharSequence queryText) {
        if (this.destroyed) {
            return;
        }
        boolean wasEmpty = DrawerSearchFilter.isEmptyQuery(this.query);
        boolean isEmpty = DrawerSearchFilter.isEmptyQuery(queryText);
        if (wasEmpty && !isEmpty) {
            this.searchOriginState = captureScrollState();
        }
        DrawerScrollState restoreState = isEmpty
                ? this.searchOriginState : captureScrollState();
        this.query = queryText == null ? "" : queryText.toString();
        submitFilteredItems(restoreState);
        if (isEmpty) {
            this.searchOriginState = DrawerScrollState.empty();
        }
        updateSearchControls();
    }

    private void submitFilteredItems(DrawerScrollState restoreState) {
        this.submissionGeneration++;
        this.displayedItems = DrawerSearchFilter.filter(this.sourceItems, this.query);
        this.gridView.setApplications(this.displayedItems);
        updateNoResultsState();
        updateFastScroll();
        restoreScrollState(restoreState);
    }

    private DrawerScrollState captureScrollState() {
        if (this.gridView.getAdapter() == null) {
            return DrawerScrollState.empty();
        }
        int count = this.gridView.getAdapter().getCount();
        if (count == 0) {
            return DrawerScrollState.empty();
        }
        int position = this.gridView.getFirstVisiblePosition();
        if (position < 0 || position >= count) {
            return DrawerScrollState.empty();
        }
        Object item = this.gridView.getItemAtPosition(position);
        if (!(item instanceof ApplicationItemInfo)) {
            return DrawerScrollState.empty();
        }
        View firstChild = this.gridView.getChildAt(0);
        int offset = firstChild == null ? 0
                : firstChild.getTop() - this.gridView.getPaddingTop();
        return DrawerScrollState.capture((ApplicationItemInfo) item, position, offset);
    }

    private void restoreScrollState(final DrawerScrollState state) {
        final int position = state.resolvePosition(this.displayedItems);
        if (position < 0) {
            return;
        }
        final int generation = this.submissionGeneration;
        this.gridView.post(new Runnable() {
            @Override
            public void run() {
                if (destroyed || generation != submissionGeneration) {
                    return;
                }
                int top = gridView.getPaddingTop() + state.getTopOffset();
                gridView.setSelectionFromTop(position, top);
            }
        });
    }

    private void updateNoResultsState() {
        boolean noRes = !DrawerSearchFilter.isEmptyQuery(this.query)
                && !this.sourceItems.isEmpty() && this.displayedItems.isEmpty();
        this.gridView.setVisibility(noRes ? GONE : VISIBLE);
        this.noResults.setVisibility(noRes ? VISIBLE : GONE);
    }

    private void updateFastScroll() {
        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(this.displayedItems);
        this.fastScrollEnabled = DrawerSearchFilter.isEmptyQuery(this.query)
                && index.hasMultipleSections();
        this.fastScroll.setIndex(index);
        if (!this.fastScrollEnabled) {
            hideFastScrollImmediately();
        }
        this.gridView.setFastScrollVisible(this.fastScrollEnabled);
        updateFastScrollFocus(this.fastScrollEnabled);
    }

    private void updateFastScrollFocus(boolean visible) {
        int gridId = R.id.apps_grid_content;
        int railId = visible ? R.id.drawer_fast_scroll : gridId;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            this.fastScroll.setNextFocusLeftId(railId);
            this.fastScroll.setNextFocusRightId(gridId);
            return;
        }
        this.fastScroll.setNextFocusLeftId(gridId);
        this.fastScroll.setNextFocusRightId(railId);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (this.gridView == null || this.fastScroll == null) {
            return;
        }
        this.gridView.setFastScrollVisible(this.fastScrollEnabled);
        updateFastScrollFocus(this.fastScrollEnabled);
        updateSearchFocus();
    }

    private void updateSearchFocus() {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            this.clearSearch.setNextFocusLeftId(R.id.drawer_search_clear);
            this.clearSearch.setNextFocusRightId(R.id.drawer_search_input);
            return;
        }
        this.clearSearch.setNextFocusLeftId(R.id.drawer_search_input);
        this.clearSearch.setNextFocusRightId(R.id.drawer_search_clear);
    }

    private void updateSearchControls() {
        boolean hasQuery = !DrawerSearchFilter.isEmptyQuery(this.query);
        this.clearSearch.setVisibility(hasQuery ? VISIBLE : GONE);
        if (hasQuery && !this.searchRevealed) {
            revealSearchBarImmediate();
        }
    }

    private void clearSearch() {
        this.searchInput.setText("");
        this.searchInput.requestFocus();
    }

    private void applyDensity() {
        this.gridView.setPreferredColumnWidth(
                DrawerDensityPolicy.getPreferredColumnWidth(getContext()));
    }

    private void collapseSearchBar() {
        if (this.searchContainer == null) {
            return;
        }
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) this.searchContainer.getLayoutParams();
        params.height = 0;
        this.searchContainer.setLayoutParams(params);
        this.searchContainer.setVisibility(GONE);
        this.searchRevealed = false;
    }

    private void revealSearchBarImmediate() {
        if (this.searchContainer == null) {
            return;
        }
        ViewGroup.LayoutParams params = this.searchContainer.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        this.searchContainer.setLayoutParams(params);
        this.searchContainer.setVisibility(VISIBLE);
        this.searchRevealed = true;
    }

    private int getSearchBarMaxHeight() {
        if (this.searchBarMaxHeight == 0 && this.searchContainer != null
                && getMeasuredWidth() > 0) {
            this.searchContainer.setVisibility(VISIBLE);
            this.searchContainer.measure(
                    MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            this.searchBarMaxHeight = this.searchContainer.getMeasuredHeight();
            this.searchContainer.setVisibility(GONE);
        }
        return this.searchBarMaxHeight;
    }

    private boolean isGridAtTop() {
        if (this.gridView.getFirstVisiblePosition() != 0) {
            return false;
        }
        View firstChild = this.gridView.getChildAt(0);
        return firstChild == null || firstChild.getTop() >= this.gridView.getPaddingTop();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.destroyed) {
            return super.onInterceptTouchEvent(event);
        }
        if (this.searchRevealed) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    this.pullStartY = event.getY();
                    this.interceptingClose = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float closeDy = event.getY() - this.pullStartY;
                    int closeSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                    if (closeDy > closeSlop && isGridAtTop()) {
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
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.pullStartY = event.getY();
                this.interceptingPull = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - this.pullStartY;
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (dy > slop && isGridAtTop()) {
                    this.interceptingPull = true;
                    this.pullCurrentHeight = 0;
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                this.interceptingPull = false;
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.interceptingClose) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    return true;
                case MotionEvent.ACTION_UP:
                    float closeDy = event.getY() - this.pullStartY;
                    this.interceptingClose = false;
                    performClick();
                    if (closeDy >= closeDragThresholdPx()) {
                        Launcher launcher = getLauncher();
                        if (launcher != null) {
                            launcher.closeAllApplications();
                        }
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    this.interceptingClose = false;
                    return true;
            }
        }
        if (!this.interceptingPull) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - this.pullStartY;
                applyPullReveal(Math.max(0f, dy));
                return true;
            case MotionEvent.ACTION_UP:
                finishPullReveal();
                this.interceptingPull = false;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                snapSearchBarToHidden(this.pullCurrentHeight);
                this.interceptingPull = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void applyPullReveal(float dragDistance) {
        int maxH = getSearchBarMaxHeight();
        if (maxH == 0) {
            return;
        }
        int newHeight = Math.min((int) dragDistance, maxH);
        this.pullCurrentHeight = newHeight;
        if (newHeight > 0 && this.searchContainer.getVisibility() != VISIBLE) {
            this.searchContainer.setVisibility(VISIBLE);
        }
        ViewGroup.LayoutParams params = this.searchContainer.getLayoutParams();
        params.height = newHeight;
        this.searchContainer.setLayoutParams(params);
    }

    private void finishPullReveal() {
        int maxH = getSearchBarMaxHeight();
        if (this.pullCurrentHeight > maxH / 2) {
            snapSearchBarToRevealed(this.pullCurrentHeight);
        } else {
            snapSearchBarToHidden(this.pullCurrentHeight);
        }
    }

    private void snapSearchBarToRevealed(int fromHeight) {
        int maxH = getSearchBarMaxHeight();
        ValueAnimator anim = ValueAnimator.ofInt(fromHeight, maxH);
        anim.setDuration(SEARCH_REVEAL_ANIM_DURATION_MS);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                ViewGroup.LayoutParams p = searchContainer.getLayoutParams();
                p.height = (int) va.getAnimatedValue();
                searchContainer.setLayoutParams(p);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ViewGroup.LayoutParams p = searchContainer.getLayoutParams();
                p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                searchContainer.setLayoutParams(p);
                searchContainer.setVisibility(VISIBLE);
                searchRevealed = true;
                searchInput.requestFocus();
            }
        });
        anim.start();
    }

    private void snapSearchBarToHidden(int fromHeight) {
        if (fromHeight == 0) {
            collapseSearchBar();
            return;
        }
        ValueAnimator anim = ValueAnimator.ofInt(fromHeight, 0);
        anim.setDuration(SEARCH_REVEAL_ANIM_DURATION_MS);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                ViewGroup.LayoutParams p = searchContainer.getLayoutParams();
                p.height = (int) va.getAnimatedValue();
                searchContainer.setLayoutParams(p);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                collapseSearchBar();
            }
        });
        anim.start();
    }

    private int closeDragThresholdPx() {
        return (int) (CLOSE_DRAG_THRESHOLD_DP * getResources().getDisplayMetrics().density);
    }

    @Override
    public void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos) {
        if (this.destroyed) {
            return;
        }
        DrawerScrollState restoreState = DrawerSearchFilter.isEmptyQuery(this.query)
                && !PreferencesUtil.rememberApplicationsPosition(getContext())
                ? DrawerScrollState.empty() : captureScrollState();
        this.sourceItems = applicationItemInfos == null
                ? new ArrayList<ApplicationItemInfo>()
                : new ArrayList<>(applicationItemInfos);
        submitFilteredItems(restoreState);
    }

    @Override
    public void open(boolean animated) {
        this.closing = false;
        this.closeGeneration++;
        this.fastScroll.clearSelection();
        hideFastScrollImmediately();
        if (this.searchRevealed && DrawerSearchFilter.isEmptyQuery(this.query)) {
            collapseSearchBar();
        }
        setDrawerControlsEnabled(this.searchControlsEnabled);
        resetVisualState();
        this.gridView.prepareOpen();
        setVisibility(VISIBLE);
        if (animated) {
            startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.apps_scale_in));
        }
        invalidate();
    }

    @Override
    public boolean close(boolean animated) {
        if (!this.gridView.prepareClose()) {
            return false;
        }
        this.closing = true;
        this.fastScroll.clearSelection();
        hideFastScrollImmediately();
        setDrawerControlsEnabled(false);
        int generation = ++this.closeGeneration;
        clearQueryForClose();
        hideKeyboard();
        resetVisualState();
        if (!animated) {
            finishClose(generation);
            return true;
        }
        startAnimation(createCloseAnimation(generation));
        return true;
    }

    private void clearQueryForClose() {
        if (!DrawerSearchFilter.isEmptyQuery(this.query)) {
            this.searchInput.setText("");
        }
        this.searchInput.clearFocus();
        collapseSearchBar();
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    private void finishClose(int generation) {
        if (generation != this.closeGeneration) {
            return;
        }
        setVisibility(INVISIBLE);
        this.gridView.finishClose();
        resetVisualState();
    }

    private Animation createCloseAnimation(final int generation) {
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
                finishClose(generation);
            }
        });
        return animation;
    }

    private void resetVisualState() {
        clearAnimation();
        setAlpha(1.0f);
        setScaleX(1.0f);
        setScaleY(1.0f);
        setTranslationX(0.0f);
        setTranslationY(0.0f);
    }

    @Override
    public void setLoading() {
        setSearchEnabled(false);
        this.gridView.setLoading();
    }

    @Override
    public void setEmpty() {
        setSearchEnabled(false);
        this.gridView.setEmpty();
    }

    @Override
    public void setError() {
        setSearchEnabled(false);
        this.gridView.setError();
    }

    @Override
    public void clearState() {
        setSearchEnabled(true);
        this.gridView.clearState();
    }

    private void setSearchEnabled(boolean enabled) {
        this.searchControlsEnabled = enabled;
        if (!this.closing) {
            setDrawerControlsEnabled(enabled);
        }
    }

    private void setDrawerControlsEnabled(boolean enabled) {
        this.searchInput.setEnabled(enabled);
        this.clearSearch.setEnabled(enabled);
        this.fastScroll.setEnabled(enabled);
    }

    @Override
    public void onDestroy() {
        this.destroyed = true;
        this.submissionGeneration++;
        this.closeGeneration++;
        if (this.fastScrollHandler != null) {
            this.fastScrollHandler.removeCallbacks(this.fastScrollHideRunnable);
        }
        this.gridView.onDestroy();
        this.fastScroll.setOnSectionSelectedListener(null);
        this.sourceItems.clear();
        this.displayedItems.clear();
    }

    @Override
    public void setBackgroundAlpha(int alpha) {
        this.backgroundAlpha = alpha;
        int surface = WallpaperColorExtractor.getSurface(getContext());
        int background = Color.argb(alpha, Color.red(surface),
                Color.green(surface), Color.blue(surface));
        setBackgroundColor(background);
        invalidate();
    }

    @Override
    public void refreshPalette() {
        if (this.destroyed) {
            return;
        }
        setBackgroundAlpha(this.backgroundAlpha);
        this.gridView.refreshPalette();
        this.fastScroll.refreshPalette();
        int onSurface = WallpaperColorExtractor.getOnSurface(getContext());
        this.searchInput.setTextColor(onSurface);
        this.searchInput.setHintTextColor(
                WallpaperColorExtractor.getOutline(getContext()));
        this.noResults.setTextColor(onSurface);
        this.clearSearch.setColorFilter(onSurface);
        this.searchInput.setBackground(createSearchBackground());
    }

    private GradientDrawable createSearchBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(WallpaperColorExtractor.getSurfaceVariant(getContext()));
        background.setCornerRadius(getResources().getDimension(
                R.dimen.shape_corner_extra_large));
        int strokeWidth = Math.max(1,
                Math.round(getResources().getDisplayMetrics().density));
        background.setStroke(strokeWidth,
                WallpaperColorExtractor.getOutline(getContext()));
        return background;
    }

    @Override
    public View getImplementingView() {
        return this;
    }

    ApplicationsGridView getGridView() {
        return this.gridView;
    }

    DrawerFastScrollView getFastScrollView() {
        return this.fastScroll;
    }

    @Override
    public Launcher getLauncher() {
        return this.gridView.getLauncher();
    }

    @Override
    public void setDragController(DragController dragController) {
        this.gridView.setDragController(dragController);
    }

    @Override
    public void setLauncher(Launcher launcher) {
        this.gridView.setLauncher(launcher);
    }

    @Override
    public int getMode() {
        return this.gridView.getMode();
    }

    @Override
    public void setMode(int mode) {
        this.gridView.setMode(mode);
    }

    @Override
    public void setNumColumns(int columns) {
        this.gridView.setNumColumns(columns);
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
        this.gridView.setSystemGestureInsets(insets);
    }
}
