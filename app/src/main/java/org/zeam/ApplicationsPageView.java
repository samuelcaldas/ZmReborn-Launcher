package org.zeam;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class ApplicationsPageView extends LinearLayout {
    private final LayoutInflater mLayoutInflater;
    private List<ApplicationItemInfo> mApplicationItemInfos;
    private View.OnClickListener mOnClickListener;
    private View.OnLongClickListener mOnLongClickListener;
    private boolean mUninstalling;
    private int mRenderedHeight = -1;
    private int mRenderedWidth = -1;

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
        setTag(new PageConfiguration(rows, columns));
        requestLayout();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        PageConfiguration configuration = (PageConfiguration) getTag();
        if (configuration != null && measuredWidth > 0 && measuredHeight > 0
                && (measuredWidth != this.mRenderedWidth || measuredHeight != this.mRenderedHeight)) {
            rebuildPage(configuration, measuredWidth, measuredHeight);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void rebuildPage(PageConfiguration configuration, int measuredWidth, int measuredHeight) {
        removeAllViews();
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(measuredWidth, measuredHeight,
                configuration.rows, configuration.columns,
                getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom(), 48, 48);
        int itemIndex = 0;
        for (int row = 0; row < metrics.getRows(); row++) {
            LinearLayout rowView = createRow(metrics.getCellHeight(), metrics.getAvailableWidth());
            for (int column = 0; column < metrics.getColumns() && hasItem(itemIndex); column++) {
                rowView.addView(createApplicationView(itemIndex, metrics.getCellWidth(), metrics.getCellHeight()),
                        new LinearLayout.LayoutParams(metrics.getCellWidth(), metrics.getCellHeight()));
                itemIndex++;
            }
            addView(rowView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, metrics.getCellHeight()));
        }
        this.mRenderedWidth = measuredWidth;
        this.mRenderedHeight = measuredHeight;
    }

    private LinearLayout createRow(int rowHeight, int contentWidth) {
        LinearLayout rowView = new LinearLayout(getContext());
        rowView.setOrientation(HORIZONTAL);
        rowView.setMinimumWidth(contentWidth);
        rowView.setMinimumHeight(rowHeight);
        rowView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));
        return rowView;
    }

    private TextView createApplicationView(int itemIndex, int cellWidth, int rowHeight) {
        ApplicationItemInfo applicationItemInfo = this.mApplicationItemInfos.get(itemIndex);
        TextView textView = (TextView) this.mLayoutInflater.inflate(
                R.layout.application_boxed_page, this, false);
        Drawable iconDrawable = resolveIcon(applicationItemInfo);
        if (applicationItemInfo instanceof AppListFolderInfo) {
            textView.setTextColor(-1);
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(getContext(), true));
        } else if (this.mUninstalling) {
            if (Utilities.canUninstallApplication(getContext(), applicationItemInfo)) {
                textView.setTextColor(-1);
                textView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                textView.setTextColor(-7829368);
                textView.setBackgroundColor(Color.TRANSPARENT);
            }
        } else {
            textView.setTextColor(-1);
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(getContext(), true));
        }
        textView.setCompoundDrawablesWithIntrinsicBounds(null, iconDrawable, null, null);
        textView.setOnLongClickListener(this.mOnLongClickListener);
        textView.setOnClickListener(this.mOnClickListener);
        textView.setText(applicationItemInfo.title);
        textView.setContentDescription(applicationItemInfo.title);
        textView.setTag(applicationItemInfo);
        textView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        textView.setLayoutParams(new LinearLayout.LayoutParams(cellWidth, rowHeight));
        return textView;
    }

    private Drawable resolveIcon(ApplicationItemInfo applicationItemInfo) {
        if (applicationItemInfo instanceof AppListFolderInfo) {
            return getContext().getResources().getDrawable(R.drawable.ic_launcher_folder);
        }
        if (!applicationItemInfo.filtered) {
            applicationItemInfo.icon = Utilities.createIconThumbnail(applicationItemInfo.icon, getContext());
            applicationItemInfo.filtered = true;
        }
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
