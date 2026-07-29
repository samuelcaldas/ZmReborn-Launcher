package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

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

    static String getSelectedAppearance(Context context) {
        requireContext(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.preferences_key_application_appearance);
        String defaultValue = context.getString(
                R.string.preferences_default_application_appearance);
        return normalizeAppearance(preferences.getString(key, defaultValue));
    }

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

    static Configuration applyBrightness(Configuration configuration, Brightness brightness) {
        requireConfiguration(configuration);
        Configuration updatedConfiguration = new Configuration(configuration);
        updatedConfiguration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | effectiveNightMode(brightness, configuration);
        return updatedConfiguration;
    }

    static Configuration applySystemNightMode(
            Configuration configuration, Configuration systemConfiguration) {
        requireConfiguration(configuration);
        requireConfiguration(systemConfiguration);
        Configuration updatedConfiguration = new Configuration(configuration);
        updatedConfiguration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | effectiveNightMode(Brightness.SYSTEM, systemConfiguration);
        return updatedConfiguration;
    }

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

    static String fingerprint(Context context) {
        requireContext(context);
        return fingerprint(getSelectedAppearance(context), context.getResources().getConfiguration());
    }

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
