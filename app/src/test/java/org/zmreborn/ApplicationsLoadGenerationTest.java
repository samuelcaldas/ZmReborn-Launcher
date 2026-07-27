package org.zmreborn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApplicationsLoadGenerationTest {
    @Test
    public void newestGenerationAcceptsOnlyNewestResult() {
        ApplicationsLoadGeneration generations = new ApplicationsLoadGeneration();
        int first = generations.start();
        int second = generations.start();

        assertFalse(generations.isCurrent(first));
        assertTrue(generations.isCurrent(second));
    }
}
