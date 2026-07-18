package org.zeam;

import java.util.Locale;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LocaleUtilTest {
    @Test
    public void normalizeLanguageAcceptsSupportedValues() {
        assertEquals("", LocaleUtil.normalizeLanguage(null));
        assertEquals("", LocaleUtil.normalizeLanguage(""));
        assertEquals("en", LocaleUtil.normalizeLanguage(" EN "));
        assertEquals("pt-BR", LocaleUtil.normalizeLanguage("pt_BR"));
        assertEquals("pt-BR", LocaleUtil.normalizeLanguage("PT-br"));
    }

    @Test
    public void normalizeLanguageFallsBackToEnglishForUnsupportedValues() {
        assertEquals("en", LocaleUtil.normalizeLanguage("fr"));
        assertEquals("en", LocaleUtil.normalizeLanguage("pt"));
        assertEquals("en", LocaleUtil.normalizeLanguage("pt-BR-extra"));
        assertEquals("en", LocaleUtil.normalizeLanguage("not a locale"));
    }

    @Test
    public void localeForLanguageCreatesSupportedLocales() {
        assertNull(LocaleUtil.localeForLanguage(null));
        assertEquals(Locale.ENGLISH, LocaleUtil.localeForLanguage("fr"));
        assertEquals(Locale.ENGLISH, LocaleUtil.localeForLanguage("en"));
        assertEquals(new Locale("pt", "BR"), LocaleUtil.localeForLanguage("pt-BR"));
    }

    @Test
    public void localeFingerprintTracksSystemAndEffectiveLocales() {
        String systemEnglish = LocaleUtil.localeFingerprint(Locale.ENGLISH, Locale.ENGLISH);
        String explicitPortuguese = LocaleUtil.localeFingerprint(Locale.ENGLISH, new Locale("pt", "BR"));
        String systemPortuguese = LocaleUtil.localeFingerprint(new Locale("pt", "BR"), Locale.ENGLISH);

        assertEquals("en|en", systemEnglish);
        assertEquals("en|pt_BR", explicitPortuguese);
        assertEquals("pt_BR|en", systemPortuguese);
        assertEquals("|", LocaleUtil.localeFingerprint(null, null));
    }
}
