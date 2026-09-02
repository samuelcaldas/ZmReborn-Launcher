package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

/**
 * Manages application light, dark, and system theme appearance preferences and configuration.
 */
final class Appearance {
    static final String SYSTEM = "system";
    static final String LIGHT = "light";
    static final String DARK = "dark";

    enum Brightness {
        SYSTEM,
        LIGHT,
        DARK
    }

    private Appearance() {
    }

    /**
     * Retrieves the configured appearance setting from shared preferences.
     */
    static String getSelectedAppearance(Context context) {
        requireContext(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.preferences_key_application_appearance);
        String defaultValue = context.getString(
                R.string.preferences_default_application_appearance);
        return normalizeAppearance(preferences.getString(key, defaultValue));
    }

    /**
     * Persists the requested appearance setting to shared preferences.
     */
    static boolean persistSelectedAppearance(Context context, String requestedAppearance) {
        requireContext(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.preferences_key_application_appearance);
        String defaultValue = context.getString(
                R.string.preferences_default_application_appearance);
        String storedAppearance = preferences.getString(key, defaultValue);
        String normalizedAppearance = normalizeAppearance(requestedAppearance);
        if (normalizedAppearance.equals(storedAppearance)) {
            return false;
        }
        boolean effectiveAppearanceChanged = !normalizedAppearance.equals(
                normalizeAppearance(storedAppearance));
        commitAppearance(preferences, key, normalizedAppearance);
        return effectiveAppearanceChanged;
    }

    /**
     * Normalizes an appearance key string to a known constant.
     */
    static String normalizeAppearance(String appearance) {
        if (appearance == null) {
            return SYSTEM;
        }
        String normalized = appearance.trim();
        if (LIGHT.equalsIgnoreCase(normalized)) {
            return LIGHT;
        }
        if (DARK.equalsIgnoreCase(normalized)) {
            return DARK;
        }
        return SYSTEM;
    }

    /**
     * Returns the brightness enum corresponding to an appearance setting.
     */
    static Brightness brightnessFor(String appearance) {
        String normalizedAppearance = normalizeAppearance(appearance);
        if (LIGHT.equals(normalizedAppearance)) {
            return Brightness.LIGHT;
        }
        if (DARK.equals(normalizedAppearance)) {
            return Brightness.DARK;
        }
        return Brightness.SYSTEM;
    }

    /**
     * Applies brightness mode to a configuration instance.
     */
    static Configuration applyBrightness(Configuration configuration, Brightness brightness) {
        requireConfiguration(configuration);
        Configuration updatedConfiguration = new Configuration(configuration);
        updatedConfiguration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | effectiveNightMode(brightness, configuration);
        return updatedConfiguration;
    }

    /**
     * Applies system night mode configuration.
     */
    static Configuration applySystemNightMode(
            Configuration configuration, Configuration systemConfiguration) {
        requireConfiguration(configuration);
        requireConfiguration(systemConfiguration);
        Configuration updatedConfiguration = new Configuration(configuration);
        updatedConfiguration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | effectiveNightMode(Brightness.SYSTEM, systemConfiguration);
        return updatedConfiguration;
    }

    /**
     * Computes the effective night mode integer for a brightness setting.
     */
    static int effectiveNightMode(Brightness brightness, Configuration configuration) {
        requireConfiguration(configuration);
        if (brightness == Brightness.LIGHT) {
            return Configuration.UI_MODE_NIGHT_NO;
        }
        if (brightness == Brightness.DARK) {
            return Configuration.UI_MODE_NIGHT_YES;
        }
        return configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    /**
     * Computes the theme fingerprint string for the active context.
     */
    static String fingerprint(Context context) {
        requireContext(context);
        return fingerprint(getSelectedAppearance(context), context.getResources().getConfiguration());
    }

    /**
     * Computes the theme fingerprint string for appearance and configuration.
     */
    static String fingerprint(String appearance, Configuration configuration) {
        requireConfiguration(configuration);
        String normalizedAppearance = normalizeAppearance(appearance);
        int nightMode = effectiveNightMode(brightnessFor(normalizedAppearance), configuration);
        return normalizedAppearance + '|' + nightMode;
    }

    private static void commitAppearance(SharedPreferences preferences, String key, String appearance) {
        if (!preferences.edit().putString(key, appearance).commit()) {
            throw new IllegalStateException("Unable to persist application appearance");
        }
    }

    private static void requireContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
    }

    private static void requireConfiguration(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
    }
}
