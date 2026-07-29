package org.zmreborn;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.test.ActivityInstrumentationTestCase2;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class PreferencesE2ETest extends ActivityInstrumentationTestCase2<Preferences> {
    private static final long ACTIVITY_RECREATION_TIMEOUT_MILLIS = 30000L;

    public PreferencesE2ETest() {
        super(Preferences.class);
    }

    public void testAllSettingsRemainReachable() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        assertEquals(38, countLeafPreferences(preferences.getPreferenceScreen()));
        assertNotNull(preferences.findPreference(preferences.getString(R.string.preferences_key_application)));
        assertNotNull(preferences.findPreference(preferences.getString(R.string.preferences_key_reset)));
    }

    public void testPreferenceScreenOpensFromTouch() {
        final Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        final PreferenceScreen generalScreen = (PreferenceScreen) preferences.getPreferenceScreen().getPreference(0);
        final android.widget.ListView listView = preferences.getListView();
        final View decorView = preferences.getWindow().getDecorView();
        final boolean[] dispatched = new boolean[2];
        final Throwable[] dispatchFailure = new Throwable[1];

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    View generalRow = listView.getChildAt(0);
                    if (generalRow == null) {
                        throw new IllegalStateException("General preference screen row is not visible");
                    }
                    Rect visibleBounds = new Rect();
                    if (!generalRow.getGlobalVisibleRect(visibleBounds)) {
                        throw new IllegalStateException("General preference screen row has no visible bounds");
                    }
                    int[] decorLocation = new int[2];
                    decorView.getLocationOnScreen(decorLocation);
                    float centerX = visibleBounds.centerX() - decorLocation[0];
                    float centerY = visibleBounds.centerY() - decorLocation[1];
                    long downTime = SystemClock.uptimeMillis();
                    dispatched[0] = dispatchTouchEvent(decorView, downTime, downTime,
                            MotionEvent.ACTION_DOWN, centerX, centerY);
                    dispatched[1] = dispatchTouchEvent(decorView, downTime,
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, centerX, centerY);
                } catch (Throwable throwable) {
                    dispatchFailure[0] = throwable;
                }
            }
        });
        getInstrumentation().waitForIdleSync();

        if (dispatchFailure[0] != null) {
            fail("Touch dispatch failed: " + dispatchFailure[0]);
        }
        assertTrue("General row must accept pointer down", dispatched[0]);
        assertTrue("General row must accept pointer up", dispatched[1]);
        assertNotNull("General preference screen must create a dialog", generalScreen.getDialog());
        assertTrue("General preference screen must open from touch", generalScreen.getDialog().isShowing());
        generalScreen.getDialog().dismiss();
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
        final Preferences preferences = getActivity();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(preferences);
        final String defaultKey = preferences.getString(R.string.preferences_key_workspace_default_screen);
        final boolean hadDefaultValue = sharedPreferences.contains(defaultKey);
        final int originalDefaultValue = sharedPreferences.getInt(defaultKey, 1);
        final DialogSeekBarPreference defaultScreen = (DialogSeekBarPreference) preferences.findPreference(defaultKey);
        assertEquals(1, defaultScreen.getMin());
        try {
            preferences.runOnUiThread(new Runnable() {
                public void run() {
                    defaultScreen.setMax(7);
                    defaultScreen.setValue(6);
                    defaultScreen.setMax(1);
                }
            });
            getInstrumentation().waitForIdleSync();
            assertEquals(1, defaultScreen.getValue());
            assertTrue(defaultScreen.getValue() <= defaultScreen.getMax());
        } finally {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (hadDefaultValue) {
                editor.putInt(defaultKey, originalDefaultValue);
            } else {
                editor.remove(defaultKey);
            }
            assertTrue(editor.commit());
        }
    }

    public void testReducingScreenCountPersistsDefaultScreenKey() {
        final Preferences preferences = getActivity();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(preferences);
        final String defaultKey = preferences.getString(R.string.preferences_key_workspace_default_screen);
        final String screenCountKey = preferences.getString(R.string.preferences_key_workspace_number_of_screens);
        final boolean hadDefaultValue = sharedPreferences.contains(defaultKey);
        final int originalDefaultValue = sharedPreferences.getInt(defaultKey, 1);
        final boolean hadScreenCountValue = sharedPreferences.contains(screenCountKey);
        final int originalScreenCountValue = sharedPreferences.getInt(screenCountKey, 3);
        final boolean originalRestart = Launcher.sRestart;
        try {
            assertTrue(sharedPreferences.edit().putInt(defaultKey, 3).putInt(screenCountKey, 3).commit());
            preferences.runOnUiThread(new Runnable() {
                public void run() {
                    DialogSeekBarPreference screenCount = (DialogSeekBarPreference) preferences.findPreference(screenCountKey);
                    screenCount.getOnPreferenceChangeListener().onPreferenceChange(screenCount, Integer.valueOf(0));
                }
            });
            getInstrumentation().waitForIdleSync();
            assertEquals(1, sharedPreferences.getInt(defaultKey, 0));
            assertEquals(3, sharedPreferences.getInt(screenCountKey, 0));
        } finally {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    Launcher.sRestart = originalRestart;
                }
            });
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (hadDefaultValue) {
                editor.putInt(defaultKey, originalDefaultValue);
            } else {
                editor.remove(defaultKey);
            }
            if (hadScreenCountValue) {
                editor.putInt(screenCountKey, originalScreenCountValue);
            } else {
                editor.remove(screenCountKey);
            }
            assertTrue(editor.commit());
        }
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

    public void testPreferenceRowsUseSemanticTextSizes() {
        Preferences preferences = getActivity();
        LayoutInflater inflater = LayoutInflater.from(preferences);
        View row = inflater.inflate(R.layout.settings_preference, null);
        TextView title = (TextView) row.findViewById(android.R.id.title);
        TextView summary = (TextView) row.findViewById(android.R.id.summary);
        TextView category = (TextView) inflater.inflate(R.layout.settings_preference_category, null);

        assertEquals(preferences.getResources().getDimension(R.dimen.text_size_title), title.getTextSize(), 0.5f);
        assertEquals(preferences.getResources().getDimension(R.dimen.text_size_label), summary.getTextSize(), 0.5f);
        assertEquals(preferences.getResources().getDimension(R.dimen.text_size_category), category.getTextSize(), 0.5f);
    }

    public void testPreferenceMinimumTouchTargets() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        android.widget.ListView listView = preferences.getListView();
        int visibleRows = listView.getChildCount();
        int minimumTouchTarget = (int) (48 * preferences.getResources().getDisplayMetrics().density + 0.5f);

        assertTrue("Settings must have focusable preferences", visibleRows > 0);
        for (int index = 0; index < visibleRows; index++) {
            View row = listView.getChildAt(index);
            assertTrue("Row height should accommodate touch target", row.getHeight() >= minimumTouchTarget);
        }
    }

    public void testDPADNavigationThroughPreferences() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        View listView = preferences.getListView();

        assertTrue("List view must be focusable", listView.isFocusable());
        assertTrue("Preferences must render initially", preferences.getPreferenceScreen().getPreferenceCount() > 0);
    }

    public void testDestructiveResetHasTitleAndSummary() {
        Preferences preferences = getActivity();
        Preference resetPref = preferences.findPreference(preferences.getString(R.string.preferences_key_reset));
        assertNotNull("Reset preference must exist", resetPref);
        assertNotNull("Reset preference must have a title", resetPref.getTitle());
        assertTrue("Reset preference title must not be empty", resetPref.getTitle().length() > 0);
        assertNotNull("Reset preference must have a summary", resetPref.getSummary());
        assertTrue("Reset preference summary must not be empty", resetPref.getSummary().length() > 0);
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

    public void testAppearancePreferencePersistsDarkThemeAndRecreatesActivity() {
        final Preferences preferences = getActivity();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(preferences);
        final String appearanceKey = preferences.getString(
                R.string.preferences_key_application_appearance);
        final boolean hadOriginalAppearance = sharedPreferences.contains(appearanceKey);
        final String originalAppearance = sharedPreferences.getString(appearanceKey, null);
        final ListPreference appearancePreference = (ListPreference) preferences.findPreference(
                appearanceKey);
        assertNotNull("Appearance preference must exist", appearancePreference);
        android.app.Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(
                Preferences.class.getName(), null, false);
        Preferences recreatedPreferences = null;
        final boolean[] listenerResult = new boolean[1];

        try {
            if (Appearance.DARK.equals(Appearance.normalizeAppearance(originalAppearance))) {
                assertTrue("Appearance setup must persist", sharedPreferences.edit()
                        .putString(appearanceKey, Appearance.SYSTEM).commit());
            }
            preferences.runOnUiThread(new Runnable() {
                public void run() {
                    listenerResult[0] = appearancePreference.getOnPreferenceChangeListener()
                            .onPreferenceChange(appearancePreference, Appearance.DARK);
                }
            });
            assertFalse("Appearance listener must handle persistence", listenerResult[0]);
            recreatedPreferences = (Preferences) getInstrumentation().waitForMonitorWithTimeout(
                    monitor, ACTIVITY_RECREATION_TIMEOUT_MILLIS);
            assertNotNull("Appearance change must recreate Preferences", recreatedPreferences);
            assertNotSame("Appearance change must replace Preferences", preferences,
                    recreatedPreferences);
            getInstrumentation().waitForIdleSync();
            assertEquals("Dark appearance must persist", Appearance.DARK,
                    sharedPreferences.getString(appearanceKey, null));
            assertEquals("Recreated Preferences must use dark mode", Configuration.UI_MODE_NIGHT_YES,
                    recreatedPreferences.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK);
            ListPreference recreatedAppearancePreference = (ListPreference) recreatedPreferences
                    .findPreference(appearanceKey);
            assertNotNull("Recreated appearance preference must exist",
                    recreatedAppearancePreference);
            assertEquals("Recreated appearance preference must use dark value", Appearance.DARK,
                    recreatedAppearancePreference.getValue());
            assertEquals("Recreated appearance summary must match selected entry",
                    recreatedAppearancePreference.getEntry(), recreatedAppearancePreference.getSummary());
        } finally {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (hadOriginalAppearance) {
                editor.putString(appearanceKey, originalAppearance);
            } else {
                editor.remove(appearanceKey);
            }
            assertTrue("Original appearance preference must be restored", editor.commit());
            getInstrumentation().removeMonitor(monitor);
        }
    }

    public void testPreferencePersistenceAfterRotation() {
        final Preferences preferences = getActivity();
        final SharedPreferences sharedPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(preferences);
        final String testKey = preferences.getString(R.string.preferences_key_application_language);
        final boolean hadOriginalValue = sharedPrefs.contains(testKey);
        final String originalValue = sharedPrefs.getString(testKey, null);
        final String temporaryValue = "en".equals(originalValue) ? "pt-BR" : "en";
        final int initialDeviceOrientation = preferences.getResources().getConfiguration().orientation;
        final int originalRequestedOrientation = preferences.getRequestedOrientation();
        final boolean initiallyLandscape = initialDeviceOrientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        final int rotatedRequestedOrientation = initiallyLandscape
                ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        final int rotatedConfigurationOrientation = initiallyLandscape
                ? android.content.res.Configuration.ORIENTATION_PORTRAIT
                : android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        final int restoredRequestedOrientation = initiallyLandscape
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        assertTrue("Device must start in a supported orientation", initiallyLandscape
                || initialDeviceOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT);
        android.app.Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(
                Preferences.class.getName(), null, false);
        Preferences recreatedPreferences = null;

        try {
            assertTrue("Temporary language must persist", sharedPrefs.edit()
                    .putString(testKey, temporaryValue).commit());
            preferences.runOnUiThread(new Runnable() {
                public void run() {
                    preferences.setRequestedOrientation(rotatedRequestedOrientation);
                }
            });
            recreatedPreferences = (Preferences) getInstrumentation().waitForMonitorWithTimeout(
                    monitor, ACTIVITY_RECREATION_TIMEOUT_MILLIS);
            assertNotNull("Rotation must recreate Preferences", recreatedPreferences);
            assertNotSame("Rotation must replace Preferences", preferences, recreatedPreferences);
            getInstrumentation().waitForIdleSync();
            assertEquals("Recreated Preferences must use target orientation", rotatedConfigurationOrientation,
                    recreatedPreferences.getResources().getConfiguration().orientation);
            SharedPreferences recreatedSharedPrefs = android.preference.PreferenceManager
                    .getDefaultSharedPreferences(recreatedPreferences);
            assertEquals("Temporary language must persist through recreation", temporaryValue,
                    recreatedSharedPrefs.getString(testKey, null));
            android.preference.ListPreference recreatedLanguagePreference =
                    (android.preference.ListPreference) recreatedPreferences.findPreference(testKey);
            assertNotNull("Recreated language preference must exist", recreatedLanguagePreference);
            assertEquals("Recreated language preference must use temporary value", temporaryValue,
                    recreatedLanguagePreference.getValue());
            assertEquals("Recreated language summary must match selected entry",
                    recreatedLanguagePreference.getEntry(), recreatedLanguagePreference.getSummary());
        } finally {
            getInstrumentation().removeMonitor(monitor);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            if (hadOriginalValue) {
                editor.putString(testKey, originalValue);
            } else {
                editor.remove(testKey);
            }
            assertTrue("Original language preference must be restored", editor.commit());
            if (recreatedPreferences != null) {
                restoreOrientation(recreatedPreferences, restoredRequestedOrientation,
                        initialDeviceOrientation, originalRequestedOrientation);
            }
        }
    }

    private void restoreOrientation(final Preferences preferences, final int requestedOrientation,
            int expectedConfigurationOrientation, final int originalRequestedOrientation) {
        android.app.Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(
                Preferences.class.getName(), null, false);
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    preferences.setRequestedOrientation(requestedOrientation);
                }
            });
            Preferences restored = (Preferences) getInstrumentation().waitForMonitorWithTimeout(
                    monitor, ACTIVITY_RECREATION_TIMEOUT_MILLIS);
            assertNotNull("Restoring orientation must recreate Preferences", restored);
            getInstrumentation().waitForIdleSync();
            assertEquals("Preferences must return to initial orientation",
                    expectedConfigurationOrientation,
                    restored.getResources().getConfiguration().orientation);
            restoreRequestedOrientation(restored, originalRequestedOrientation);
        } finally {
            getInstrumentation().removeMonitor(monitor);
        }
    }

    private void restoreRequestedOrientation(final Preferences preferences,
            final int originalRequestedOrientation) {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                preferences.setRequestedOrientation(originalRequestedOrientation);
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private static boolean dispatchTouchEvent(View target, long downTime, long eventTime,
            int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            return target.dispatchTouchEvent(event);
        } finally {
            event.recycle();
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
