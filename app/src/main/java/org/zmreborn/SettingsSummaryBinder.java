package org.zmreborn;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

/** Keeps visible preference state synchronized with persisted values. */
final class SettingsSummaryBinder implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final PreferenceScreen mScreen;
    private final SharedPreferences mPreferences;

    private SettingsSummaryBinder(PreferenceScreen screen, SharedPreferences preferences) {
        mScreen = screen;
        mPreferences = preferences;
    }

    static SettingsSummaryBinder attach(PreferenceScreen screen, SharedPreferences preferences) {
        SettingsSummaryBinder binder = new SettingsSummaryBinder(screen, preferences);
        preferences.registerOnSharedPreferenceChangeListener(binder);
        binder.refresh();
        return binder;
    }

    void refresh() {
        bindGroup(mScreen);
    }

    void detach() {
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        refresh();
    }

    private void bindGroup(PreferenceGroup group) {
        int count = group.getPreferenceCount();
        for (int index = 0; index < count; index++) {
            Preference preference = group.getPreference(index);
            if (preference instanceof PreferenceGroup) {
                bindGroup((PreferenceGroup) preference);
                continue;
            }
            bindPreference(preference);
        }
    }

    private void bindPreference(Preference preference) {
        if (preference instanceof ListPreference) {
            bindListSummary((ListPreference) preference);
            return;
        }
        if (preference instanceof DialogSeekBarPreference) {
            preference.setSummary(((DialogSeekBarPreference) preference).getDisplayValue());
            return;
        }
        if (preference instanceof ColourPickerPreference) {
            preference.setSummary(ColourPickerPreference.convertToARGB(((ColourPickerPreference) preference).getValue()));
        }
    }

    private void bindListSummary(ListPreference preference) {
        CharSequence summary = resolveListSummary(preference.getEntries(),
                preference.getEntryValues(), preference.getValue());
        preference.setSummary(summary);
    }

    static CharSequence resolveListSummary(CharSequence[] entries, CharSequence[] entryValues,
            String value) {
        if (entries == null || entryValues == null || value == null) {
            return null;
        }
        int count = Math.min(entries.length, entryValues.length);
        for (int index = 0; index < count; index++) {
            CharSequence entryValue = entryValues[index];
            if (entryValue != null && value.contentEquals(entryValue)) {
                return entries[index];
            }
        }
        return null;
    }
}
