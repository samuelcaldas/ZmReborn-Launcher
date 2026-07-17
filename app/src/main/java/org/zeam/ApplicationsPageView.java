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
        Drawable iconDrawable;
        Context context = getContext();
        this.mLayoutInflater = LayoutInflater.from(context);
        Rect rect = new Rect();
        getWindowVisibleDisplayFrame(rect);
        int height = rect.height();
        int rowHeight = (int) (((double) (height / rows)) * 0.83d);
        int widthMinusPadding = (rect.width() - getPaddingRight()) - getPaddingLeft();
        int paddingLeft = getPaddingLeft();
        setPadding(paddingLeft, (int) (((double) (height - (rowHeight * rows))) / 3.2d), getPaddingRight(), getPaddingBottom());
        int index = 0;
        for (int r = 0; r < rows; r++) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
            linearLayout.setOrientation(0);
            linearLayout.setMinimumWidth(widthMinusPadding);
            linearLayout.setMinimumHeight(rowHeight);
            for (int c = 0; c < columns && index < applicationItemInfos.size(); c++) {
                ApplicationItemInfo applicationItemInfo = applicationItemInfos.get(index);
                TextView textView = (TextView) this.mLayoutInflater.inflate(C0041R.layout.application_boxed_page, (ViewGroup) null, false);
                if (!applicationItemInfo.filtered) {
                    applicationItemInfo.icon = Utilities.createIconThumbnail(applicationItemInfo.icon, context);
                    applicationItemInfo.filtered = true;
                }
                if (uninstalling) {
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
                textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, iconDrawable, (Drawable) null, (Drawable) null);
                textView.setOnLongClickListener(onLongClickListener);
                textView.setWidth(widthMinusPadding / columns);
                textView.setOnClickListener(onClickListener);
                textView.setText(applicationItemInfo.title);
                textView.setTag(applicationItemInfo);
                textView.setDrawingCacheQuality(524288);
                linearLayout.addView(textView);
                index++;
            }
            addView(linearLayout, new LinearLayout.LayoutParams(-1, -1));
        }
    }
}
