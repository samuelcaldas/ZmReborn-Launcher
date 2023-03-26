package org.zeam;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class ApplicationsAdapter extends ArrayAdapter<ApplicationItemInfo> {
    private static boolean sUninstalling = false;
    private final LayoutInflater mLayoutInflater;

    public ApplicationsAdapter(Context context, ArrayList<ApplicationItemInfo> applicationItemInfos) {
        super(context, 0, applicationItemInfos);
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        Context context = getContext();
        ApplicationItemInfo applicationItemInfo = (ApplicationItemInfo) getItem(position);
        if (convertView == null) {
            convertView = this.mLayoutInflater.inflate(C0041R.layout.application_boxed_grid, parent, false);
            textView = (TextView) convertView;
            textView.setDrawingCacheQuality(524288);
        } else {
            textView = (TextView) convertView;
        }
        if (!sUninstalling) {
            textView.setTextColor(-1);
            textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, applicationItemInfo.icon, (Drawable) null, (Drawable) null);
        } else if (Utilities.canUninstallApplication(context, applicationItemInfo)) {
            textView.setTextColor(-1);
            textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, Utilities.overlayUninstallIcon(context, applicationItemInfo.icon), (Drawable) null, (Drawable) null);
        } else {
            textView.setTextColor(-7829368);
            textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, Utilities.adjustIconOpacity(applicationItemInfo.icon), (Drawable) null, (Drawable) null);
        }
        textView.setText(applicationItemInfo.title);
        return convertView;
    }

    /* access modifiers changed from: package-private */
    public void setUninstalling(boolean uninstalling) {
        sUninstalling = uninstalling;
    }
}
