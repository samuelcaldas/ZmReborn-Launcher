package org.zmreborn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import org.zmreborn.theme.WallpaperColorExtractor;

public class ApplicationsDrawerView extends LinearLayout implements ApplicationsView {
    private ApplicationsGridView mGridView;
    private EditText mSearchInput;
    private ImageButton mClearSearch;
    private TextView mNoResults;
    private DrawerFastScrollView mFastScroll;
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

    public ApplicationsDrawerView(Context context) {
        super(context);
    }

    public ApplicationsDrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

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
    }

    private void bindViews() {
        this.mGridView = (ApplicationsGridView) findViewById(R.id.apps_grid_content);
        this.mSearchInput = (EditText) findViewById(R.id.drawer_search_input);
        this.mClearSearch = (ImageButton) findViewById(R.id.drawer_search_clear);
        this.mNoResults = (TextView) findViewById(R.id.drawer_search_empty);
        this.mFastScroll = (DrawerFastScrollView) findViewById(R.id.drawer_fast_scroll);
        if (this.mGridView == null || this.mSearchInput == null
                || this.mClearSearch == null || this.mNoResults == null
                || this.mFastScroll == null) {
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

    private void updateFastScroll() {
        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(this.mDisplayedItems);
        boolean showFastScroll = DrawerSearchFilter.isEmptyQuery(this.mQuery)
                && index.hasMultipleSections();
        this.mFastScroll.setIndex(index);
        this.mFastScroll.setVisibility(showFastScroll ? VISIBLE : GONE);
        this.mGridView.setFastScrollVisible(showFastScroll);
        updateFastScrollFocus(showFastScroll);
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
        boolean fastScrollVisible = this.mFastScroll.getVisibility() == VISIBLE;
        this.mGridView.setFastScrollVisible(fastScrollVisible);
        updateFastScrollFocus(fastScrollVisible);
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
    }

    private void clearSearch() {
        this.mSearchInput.setText("");
        this.mSearchInput.requestFocus();
    }

    private void applyDensity() {
        this.mGridView.setPreferredColumnWidth(
                DrawerDensityPolicy.getPreferredColumnWidth(getContext()));
    }

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
