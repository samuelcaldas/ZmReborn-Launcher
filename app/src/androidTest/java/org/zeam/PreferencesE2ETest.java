package org.zeam;

import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

public class PreferencesE2ETest extends ActivityInstrumentationTestCase2<Preferences> {
    public PreferencesE2ETest() {
        super(Preferences.class);
    }

    public void testAllSettingsRemainReachable() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        assertEquals(36, countLeafPreferences(preferences.getPreferenceScreen()));
        assertNotNull(preferences.findPreference(preferences.getString(R.string.preferences_key_application)));
        assertNotNull(preferences.findPreference(preferences.getString(R.string.preferences_key_reset)));
    }

    public void testSeekPreferenceClampsStoredValueToNewMaximum() {
        Preferences preferences = getActivity();
        final DialogSeekBarPreference defaultScreen = (DialogSeekBarPreference) preferences.findPreference(
                preferences.getString(R.string.preferences_key_workspace_default_screen));
        preferences.runOnUiThread(new Runnable() {
            public void run() {
                defaultScreen.setMax(1);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(1, defaultScreen.getValue());
        assertTrue(defaultScreen.getValue() <= defaultScreen.getMax());
    }

    public void testReducingScreenCountPersistsDefaultScreenKey() {
        final Preferences preferences = getActivity();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(preferences);
        final String defaultKey = preferences.getString(R.string.preferences_key_workspace_default_screen);
        final String screenCountKey = preferences.getString(R.string.preferences_key_workspace_number_of_screens);
        sharedPreferences.edit().putInt(defaultKey, 3).putInt(screenCountKey, 3).commit();
        preferences.runOnUiThread(new Runnable() {
            public void run() {
                DialogSeekBarPreference screenCount = (DialogSeekBarPreference) preferences.findPreference(screenCountKey);
                screenCount.getOnPreferenceChangeListener().onPreferenceChange(screenCount, Integer.valueOf(0));
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(1, sharedPreferences.getInt(defaultKey, 0));
        assertEquals(3, sharedPreferences.getInt(screenCountKey, 0));
    }

    public void testPreferenceRowsHavePositiveBounds() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        int visibleRows = preferences.getListView().getChildCount();
        assertTrue("Settings list must render at least one row", visibleRows > 0);
        for (int index = 0; index < visibleRows; index++) {
            View row = preferences.getListView().getChildAt(index);
            assertTrue("Settings row must have positive width", row.getWidth() > 0);
            assertTrue("Settings row must have positive height", row.getHeight() > 0);
        }
    }

    private static int countLeafPreferences(PreferenceGroup group) {
        int count = 0;
        for (int index = 0; index < group.getPreferenceCount(); index++) {
            Preference preference = group.getPreference(index);
            if (preference instanceof PreferenceScreen) {
                count += countLeafPreferences((PreferenceGroup) preference);
            } else if (preference instanceof PreferenceGroup) {
                count += countLeafPreferences((PreferenceGroup) preference);
            } else {
                count++;
            }
        }
        return count;
    }
}
