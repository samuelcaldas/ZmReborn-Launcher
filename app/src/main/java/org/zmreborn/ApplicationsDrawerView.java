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

/** Vertical app drawer: search bar, alphabet fast-scroll rail, and applications grid. */
public class ApplicationsDrawerView extends LinearLayout implements ApplicationsView {
    private static final int FAST_SCROLL_HIDE_DELAY_MS = 1000;
    private static final int FAST_SCROLL_ANIM_DURATION_MS = 150;
    private static final int SEARCH_REVEAL_ANIM_DURATION_MS = 150;
    private static final int CLOSE_DRAG_THRESHOLD_DP = 72;

    private ApplicationsGridView mGridView;
    private EditText mSearchInput;
    private ImageButton mClearSearch;
    private TextView mNoResults;
    private DrawerFastScrollView mFastScroll;
    private FrameLayout mSearchContainer;
    private ArrayList<ApplicationItemInfo> mSourceItems = new ArrayList<>();
    private ArrayList<ApplicationItemInfo> mDisplayedItems = new ArrayList<>();
    private DrawerScrollState mSearchOriginState = DrawerScrollState.empty();
    private String mQuery = "";
    private boolean mDestroyed;
    private boolean mClosing;
    private boolean mSearchControlsEnabled = true;
    private int mSubmissionGeneration;
    private int mCloseGeneration;
    private int mBackgroundAlpha = 255;
    private int mBasePaddingLeft;
    private int mBasePaddingTop;
    private int mBasePaddingRight;
    private int mBasePaddingBottom;

    // Fast-scroll auto-hide state (Fix 2)
    private boolean mFastScrollEnabled;
    private boolean mFastScrollVisible;
    private Handler mFastScrollHandler;
    private Runnable mFastScrollHideRunnable;

    // Pull-to-reveal search bar state (Fix 3)
    private int mSearchBarMaxHeight;
    private boolean mSearchRevealed;
    private float mPullStartY;
    private int mPullCurrentHeight;
    private boolean mInterceptingPull;
    private boolean mInterceptingClose;

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
        this.mGridView = (ApplicationsGridView) findViewById(R.id.apps_grid_content);
        this.mSearchInput = (EditText) findViewById(R.id.drawer_search_input);
        this.mClearSearch = (ImageButton) findViewById(R.id.drawer_search_clear);
        this.mNoResults = (TextView) findViewById(R.id.drawer_search_empty);
        this.mFastScroll = (DrawerFastScrollView) findViewById(R.id.drawer_fast_scroll);
        this.mSearchContainer = (FrameLayout) findViewById(R.id.drawer_search_container);
        if (this.mGridView == null || this.mSearchInput == null
                || this.mClearSearch == null || this.mNoResults == null
                || this.mFastScroll == null || this.mSearchContainer == null) {
            throw new IllegalStateException("Applications drawer layout is incomplete");
        }
    }

    private void captureBasePadding() {
        this.mBasePaddingLeft = getPaddingLeft();
        this.mBasePaddingTop = getPaddingTop();
        this.mBasePaddingRight = getPaddingRight();
        this.mBasePaddingBottom = getPaddingBottom();
    }

    private void bindSearch() {
        this.mSearchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence value, int start, int before, int count) {
                updateQuery(value);
            }

            public void afterTextChanged(Editable value) {
            }
        });
        this.mClearSearch.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clearSearch();
            }
        });
    }

    private void bindFastScroll() {
        this.mFastScroll.setOnSectionSelectedListener(
                new DrawerFastScrollView.OnSectionSelectedListener() {
            public void onSectionSelected(int position) {
                scrollToFastScrollPosition(position);
            }
        });
        // Fix 1 + Fix 2: clear selection and manage auto-hide on scroll state changes.
        this.mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                onGridScrollStateChanged(scrollState);
            }

            public void onScroll(AbsListView view, int first, int visible, int total) {
            }
        });
    }

    private void initFastScrollHandler() {
        this.mFastScrollHandler = new Handler(Looper.getMainLooper());
        this.mFastScrollHideRunnable = new Runnable() {
            public void run() {
                hideFastScrollAnimated();
            }
        };
    }

    /**
     * Handles grid scroll state: clears rail selection so repeat taps always fire (Fix 1),
     * shows the fast-scroll rail during motion and schedules auto-hide at idle (Fix 2).
     */
    private void onGridScrollStateChanged(int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            this.mFastScrollHandler.removeCallbacks(this.mFastScrollHideRunnable);
            this.mFastScroll.clearSelection();
            if (this.mFastScrollEnabled && !this.mFastScrollVisible) {
                this.mFastScrollVisible = true;
                this.mFastScroll.setVisibility(VISIBLE);
                this.mFastScroll.setAlpha(0.0f);
                this.mFastScroll.animate()
                        .alpha(1.0f)
                        .setDuration(FAST_SCROLL_ANIM_DURATION_MS)
                        .start();
            }
        } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            this.mFastScrollHandler.postDelayed(
                    this.mFastScrollHideRunnable, FAST_SCROLL_HIDE_DELAY_MS);
        }
    }

    private void hideFastScrollAnimated() {
        if (!this.mFastScrollVisible) {
            return;
        }
        this.mFastScrollVisible = false;
        this.mFastScroll.animate()
                .alpha(0.0f)
                .setDuration(FAST_SCROLL_ANIM_DURATION_MS)
                .withEndAction(new Runnable() {
                    public void run() {
                        if (!mFastScrollVisible) {
                            mFastScroll.setVisibility(GONE);
                        }
                    }
                })
                .start();
    }

    private void hideFastScrollImmediately() {
        if (this.mFastScrollHandler != null) {
            this.mFastScrollHandler.removeCallbacks(this.mFastScrollHideRunnable);
        }
        this.mFastScrollVisible = false;
        this.mFastScroll.animate().cancel();
        this.mFastScroll.setAlpha(1.0f);
        this.mFastScroll.setVisibility(GONE);
    }

    private void scrollToFastScrollPosition(int position) {
        if (this.mDestroyed || this.mClosing || position < 0) {
            return;
        }
        this.mGridView.setSelectionFromTop(
                position, this.mGridView.getPaddingTop());
    }

    private void updateQuery(CharSequence query) {
        if (this.mDestroyed) {
            return;
        }
        boolean wasEmpty = DrawerSearchFilter.isEmptyQuery(this.mQuery);
        boolean isEmpty = DrawerSearchFilter.isEmptyQuery(query);
        if (wasEmpty && !isEmpty) {
            this.mSearchOriginState = captureScrollState();
        }
        DrawerScrollState restoreState = isEmpty
                ? this.mSearchOriginState : captureScrollState();
        this.mQuery = query == null ? "" : query.toString();
        submitFilteredItems(restoreState);
        if (isEmpty) {
            this.mSearchOriginState = DrawerScrollState.empty();
        }
        updateSearchControls();
    }

    private void submitFilteredItems(DrawerScrollState restoreState) {
        this.mSubmissionGeneration++;
        this.mDisplayedItems = DrawerSearchFilter.filter(
                this.mSourceItems, this.mQuery);
        this.mGridView.setApplications(this.mDisplayedItems);
        updateNoResultsState();
        updateFastScroll();
        restoreScrollState(restoreState);
    }

    private DrawerScrollState captureScrollState() {
        if (this.mGridView.getAdapter() == null) {
            return DrawerScrollState.empty();
        }
        int count = this.mGridView.getAdapter().getCount();
        if (count == 0) {
            return DrawerScrollState.empty();
        }
        int position = this.mGridView.getFirstVisiblePosition();
        if (position < 0 || position >= count) {
            return DrawerScrollState.empty();
        }
        Object item = this.mGridView.getItemAtPosition(position);
        if (!(item instanceof ApplicationItemInfo)) {
            return DrawerScrollState.empty();
        }
        View firstChild = this.mGridView.getChildAt(0);
        int offset = firstChild == null ? 0
                : firstChild.getTop() - this.mGridView.getPaddingTop();
        return DrawerScrollState.capture(
                (ApplicationItemInfo) item, position, offset);
    }

    private void restoreScrollState(final DrawerScrollState state) {
        final int position = state.resolvePosition(this.mDisplayedItems);
        if (position < 0) {
            return;
        }
        final int generation = this.mSubmissionGeneration;
        this.mGridView.post(new Runnable() {
            public void run() {
                if (mDestroyed || generation != mSubmissionGeneration) {
                    return;
                }
                int top = mGridView.getPaddingTop() + state.getTopOffset();
                mGridView.setSelectionFromTop(position, top);
            }
        });
    }

    private void updateNoResultsState() {
        boolean noResults = !DrawerSearchFilter.isEmptyQuery(this.mQuery)
                && !this.mSourceItems.isEmpty() && this.mDisplayedItems.isEmpty();
        this.mGridView.setVisibility(noResults ? GONE : VISIBLE);
        this.mNoResults.setVisibility(noResults ? VISIBLE : GONE);
    }

    /** Updates fast-scroll availability; hides rail immediately when conditions no longer hold. */
    private void updateFastScroll() {
        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(this.mDisplayedItems);
        this.mFastScrollEnabled = DrawerSearchFilter.isEmptyQuery(this.mQuery)
                && index.hasMultipleSections();
        this.mFastScroll.setIndex(index);
        if (!this.mFastScrollEnabled) {
            hideFastScrollImmediately();
        }
        this.mGridView.setFastScrollVisible(this.mFastScrollEnabled);
        updateFastScrollFocus(this.mFastScrollEnabled);
    }

    private void updateFastScrollFocus(boolean visible) {
        int gridId = R.id.apps_grid_content;
        int railId = visible ? R.id.drawer_fast_scroll : gridId;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            this.mFastScroll.setNextFocusLeftId(railId);
            this.mFastScroll.setNextFocusRightId(gridId);
            return;
        }
        this.mFastScroll.setNextFocusLeftId(gridId);
        this.mFastScroll.setNextFocusRightId(railId);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (this.mGridView == null || this.mFastScroll == null) {
            return;
        }
        this.mGridView.setFastScrollVisible(this.mFastScrollEnabled);
        updateFastScrollFocus(this.mFastScrollEnabled);
        updateSearchFocus();
    }

    private void updateSearchFocus() {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            this.mClearSearch.setNextFocusLeftId(R.id.drawer_search_clear);
            this.mClearSearch.setNextFocusRightId(R.id.drawer_search_input);
            return;
        }
        this.mClearSearch.setNextFocusLeftId(R.id.drawer_search_input);
        this.mClearSearch.setNextFocusRightId(R.id.drawer_search_clear);
    }

    private void updateSearchControls() {
        boolean hasQuery = !DrawerSearchFilter.isEmptyQuery(this.mQuery);
        this.mClearSearch.setVisibility(hasQuery ? VISIBLE : GONE);
        if (hasQuery && !this.mSearchRevealed) {
            revealSearchBarImmediate();
        }
    }

    private void clearSearch() {
        this.mSearchInput.setText("");
        this.mSearchInput.requestFocus();
    }

    private void applyDensity() {
        this.mGridView.setPreferredColumnWidth(
                DrawerDensityPolicy.getPreferredColumnWidth(getContext()));
    }

    // --- Pull-to-reveal search bar (Fix 3) ---

    /** Collapses search bar to zero height and marks it as not revealed. */
    private void collapseSearchBar() {
        if (this.mSearchContainer == null) {
            return;
        }
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) this.mSearchContainer.getLayoutParams();
        params.height = 0;
        this.mSearchContainer.setLayoutParams(params);
        this.mSearchContainer.setVisibility(GONE);
        this.mSearchRevealed = false;
    }

    /** Shows search bar at full height without animation. */
    private void revealSearchBarImmediate() {
        if (this.mSearchContainer == null) {
            return;
        }
        ViewGroup.LayoutParams params = this.mSearchContainer.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        this.mSearchContainer.setLayoutParams(params);
        this.mSearchContainer.setVisibility(VISIBLE);
        this.mSearchRevealed = true;
    }

    /**
     * Returns the measured height of the search container, measuring on demand.
     * Safe to call only while the drawer is visible and has been laid out.
     */
    private int getSearchBarMaxHeight() {
        if (this.mSearchBarMaxHeight == 0 && this.mSearchContainer != null
                && getMeasuredWidth() > 0) {
            this.mSearchContainer.setVisibility(VISIBLE);
            this.mSearchContainer.measure(
                    MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            this.mSearchBarMaxHeight = this.mSearchContainer.getMeasuredHeight();
            this.mSearchContainer.setVisibility(GONE);
        }
        return this.mSearchBarMaxHeight;
    }

    private boolean isGridAtTop() {
        if (this.mGridView.getFirstVisiblePosition() != 0) {
            return false;
        }
        View firstChild = this.mGridView.getChildAt(0);
        return firstChild == null || firstChild.getTop() >= this.mGridView.getPaddingTop();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mDestroyed) {
            return super.onInterceptTouchEvent(event);
        }
        if (this.mSearchRevealed) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    this.mPullStartY = event.getY();
                    this.mInterceptingClose = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float closeDy = event.getY() - this.mPullStartY;
                    int closeSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                    if (closeDy > closeSlop && isGridAtTop()) {
                        this.mInterceptingClose = true;
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    this.mInterceptingClose = false;
                    break;
            }
            return super.onInterceptTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.mPullStartY = event.getY();
                this.mInterceptingPull = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - this.mPullStartY;
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (dy > slop && isGridAtTop()) {
                    this.mInterceptingPull = true;
                    this.mPullCurrentHeight = 0;
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                this.mInterceptingPull = false;
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mInterceptingClose) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    return true;
                case MotionEvent.ACTION_UP:
                    float closeDy = event.getY() - this.mPullStartY;
                    this.mInterceptingClose = false;
                    performClick();
                    if (closeDy >= closeDragThresholdPx()) {
                        Launcher launcher = getLauncher();
                        if (launcher != null) {
                            launcher.closeAllApplications();
                        }
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    this.mInterceptingClose = false;
                    return true;
            }
        }
        if (!this.mInterceptingPull) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - this.mPullStartY;
                applyPullReveal(Math.max(0f, dy));
                return true;
            case MotionEvent.ACTION_UP:
                finishPullReveal();
                this.mInterceptingPull = false;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                snapSearchBarToHidden(this.mPullCurrentHeight);
                this.mInterceptingPull = false;
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
        this.mPullCurrentHeight = newHeight;
        if (newHeight > 0 && this.mSearchContainer.getVisibility() != VISIBLE) {
            this.mSearchContainer.setVisibility(VISIBLE);
        }
        ViewGroup.LayoutParams params = this.mSearchContainer.getLayoutParams();
        params.height = newHeight;
        this.mSearchContainer.setLayoutParams(params);
    }

    private void finishPullReveal() {
        int maxH = getSearchBarMaxHeight();
        if (this.mPullCurrentHeight > maxH / 2) {
            snapSearchBarToRevealed(this.mPullCurrentHeight);
        } else {
            snapSearchBarToHidden(this.mPullCurrentHeight);
        }
    }

    private void snapSearchBarToRevealed(int fromHeight) {
        int maxH = getSearchBarMaxHeight();
        ValueAnimator anim = ValueAnimator.ofInt(fromHeight, maxH);
        anim.setDuration(SEARCH_REVEAL_ANIM_DURATION_MS);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator va) {
                ViewGroup.LayoutParams p = mSearchContainer.getLayoutParams();
                p.height = (int) va.getAnimatedValue();
                mSearchContainer.setLayoutParams(p);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ViewGroup.LayoutParams p = mSearchContainer.getLayoutParams();
                p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mSearchContainer.setLayoutParams(p);
                mSearchContainer.setVisibility(VISIBLE);
                mSearchRevealed = true;
                mSearchInput.requestFocus();
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
            public void onAnimationUpdate(ValueAnimator va) {
                ViewGroup.LayoutParams p = mSearchContainer.getLayoutParams();
                p.height = (int) va.getAnimatedValue();
                mSearchContainer.setLayoutParams(p);
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

    // --- Lifecycle ---

    @Override
    public void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos) {
        if (this.mDestroyed) {
            return;
        }
        DrawerScrollState restoreState = DrawerSearchFilter.isEmptyQuery(this.mQuery)
                && !PreferencesUtil.rememberApplicationsPosition(getContext())
                ? DrawerScrollState.empty() : captureScrollState();
        this.mSourceItems = applicationItemInfos == null
                ? new ArrayList<ApplicationItemInfo>()
                : new ArrayList<ApplicationItemInfo>(applicationItemInfos);
        submitFilteredItems(restoreState);
    }

    @Override
    public void open(boolean animated) {
        this.mClosing = false;
        this.mCloseGeneration++;
        this.mFastScroll.clearSelection();
        hideFastScrollImmediately();
        if (this.mSearchRevealed && DrawerSearchFilter.isEmptyQuery(this.mQuery)) {
            collapseSearchBar();
        }
        setDrawerControlsEnabled(this.mSearchControlsEnabled);
        resetVisualState();
        this.mGridView.prepareOpen();
        setVisibility(VISIBLE);
        if (animated) {
            startAnimation(AnimationUtils.loadAnimation(
                    getContext(), R.anim.apps_scale_in));
        }
        invalidate();
    }

    @Override
    public boolean close(boolean animated) {
        if (!this.mGridView.prepareClose()) {
            return false;
        }
        this.mClosing = true;
        this.mFastScroll.clearSelection();
        hideFastScrollImmediately();
        setDrawerControlsEnabled(false);
        int closeGeneration = ++this.mCloseGeneration;
        clearQueryForClose();
        hideKeyboard();
        resetVisualState();
        if (!animated) {
            finishClose(closeGeneration);
            return true;
        }
        startAnimation(createCloseAnimation(closeGeneration));
        return true;
    }

    private void clearQueryForClose() {
        if (!DrawerSearchFilter.isEmptyQuery(this.mQuery)) {
            this.mSearchInput.setText("");
        }
        this.mSearchInput.clearFocus();
        collapseSearchBar();
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    private void finishClose(int closeGeneration) {
        if (closeGeneration != this.mCloseGeneration) {
            return;
        }
        setVisibility(INVISIBLE);
        this.mGridView.finishClose();
        resetVisualState();
    }

    private Animation createCloseAnimation(final int closeGeneration) {
        Animation animation = AnimationUtils.loadAnimation(
                getContext(), R.anim.apps_scale_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation ignored) {
            }

            public void onAnimationRepeat(Animation ignored) {
            }

            public void onAnimationEnd(Animation ignored) {
                finishClose(closeGeneration);
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
        this.mGridView.setLoading();
    }

    @Override
    public void setEmpty() {
        setSearchEnabled(false);
        this.mGridView.setEmpty();
    }

    @Override
    public void setError() {
        setSearchEnabled(false);
        this.mGridView.setError();
    }

    @Override
    public void clearState() {
        setSearchEnabled(true);
        this.mGridView.clearState();
    }

    private void setSearchEnabled(boolean enabled) {
        this.mSearchControlsEnabled = enabled;
        if (!this.mClosing) {
            setDrawerControlsEnabled(enabled);
        }
    }

    private void setDrawerControlsEnabled(boolean enabled) {
        this.mSearchInput.setEnabled(enabled);
        this.mClearSearch.setEnabled(enabled);
        this.mFastScroll.setEnabled(enabled);
    }

    @Override
    public void onDestroy() {
        this.mDestroyed = true;
        this.mSubmissionGeneration++;
        this.mCloseGeneration++;
        if (this.mFastScrollHandler != null) {
            this.mFastScrollHandler.removeCallbacks(this.mFastScrollHideRunnable);
        }
        this.mGridView.onDestroy();
        this.mFastScroll.setOnSectionSelectedListener(null);
        this.mSourceItems.clear();
        this.mDisplayedItems.clear();
    }

    @Override
    public void setBackgroundAlpha(int alpha) {
        this.mBackgroundAlpha = alpha;
        int surface = WallpaperColorExtractor.getSurface(getContext());
        int background = Color.argb(alpha, Color.red(surface),
                Color.green(surface), Color.blue(surface));
        setBackgroundColor(background);
        invalidate();
    }

    @Override
    public void refreshPalette() {
        if (this.mDestroyed) {
            return;
        }
        setBackgroundAlpha(this.mBackgroundAlpha);
        this.mGridView.refreshPalette();
        this.mFastScroll.refreshPalette();
        int onSurface = WallpaperColorExtractor.getOnSurface(getContext());
        this.mSearchInput.setTextColor(onSurface);
        this.mSearchInput.setHintTextColor(
                WallpaperColorExtractor.getOutline(getContext()));
        this.mNoResults.setTextColor(onSurface);
        this.mClearSearch.setColorFilter(onSurface);
        this.mSearchInput.setBackground(createSearchBackground());
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
        return this.mGridView;
    }

    DrawerFastScrollView getFastScrollView() {
        return this.mFastScroll;
    }

    @Override
    public Launcher getLauncher() {
        return this.mGridView.getLauncher();
    }

    @Override
    public void setDragController(DragController dragController) {
        this.mGridView.setDragController(dragController);
    }

    @Override
    public void setLauncher(Launcher launcher) {
        this.mGridView.setLauncher(launcher);
    }

    @Override
    public int getMode() {
        return this.mGridView.getMode();
    }

    @Override
    public void setMode(int mode) {
        this.mGridView.setMode(mode);
    }

    @Override
    public void setNumColumns(int columns) {
        this.mGridView.setNumColumns(columns);
    }

    @Override
    public void setSystemBarInsets(int left, int top, int right, int bottom) {
        setPadding(this.mBasePaddingLeft + Math.max(0, left),
                this.mBasePaddingTop + Math.max(0, top),
                this.mBasePaddingRight + Math.max(0, right),
                this.mBasePaddingBottom + Math.max(0, bottom));
        requestLayout();
    }

    @Override
    public void setSystemGestureInsets(Rect insets) {
        this.mGridView.setSystemGestureInsets(insets);
    }
}
