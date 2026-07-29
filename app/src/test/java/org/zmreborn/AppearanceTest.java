package org.zmreborn;

import android.content.res.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class AppearanceTest {
    @Test
    public void normalizeAppearanceAcceptsStableValuesAndDefaultsToSystem() {
        assertEquals(Appearance.SYSTEM, Appearance.normalizeAppearance(null));
        assertEquals(Appearance.SYSTEM, Appearance.normalizeAppearance("  "));
        assertEquals(Appearance.SYSTEM, Appearance.normalizeAppearance("legacy"));
        assertEquals(Appearance.LIGHT, Appearance.normalizeAppearance(" LIGHT "));
        assertEquals(Appearance.DARK, Appearance.normalizeAppearance("dark"));
    }

    @Test
    public void applyBrightnessPreservesTypeAndOtherUiModeBitsForLightAndDark() {
        Configuration darkConfiguration = configurationWith(
                Configuration.UI_MODE_TYPE_DESK | Configuration.UI_MODE_NIGHT_YES | 0x100);
        Configuration lightConfiguration = Appearance.applyBrightness(
                darkConfiguration, Appearance.Brightness.LIGHT);

        assertNotSame(darkConfiguration, lightConfiguration);
        assertEquals(Configuration.UI_MODE_TYPE_DESK | Configuration.UI_MODE_NIGHT_NO | 0x100,
                lightConfiguration.uiMode);
        assertEquals(Configuration.UI_MODE_TYPE_DESK | Configuration.UI_MODE_NIGHT_YES | 0x100,
                darkConfiguration.uiMode);

        Configuration restoredDarkConfiguration = Appearance.applyBrightness(
                lightConfiguration, Appearance.Brightness.DARK);
        assertEquals(Configuration.UI_MODE_TYPE_DESK | Configuration.UI_MODE_NIGHT_YES | 0x100,
                restoredDarkConfiguration.uiMode);
    }

    @Test
    public void systemBrightnessPreservesCurrentNightMode() {
        Configuration undefinedConfiguration = configurationWith(
                Configuration.UI_MODE_TYPE_CAR | Configuration.UI_MODE_NIGHT_UNDEFINED | 0x100);
        Configuration systemConfiguration = Appearance.applyBrightness(
                undefinedConfiguration, Appearance.Brightness.SYSTEM);

        assertEquals(undefinedConfiguration.uiMode, systemConfiguration.uiMode);
        assertEquals(Configuration.UI_MODE_NIGHT_UNDEFINED,
                Appearance.effectiveNightMode(Appearance.Brightness.SYSTEM, undefinedConfiguration));
    }

    @Test
    public void applySystemNightModeUsesSystemNightBitsWithoutChangingOtherBits() {
        Configuration configuration = configurationWith(
                Configuration.UI_MODE_TYPE_DESK | Configuration.UI_MODE_NIGHT_NO | 0x100);
        Configuration systemConfiguration = configurationWith(
                Configuration.UI_MODE_TYPE_CAR | Configuration.UI_MODE_NIGHT_YES | 0x200);

        Configuration updatedConfiguration = Appearance.applySystemNightMode(
                configuration, systemConfiguration);

        assertEquals(Configuration.UI_MODE_TYPE_DESK | Configuration.UI_MODE_NIGHT_YES | 0x100,
                updatedConfiguration.uiMode);
        assertEquals(Configuration.UI_MODE_TYPE_CAR | Configuration.UI_MODE_NIGHT_YES | 0x200,
                systemConfiguration.uiMode);
    }

    @Test
    public void fingerprintCombinesNormalizedSelectionAndEffectiveNightMode() {
        Configuration darkConfiguration = configurationWith(Configuration.UI_MODE_NIGHT_YES);

        assertEquals("system|" + Configuration.UI_MODE_NIGHT_YES,
                Appearance.fingerprint("unknown", darkConfiguration));
        assertEquals("light|" + Configuration.UI_MODE_NIGHT_NO,
                Appearance.fingerprint(Appearance.LIGHT, darkConfiguration));
        assertEquals("dark|" + Configuration.UI_MODE_NIGHT_YES,
                Appearance.fingerprint(Appearance.DARK, darkConfiguration));
    }

    private static Configuration configurationWith(int uiMode) {
        Configuration configuration = new Configuration();
        configuration.uiMode = uiMode;
        return configuration;
    }
}
