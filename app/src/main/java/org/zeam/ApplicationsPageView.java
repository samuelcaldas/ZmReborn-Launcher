package org.zeam;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class ApplicationsPageView extends LinearLayout {
    private LayoutInflater mLayoutInflater;

    public ApplicationsPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ApplicationsPageView(Context context) {
        super(context);
    }

    /* access modifiers changed from: package-private */
    public void populatePage(boolean uninstalling, int rows, int columns, List<ApplicationItemInfo> applicationItemInfos, View.OnLongClickListener onLongClickListener, View.OnClickListener onClickListener) {
        Context context = getContext();
        this.mLayoutInflater = LayoutInflater.from(context);
        removeAllViews();
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if (measuredWidth <= 0 || measuredHeight <= 0) {
            measuredWidth = context.getResources().getDisplayMetrics().widthPixels;
            measuredHeight = context.getResources().getDisplayMetrics().heightPixels;
        }
        DrawerLayoutMetrics metrics = DrawerLayoutMetrics.calculate(measuredWidth, measuredHeight,
                rows, columns, getPaddingLeft() + getPaddingRight(),
                getPaddingTop() + getPaddingBottom(), 48, 48);
        int index = 0;
        for (int row = 0; row < metrics.getRows(); row++) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(0);
            rowLayout.setMinimumWidth(metrics.getAvailableWidth());
            rowLayout.setMinimumHeight(metrics.getCellHeight());
            addView(rowLayout, new LinearLayout.LayoutParams(-1, metrics.getCellHeight()));
            for (int column = 0; column < metrics.getColumns() && index < applicationItemInfos.size(); column++) {
                ApplicationItemInfo applicationItemInfo = applicationItemInfos.get(index);
                TextView textView = createApplicationTile(uninstalling, applicationItemInfo,
                        onLongClickListener, onClickListener);
                rowLayout.addView(textView, new LinearLayout.LayoutParams(metrics.getCellWidth(),
                        metrics.getCellHeight()));
                index++;
            }
        }
    }

    private TextView createApplicationTile(boolean uninstalling, ApplicationItemInfo applicationItemInfo,
            View.OnLongClickListener onLongClickListener, View.OnClickListener onClickListener) {
        Context context = getContext();
        Drawable iconDrawable = null;
        TextView textView = (TextView) this.mLayoutInflater.inflate(R.layout.application_boxed_page,
                (ViewGroup) null, false);
        if (applicationItemInfo instanceof AppListFolderInfo) {
            textView.setTextColor(-1);
            iconDrawable = context.getResources().getDrawable(R.drawable.ic_launcher_folder);
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        } else if (!applicationItemInfo.filtered) {
            applicationItemInfo.icon = Utilities.createIconThumbnail(applicationItemInfo.icon, context);
            applicationItemInfo.filtered = true;
        }
        if (applicationItemInfo instanceof AppListFolderInfo) {
            // Folder tile keeps its ledger icon and remains outside uninstall mode.
        } else if (uninstalling) {
            if (Utilities.canUninstallApplication(context, applicationItemInfo)) {
                textView.setTextColor(-1);
                iconDrawable = Utilities.overlayUninstallIcon(context, applicationItemInfo.icon);
            } else {
                textView.setTextColor(-7829368);
                iconDrawable = Utilities.adjustIconOpacity(applicationItemInfo.icon);
            }
            textView.setBackgroundResource(17170445);
        } else {
            textView.setTextColor(-1);
            iconDrawable = applicationItemInfo.icon;
            textView.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        }
        textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, iconDrawable,
                (Drawable) null, (Drawable) null);
        textView.setOnLongClickListener(onLongClickListener);
        textView.setOnClickListener(onClickListener);
        textView.setText(applicationItemInfo.title);
        textView.setTag(applicationItemInfo);
        textView.setDrawingCacheQuality(524288);
        return textView;
    }
}
