package org.zmreborn.widget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.drawable.Drawable;
import org.zmreborn.R;

/** Represents one selectable item in the widget preview picker. */
public final class WidgetPickerEntry {
    private final AppWidgetProviderInfo provider;
    private final CharSequence label;
    private final Drawable preview;
    private final CharSequence detail;

    private WidgetPickerEntry(AppWidgetProviderInfo provider, CharSequence label,
            Drawable preview, CharSequence detail) {
        if (label == null || preview == null || detail == null) {
            throw new IllegalArgumentException("Widget picker entry requires presentation data");
        }
        this.provider = provider;
        this.label = label;
        this.preview = preview;
        this.detail = detail;
    }

    /** Creates launcher-owned Search entry without an app-widget provider. */
    public static WidgetPickerEntry search(Context context, CharSequence detail) {
        if (context == null) {
            throw new IllegalArgumentException("Search widget entry requires context");
        }
        Drawable preview = context.getDrawable(R.drawable.ic_search_widget);
        return new WidgetPickerEntry(null, context.getText(R.string.group_search),
                preview, detail);
    }

    /** Creates an external provider entry with resolved presentation data. */
    public static WidgetPickerEntry provider(AppWidgetProviderInfo provider,
            CharSequence label, Drawable preview, CharSequence detail) {
        if (provider == null || provider.provider == null) {
            throw new IllegalArgumentException("Widget provider entry requires component");
        }
        return new WidgetPickerEntry(provider, label, preview, detail);
    }

    /** Returns whether this entry adds launcher-owned Search. */
    public boolean isSearch() {
        return this.provider == null;
    }

    /** Returns external provider metadata, or {@code null} for Search. */
    public AppWidgetProviderInfo getProvider() {
        return this.provider;
    }

    /** Returns localized display label. */
    public CharSequence getLabel() {
        return this.label;
    }

    /** Returns resolved preview, icon, or launcher fallback drawable. */
    public Drawable getPreview() {
        return this.preview;
    }

    /** Returns copy with updated target-grid size detail. */
    public WidgetPickerEntry withDetail(CharSequence updatedDetail) {
        return new WidgetPickerEntry(this.provider, this.label,
                this.preview, updatedDetail);
    }

    /** Returns minimum-size detail shown below label. */
    public CharSequence getDetail() {
        return this.detail;
    }
}
