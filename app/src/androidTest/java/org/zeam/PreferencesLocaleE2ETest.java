package org.zeam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import java.util.Locale;

public class PreferencesLocaleE2ETest extends ActivityInstrumentationTestCase2<Preferences> {
    private Context targetContext;
    private String originalLanguage;
    private Locale originalLocale;
    private Configuration originalConfiguration;

    public PreferencesLocaleE2ETest() {
        super(Preferences.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.targetContext = getInstrumentation().getTargetContext();
        this.originalLanguage = LocaleUtil.getSelectedLanguage(this.targetContext);
        this.originalLocale = Locale.getDefault();
        this.originalConfiguration = new Configuration(
                this.targetContext.getResources().getConfiguration());
        LocaleUtil.persistSelectedLanguage(this.targetContext, LocaleUtil.BRAZILIAN_PORTUGUESE_LANGUAGE);
        getActivity();
        getInstrumentation().waitForIdleSync();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            LocaleUtil.persistSelectedLanguage(this.targetContext, this.originalLanguage);
            getActivity().finish();
            restoreLocaleState();
        } finally {
            super.tearDown();
        }
    }

    private void restoreLocaleState() {
        Locale.setDefault(this.originalLocale);
        Resources resources = this.targetContext.getResources();
        resources.updateConfiguration(
                this.originalConfiguration,
                resources.getDisplayMetrics());
    }

    public void testBrazilianPortuguesePreferencesRender() {
        Preferences preferences = getActivity();
        Preference languagePreference = preferences.findPreference(
                preferences.getString(R.string.preferences_key_application_language));
        ListPreference languageList = (ListPreference) languagePreference;

        assertEquals("Idioma", languageList.getTitle());
        assertEquals("Padrão do sistema", languageList.getEntries()[0]);
        assertEquals("English", languageList.getEntries()[1]);
        assertEquals("Português (Brasil)", languageList.getEntries()[2]);
        assertEquals("pt-BR", languageList.getValue());
    }

    public void testSystemDefaultRepairsUnsupportedStoredValue() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.targetContext);
        String key = this.targetContext.getString(R.string.preferences_key_application_language);
        assertTrue(preferences.edit().putString(key, "unsupported").commit());

        assertFalse(LocaleUtil.persistSelectedLanguage(
                this.targetContext,
                LocaleUtil.SYSTEM_DEFAULT_LANGUAGE));
        assertEquals("", preferences.getString(key, null));
    }
}
