package org.zeam;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import java.io.File;

public class Preferences extends PreferenceActivity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        bindLanguagePreference();
        Preference.OnPreferenceChangeListener restartChangeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object object) {
                Launcher.sRestart = true;
                return true;
            }
        };
        Preference.OnPreferenceChangeListener restartLoadersChangeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object object) {
                Launcher.sRestartLoaders = true;
                return true;
            }
        };
        findPreference(getString(R.string.preferences_key_general_selector_colour_pressed)).setOnPreferenceChangeListener(restartChangeListener);
        findPreference(getString(R.string.preferences_key_general_selector_colour_focused)).setOnPreferenceChangeListener(restartChangeListener);
        final DialogSeekBarPreference defaultScreenPreference = (DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_workspace_default_screen));
        ((DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_workspace_number_of_screens))).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                int numberOfScreens = ((Integer) value).intValue() + 1;
                defaultScreenPreference.setMax(numberOfScreens);
                if (numberOfScreens < PreferencesUtil.getDefaultScreen(Preferences.this)) {
                    SharedPreferences.Editor editor = defaultScreenPreference.getEditor();
                    editor.putInt(Preferences.this.getString(R.string.preferences_key_workspace_number_of_screens), numberOfScreens);
                    editor.commit();
                }
                Launcher.sRestart = true;
                return true;
            }
        });
        defaultScreenPreference.setMax(PreferencesUtil.getNumberOfScreens(this));
        defaultScreenPreference.setOnPreferenceChangeListener(restartChangeListener);
        ((DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_workspace_content_grid_rows))).setOnPreferenceChangeListener(restartChangeListener);
        ((DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_workspace_content_grid_columns))).setOnPreferenceChangeListener(restartChangeListener);
        findPreference(getString(R.string.preferences_key_workspace_content_grid_auto_fit)).setOnPreferenceChangeListener(restartChangeListener);
        findPreference(getString(R.string.preferences_key_workspace_show_shortcut_titles)).setOnPreferenceChangeListener(restartLoadersChangeListener);
        findPreference(getString(R.string.preferences_key_workspace_manage_wallpaper)).setOnPreferenceChangeListener(restartChangeListener);
        loadAppsGridRowsColumns(PreferencesUtil.getAppsGridType(this));
        findPreference(getString(R.string.preferences_key_apps_grid_type)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object object) {
                Preferences.this.loadAppsGridRowsColumns(Integer.parseInt(String.valueOf(object)));
                Launcher.sRestart = true;
                return true;
            }
        });
        findPreference(getString(R.string.preferences_key_apps_grid_bg_alpha)).setOnPreferenceChangeListener(restartChangeListener);
        findPreference(getString(R.string.preferences_key_dock_item_width)).setOnPreferenceChangeListener(restartChangeListener);
        findPreference(getString(R.string.preferences_key_dock_item_alignment)).setOnPreferenceChangeListener(restartChangeListener);
        final Preference resetTo = findPreference(getString(R.string.preferences_key_dock_reset_to));
        findPreference(getString(R.string.preferences_key_dock_reset_home)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object valueObject) {
                resetTo.setEnabled(((Boolean) valueObject).booleanValue());
                return true;
            }
        });
        resetTo.setEnabled(PreferencesUtil.getDockResetHome(this));
        findPreference(getString(R.string.preferences_key_restart)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Preferences.restart();
                return false;
            }
        });
        findPreference(getString(R.string.preferences_key_reset)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(Preferences.this).setMessage(Preferences.this.getString(R.string.preferences_confirm_reset)).setPositiveButton(Preferences.this.getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Preferences.this.resetAlertRestart();
                    }
                }).setNegativeButton(Preferences.this.getString(R.string.button_no), (DialogInterface.OnClickListener) null).show();
                return false;
            }
        });
        Preference applicationPreference = findPreference(getString(R.string.preferences_key_application));
        applicationPreference.setTitle(getString(
                R.string.preferences_application_version_format,
                getString(R.string.application_name),
                LauncherApplication.getVersionName(this)));
        applicationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Preferences.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://zeam.org/")));
                return true;
            }
        });
    }

    private void bindLanguagePreference() {
        Preference languagePreference = findPreference(getString(R.string.preferences_key_application_language));
        languagePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                if (LocaleUtil.persistSelectedLanguage(Preferences.this, String.valueOf(value))) {
                    Preferences.restart();
                }
                return false;
            }
        });
    }

    /* access modifiers changed from: private */
    public void loadAppsGridRowsColumns(int appsGridType) {
        final DialogSeekBarPreference appsGridContentRowsPortrait = (DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_apps_grid_content_rows_port));
        final DialogSeekBarPreference appsGridContentColumnsPortrait = (DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_apps_grid_content_columns_port));
        final DialogSeekBarPreference appsGridContentRowsLandscape = (DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_apps_grid_content_rows_land));
        final DialogSeekBarPreference appsGridContentColumnsLandscape = (DialogSeekBarPreference) findPreference(getString(R.string.preferences_key_apps_grid_content_columns_land));
        if (appsGridType == 1) {
            appsGridContentRowsPortrait.setEnabled(false);
            appsGridContentRowsLandscape.setEnabled(false);
            appsGridContentColumnsPortrait.setEnabled(true);
            appsGridContentColumnsLandscape.setEnabled(true);
            appsGridContentColumnsPortrait.setMin(4);
            appsGridContentColumnsPortrait.setMax(6);
            appsGridContentColumnsLandscape.setMin(4);
            appsGridContentColumnsLandscape.setMax(6);
            appsGridContentColumnsPortrait.setValue(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsPortrait(this) - appsGridContentColumnsPortrait.getMin());
            appsGridContentColumnsLandscape.setValue(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsLandscape(this) - appsGridContentColumnsLandscape.getMin());
            appsGridContentRowsPortrait.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) null);
            appsGridContentRowsLandscape.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) null);
            appsGridContentColumnsPortrait.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(Preferences.this).edit();
                    editor.putInt(Preferences.this.getString(R.string.preferences_key_apps_grid_vertical_scrolling_content_columns_port), ((Integer) value).intValue() + appsGridContentColumnsPortrait.getMin());
                    editor.commit();
                    return true;
                }
            });
            appsGridContentColumnsLandscape.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(Preferences.this).edit();
                    editor.putInt(Preferences.this.getString(R.string.preferences_key_apps_grid_vertical_scrolling_content_columns_land), ((Integer) value).intValue() + appsGridContentColumnsLandscape.getMin());
                    editor.commit();
                    return true;
                }
            });
        } else if (appsGridType == 2) {
            appsGridContentRowsPortrait.setEnabled(true);
            appsGridContentColumnsPortrait.setEnabled(true);
            appsGridContentRowsLandscape.setEnabled(true);
            appsGridContentColumnsLandscape.setEnabled(true);
            appsGridContentRowsPortrait.setMin(1);
            appsGridContentRowsPortrait.setMax(6);
            appsGridContentColumnsPortrait.setMin(1);
            appsGridContentColumnsPortrait.setMax(6);
            appsGridContentRowsLandscape.setMin(1);
            appsGridContentRowsLandscape.setMax(5);
            appsGridContentColumnsLandscape.setMin(1);
            appsGridContentColumnsLandscape.setMax(8);
            appsGridContentRowsPortrait.setValue(PreferencesUtil.getAppsGridHorizontalPagingContentRowsPortrait(this) - appsGridContentRowsPortrait.getMin());
            appsGridContentColumnsPortrait.setValue(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsPortrait(this) - appsGridContentColumnsPortrait.getMin());
            appsGridContentRowsLandscape.setValue(PreferencesUtil.getAppsGridHorizontalPagingContentRowsLandscape(this) - appsGridContentRowsLandscape.getMin());
            appsGridContentColumnsLandscape.setValue(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsLandscape(this) - appsGridContentColumnsLandscape.getMin());
            appsGridContentRowsPortrait.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(Preferences.this).edit();
                    editor.putInt(Preferences.this.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_rows_port), ((Integer) value).intValue() + appsGridContentRowsPortrait.getMin());
                    editor.commit();
                    return true;
                }
            });
            appsGridContentColumnsPortrait.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(Preferences.this).edit();
                    editor.putInt(Preferences.this.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_columns_port), ((Integer) value).intValue() + appsGridContentColumnsPortrait.getMin());
                    editor.commit();
                    return true;
                }
            });
            appsGridContentRowsLandscape.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(Preferences.this).edit();
                    editor.putInt(Preferences.this.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_rows_land), ((Integer) value).intValue() + appsGridContentRowsLandscape.getMin());
                    editor.commit();
                    return true;
                }
            });
            appsGridContentColumnsLandscape.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(Preferences.this).edit();
                    editor.putInt(Preferences.this.getString(R.string.preferences_key_apps_grid_horizontal_paging_content_columns_land), ((Integer) value).intValue() + appsGridContentColumnsLandscape.getMin());
                    editor.commit();
                    return true;
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void resetAlertRestart() {
        getPreferencesFile(this).delete();
        alertRestart(this);
    }

    static void alertRestart(Context context) {
        new AlertDialog.Builder(context).setCancelable(false).setMessage(context.getString(R.string.preferences_alert_dialog_restart_message)).setPositiveButton(context.getString(R.string.preferences_alert_dialog_restart_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Preferences.restart();
            }
        }).show();
    }

    static void restart() {
        Process.killProcess(Process.myPid());
    }

    static File getPreferencesFile(Context context) {
        return new File(String.valueOf(getApplicationDataPath(context)) + "shared_prefs/" + context.getPackageName() + "_preferences.xml");
    }

    static String getApplicationDataPath(Context context) {
        return "/data/data/" + context.getPackageName() + "/";
    }
}
