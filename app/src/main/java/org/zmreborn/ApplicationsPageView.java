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

/**
 * View displaying a single grid page of application shortcuts within the horizontal paging drawer.
 */
public class ApplicationsPageView extends ViewGroup {
    private final LayoutInflater layoutInflater;
    private List<ApplicationItemInfo> applicationItemInfos;
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;
    private boolean uninstalling;
    private int renderedHeight = -1;
    private int renderedWidth = -1;
    private DrawerLayoutMetrics metrics;

    /**
     * Creates an applications page view with context and XML attributes.
     */
    public ApplicationsPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.layoutInflater = LayoutInflater.from(context);
    }

    /**
     * Creates an applications page view with context.
     */
    public ApplicationsPageView(Context context) {
        super(context);
        this.layoutInflater = LayoutInflater.from(context);
    }

    void populatePage(boolean uninstalling, int rows, int columns,
            List<ApplicationItemInfo> applicationItemInfos,
            View.OnLongClickListener onLongClickListener,
            View.OnClickListener onClickListener) {
        this.uninstalling = uninstalling;
        this.applicationItemInfos = applicationItemInfos;
        this.onLongClickListener = onLongClickListener;
        this.onClickListener = onClickListener;
        this.renderedWidth = -1;
        this.renderedHeight = -1;
        this.metrics = null;
        setTag(new PageConfiguration(rows, columns));
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        PageConfiguration configuration = (PageConfiguration) getTag();
        if (configuration != null && measuredWidth > 0 && measuredHeight > 0
                && (measuredWidth != this.renderedWidth || measuredHeight != this.renderedHeight)) {
            rebuildPage(configuration, measuredWidth, measuredHeight);
        }
        measureSlottedChildren();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.metrics == null) {
            return;
        }
        int cols = this.metrics.getColumns();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int row = i / cols;
            int col = i % cols;
            child.layout(
                    paddingLeft + this.metrics.columnLeft(col),
                    paddingTop + this.metrics.rowTop(row),
                    paddingLeft + this.metrics.columnRight(col),
                    paddingTop + this.metrics.rowBottom(row));
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
        this.renderedWidth = -1;
        this.renderedHeight = -1;
        this.metrics = null;
        requestLayout();
    }

    private void rebuildPage(PageConfiguration configuration, int measuredWidth, int measuredHeight) {
        removeAllViews();
        int minimumCellWidth = getResources().getDimensionPixelSize(R.dimen.drawer_cell_min_width);
        int minimumCellHeight = getResources().getDimensionPixelSize(R.dimen.drawer_cell_min_height);
        this.metrics = DrawerLayoutMetrics.calculate(measuredWidth, measuredHeight,
                configuration.rows, configuration.columns,
                getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom(),
                minimumCellWidth, minimumCellHeight);
        int capacity = this.metrics.getRows() * this.metrics.getColumns();
        for (int itemIndex = 0; hasItem(itemIndex) && itemIndex < capacity; itemIndex++) {
            addView(createApplicationView(itemIndex));
        }
        this.renderedWidth = measuredWidth;
        this.renderedHeight = measuredHeight;
    }

    private void measureSlottedChildren() {
        if (this.metrics == null) {
            return;
        }
        int cols = this.metrics.getColumns();
        for (int i = 0; i < getChildCount(); i++) {
            int row = i / cols;
            int col = i % cols;
            int slotWidth = this.metrics.columnRight(col) - this.metrics.columnLeft(col);
            int slotHeight = this.metrics.rowBottom(row) - this.metrics.rowTop(row);
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(slotWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(slotHeight, MeasureSpec.EXACTLY));
        }
    }

    private TextView createApplicationView(int itemIndex) {
        ApplicationItemInfo applicationItemInfo = this.applicationItemInfos.get(itemIndex);
        TextView textView = (TextView) this.layoutInflater.inflate(
                R.layout.application_boxed_page, this, false);
        Drawable iconDrawable = resolveIcon(applicationItemInfo);
        applyItemStyling(textView, applicationItemInfo, iconDrawable);
        textView.setOnLongClickListener(this.onLongClickListener);
        textView.setOnClickListener(this.onClickListener);
        textView.setText(applicationItemInfo.title);
        textView.setContentDescription(applicationItemInfo.title);
        textView.setTag(applicationItemInfo);
        return textView;
    }

    private void applyItemStyling(TextView textView, ApplicationItemInfo item, Drawable iconDrawable) {
        Context context = getContext();
        if (item instanceof AppListFolderInfo) {
            textView.setTextColor(WallpaperColorExtractor.getOnSurface(context));
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
            textView.setCompoundDrawablesWithIntrinsicBounds(null, iconDrawable, null, null);
            return;
        }
        if (this.uninstalling) {
            int textColor = Utilities.canUninstallApplication(context, item)
                    ? WallpaperColorExtractor.getOnSurface(context)
                    : WallpaperColorExtractor.getOutline(context);
            textView.setTextColor(textColor);
            textView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            textView.setTextColor(WallpaperColorExtractor.getOnSurface(context));
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        }
        Utilities.setCompoundApplicationIcon(textView, iconDrawable, context);
    }

    private Drawable resolveIcon(ApplicationItemInfo applicationItemInfo) {
        if (applicationItemInfo instanceof AppListFolderInfo) {
            return getContext().getDrawable(R.drawable.ic_launcher_folder);
        }
        applicationItemInfo.icon = Utilities.normalizeApplicationIcon(
                applicationItemInfo.icon, getContext());
        applicationItemInfo.filtered = true;
        if (!this.uninstalling) {
            return applicationItemInfo.icon;
        }
        if (Utilities.canUninstallApplication(getContext(), applicationItemInfo)) {
            return Utilities.overlayUninstallIcon(getContext(), applicationItemInfo.icon);
        }
        return Utilities.adjustIconOpacity(applicationItemInfo.icon);
    }

    private boolean hasItem(int itemIndex) {
        return this.applicationItemInfos != null && itemIndex < this.applicationItemInfos.size();
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
