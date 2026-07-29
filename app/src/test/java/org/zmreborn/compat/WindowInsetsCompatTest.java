package org.zmreborn.compat;

import android.view.View;
import android.view.WindowInsets;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * The project's local unit tests run against the unmocked Android stub jar (no Robolectric,
 * see app/build.gradle), where every stubbed method throws and {@code Build.VERSION.SDK_INT}
 * reads as 0. That makes the API 29+ extraction paths unreachable here; those are covered by
 * the instrumentation smoke checks in LauncherE2ETest instead. What plain JUnit CAN verify
 * deterministically is the API-24..28 fallback contract: gesture insets are unavailable and
 * the compat layer reports that with null rather than guessing.
 */
public class WindowInsetsCompatTest {

    @Test
    public void gestureInsetsUnavailableForNullInsets() {
        assertNull(WindowInsetsCompat.getSystemGestureInsets((WindowInsets) null));
    }

    @Test
    public void gestureInsetsUnavailableForNullView() {
        assertNull(WindowInsetsCompat.getSystemGestureInsets((View) null));
    }
}
