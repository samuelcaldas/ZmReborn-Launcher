package org.zmreborn;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.zmreborn.theme.WallpaperColorExtractor;

/**
 * Adapter backing the all-applications grid view, managing items, stable multi-profile IDs, and uninstall state.
 */
public class ApplicationsAdapter extends ArrayAdapter<ApplicationItemInfo> {
    private boolean uninstalling;
    private final LayoutInflater layoutInflater;
    private final String profileToken;

    /**
     * Constructs an adapter with context and item list.
     *
     * @param context application or view context
     * @param applicationItemInfos list of applications to display
     */
    public ApplicationsAdapter(
            Context context,
            List<ApplicationItemInfo> applicationItemInfos) {
        super(context, 0, snapshot(applicationItemInfos));
        this.layoutInflater = LayoutInflater.from(context);
        this.profileToken = resolveProfileToken(context);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        ApplicationItemInfo item = getItem(position);
        if (item == null) {
            return position;
        }
        return stableId(this.profileToken + '|' + item.getStableKey());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = getApplicationView(convertView, parent);
        ApplicationItemInfo applicationItemInfo = getItem(position);
        bindApplication(textView, applicationItemInfo);
        return textView;
    }

    private TextView getApplicationView(View convertView, ViewGroup parent) {
        if (convertView != null) {
            return (TextView) convertView;
        }
        return (TextView) this.layoutInflater.inflate(
                R.layout.application_boxed_grid, parent, false);
    }

    private void bindApplication(TextView textView, ApplicationItemInfo application) {
        Context context = getContext();
        textView.setBackgroundDrawable(SelectorDrawable.createSelector(context, true));
        textView.setTextColor(WallpaperColorExtractor.getOnSurface(context));
        if (application instanceof AppListFolderInfo) {
            bindFolderIcon(textView, context);
        } else {
            bindApplicationIcon(textView, application);
        }
        textView.setText(application == null ? "" : application.title);
        textView.setContentDescription(buildApplicationDescription(application));
    }

    private void bindFolderIcon(TextView textView, Context context) {
        textView.setCompoundDrawablesWithIntrinsicBounds(null,
                context.getDrawable(R.drawable.ic_launcher_folder),
                null, null);
    }

    private void bindApplicationIcon(
            TextView textView,
            ApplicationItemInfo application) {
        Context context = getContext();
        Drawable normalizedIcon = Utilities.normalizeApplicationIcon(
                application == null ? null : application.icon, context);
        if (!this.uninstalling) {
            Utilities.setCompoundApplicationIcon(textView, normalizedIcon, context);
            return;
        }
        bindUninstallIcon(textView, application, normalizedIcon);
    }

    private void bindUninstallIcon(
            TextView textView,
            ApplicationItemInfo application,
            Drawable normalizedIcon) {
        Context context = getContext();
        if (application != null
                && Utilities.canUninstallApplication(context, application)) {
            Drawable icon = Utilities.overlayUninstallIcon(context, normalizedIcon);
            Utilities.setCompoundApplicationIcon(textView, icon, context);
            return;
        }
        textView.setTextColor(WallpaperColorExtractor.getOutline(context));
        Drawable icon = Utilities.adjustIconOpacity(normalizedIcon);
        Utilities.setCompoundApplicationIcon(textView, icon, context);
    }

    void setUninstalling(boolean uninstalling) {
        this.uninstalling = uninstalling;
    }

    private CharSequence buildApplicationDescription(ApplicationItemInfo info) {
        return info == null || info.title == null ? "" : info.title;
    }

    static long stableId(String stableKey) {
        long hash = 1125899906842597L;
        for (int index = 0; index < stableKey.length(); index++) {
            hash = (hash * 31L) + stableKey.charAt(index);
        }
        return hash;
    }

    private static ArrayList<ApplicationItemInfo> snapshot(
            List<ApplicationItemInfo> applicationItemInfos) {
        if (applicationItemInfos == null) {
            return new ArrayList<ApplicationItemInfo>();
        }
        return new ArrayList<ApplicationItemInfo>(applicationItemInfos);
    }

    private static String resolveProfileToken(Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) {
            return Process.myUserHandle().toString();
        }
        long serialNumber = userManager.getSerialNumberForUser(Process.myUserHandle());
        return Long.toString(serialNumber);
    }
}
