package org.zmreborn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.test.InstrumentationTestCase;

/** Verifies rendering behavior of the oblong selector drawable. */
public class SelectorDrawableInstrumentationTest extends InstrumentationTestCase {
    public void testOblongSelectorPressedStateRendersFilledPill() {
        SelectorDrawable drawable = SelectorDrawable.createOblongSelector(
                getInstrumentation().getTargetContext());
        drawable.setBounds(0, 0, 100, 40);
        drawable.setState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});

        Bitmap bitmap = Bitmap.createBitmap(100, 40, Bitmap.Config.ARGB_8888);
        try {
            drawable.draw(new Canvas(bitmap));

            assertTrue("Pill center must be filled", bitmap.getPixel(50, 20) >>> 24 > 0);
            assertEquals("Pill corner must remain transparent", 0, bitmap.getPixel(0, 0) >>> 24);
        } finally {
            bitmap.recycle();
        }
    }
}
