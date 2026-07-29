package org.zmreborn;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;
import java.util.Arrays;
import org.zmreborn.compat.AdaptiveIconCompat;

public class ApplicationIconE2ETest extends ActivityInstrumentationTestCase2<Launcher> {
    public ApplicationIconE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testNullApplicationIconUsesDeterministicFallback() {
        Drawable icon = Utilities.normalizeApplicationIcon(null, getActivity());
        assertNotNull("Null application icon must use fallback", icon);
    }

    public void testAdaptiveIconIdentityAndLayersArePreserved() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        Drawable icon = Api26.createAdaptiveIcon();
        Drawable normalized = Utilities.normalizeApplicationIcon(icon, getActivity());
        assertSame("Adaptive icon instance must be preserved", icon, normalized);

        final Drawable uninstallIcon = Utilities.overlayUninstallIcon(
                getActivity(), normalized);
        final Drawable disabledIcon = Utilities.adjustIconOpacity(normalized);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Drawable boundIcon = bindAndAssertVisible(icon);
                assertTrue("Bound adaptive icon must remain adaptive",
                        AdaptiveIconCompat.isAdaptiveIcon(boundIcon));
                assertNotSame("View binding must own a drawable copy", icon, boundIcon);
                assertNull("Model icon callback must remain unclaimed", icon.getCallback());
                assertEquals("Model icon bounds must remain untouched",
                        0, icon.getBounds().width());
                Drawable boundUninstallIcon = bindAndAssertVisible(uninstallIcon);
                assertFalse("Uninstall overlay must change rendered icon",
                        Arrays.equals(renderPixels(boundIcon),
                                renderPixels(boundUninstallIcon)));
                Drawable boundDisabledIcon = bindAndAssertVisible(disabledIcon);
                assertEquals("Disabled alpha must survive view-owned copy",
                        95, boundDisabledIcon.getAlpha());
            }
        });
    }

    private Drawable bindAndAssertVisible(Drawable icon) {
        TextView textView = new TextView(getActivity());
        Utilities.setCompoundApplicationIcon(textView, icon, getActivity());
        Drawable boundIcon = textView.getCompoundDrawables()[1];
        assertNotNull("Compound icon must be bound", boundIcon);
        assertSame("Bound icon callback must belong to its TextView",
                textView, boundIcon.getCallback());
        assertTrue("Compound icon width must be positive",
                boundIcon.getBounds().width() > 0);
        assertTrue("Compound icon height must be positive",
                boundIcon.getBounds().height() > 0);
        assertDrawableRenders(boundIcon);
        return boundIcon;
    }

    private static void assertDrawableRenders(Drawable drawable) {
        for (int pixel : renderPixels(drawable)) {
            if (Color.alpha(pixel) > 0) {
                return;
            }
        }
        fail("Compound icon must render visible pixels");
    }

    private static int[] renderPixels(Drawable drawable) {
        int width = drawable.getBounds().width();
        int height = drawable.getBounds().height();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return pixels;
    }

    @TargetApi(26)
    private static final class Api26 {
        private Api26() {
        }

        static Drawable createAdaptiveIcon() {
            ColorDrawable background = new ColorDrawable(Color.BLUE);
            ColorDrawable foreground = new ColorDrawable(Color.WHITE);
            return new android.graphics.drawable.AdaptiveIconDrawable(
                    background, foreground);
        }
    }
}
