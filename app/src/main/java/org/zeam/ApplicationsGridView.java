package org.zeam;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import java.util.ArrayList;

public class ApplicationsGridView extends GridView implements ApplicationsView, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DragSource {
    private Animation.AnimationListener mAnimationListener;
    private DragController mDragController;
    private Launcher mLauncher;
    public int mMode;
    private boolean mResetMode;
    /* access modifiers changed from: private */
    public boolean mScrollFromListener;

    public ApplicationsGridView(Context context) {
        super(context);
        this.mMode = 0;
        this.mScrollFromListener = false;
    }

    public ApplicationsGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842865);
    }

    public ApplicationsGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mMode = 0;
        this.mScrollFromListener = false;
        this.mResetMode = true;
        setSelector(SelectorDrawable.createSelector(context, true));
        setTextFilterEnabled(false);
        setScrollingCacheEnabled(true);
        setDrawingCacheEnabled(true);
        setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView listView, int state) {
                if (state == 2) {
                    ApplicationsGridView.this.mScrollFromListener = true;
                } else if (state != 0) {
                } else {
                    if (ApplicationsGridView.this.mScrollFromListener) {
                        ApplicationsGridView.this.mScrollFromListener = false;
                        return;
                    }
                    ApplicationsGridView.this.mScrollFromListener = true;
                    ApplicationsGridView.this.smoothScrollBy(0, 0);
                }
            }

            public void onScroll(AbsListView listView, int a, int b, int c) {
            }
        });
        this.mAnimationListener = new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                ApplicationsGridView.this.setDrawingCacheEnabled(false);
                ApplicationsGridView.this.postDelayed(new Runnable() {
                    public void run() {
                        ApplicationsGridView.this.setDrawingCacheEnabled(true);
                    }
                }, 1);
            }
        };
    }

    public void setBackgroundAlpha(int alpha) {
        int i = 0;
        setBackgroundColor(Color.argb(alpha, 18, 26, 33));
        if (alpha == 255) {
            i = -15590879; // #ff121a21 (zeam_slate)
        }
        setCacheColorHint(i);
        invalidate();
    }

    public void setMode(int mode) {
        if (this.mMode == mode) {
            setMode(0);
            return;
        }
        Context context = getContext();
        ApplicationsAdapter applicationsAdapter = (ApplicationsAdapter) getAdapter();
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
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
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
        if (this.mMode != 0 || !view.isInTouchMode()) {
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
        Context context = getContext();
        if (!PreferencesUtil.rememberApplicationsPosition(context)) {
            setSelection(0);
        }
        if (animated) {
            Animation inAnimation = AnimationUtils.loadAnimation(context, R.anim.apps_scale_in);
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
        invalidate();
        return true;
    }

    public void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos) {
        setAdapter(new ApplicationsAdapter(getContext(), applicationItemInfos));
    }

    public void onDestroy() {
        clearTextFilter();
        setAdapter((ListAdapter) null);
    }

    public View getImplementingView() {
        return this;
    }

    public Launcher getLauncher() {
        return this.mLauncher;
    }
}
