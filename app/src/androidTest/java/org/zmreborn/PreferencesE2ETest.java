package org.zmreborn;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.test.ActivityInstrumentationTestCase2;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PreferencesE2ETest extends ActivityInstrumentationTestCase2<Preferences> {
    private static final long ACTIVITY_RECREATION_TIMEOUT_MILLIS = 30000L;

    public PreferencesE2ETest() {
        super(Preferences.class);
    }

    public void testAllSettingsRemainReachable() {
        Preferences preferences = getActivity();
        getInstrumentation().waitForIdleSync();
        assertEquals(37, countLeafPreferences(preferences.getPreferenceScreen()));
        assertNotNull(preferences.findPreference(preferences.getString(R.string.preferences_key_application)));
        assertNotNull(preferences.findPreference(preferences.getString(R.string.preferences_key_reset)));
    }

    public void testBlurBackgroundPreferencePersistsToggle() throws Throwable {
        final Preferences preferences = getActivity();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(preferences);
        final String key = preferences.getString(R.string.preferences_key_blur_backgrounds);
        final boolean hadOriginalValue = sharedPreferences.contains(key);
        final boolean originalValue = sharedPreferences.getBoolean(key, false);
        final SwitchPreference blurPreference = (SwitchPreference) preferences.findPreference(key);
        assertNotNull("Blur backgrounds preference must exist", blurPreference);

        try {
            final boolean toggledValue = !originalValue;
            runTestOnUiThread(new Runnable() {
                public void run() {
                    blurPreference.setChecked(toggledValue);
                }
            });
            assertEquals("Blur backgrounds toggle must persist", toggledValue,
                    sharedPreferences.getBoolean(key, originalValue));
        } finally {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (hadOriginalValue) {
                editor.putBoolean(key, originalValue);
            } else {
                editor.remove(key);
            }
            assertTrue("Blur backgrounds preference must restore", editor.commit());
        }
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

    public void testInlineStepperDebouncesRapidTaps() {
        final Preferences preferences = getActivity();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(preferences);
        final String key = preferences.getString(
                R.string.preferences_key_workspace_content_grid_rows);
        final boolean hadValue = sharedPreferences.contains(key);
        final int originalValue = sharedPreferences.getInt(key, 4);
        final boolean originalRestart = Launcher.sRestart;
        final InlineStepperPreference stepper = (InlineStepperPreference) preferences
                .findPreference(key);
        final View row = bindPreferenceRow(preferences, stepper);
        final int initialValue = stepper.getValue();
        final int direction = stepper.getMax() - initialValue >= 2 ? 1 : -1;
        final ImageButton action = (ImageButton) row.findViewById(
                direction > 0 ? R.id.increment : R.id.decrement);
        final int[] writeCount = new int[1];
        final boolean[] storageStayedPending = new boolean[1];
        SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences values,
                            String changedKey) {
                        if (key.equals(changedKey)) {
                            writeCount[0]++;
                        }
                    }
                };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    assertTrue(action.performClick());
                    assertTrue(action.performClick());
                    storageStayedPending[0] = storedValueMatches(
                            sharedPreferences, key, hadValue, originalValue);
                }
            });
            int expected = initialValue + (direction * 2);
            assertEquals(expected, stepper.getValue());
            assertEquals(String.valueOf(expected), ((TextView) row.findViewById(
                    R.id.settings_numeric_value)).getText().toString());
            assertTrue("Rapid taps must stay pending inside input batch",
                    storageStayedPending[0]);
            awaitIntPreference(sharedPreferences, key, expected);
            SystemClock.sleep(350L);
            getInstrumentation().waitForIdleSync();
            assertEquals("Rapid taps must produce one trailing write", 1, writeCount[0]);
        } finally {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
            restoreIntPreference(sharedPreferences, key, hadValue, originalValue);
            Launcher.sRestart = originalRestart;
        }
    }

    public void testInlineStepperHonorsBoundsLongClickAndListenerRejection() {
        final Preferences preferences = getActivity();
        final String key = preferences.getString(
                R.string.preferences_key_workspace_number_of_screens);
        final InlineStepperPreference stepper = (InlineStepperPreference) preferences
                .findPreference(key);
        final Preference.OnPreferenceChangeListener originalListener =
                stepper.getOnPreferenceChangeListener();
        final int originalValue = stepper.getValue();
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    stepper.setValueFromRuntime(stepper.getMin());
                }
            });
            View minimumRow = bindPreferenceRow(preferences, stepper);
            final ImageButton decrement = (ImageButton) minimumRow.findViewById(R.id.decrement);
            final ImageButton increment = (ImageButton) minimumRow.findViewById(R.id.increment);
            assertFalse(decrement.isEnabled());
            assertTrue(increment.isEnabled());
            assertTrue(decrement.isFocusable());
            assertTrue(increment.isFocusable());
            performLongClick(decrement);
            assertEquals(stepper.getMin(), stepper.getValue());
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    stepper.setOnPreferenceChangeListener(
                            new Preference.OnPreferenceChangeListener() {
                                public boolean onPreferenceChange(Preference preference,
                                        Object value) {
                                    return false;
                                }
                            });
                }
            });
            performClick(increment);
            assertEquals(stepper.getMin(), stepper.getValue());
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    stepper.setValueFromRuntime(stepper.getMax());
                }
            });
            View maximumRow = bindPreferenceRow(preferences, stepper);
            assertFalse(maximumRow.findViewById(R.id.increment).isEnabled());
            assertTrue(maximumRow.findViewById(R.id.decrement).isEnabled());
        } finally {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    stepper.setOnPreferenceChangeListener(originalListener);
                    stepper.setValueFromRuntime(originalValue);
                }
            });
        }
    }

    public void testReducingScreenCountClampsDefaultAndFlushesOnPause() {
        Context context = getInstrumentation().getTargetContext();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);
        final String defaultKey = context.getString(
                R.string.preferences_key_workspace_default_screen);
        final String screenCountKey = context.getString(
                R.string.preferences_key_workspace_number_of_screens);
        final boolean hadDefault = sharedPreferences.contains(defaultKey);
        final int originalDefault = sharedPreferences.getInt(defaultKey, 2);
        final boolean hadScreenCount = sharedPreferences.contains(screenCountKey);
        final int originalScreenCount = sharedPreferences.getInt(screenCountKey, 3);
        final boolean originalRestart = Launcher.sRestart;
        assertTrue(sharedPreferences.edit().putInt(defaultKey, 3)
                .putInt(screenCountKey, 3).commit());
        final Preferences preferences = getActivity();
        final InlineStepperPreference screenCount = (InlineStepperPreference) preferences
                .findPreference(screenCountKey);
        final InlineStepperPreference defaultScreen = (InlineStepperPreference) preferences
                .findPreference(defaultKey);
        final View row = bindPreferenceRow(preferences, screenCount);
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    assertTrue(row.findViewById(R.id.decrement).performClick());
                    assertEquals(2, defaultScreen.getMax());
                    assertEquals(2, defaultScreen.getValue());
                    preferences.finish();
                }
            });
            getInstrumentation().waitForIdleSync();
            assertEquals(2, sharedPreferences.getInt(screenCountKey, 0));
            assertEquals(2, sharedPreferences.getInt(defaultKey, 0));
            assertTrue(Launcher.sRestart);
        } finally {
            restoreIntPreference(sharedPreferences, defaultKey, hadDefault, originalDefault);
            restoreIntPreference(sharedPreferences, screenCountKey, hadScreenCount,
                    originalScreenCount);
            Launcher.sRestart = originalRestart;
        }
    }

    public void testInlineTransparencyUpdatesImmediatelyAndFlushesOnPause() {
        Context context = getInstrumentation().getTargetContext();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);
        final String key = context.getString(R.string.preferences_key_apps_grid_bg_alpha);
        final boolean hadValue = sharedPreferences.contains(key);
        final int originalValue = sharedPreferences.getInt(key, 255);
        final boolean originalRestart = Launcher.sRestart;
        assertTrue(sharedPreferences.edit().putInt(key, 255).commit());
        final Preferences preferences = getActivity();
        final InlineSliderPreference sliderPreference = (InlineSliderPreference) preferences
                .findPreference(key);
        final View row = bindPreferenceRow(preferences, sliderPreference);
        final SeekBar slider = (SeekBar) row.findViewById(R.id.settings_inline_slider);
        final boolean[] immediateValueVisible = new boolean[1];
        PausedActivityObserver pauseObserver = new PausedActivityObserver(preferences);
        preferences.getApplication().registerActivityLifecycleCallbacks(pauseObserver);
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    slider.setProgress(191);
                    sliderPreference.onProgressChanged(slider, 191, true);
                    slider.setProgress(192);
                    sliderPreference.onProgressChanged(slider, 192, true);
                    TextView value = (TextView) row.findViewById(R.id.settings_numeric_value);
                    immediateValueVisible[0] = "192".contentEquals(value.getText());
                    preferences.finish();
                }
            });
            assertTrue("Settings must pause after finish", pauseObserver.awaitPause());
            assertTrue("Inline alpha value must update before persistence",
                    immediateValueVisible[0]);
            assertEquals("Pause must flush final alpha", 192,
                    sharedPreferences.getInt(key, -1));
            assertTrue(Launcher.sRestart);
        } finally {
            preferences.getApplication().unregisterActivityLifecycleCallbacks(pauseObserver);
            restoreIntPreference(sharedPreferences, key, hadValue, originalValue);
            Launcher.sRestart = originalRestart;
        }
    }

    public void testRecycledSliderCallbacksUpdateTheirOwnRow() {
        final Preferences preferences = getActivity();
        final InlineSliderPreference sliderPreference = (InlineSliderPreference) preferences
                .findPreference(preferences.getString(
                        R.string.preferences_key_apps_grid_bg_alpha));
        final int originalValue = sliderPreference.getValue();
        final boolean originalRestart = Launcher.sRestart;
        final int targetValue = originalValue > sliderPreference.getMin()
                ? originalValue - 1 : originalValue + 1;
        final View firstRow = bindPreferenceRow(preferences, sliderPreference);
        final View secondRow = bindPreferenceRow(preferences, sliderPreference);
        final SeekBar firstSlider = (SeekBar) firstRow.findViewById(
                R.id.settings_inline_slider);
        final String[] boundValues = new String[2];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                sliderPreference.onProgressChanged(firstSlider,
                        targetValue - sliderPreference.getMin(), true);
                TextView firstValue = (TextView) firstRow.findViewById(
                        R.id.settings_numeric_value);
                TextView secondValue = (TextView) secondRow.findViewById(
                        R.id.settings_numeric_value);
                boundValues[0] = firstValue.getText().toString();
                boundValues[1] = secondValue.getText().toString();
                sliderPreference.setValueFromRuntime(originalValue);
            }
        });
        try {
            assertEquals("Slider callback must update its own row",
                    String.valueOf(targetValue), boundValues[0]);
            assertEquals("Older row callback must not mutate latest bound row",
                    String.valueOf(originalValue), boundValues[1]);
        } finally {
            Launcher.sRestart = originalRestart;
        }
    }

    public void testDurableFlushSkipsUntouchedDisabledGridWrappers()
            throws Exception {
        Context context = getInstrumentation().getTargetContext();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);
        String typeKey = context.getString(R.string.preferences_key_apps_grid_type);
        boolean hadType = sharedPreferences.contains(typeKey);
        String originalType = sharedPreferences.getString(typeKey, null);
        int[] primaryResources = horizontalPrimaryKeyResources();
        boolean[] hadPrimary = containsKeys(context, sharedPreferences, primaryResources);
        int[] originalPrimary = readIntValues(context, sharedPreferences, primaryResources);
        SharedPreferences.Editor setup = sharedPreferences.edit().putString(typeKey, "1");
        removeKeys(context, setup, primaryResources);
        assertTrue(setup.commit());
        final Preferences preferences = getActivity();
        final Method durableFlush = Preferences.class.getDeclaredMethod(
                "flushNumericPreferencesDurably");
        durableFlush.setAccessible(true);
        final Throwable[] failure = new Throwable[1];
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    try {
                        durableFlush.invoke(preferences);
                    } catch (Throwable throwable) {
                        failure[0] = throwable;
                    }
                }
            });
            if (failure[0] != null) {
                fail("Durable flush failed: " + failure[0]);
            }
            for (int keyResource : primaryResources) {
                assertFalse("Untouched disabled grid wrapper must remain absent",
                        sharedPreferences.contains(context.getString(keyResource)));
            }
        } finally {
            restoreStringPreference(sharedPreferences, typeKey, hadType, originalType);
            restoreIntPreferences(context, sharedPreferences, primaryResources, hadPrimary,
                    originalPrimary);
        }
    }

    public void testHorizontalGridSteppersPersistPrimaryAndRuntimeAliases() {
        Context context = getInstrumentation().getTargetContext();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);
        String typeKey = context.getString(R.string.preferences_key_apps_grid_type);
        boolean hadType = sharedPreferences.contains(typeKey);
        String originalType = sharedPreferences.getString(typeKey, null);
        int[] primaryResources = horizontalPrimaryKeyResources();
        int[] aliasResources = horizontalAliasKeyResources();
        boolean[] hadPrimary = containsKeys(context, sharedPreferences, primaryResources);
        boolean[] hadAlias = containsKeys(context, sharedPreferences, aliasResources);
        int[] originalPrimary = readIntValues(context, sharedPreferences, primaryResources);
        int[] originalAlias = readIntValues(context, sharedPreferences, aliasResources);
        int[] initialValues = {4, 4, 5, 3};
        SharedPreferences.Editor setup = sharedPreferences.edit().putString(typeKey, "2");
        putIntValues(context, setup, aliasResources, initialValues);
        assertTrue(setup.commit());
        final Preferences preferences = getActivity();
        try {
            for (int index = 0; index < primaryResources.length; index++) {
                String key = context.getString(primaryResources[index]);
                InlineStepperPreference stepper = (InlineStepperPreference) preferences
                        .findPreference(key);
                View row = bindPreferenceRow(preferences, stepper);
                performClick(row.findViewById(R.id.increment));
            }
            for (int index = 0; index < primaryResources.length; index++) {
                String primaryKey = context.getString(primaryResources[index]);
                String aliasKey = context.getString(aliasResources[index]);
                awaitIntPreference(sharedPreferences, primaryKey, initialValues[index] + 1);
                assertEquals(initialValues[index] + 1,
                        sharedPreferences.getInt(aliasKey, -1));
            }
        } finally {
            restoreStringPreference(sharedPreferences, typeKey, hadType, originalType);
            restoreIntPreferences(context, sharedPreferences, primaryResources, hadPrimary,
                    originalPrimary);
            restoreIntPreferences(context, sharedPreferences, aliasResources, hadAlias,
                    originalAlias);
        }
    }

    public void testOutOfRangeStoredStepperValueIsClampedAndPersisted() {
        Context context = getInstrumentation().getTargetContext();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);
        final String key = context.getString(
                R.string.preferences_key_workspace_content_grid_rows);
        final boolean hadValue = sharedPreferences.contains(key);
        final int originalValue = sharedPreferences.getInt(key, 4);
        assertTrue(sharedPreferences.edit().putInt(key, 99).commit());
        final Preferences preferences = getActivity();
        InlineStepperPreference stepper = (InlineStepperPreference) preferences
                .findPreference(key);
        try {
            assertEquals(stepper.getMax(), stepper.getValue());
            preferences.finish();
            getInstrumentation().waitForIdleSync();
            assertEquals(stepper.getMax(), sharedPreferences.getInt(key, -1));
        } finally {
            restoreIntPreference(sharedPreferences, key, hadValue, originalValue);
        }
    }

    public void testResetClearsStorageAfterCancelingPendingNumericWrites()
            throws Exception {
        Context context = getInstrumentation().getTargetContext();
        final SharedPreferences sharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);
        Map<String, ?> originalValues = new HashMap<String, Object>(
                sharedPreferences.getAll());
        final Preferences preferences = getActivity();
        InlineStepperPreference stepper = (InlineStepperPreference) preferences.findPreference(
                context.getString(R.string.preferences_key_workspace_content_grid_rows));
        View row = bindPreferenceRow(preferences, stepper);
        int actionId = stepper.getValue() < stepper.getMax()
                ? R.id.increment : R.id.decrement;
        performClick(row.findViewById(actionId));
        final Method resetPreferences = Preferences.class.getDeclaredMethod("resetPreferences");
        resetPreferences.setAccessible(true);
        final Throwable[] failure = new Throwable[1];
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    try {
                        resetPreferences.invoke(preferences);
                    } catch (Throwable throwable) {
                        failure[0] = throwable;
                    }
                }
            });
            if (failure[0] != null) {
                fail("Reset failed: " + failure[0]);
            }
            assertTrue("Reset must clear in-memory and on-disk preferences",
                    sharedPreferences.getAll().isEmpty());
            SystemClock.sleep(350L);
            getInstrumentation().waitForIdleSync();
            assertTrue("Canceled debounce must not recreate reset values",
                    sharedPreferences.getAll().isEmpty());
        } finally {
            restorePreferences(sharedPreferences, originalValues);
        }
    }

    public void testNestedNumericScreensAllowChildFocus() {
        Preferences preferences = getActivity();
        PreferenceScreen workspace = (PreferenceScreen) preferences.getPreferenceScreen()
                .getPreference(1);
        ListView workspaceList = openPreferenceScreen(preferences, workspace);
        InlineStepperPreference rows = (InlineStepperPreference) preferences.findPreference(
                preferences.getString(R.string.preferences_key_workspace_content_grid_rows));
        View row = preferenceRow(workspaceList, rows);
        final View increment = row.findViewById(R.id.increment);
        assertTrue("Nested preference list must allow child focus",
                workspaceList.getItemsCanFocus());
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                assertTrue("Inline increment must accept keyboard focus",
                        increment.requestFocus());
            }
        });
        assertTrue(increment.hasFocus());
        workspace.getDialog().dismiss();
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
        ReplacementPreferencesObserver observer =
                new ReplacementPreferencesObserver(preferences);
        preferences.getApplication().registerActivityLifecycleCallbacks(observer);
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
            recreatedPreferences = observer.awaitPreferences();
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
            try {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (hadOriginalAppearance) {
                    editor.putString(appearanceKey, originalAppearance);
                } else {
                    editor.remove(appearanceKey);
                }
                assertTrue("Original appearance preference must be restored", editor.commit());
            } finally {
                preferences.getApplication().unregisterActivityLifecycleCallbacks(observer);
            }
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
        ReplacementPreferencesObserver observer =
                new ReplacementPreferencesObserver(preferences);
        preferences.getApplication().registerActivityLifecycleCallbacks(observer);
        Preferences recreatedPreferences = null;

        try {
            assertTrue("Temporary language must persist", sharedPrefs.edit()
                    .putString(testKey, temporaryValue).commit());
            preferences.runOnUiThread(new Runnable() {
                public void run() {
                    preferences.setRequestedOrientation(rotatedRequestedOrientation);
                }
            });
            recreatedPreferences = observer.awaitPreferences();
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
            try {
                SharedPreferences.Editor editor = sharedPrefs.edit();
                if (hadOriginalValue) {
                    editor.putString(testKey, originalValue);
                } else {
                    editor.remove(testKey);
                }
                assertTrue("Original language preference must be restored", editor.commit());
            } finally {
                try {
                    restoreOrientationState(observer, recreatedPreferences,
                            restoredRequestedOrientation, initialDeviceOrientation,
                            originalRequestedOrientation);
                } finally {
                    preferences.getApplication().unregisterActivityLifecycleCallbacks(observer);
                }
            }
        }
    }

    private ListView openPreferenceScreen(final Preferences preferences,
            final PreferenceScreen screen) {
        final ListView rootList = preferences.getListView();
        final ListAdapter adapter = rootList.getAdapter();
        final int position = preferencePosition(adapter, screen);
        assertTrue("Preference screen must exist in root list", position >= 0);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                View row = adapter.getView(position, null, rootList);
                rootList.performItemClick(row, position, adapter.getItemId(position));
            }
        });
        getInstrumentation().waitForIdleSync();
        Dialog dialog = screen.getDialog();
        assertNotNull("Preference screen dialog must open", dialog);
        ListView listView = (ListView) dialog.findViewById(android.R.id.list);
        assertNotNull("Preference screen list must exist", listView);
        return listView;
    }

    private View preferenceRow(final ListView listView, Preference preference) {
        ListAdapter adapter = listView.getAdapter();
        final int position = preferencePosition(adapter, preference);
        assertTrue("Preference must exist in nested list", position >= 0);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                listView.setSelection(position);
            }
        });
        getInstrumentation().waitForIdleSync();
        int childIndex = position - listView.getFirstVisiblePosition();
        View row = listView.getChildAt(childIndex);
        assertNotNull("Nested preference row must become visible", row);
        return row;
    }

    private static int preferencePosition(ListAdapter adapter, Preference preference) {
        for (int position = 0; position < adapter.getCount(); position++) {
            if (adapter.getItem(position) == preference) {
                return position;
            }
        }
        return -1;
    }

    private void performClick(final View view) {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                assertTrue(view.performClick());
            }
        });
    }

    private void performLongClick(final View view) {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                assertTrue(view.performLongClick());
            }
        });
    }

    private View bindPreferenceRow(final Preferences preferences,
            final Preference preference) {
        final View[] row = new View[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                row[0] = preference.getView(null, preferences.getListView());
            }
        });
        assertNotNull("Preference row must bind", row[0]);
        return row[0];
    }

    private void awaitIntPreference(SharedPreferences preferences, String key,
            int expectedValue) {
        long deadline = SystemClock.uptimeMillis() + 3000L;
        while (preferences.getInt(key, Integer.MIN_VALUE) != expectedValue
                && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(25L);
        }
        assertEquals("Timed out waiting for debounced preference: " + key, expectedValue,
                preferences.getInt(key, Integer.MIN_VALUE));
    }

    private static boolean storedValueMatches(SharedPreferences preferences, String key,
            boolean hadValue, int originalValue) {
        if (preferences.contains(key) != hadValue) {
            return false;
        }
        return !hadValue || preferences.getInt(key, Integer.MIN_VALUE) == originalValue;
    }

    private static int[] horizontalPrimaryKeyResources() {
        return new int[] {
                R.string.preferences_key_apps_grid_content_columns_port,
                R.string.preferences_key_apps_grid_content_rows_port,
                R.string.preferences_key_apps_grid_content_columns_land,
                R.string.preferences_key_apps_grid_content_rows_land
        };
    }

    private static int[] horizontalAliasKeyResources() {
        return new int[] {
                R.string.preferences_key_apps_grid_horizontal_paging_content_columns_port,
                R.string.preferences_key_apps_grid_horizontal_paging_content_rows_port,
                R.string.preferences_key_apps_grid_horizontal_paging_content_columns_land,
                R.string.preferences_key_apps_grid_horizontal_paging_content_rows_land
        };
    }

    private static boolean[] containsKeys(Context context, SharedPreferences preferences,
            int[] keyResources) {
        boolean[] result = new boolean[keyResources.length];
        for (int index = 0; index < keyResources.length; index++) {
            result[index] = preferences.contains(context.getString(keyResources[index]));
        }
        return result;
    }

    private static int[] readIntValues(Context context, SharedPreferences preferences,
            int[] keyResources) {
        int[] result = new int[keyResources.length];
        for (int index = 0; index < keyResources.length; index++) {
            result[index] = preferences.getInt(context.getString(keyResources[index]), 0);
        }
        return result;
    }

    private static void putIntValues(Context context, SharedPreferences.Editor editor,
            int[] keyResources, int[] values) {
        for (int index = 0; index < keyResources.length; index++) {
            editor.putInt(context.getString(keyResources[index]), values[index]);
        }
    }

    private static void removeKeys(Context context, SharedPreferences.Editor editor,
            int[] keyResources) {
        for (int keyResource : keyResources) {
            editor.remove(context.getString(keyResource));
        }
    }

    private static void restoreIntPreferences(Context context, SharedPreferences preferences,
            int[] keyResources, boolean[] hadValues, int[] values) {
        SharedPreferences.Editor editor = preferences.edit();
        for (int index = 0; index < keyResources.length; index++) {
            String key = context.getString(keyResources[index]);
            if (hadValues[index]) {
                editor.putInt(key, values[index]);
            } else {
                editor.remove(key);
            }
        }
        assertTrue(editor.commit());
    }

    private static void restorePreferences(SharedPreferences preferences,
            Map<String, ?> values) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                editor.putBoolean(entry.getKey(), ((Boolean) value).booleanValue());
            } else if (value instanceof Integer) {
                editor.putInt(entry.getKey(), ((Integer) value).intValue());
            } else if (value instanceof Long) {
                editor.putLong(entry.getKey(), ((Long) value).longValue());
            } else if (value instanceof Float) {
                editor.putFloat(entry.getKey(), ((Float) value).floatValue());
            } else if (value instanceof String) {
                editor.putString(entry.getKey(), (String) value);
            } else if (value instanceof Set) {
                editor.putStringSet(entry.getKey(), (Set<String>) value);
            } else {
                fail("Unsupported SharedPreferences value: " + value);
            }
        }
        assertTrue(editor.commit());
    }

    private static void restoreIntPreference(SharedPreferences preferences, String key,
            boolean hadValue, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        if (hadValue) {
            editor.putInt(key, value);
        } else {
            editor.remove(key);
        }
        assertTrue(editor.commit());
    }

    private static void restoreStringPreference(SharedPreferences preferences, String key,
            boolean hadValue, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        if (hadValue) {
            editor.putString(key, value);
        } else {
            editor.remove(key);
        }
        assertTrue(editor.commit());
    }

    private void restoreOrientationState(ReplacementPreferencesObserver observer,
            Preferences preferences, int requestedOrientation,
            int expectedConfigurationOrientation, int originalRequestedOrientation) {
        Preferences restorationTarget = preferences != null
                ? preferences : observer.latestPreferencesOrOriginal();
        if (restorationTarget.isDestroyed()) {
            restorationTarget = observer.awaitActiveReplacement();
        }
        assertNotNull("Orientation cleanup requires an active Preferences", restorationTarget);
        int currentOrientation = restorationTarget.getResources().getConfiguration().orientation;
        if (currentOrientation == expectedConfigurationOrientation) {
            restoreRequestedOrientation(restorationTarget, originalRequestedOrientation);
            return;
        }
        restoreOrientation(restorationTarget, requestedOrientation,
                expectedConfigurationOrientation, originalRequestedOrientation);
    }

    private void restoreOrientation(final Preferences preferences, final int requestedOrientation,
            int expectedConfigurationOrientation, final int originalRequestedOrientation) {
        ReplacementPreferencesObserver observer =
                new ReplacementPreferencesObserver(preferences);
        preferences.getApplication().registerActivityLifecycleCallbacks(observer);
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    preferences.setRequestedOrientation(requestedOrientation);
                }
            });
            Preferences restored = observer.awaitPreferences();
            assertNotNull("Restoring orientation must recreate Preferences", restored);
            getInstrumentation().waitForIdleSync();
            assertEquals("Preferences must return to initial orientation",
                    expectedConfigurationOrientation,
                    restored.getResources().getConfiguration().orientation);
            restoreRequestedOrientation(restored, originalRequestedOrientation);
        } finally {
            preferences.getApplication().unregisterActivityLifecycleCallbacks(observer);
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

    private static final class ReplacementPreferencesObserver
            implements Application.ActivityLifecycleCallbacks {
        private final Preferences originalPreferences;
        private Preferences replacementPreferences;

        ReplacementPreferencesObserver(Preferences originalPreferences) {
            this.originalPreferences = originalPreferences;
        }

        public synchronized void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (!(activity instanceof Preferences) || activity == this.originalPreferences) {
                return;
            }
            this.replacementPreferences = (Preferences) activity;
            notifyAll();
        }

        public void onActivityStarted(Activity activity) {
        }

        public void onActivityResumed(Activity activity) {
        }

        public void onActivityPaused(Activity activity) {
        }

        public void onActivityStopped(Activity activity) {
        }

        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        public void onActivityDestroyed(Activity activity) {
        }

        synchronized Preferences awaitPreferences() {
            long deadline = SystemClock.uptimeMillis()
                    + ACTIVITY_RECREATION_TIMEOUT_MILLIS;
            while (this.replacementPreferences == null) {
                long remaining = deadline - SystemClock.uptimeMillis();
                if (remaining <= 0L) {
                    return null;
                }
                try {
                    wait(remaining);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return this.replacementPreferences;
        }

        synchronized Preferences latestPreferencesOrOriginal() {
            if (this.replacementPreferences != null) {
                return this.replacementPreferences;
            }
            return this.originalPreferences;
        }

        synchronized Preferences awaitActiveReplacement() {
            long deadline = SystemClock.uptimeMillis()
                    + ACTIVITY_RECREATION_TIMEOUT_MILLIS;
            boolean interrupted = Thread.interrupted();
            try {
                while (this.replacementPreferences == null
                        || this.replacementPreferences.isDestroyed()) {
                    long remaining = deadline - SystemClock.uptimeMillis();
                    if (remaining <= 0L) {
                        return null;
                    }
                    try {
                        wait(remaining);
                    } catch (InterruptedException exception) {
                        interrupted = true;
                    }
                }
                return this.replacementPreferences;
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static final class PausedActivityObserver
            implements Application.ActivityLifecycleCallbacks {
        private final Activity mTarget;
        private boolean mPaused;

        PausedActivityObserver(Activity target) {
            mTarget = target;
        }

        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        public void onActivityStarted(Activity activity) {
        }

        public void onActivityResumed(Activity activity) {
        }

        public synchronized void onActivityPaused(Activity activity) {
            if (activity != mTarget) {
                return;
            }
            mPaused = true;
            notifyAll();
        }

        public void onActivityStopped(Activity activity) {
        }

        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        public void onActivityDestroyed(Activity activity) {
        }

        synchronized boolean awaitPause() {
            long deadline = SystemClock.uptimeMillis()
                    + ACTIVITY_RECREATION_TIMEOUT_MILLIS;
            while (!mPaused) {
                long remaining = deadline - SystemClock.uptimeMillis();
                if (remaining <= 0L) {
                    return false;
                }
                try {
                    wait(remaining);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
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
