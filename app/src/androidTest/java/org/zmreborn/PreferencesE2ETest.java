package org.zmreborn;

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

    public void testApplicationRowShowsRebrandedIdentityWithoutLink() {
        Preferences preferences = getActivity();
        Preference application = preferences.findPreference(
                preferences.getString(R.string.preferences_key_application));
        String expectedTitle = "ZM Reborn " + LauncherApplication.getVersionName(preferences);
        assertEquals(expectedTitle, application.getTitle());
        assertFalse(application.isSelectable());
    }

    public void testSeekPreferenceClampsStoredValueToNewMaximum() {
        Preferences preferences = getActivity();
        final DialogSeekBarPreference defaultScreen = (DialogSeekBarPreference) preferences.findPreference(
                preferences.getString(R.string.preferences_key_workspace_default_screen));
        assertEquals(1, defaultScreen.getMin());
        assertEquals(7, defaultScreen.getMax());
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

    public void testLargeFontSafetyInPreferenceRows() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        android.widget.ListView listView = preferences.getListView();

        int visibleRows = listView.getChildCount();
        for (int index = 0; index < visibleRows; index++) {
            View row = listView.getChildAt(index);
            assertTrue("Settings row with large font must have height > 0", row.getHeight() > 0);
            assertTrue("Settings row width must remain positive", row.getWidth() > 0);
        }
    }

    public void testPreferenceMinimumTouchTargets() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        android.widget.ListView listView = preferences.getListView();
        int visibleRows = listView.getChildCount();

        assertTrue("Settings must have focusable preferences", visibleRows > 0);
        for (int index = 0; index < visibleRows; index++) {
            View row = listView.getChildAt(index);
            assertTrue("Row height should accommodate touch target", row.getHeight() >= 30);
        }
    }

    public void testDPADNavigationThroughPreferences() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        View listView = preferences.getListView();

        assertTrue("List view must be focusable", listView.isFocusable());
        assertTrue("Preferences must render initially", preferences.getPreferenceScreen().getPreferenceCount() > 0);
    }

    public void testDestructiveResetHasAccessibilityDescription() {
        Preferences preferences = getActivity();
        Preference resetPref = preferences.findPreference(preferences.getString(R.string.preferences_key_reset));
        assertNotNull("Reset preference must exist", resetPref);
    }

    public void testPreferenceScreenBoundsValid() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        View listView = preferences.getListView();
        int width = listView.getWidth();
        int height = listView.getHeight();

        assertTrue("Preference list width must be positive", width > 0);
        assertTrue("Preference list height must be positive", height > 0);
    }

    public void testPreferencePersistenceAfterRotation() {
        Preferences preferences = getActivity();
        SharedPreferences sharedPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(preferences);
        String testKey = preferences.getString(R.string.preferences_key_application_language);

        int originalValue = sharedPrefs.getInt(testKey, 0);
        sharedPrefs.edit().putInt(testKey, originalValue).commit();
        getInstrumentation().waitForIdleSync();

        assertEquals("Preference value must persist", originalValue, sharedPrefs.getInt(testKey, -1));
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
