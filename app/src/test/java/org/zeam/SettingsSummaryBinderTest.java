package org.zeam;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SettingsSummaryBinderTest {
    @Test
    public void resolvesMatchingListEntry() {
        CharSequence summary = SettingsSummaryBinder.resolveListSummary(
                new CharSequence[] {"English", "Português"},
                new CharSequence[] {"en", "pt-BR"}, "pt-BR");

        assertEquals("Português", summary);
    }

    @Test
    public void missingListValueDoesNotThrowOrProduceSummary() {
        CharSequence summary = SettingsSummaryBinder.resolveListSummary(
                new CharSequence[] {"English"}, new CharSequence[] {"en"}, null);

        assertNull(summary);
    }

    @Test
    public void mismatchedListArraysProduceNoSummary() {
        CharSequence summary = SettingsSummaryBinder.resolveListSummary(
                new CharSequence[] {"English"}, new CharSequence[] {"en", "pt-BR"}, "pt-BR");

        assertNull(summary);
    }
}
