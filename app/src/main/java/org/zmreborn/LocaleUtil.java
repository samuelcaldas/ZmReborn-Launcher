package org.zmreborn;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import java.util.Locale;

@SuppressLint("AppBundleLocaleChanges")
final class LocaleUtil {
    static final String SYSTEM_DEFAULT_LANGUAGE = "";
    static final String ENGLISH_LANGUAGE = "en";
    static final String BRAZILIAN_PORTUGUESE_LANGUAGE = "pt-BR";

    private LocaleUtil() {
    }

    static Context wrap(Context baseContext) {
        requireContext(baseContext);
        Configuration baseConfiguration = baseContext.getResources().getConfiguration();
        Configuration configuration = new Configuration(baseConfiguration);
        Locale locale = localeForLanguage(getSelectedLanguage(baseContext));
        if (locale != null) {
            configuration.setLocale(locale);
        }
        Appearance.Brightness brightness = Appearance.brightnessFor(
                Appearance.getSelectedAppearance(baseContext));
        Configuration brightnessConfiguration = Appearance.applyBrightness(configuration, brightness);
        if (brightness == Appearance.Brightness.SYSTEM) {
            brightnessConfiguration = Appearance.applySystemNightMode(
                    configuration, Resources.getSystem().getConfiguration());
        }
        if (locale == null && brightnessConfiguration.uiMode == baseConfiguration.uiMode) {
            return baseContext;
        }
        return baseContext.createConfigurationContext(brightnessConfiguration);
    }

    static String getSelectedLanguage(Context context) {
        requireContext(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.preferences_key_application_language);
        String defaultValue = context.getString(R.string.preferences_default_application_language);
        return normalizeLanguage(preferences.getString(key, defaultValue));
    }

    static boolean persistSelectedLanguage(Context context, String requestedLanguage) {
        requireContext(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.preferences_key_application_language);
        String defaultValue = context.getString(R.string.preferences_default_application_language);
        String storedLanguage = preferences.getString(key, defaultValue);
        String normalizedLanguage = normalizeLanguage(requestedLanguage);
        if (normalizedLanguage.equals(storedLanguage)) {
            return false;
        }
        boolean effectiveLanguageChanged = !normalizedLanguage.equals(normalizeLanguage(storedLanguage));
        commitLanguage(preferences, key, normalizedLanguage);
        return effectiveLanguageChanged;
    }

    private static void commitLanguage(SharedPreferences preferences, String key, String language) {
        if (!preferences.edit().putString(key, language).commit()) {
            throw new IllegalStateException("Unable to persist application language");
        }
    }

    static String normalizeLanguage(String language) {
        if (language == null) {
            return SYSTEM_DEFAULT_LANGUAGE;
        }
        String normalized = language.trim().replace('_', '-');
        if (normalized.length() == 0) {
            return SYSTEM_DEFAULT_LANGUAGE;
        }
        if (ENGLISH_LANGUAGE.equalsIgnoreCase(normalized)) {
            return ENGLISH_LANGUAGE;
        }
        if (BRAZILIAN_PORTUGUESE_LANGUAGE.equalsIgnoreCase(normalized)) {
            return BRAZILIAN_PORTUGUESE_LANGUAGE;
        }
        return ENGLISH_LANGUAGE;
    }

    static Locale localeForLanguage(String language) {
        String normalizedLanguage = normalizeLanguage(language);
        if (ENGLISH_LANGUAGE.equals(normalizedLanguage)) {
            return Locale.ENGLISH;
        }
        if (BRAZILIAN_PORTUGUESE_LANGUAGE.equals(normalizedLanguage)) {
            return new Locale("pt", "BR");
        }
        return null;
    }

    static String localeFingerprint(Locale systemLocale, Locale effectiveLocale) {
        return localeKey(systemLocale) + '|' + localeKey(effectiveLocale);
    }

    static String currentLocaleFingerprint(Context context) {
        Configuration systemConfiguration = Resources.getSystem().getConfiguration();
        Locale systemLocale = getPrimaryLocale(systemConfiguration);
        Locale selectedLocale = localeForLanguage(getSelectedLanguage(context));
        Locale effectiveLocale = selectedLocale == null ? systemLocale : selectedLocale;
        return localeListKey(systemConfiguration) + '|' + localeKey(effectiveLocale);
    }

    private static Locale getPrimaryLocale(Configuration configuration) {
        LocaleList locales = configuration.getLocales();
        if (locales.isEmpty()) {
            return null;
        }
        return locales.get(0);
    }

    private static String localeListKey(Configuration configuration) {
        LocaleList locales = configuration.getLocales();
        StringBuilder key = new StringBuilder();
        for (int index = 0; index < locales.size(); index++) {
            appendLocaleKey(key, locales.get(index));
        }
        return key.toString();
    }

    private static void appendLocaleKey(StringBuilder key, Locale locale) {
        if (key.length() > 0) {
            key.append(',');
        }
        key.append(localeKey(locale));
    }

    private static String localeKey(Locale locale) {
        return locale == null ? "" : locale.toString();
    }

    private static void requireContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
    }
}
