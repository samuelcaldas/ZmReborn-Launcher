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
        boolean uninstalling = true;
        ArrayList<ApplicationItemInfo> applicationItemInfos = this.mApplicationItemInfos;
        if (applicationItemInfos != null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            LinkedHashMap<Integer, List<ApplicationItemInfo>> pageContents = loadPageContents(sRows, sColumns, applicationItemInfos);
            if (this.mMode != 1) {
                uninstalling = false;
            }
            ArrayList<View> pageViews = new ArrayList<>();
            for (Integer intValue : pageContents.keySet()) {
                int page = intValue.intValue();
                ApplicationsPageView applicationsPageView = (ApplicationsPageView) layoutInflater.inflate(R.layout.apps_page_view, (ViewGroup) null);
                applicationsPageView.populatePage(uninstalling, sRows, sColumns, pageContents.get(Integer.valueOf(page)), this, this);
                pageViews.add(applicationsPageView);
            }
            this.mViewPager.clearPagingViews();
            this.mViewPager.setPagingViews(pageViews);
            System.gc();
        }
    }

    private static LinkedHashMap<Integer, List<ApplicationItemInfo>> loadPageContents(int rows, int columns, ArrayList<ApplicationItemInfo> applicationItemInfos) {
        LinkedHashMap<Integer, List<ApplicationItemInfo>> pageContents = new LinkedHashMap<>();
        int itemsPerPage = rows * columns;
        double pageCountDouble = ((double) (applicationItemInfos.size() + 1)) / ((double) itemsPerPage);
        if (pageCountDouble != ((double) Math.round(pageCountDouble))) {
            pageCountDouble = Math.floor(pageCountDouble) + 1.0d;
        }
        int pageCount = (int) pageCountDouble;
        for (int p = 0; p < pageCount; p++) {
            int start = itemsPerPage * p;
            int end = start + itemsPerPage;
            int applicationCount = applicationItemInfos.size();
            if (end >= applicationCount) {
                end = applicationCount;
            }
            List<ApplicationItemInfo> pageList = applicationItemInfos.subList(start, end);
            if (pageList.size() > 0) {
                pageContents.put(Integer.valueOf(p + 1), pageList);
            }
        }
        return pageContents;
    }

    public void onDestroy() {
        this.mApplicationItemInfos = null;
    }

    private void initIndicator() {
        if (this.mScreenIndicator != null) {
            this.mScreenIndicator.setItems(this.mViewPager.getPageCount());
            this.mScreenIndicator.setType(1);
            this.mScreenIndicator.setAutoHide(false);
            if (PreferencesUtil.rememberApplicationsPosition(getContext())) {
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
        this.mDragController.startDrag(view, this, (ApplicationItemInfo) view.getTag(), 1);
        this.mLauncher.closeAllApplications();
        return true;
    }

    public void onClick(View view) {
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) view.getTag();
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
        this.mScreenIndicator.indicate(((float) this.mViewPager.getScrollX()) / ((float) (this.mViewPager.getPageCount() * getWidth())));
    }
}
