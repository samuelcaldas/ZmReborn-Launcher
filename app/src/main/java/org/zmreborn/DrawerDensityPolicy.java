package org.zmreborn;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

final class DrawerDensityPolicy {
    static final String AUTOMATIC = "automatic";
    static final String COMFORTABLE = "comfortable";
    static final String DEFAULT = "default";
    static final String COMPACT = "compact";

    private DrawerDensityPolicy() {
    }

    static int getPreferredColumnWidth(Context context) {
        int widthDp = getViewportWidthDp(context);
        String density = PreferencesUtil.getAppsGridDensity(context);
        int dimension = resolvePreferredWidthResource(density, widthDp);
        return context.getResources().getDimensionPixelSize(dimension);
    }

    static int resolvePreferredWidthResource(String density, int viewportWidthDp) {
        if (COMPACT.equals(density)) {
            return R.dimen.drawer_cell_compact_width;
        }
        if (COMFORTABLE.equals(density)) {
            return R.dimen.drawer_cell_comfortable_width;
        }
        if (DEFAULT.equals(density)) {
            return R.dimen.drawer_cell_preferred_width;
        }
        if (viewportWidthDp >= 840) {
            return R.dimen.drawer_cell_expanded_width;
        }
        if (viewportWidthDp >= 600) {
            return R.dimen.drawer_cell_comfortable_width;
        }
        return R.dimen.drawer_cell_preferred_width;
    }

    private static int getViewportWidthDp(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        if (configuration.screenWidthDp > 0) {
            return configuration.screenWidthDp;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(metrics.widthPixels / metrics.density);
    }
}
