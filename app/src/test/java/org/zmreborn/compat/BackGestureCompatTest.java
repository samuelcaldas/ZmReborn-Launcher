package org.zmreborn.compat;

import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * The plain JVM suite cannot instantiate Android activities or enter API 33+ callback paths.
 * It verifies the null boundary contract; callback registration itself is covered on device.
 */
public class BackGestureCompatTest {

    @Test
    public void registrationIgnoresNullActivity() {
        assertNull(BackGestureCompat.registerBackHandler(null, new TestBackHandler()));
    }

    @Test
    public void registrationIgnoresNullHandler() {
        assertNull(BackGestureCompat.registerBackHandler(null, null));
    }

    @Test
    public void unregistrationIgnoresNullInputs() {
        BackGestureCompat.unregisterBackHandler(null, null);
    }

    private static final class TestBackHandler implements BackGestureCompat.BackHandler {
        public void onBackInvoked() {
        }

        public void onBackProgressed(float progress) {
        }

        public void onBackCancelled() {
        }
    }
}
