package org.zmreborn.compat;

import java.util.Collections;
import org.junit.Test;

/**
 * Guard-clause regression coverage: a null view must never be dereferenced, regardless of
 * API level. Real API 29+ exclusion-rect behavior needs an inflated View and is covered by
 * the instrumentation smoke checks in PredictiveBackE2ETest instead (see WindowInsetsCompatTest
 * for why: this project's local unit tests run against the unmocked Android stub jar).
 */
public class GestureExclusionCompatTest {

    @Test
    public void nullViewIsIgnored() {
        GestureExclusionCompat.setSystemGestureExclusionRects(null, Collections.emptyList());
        GestureExclusionCompat.clearSystemGestureExclusionRects(null);
    }
}
