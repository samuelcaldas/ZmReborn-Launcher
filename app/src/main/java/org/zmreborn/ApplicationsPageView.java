package org.zmreborn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;
import org.zmreborn.theme.WallpaperColorExtractor;

public class ApplicationsPageView extends ViewGroup {
    private final LayoutInflater mLayoutInflater;
    private List<ApplicationItemInfo> mApplicationItemInfos;
    private View.OnClickListener mOnClickListener;
    private View.OnLongClickListener mOnLongClickListener;
    private boolean mUninstalling;
    private int mRenderedHeight = -1;
    private int mRenderedWidth = -1;
    private DrawerLayoutMetrics mMetrics;

    public ApplicationsPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    public ApplicationsPageView(Context context) {
        super(context);
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    void populatePage(boolean uninstalling, int rows, int columns,
            List<ApplicationItemInfo> applicationItemInfos,
            View.OnLongClickListener onLongClickListener,
            View.OnClickListener onClickListener) {
        this.mUninstalling = uninstalling;
        this.mApplicationItemInfos = applicationItemInfos;
        this.mOnLongClickListener = onLongClickListener;
        this.mOnClickListener = onClickListener;
        this.mRenderedWidth = -1;
        this.mRenderedHeight = -1;
        this.mMetrics = null;
        setTag(new PageConfiguration(rows, columns));
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        PageConfiguration configuration = (PageConfiguration) getTag();
        if (configuration != null && measuredWidth > 0 && measuredHeight > 0
                && (measuredWidth != this.mRenderedWidth || measuredHeight != this.mRenderedHeight)) {
            rebuildPage(configuration, measuredWidth, measuredHeight);
        }
        measureSlottedChildren();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mMetrics == null) {
            return;
        }
        int cols = this.mMetrics.getColumns();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int row = i / cols;
            int col = i % cols;
            child.layout(
                    paddingLeft + this.mMetrics.columnLeft(col),
                    paddingTop + this.mMetrics.rowTop(row),
                    paddingLeft + this.mMetrics.columnRight(col),
                    paddingTop + this.mMetrics.rowBottom(row));
        }
    }

    void refreshPalette() {
        PageConfiguration configuration = (PageConfiguration) getTag();
        if (configuration == null) {
            return;
        }
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if (measuredWidth > 0 && measuredHeight > 0) {
            rebuildPage(configuration, measuredWidth, measuredHeight);
            requestLayout();
            return;
        }
        this.mRenderedWidth = -1;
        this.mRenderedHeight = -1;
        this.mMetrics = null;
        requestLayout();
    }

    private void rebuildPage(PageConfiguration configuration, int measuredWidth, int measuredHeight) {
        removeAllViews();
        int minimumCellWidth = getResources().getDimensionPixelSize(R.dimen.drawer_cell_min_width);
        int minimumCellHeight = getResources().getDimensionPixelSize(R.dimen.drawer_cell_min_height);
        this.mMetrics = DrawerLayoutMetrics.calculate(measuredWidth, measuredHeight,
                configuration.rows, configuration.columns,
                getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom(),
                minimumCellWidth, minimumCellHeight);
        int capacity = this.mMetrics.getRows() * this.mMetrics.getColumns();
        for (int itemIndex = 0; hasItem(itemIndex) && itemIndex < capacity; itemIndex++) {
            addView(createApplicationView(itemIndex));
        }
        this.mRenderedWidth = measuredWidth;
        this.mRenderedHeight = measuredHeight;
    }

    private void measureSlottedChildren() {
        if (this.mMetrics == null) {
            return;
        }
        int cols = this.mMetrics.getColumns();
        for (int i = 0; i < getChildCount(); i++) {
            int row = i / cols;
            int col = i % cols;
            int slotWidth = this.mMetrics.columnRight(col) - this.mMetrics.columnLeft(col);
            int slotHeight = this.mMetrics.rowBottom(row) - this.mMetrics.rowTop(row);
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(slotWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(slotHeight, MeasureSpec.EXACTLY));
        }
    }

    private TextView createApplicationView(int itemIndex) {
        ApplicationItemInfo applicationItemInfo = this.mApplicationItemInfos.get(itemIndex);
        TextView textView = (TextView) this.mLayoutInflater.inflate(
                R.layout.application_boxed_page, this, false);
        Drawable iconDrawable = resolveIcon(applicationItemInfo);
        if (applicationItemInfo instanceof AppListFolderInfo) {
            textView.setTextColor(WallpaperColorExtractor.getOnSurface(getContext()));
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(getContext(), true));
        } else if (this.mUninstalling) {
            if (Utilities.canUninstallApplication(getContext(), applicationItemInfo)) {
                textView.setTextColor(WallpaperColorExtractor.getOnSurface(getContext()));
                textView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                textView.setTextColor(WallpaperColorExtractor.getOutline(getContext()));
                textView.setBackgroundColor(Color.TRANSPARENT);
            }
        } else {
            textView.setTextColor(WallpaperColorExtractor.getOnSurface(getContext()));
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(getContext(), true));
        }
        if (applicationItemInfo instanceof AppListFolderInfo) {
            textView.setCompoundDrawablesWithIntrinsicBounds(null, iconDrawable, null, null);
        } else {
            Utilities.setCompoundApplicationIcon(textView, iconDrawable, getContext());
        }
        textView.setOnLongClickListener(this.mOnLongClickListener);
        textView.setOnClickListener(this.mOnClickListener);
        textView.setText(applicationItemInfo.title);
        textView.setContentDescription(applicationItemInfo.title);
        textView.setTag(applicationItemInfo);
        return textView;
    }

    private Drawable resolveIcon(ApplicationItemInfo applicationItemInfo) {
        if (applicationItemInfo instanceof AppListFolderInfo) {
            return getContext().getResources().getDrawable(R.drawable.ic_launcher_folder);
        }
        applicationItemInfo.icon = Utilities.normalizeApplicationIcon(
                applicationItemInfo.icon, getContext());
        applicationItemInfo.filtered = true;
        if (!this.mUninstalling) {
            return applicationItemInfo.icon;
        }
        if (Utilities.canUninstallApplication(getContext(), applicationItemInfo)) {
            return Utilities.overlayUninstallIcon(getContext(), applicationItemInfo.icon);
        }
        return Utilities.adjustIconOpacity(applicationItemInfo.icon);
    }

    private boolean hasItem(int itemIndex) {
        return this.mApplicationItemInfos != null && itemIndex < this.mApplicationItemInfos.size();
    }

    private static final class PageConfiguration {
        private final int rows;
        private final int columns;

        private PageConfiguration(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
        }
    }
}
