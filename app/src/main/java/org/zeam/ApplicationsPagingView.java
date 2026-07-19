package org.zeam;

import android.content.Context;
import android.graphics.Color;
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
import org.zeam.ViewPager;

public class ApplicationsPagingView extends FrameLayout implements ApplicationsView, View.OnClickListener, View.OnLongClickListener, DragSource {
    private static int sColumns;
    private static int sRows;
    private Animation.AnimationListener mAnimationListener;
    private ArrayList<ApplicationItemInfo> mApplicationItemInfos;
    private DragController mDragController;
    private Launcher mLauncher;
    public int mMode = 0;
    private boolean mResetMode;
    private ScreenIndicator mScreenIndicator;
    private ViewPager mViewPager;

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
        this.mResetMode = true;
        this.mViewPager = (ViewPager) findViewById(R.id.view_pager);
        this.mViewPager.setDrawingCacheEnabled(true);
        setDrawingCacheEnabled(true);
        this.mViewPager.setDrawingCacheQuality(524288);
        this.mViewPager.setOnPageScrollListener(new ViewPager.OnPageScrollListener() {
            public void onScroll() {
                ApplicationsPagingView.this.indicate();
            }
        });
        this.mScreenIndicator = (ScreenIndicator) findViewById(R.id.apps_paging_screen_indicator);
        this.mAnimationListener = new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                ApplicationsPagingView.this.setDrawingCacheEnabled(false);
                ApplicationsPagingView.this.postDelayed(new Runnable() {
                    public void run() {
                        ApplicationsPagingView.this.setDrawingCacheEnabled(true);
                    }
                }, 1);
            }
        };
    }

    public void setNumColumns(int columns) {
        sColumns = columns;
    }

    public void setNumRows(int rows) {
        sRows = rows;
    }

    public void setBackgroundAlpha(int alpha) {
        setBackgroundColor(Color.argb(alpha, 18, 26, 33));
        invalidate();
    }

    public void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos) {
        this.mApplicationItemInfos = applicationItemInfos;
        buildPages();
        initIndicator();
    }

    private void buildPages() {
        if (this.mViewPager == null) {
            return;
        }
        ArrayList<ApplicationItemInfo> applicationItemInfos = this.mApplicationItemInfos;
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
        clampCurrentPageIndex();
        System.gc();
    }

    private DrawerLayoutMetrics calculatePageMetrics() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
            height = getResources().getDisplayMetrics().heightPixels;
        }
        return DrawerLayoutMetrics.calculate(width, height, sRows, sColumns,
                getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom(), 48, 48);
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
            if (PreferencesUtil.rememberApplicationsPosition(getContext())) {
                clampCurrentPageIndex();
                this.mScreenIndicator.fullIndicate(this.mViewPager.getCurrentPageIndex());
                return;
            }
            this.mScreenIndicator.fullIndicate(0);
            this.mViewPager.resetScroll();
        }
    }

    public void open(boolean animated) {
        buildPages();
        initIndicator();
        if (animated) {
            Animation inAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.apps_scale_in);
            inAnimation.setAnimationListener(this.mAnimationListener);
            setAnimation(inAnimation);
        }
        setVisibility(0);
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
        if (animated) {
            setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.apps_scale_out));
        }
        setVisibility(4);
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
            setMode(0);
            return;
        }
        this.mMode = mode;
        buildPages();
    }

    public Launcher getLauncher() {
        return this.mLauncher;
    }

    public void onDropCompleted(View target, boolean success) {
    }

    public boolean onLongClick(View view) {
        if (this.mMode != 0 || !view.isInTouchMode()) {
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
        if (pageCount > 0 && getWidth() > 0) {
            this.mScreenIndicator.indicate(((float) this.mViewPager.getScrollX()) / ((float) (pageCount * getWidth())));
        }
    }

    private void clampCurrentPageIndex() {
        int pageCount = this.mViewPager.getPageCount();
        if (pageCount <= 0) {
            this.mViewPager.resetScroll();
            return;
        }
        int currentIndex = this.mViewPager.getCurrentPageIndex();
        if (currentIndex >= pageCount) {
            this.mViewPager.resetScroll();
        }
    }
}
