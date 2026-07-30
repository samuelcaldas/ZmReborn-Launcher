package org.zmreborn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.io.File;
import java.util.ArrayList;
import org.zmreborn.theme.WallpaperColorExtractor;

public class Preferences extends PreferenceActivity {
    private final ArrayList<DebouncedIntegerPreference> mNumericPreferences =
            new ArrayList<DebouncedIntegerPreference>(9);
    private SettingsSummaryBinder mSummaryBinder;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        configureList();
        bindLanguagePreference();
        bindAppearancePreference();
        bindGeneralPreferences();
        bindWorkspacePreferences();
        bindAppsGridPreferences();
        bindDockPreferences();
        bindApplicationPreferences();
        mSummaryBinder = SettingsSummaryBinder.attach(getPreferenceScreen(),
                PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference instanceof PreferenceScreen) {
            configureNestedListWhenOpened((PreferenceScreen) preference);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected void onPause() {
        flushNumericPreferences();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        flushNumericPreferences();
        if (mSummaryBinder != null) {
            mSummaryBinder.detach();
            mSummaryBinder = null;
        }
        super.onDestroy();
    }

    private void configureList() {
        ListView list = getListView();
        list.setBackgroundColor(WallpaperColorExtractor.getSurface(this));
        list.setDivider(null);
        list.setDividerHeight(0);
        configureChildFocus(list);
    }

    private void configureNestedListWhenOpened(final PreferenceScreen screen) {
        getListView().post(new Runnable() {
            public void run() {
                if (screen.getDialog() == null) {
                    throw new IllegalStateException("Preference screen dialog did not open");
                }
                View list = screen.getDialog().findViewById(android.R.id.list);
                if (!(list instanceof ListView)) {
                    throw new IllegalStateException("Preference screen dialog has no list");
                }
                configureChildFocus((ListView) list);
            }
        });
    }

    private static void configureChildFocus(ListView list) {
        list.setItemsCanFocus(true);
        list.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    private void bindGeneralPreferences() {
        Preference.OnPreferenceChangeListener restart = createRestartChangeListener();
        findPreference(getString(R.string.preferences_key_general_selector_colour_pressed))
                .setOnPreferenceChangeListener(restart);
        findPreference(getString(R.string.preferences_key_general_selector_colour_focused))
                .setOnPreferenceChangeListener(restart);
    }

    private void bindWorkspacePreferences() {
        final InlineStepperPreference defaultScreenPreference = stepperPreference(
                R.string.preferences_key_workspace_default_screen);
        InlineStepperPreference screenCountPreference = stepperPreference(
                R.string.preferences_key_workspace_number_of_screens);
        screenCountPreference.setOnPreferenceChangeListener(
                createScreenCountChangeListener(defaultScreenPreference));
        defaultScreenPreference.setMax(PreferencesUtil.getNumberOfScreens(this));
        defaultScreenPreference.setOnPreferenceChangeListener(createRestartChangeListener());
        stepperPreference(R.string.preferences_key_workspace_content_grid_rows)
                .setOnPreferenceChangeListener(createRestartChangeListener());
        stepperPreference(R.string.preferences_key_workspace_content_grid_columns)
                .setOnPreferenceChangeListener(createRestartChangeListener());
        findPreference(getString(R.string.preferences_key_workspace_content_grid_auto_fit))
                .setOnPreferenceChangeListener(createRestartChangeListener());
        findPreference(getString(R.string.preferences_key_workspace_show_shortcut_titles))
                .setOnPreferenceChangeListener(createRestartLoadersChangeListener());
        findPreference(getString(R.string.preferences_key_workspace_manage_wallpaper))
                .setOnPreferenceChangeListener(createRestartChangeListener());
    }

    private Preference.OnPreferenceChangeListener createScreenCountChangeListener(
            final InlineStepperPreference defaultScreenPreference) {
        return new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                int numberOfScreens = ((Integer) value).intValue();
                defaultScreenPreference.setMax(numberOfScreens);
                Launcher.sRestart = true;
                return true;
            }
        };
    }

    private void bindAppsGridPreferences() {
        loadAppsGridRowsColumns(PreferencesUtil.getAppsGridType(this));
        findPreference(getString(R.string.preferences_key_apps_grid_type))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        flushAppsGridPreferences();
                        loadAppsGridRowsColumns(Integer.parseInt(String.valueOf(value)));
                        Launcher.sRestart = true;
                        return true;
                    }
                });
        sliderPreference(R.string.preferences_key_apps_grid_bg_alpha)
                .setOnPreferenceChangeListener(createRestartChangeListener());
        findPreference(getString(R.string.preferences_key_apps_grid_density))
                .setOnPreferenceChangeListener(createRestartChangeListener());
    }

    private void bindDockPreferences() {
        findPreference(getString(R.string.preferences_key_dock_item_width))
                .setOnPreferenceChangeListener(createRestartChangeListener());
        findPreference(getString(R.string.preferences_key_dock_item_alignment))
                .setOnPreferenceChangeListener(createRestartChangeListener());
        final Preference resetTo = findPreference(
                getString(R.string.preferences_key_dock_reset_to));
        findPreference(getString(R.string.preferences_key_dock_reset_home))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        resetTo.setEnabled(((Boolean) value).booleanValue());
                        return true;
                    }
                });
        resetTo.setEnabled(PreferencesUtil.getDockResetHome(this));
    }

    private void bindApplicationPreferences() {
        bindRestartPreference();
        bindResetPreference();
        Preference applicationPreference = findPreference(
                getString(R.string.preferences_key_application));
        applicationPreference.setTitle(getString(
                R.string.preferences_application_version_format,
                getString(R.string.application_name),
                LauncherApplication.getVersionName(this)));
        applicationPreference.setSelectable(false);
    }

    private void bindRestartPreference() {
        findPreference(getString(R.string.preferences_key_restart))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        flushNumericPreferencesDurably();
                        Preferences.restart();
                        return false;
                    }
                });
    }

    private void bindResetPreference() {
        findPreference(getString(R.string.preferences_key_reset))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        showResetConfirmation();
                        return false;
                    }
                });
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.preferences_confirm_reset))
                .setPositiveButton(getString(R.string.button_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                resetAlertRestart();
                            }
                        })
                .setNegativeButton(getString(R.string.button_no), null)
                .show();
    }

    private Preference.OnPreferenceChangeListener createRestartChangeListener() {
        return new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                Launcher.sRestart = true;
                return true;
            }
        };
    }

    private Preference.OnPreferenceChangeListener createRestartLoadersChangeListener() {
        return new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                Launcher.sRestartLoaders = true;
                return true;
            }
        };
    }

    private void bindLanguagePreference() {
        Preference languagePreference = findPreference(
                getString(R.string.preferences_key_application_language));
        languagePreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        if (LocaleUtil.persistSelectedLanguage(
                                Preferences.this, String.valueOf(value))) {
                            flushNumericPreferencesDurably();
                            Preferences.restart();
                        }
                        return false;
                    }
                });
    }

    private void bindAppearancePreference() {
        Preference appearancePreference = findPreference(
                getString(R.string.preferences_key_application_appearance));
        appearancePreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        if (Appearance.persistSelectedAppearance(
                                Preferences.this, String.valueOf(value))) {
                            recreate();
                        }
                        return false;
                    }
                });
    }

    /* access modifiers changed from: private */
    public void loadAppsGridRowsColumns(int appsGridType) {
        validateAppsGridType(appsGridType);
        Preference appsGridDensity = findPreference(
                getString(R.string.preferences_key_apps_grid_density));
        appsGridDensity.setEnabled(appsGridType == 1);
        InlineStepperPreference appsGridContentRowsPortrait = stepperPreference(
                R.string.preferences_key_apps_grid_content_rows_port);
        InlineStepperPreference appsGridContentColumnsPortrait = stepperPreference(
                R.string.preferences_key_apps_grid_content_columns_port);
        InlineStepperPreference appsGridContentRowsLandscape = stepperPreference(
                R.string.preferences_key_apps_grid_content_rows_land);
        InlineStepperPreference appsGridContentColumnsLandscape = stepperPreference(
                R.string.preferences_key_apps_grid_content_columns_land);
        if (appsGridType == 1) {
            disableAppsGridSteppers(appsGridContentRowsPortrait,
                    appsGridContentColumnsPortrait, appsGridContentRowsLandscape,
                    appsGridContentColumnsLandscape);
            return;
        }
        enableHorizontalAppsGridSteppers(appsGridContentRowsPortrait,
                appsGridContentColumnsPortrait, appsGridContentRowsLandscape,
                appsGridContentColumnsLandscape);
    }

    private void disableAppsGridSteppers(InlineStepperPreference appsGridContentRowsPortrait,
            InlineStepperPreference appsGridContentColumnsPortrait,
            InlineStepperPreference appsGridContentRowsLandscape,
            InlineStepperPreference appsGridContentColumnsLandscape) {
        appsGridContentRowsPortrait.setEnabled(false);
        appsGridContentColumnsPortrait.setEnabled(false);
        appsGridContentRowsLandscape.setEnabled(false);
        appsGridContentColumnsLandscape.setEnabled(false);
        clearAppsGridBinding(appsGridContentRowsPortrait);
        clearAppsGridBinding(appsGridContentColumnsPortrait);
        clearAppsGridBinding(appsGridContentRowsLandscape);
        clearAppsGridBinding(appsGridContentColumnsLandscape);
    }

    private void enableHorizontalAppsGridSteppers(
            InlineStepperPreference appsGridContentRowsPortrait,
            InlineStepperPreference appsGridContentColumnsPortrait,
            InlineStepperPreference appsGridContentRowsLandscape,
            InlineStepperPreference appsGridContentColumnsLandscape) {
        appsGridContentRowsPortrait.setEnabled(true);
        appsGridContentColumnsPortrait.setEnabled(true);
        appsGridContentRowsLandscape.setEnabled(true);
        appsGridContentColumnsLandscape.setEnabled(true);
        configureAppsGridRanges(appsGridContentRowsPortrait,
                appsGridContentColumnsPortrait, appsGridContentRowsLandscape,
                appsGridContentColumnsLandscape);
        bindAppsGridRuntimeValues(appsGridContentRowsPortrait,
                appsGridContentColumnsPortrait, appsGridContentRowsLandscape,
                appsGridContentColumnsLandscape);
    }

    private void configureAppsGridRanges(InlineStepperPreference appsGridContentRowsPortrait,
            InlineStepperPreference appsGridContentColumnsPortrait,
            InlineStepperPreference appsGridContentRowsLandscape,
            InlineStepperPreference appsGridContentColumnsLandscape) {
        appsGridContentRowsPortrait.setMin(1);
        appsGridContentRowsPortrait.setMax(6);
        appsGridContentColumnsPortrait.setMin(1);
        appsGridContentColumnsPortrait.setMax(6);
        appsGridContentRowsLandscape.setMin(1);
        appsGridContentRowsLandscape.setMax(5);
        appsGridContentColumnsLandscape.setMin(1);
        appsGridContentColumnsLandscape.setMax(8);
    }

    private void bindAppsGridRuntimeValues(
            InlineStepperPreference appsGridContentRowsPortrait,
            InlineStepperPreference appsGridContentColumnsPortrait,
            InlineStepperPreference appsGridContentRowsLandscape,
            InlineStepperPreference appsGridContentColumnsLandscape) {
        bindAppsGridAlias(appsGridContentRowsPortrait,
                PreferencesUtil.getAppsGridHorizontalPagingContentRowsPortrait(this),
                R.string.preferences_key_apps_grid_horizontal_paging_content_rows_port);
        bindAppsGridAlias(appsGridContentColumnsPortrait,
                PreferencesUtil.getAppsGridHorizontalPagingContentColumnsPortrait(this),
                R.string.preferences_key_apps_grid_horizontal_paging_content_columns_port);
        bindAppsGridAlias(appsGridContentRowsLandscape,
                PreferencesUtil.getAppsGridHorizontalPagingContentRowsLandscape(this),
                R.string.preferences_key_apps_grid_horizontal_paging_content_rows_land);
        bindAppsGridAlias(appsGridContentColumnsLandscape,
                PreferencesUtil.getAppsGridHorizontalPagingContentColumnsLandscape(this),
                R.string.preferences_key_apps_grid_horizontal_paging_content_columns_land);
    }

    private void bindAppsGridAlias(InlineStepperPreference preference, int runtimeValue,
            int aliasKeyResource) {
        final String aliasKey = getString(aliasKeyResource);
        preference.setValueFromRuntime(runtimeValue);
        preference.setOnPreferenceChangeListener(null);
        preference.setAdditionalValueWriter(
                new DebouncedIntegerPreference.AdditionalValueWriter() {
                    public void append(SharedPreferences.Editor editor, int value) {
                        editor.putInt(aliasKey, value);
                    }
                });
    }

    private static void clearAppsGridBinding(InlineStepperPreference preference) {
        preference.setOnPreferenceChangeListener(null);
        preference.setAdditionalValueWriter(null);
    }

    private static void validateAppsGridType(int appsGridType) {
        if (appsGridType != 1 && appsGridType != 2) {
            throw new IllegalArgumentException("Unsupported applications grid type: "
                    + appsGridType);
        }
    }

    private InlineStepperPreference stepperPreference(int keyResource) {
        Preference preference = findPreference(getString(keyResource));
        if (!(preference instanceof InlineStepperPreference)) {
            throw new IllegalStateException("Expected inline stepper for key: "
                    + getString(keyResource));
        }
        registerNumericPreference((InlineStepperPreference) preference);
        return (InlineStepperPreference) preference;
    }

    private InlineSliderPreference sliderPreference(int keyResource) {
        Preference preference = findPreference(getString(keyResource));
        if (!(preference instanceof InlineSliderPreference)) {
            throw new IllegalStateException("Expected inline slider for key: "
                    + getString(keyResource));
        }
        registerNumericPreference((InlineSliderPreference) preference);
        return (InlineSliderPreference) preference;
    }

    private void registerNumericPreference(DebouncedIntegerPreference preference) {
        if (!mNumericPreferences.contains(preference)) {
            mNumericPreferences.add(preference);
        }
    }

    private void flushAppsGridPreferences() {
        stepperPreference(R.string.preferences_key_apps_grid_content_rows_port)
                .flushPendingValue();
        stepperPreference(R.string.preferences_key_apps_grid_content_columns_port)
                .flushPendingValue();
        stepperPreference(R.string.preferences_key_apps_grid_content_rows_land)
                .flushPendingValue();
        stepperPreference(R.string.preferences_key_apps_grid_content_columns_land)
                .flushPendingValue();
    }

    private void flushNumericPreferences() {
        for (DebouncedIntegerPreference preference : mNumericPreferences) {
            preference.flushPendingValue();
        }
    }

    private void flushNumericPreferencesDurably() {
        for (DebouncedIntegerPreference preference : mNumericPreferences) {
            preference.flushPendingValueDurably();
        }
    }

    private void cancelPendingNumericPreferences() {
        for (DebouncedIntegerPreference preference : mNumericPreferences) {
            preference.cancelPendingValue();
        }
    }

    void resetPreferences() {
        cancelPendingNumericPreferences();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.edit().clear().commit()) {
            throw new IllegalStateException("Unable to reset launcher preferences");
        }
    }

    /* access modifiers changed from: private */
    public void resetAlertRestart() {
        resetPreferences();
        alertRestart(this);
    }

    static void alertRestart(Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setMessage(context.getString(R.string.preferences_alert_dialog_restart_message))
                .setPositiveButton(
                        context.getString(R.string.preferences_alert_dialog_restart_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Preferences.restart();
                            }
                        })
                .show();
    }

    static void restart() {
        Process.killProcess(Process.myPid());
    }

    static File getPreferencesFile(Context context) {
        return new File(String.valueOf(getApplicationDataPath(context)) + "shared_prefs/"
                + context.getPackageName() + "_preferences.xml");
    }

    static String getApplicationDataPath(Context context) {
        return "/data/data/" + context.getPackageName() + "/";
    }
}
