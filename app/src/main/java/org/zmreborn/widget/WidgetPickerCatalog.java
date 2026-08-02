package org.zmreborn.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.zmreborn.CellLayout;
import org.zmreborn.R;

/** Loads validated widget-provider metadata and preview presentation. */
public final class WidgetPickerCatalog {
    private WidgetPickerCatalog() {
    }

    /** Loads Search plus installed providers without allocating widget IDs. */
    public static List<WidgetPickerEntry> load(Context context,
            AppWidgetManager manager) {
        if (context == null || manager == null) {
            throw new IllegalArgumentException("Widget catalog requires context and manager");
        }
        ArrayList<WidgetPickerEntry> providers = loadProviders(context, manager);
        sortProviders(providers);
        ArrayList<WidgetPickerEntry> entries = new ArrayList<>();
        entries.add(WidgetPickerEntry.search(context,
                formatSpan(context, WidgetSearchSpan.COLUMNS, WidgetSearchSpan.ROWS)));
        entries.addAll(providers);
        return entries;
    }

    /** Applies live target-grid spans on main thread when geometry is ready. */
    public static List<WidgetPickerEntry> applyTargetSpans(Context context,
            List<WidgetPickerEntry> entries, CellLayout targetLayout) {
        if (context == null || entries == null) {
            throw new IllegalArgumentException("Widget spans require context and entries");
        }
        if (!isTargetGeometryReady(targetLayout)) {
            return entries;
        }
        ArrayList<WidgetPickerEntry> adjusted = new ArrayList<>();
        for (WidgetPickerEntry entry : entries) {
            adjusted.add(applyTargetSpan(context, entry, targetLayout));
        }
        return adjusted;
    }

    private static ArrayList<WidgetPickerEntry> loadProviders(Context context,
            AppWidgetManager manager) {
        ArrayList<WidgetPickerEntry> entries = new ArrayList<>();
        List<AppWidgetProviderInfo> providers = manager.getInstalledProviders();
        if (providers == null) {
            return entries;
        }
        for (AppWidgetProviderInfo provider : providers) {
            WidgetPickerEntry entry = createProviderEntry(context, provider);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static WidgetPickerEntry createProviderEntry(Context context,
            AppWidgetProviderInfo provider) {
        if (provider == null || provider.provider == null
                || provider.minWidth < 0 || provider.minHeight < 0) {
            return null;
        }
        CharSequence label = loadLabel(context, provider);
        Drawable preview = loadPreview(context, provider);
        CharSequence detail = formatProviderSize(context, provider);
        return WidgetPickerEntry.provider(provider, label, preview, detail);
    }

    private static CharSequence loadLabel(Context context,
            AppWidgetProviderInfo provider) {
        CharSequence label = tryLoadLabel(context.getPackageManager(), provider);
        if (label != null && label.toString().trim().length() > 0) {
            return label;
        }
        return provider.provider.getPackageName();
    }

    private static CharSequence tryLoadLabel(PackageManager packageManager,
            AppWidgetProviderInfo provider) {
        try {
            return provider.loadLabel(packageManager);
        } catch (Resources.NotFoundException exception) {
            return null;
        } catch (SecurityException exception) {
            return null;
        }
    }

    private static Drawable loadPreview(Context context,
            AppWidgetProviderInfo provider) {
        int density = context.getResources().getDisplayMetrics().densityDpi;
        Drawable preview = loadPreviewImage(context, provider, density);
        if (preview != null) {
            return preview;
        }
        Drawable icon = loadProviderIcon(context, provider, density);
        if (icon != null) {
            return icon;
        }
        return context.getDrawable(R.drawable.ic_launcher_appwidget);
    }

    private static Drawable loadPreviewImage(Context context,
            AppWidgetProviderInfo provider, int density) {
        try {
            return provider.loadPreviewImage(context, density);
        } catch (Resources.NotFoundException exception) {
            return null;
        } catch (SecurityException exception) {
            return null;
        }
    }

    private static Drawable loadProviderIcon(Context context,
            AppWidgetProviderInfo provider, int density) {
        try {
            return provider.loadIcon(context, density);
        } catch (Resources.NotFoundException exception) {
            return null;
        } catch (SecurityException exception) {
            return null;
        }
    }

    private static CharSequence formatProviderSize(Context context,
            AppWidgetProviderInfo provider) {
        int width = Math.max(0, provider.minWidth);
        int height = Math.max(0, provider.minHeight);
        return context.getString(R.string.widget_picker_minimum_size_dp,
                width, height);
    }

    private static WidgetPickerEntry applyTargetSpan(Context context,
            WidgetPickerEntry entry, CellLayout targetLayout) {
        if (entry.isSearch()) {
            return entry;
        }
        AppWidgetProviderInfo provider = entry.getProvider();
        int[] span = targetLayout.rectToCellFromDp(
                provider.minWidth, provider.minHeight);
        return entry.withDetail(formatSpan(context, span[0], span[1]));
    }

    private static boolean isTargetGeometryReady(CellLayout targetLayout) {
        return targetLayout != null && targetLayout.isAttachedToWindow()
                && targetLayout.isLaidOut() && !targetLayout.isLayoutRequested()
                && targetLayout.getWidth() > 0 && targetLayout.getHeight() > 0
                && targetLayout.getCountX() > 0 && targetLayout.getCountY() > 0;
    }

    private static String formatSpan(Context context, int columns, int rows) {
        return context.getString(R.string.widget_picker_span, columns, rows);
    }

    private static void sortProviders(ArrayList<WidgetPickerEntry> providers) {
        final Collator collator = Collator.getInstance();
        Collections.sort(providers, new Comparator<WidgetPickerEntry>() {
            public int compare(WidgetPickerEntry first, WidgetPickerEntry second) {
                int labelOrder = collator.compare(first.getLabel(), second.getLabel());
                if (labelOrder != 0) {
                    return labelOrder;
                }
                String firstComponent = first.getProvider().provider.flattenToString();
                String secondComponent = second.getProvider().provider.flattenToString();
                return firstComponent.compareTo(secondComponent);
            }
        });
    }

    private static final class WidgetSearchSpan {
        static final int COLUMNS = 4;
        static final int ROWS = 1;

        private WidgetSearchSpan() {
        }
    }
}
