package org.zmreborn;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DrawerDensityPolicyTest {
    @Test
    public void explicitDensityPresetsSelectTheirOwnDimensions() {
        assertEquals(R.dimen.drawer_cell_compact_width,
                DrawerDensityPolicy.resolvePreferredWidthResource("compact", 360));
        assertEquals(R.dimen.drawer_cell_preferred_width,
                DrawerDensityPolicy.resolvePreferredWidthResource("default", 840));
        assertEquals(R.dimen.drawer_cell_comfortable_width,
                DrawerDensityPolicy.resolvePreferredWidthResource("comfortable", 360));
    }

    @Test
    public void automaticDensityAdaptsAcrossWindowWidths() {
        assertEquals(R.dimen.drawer_cell_preferred_width,
                DrawerDensityPolicy.resolvePreferredWidthResource("automatic", 360));
        assertEquals(R.dimen.drawer_cell_comfortable_width,
                DrawerDensityPolicy.resolvePreferredWidthResource("automatic", 600));
        assertEquals(R.dimen.drawer_cell_expanded_width,
                DrawerDensityPolicy.resolvePreferredWidthResource("automatic", 840));
    }

    @Test
    public void unknownStoredValueFailsSafeToAutomatic() {
        assertEquals(R.dimen.drawer_cell_comfortable_width,
                DrawerDensityPolicy.resolvePreferredWidthResource("legacy", 600));
    }
}
