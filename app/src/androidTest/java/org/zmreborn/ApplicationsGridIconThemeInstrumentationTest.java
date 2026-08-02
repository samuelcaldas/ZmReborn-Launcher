package org.zmreborn;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;

public class ApplicationsGridIconThemeInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {
    public ApplicationsGridIconThemeInstrumentationTest() {
        super(Launcher.class);
    }

    public void testResolveIconReflectsContextThemeRatherThanConstructionTimeTheme() {
        Context targetContext = getInstrumentation().getTargetContext();
        Context lightContext = configurationContext(targetContext, Configuration.UI_MODE_NIGHT_NO);
        Context darkContext = configurationContext(targetContext, Configuration.UI_MODE_NIGHT_YES);

        ApplicationsGridItemInfo applicationsGridItemInfo = new ApplicationsGridItemInfo(lightContext);

        int lightRenderedColor = firstFullyOpaquePixel(applicationsGridItemInfo.resolveIcon(lightContext));
        int darkRenderedColor = firstFullyOpaquePixel(applicationsGridItemInfo.resolveIcon(darkContext));

        int expectedLightColor = lightContext.getResources().getColor(
                R.color.m3_on_surface_variant);
        int expectedDarkColor = darkContext.getResources().getColor(
                R.color.m3_on_surface_variant);
        assertEquals("Icon resolved in a light-theme context must use the light theme color",
                expectedLightColor, lightRenderedColor);
        assertEquals("Icon resolved in a dark-theme context after construction must reflect "
                        + "the dark theme, not the color cached at construction time",
                expectedDarkColor, darkRenderedColor);
    }

    private static Context configurationContext(Context base, int nightMode) {
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        return base.createConfigurationContext(configuration);
    }

    private static int firstFullyOpaquePixel(Drawable icon) {
        int width = icon.getIntrinsicWidth();
        int height = icon.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, width, height);
        icon.draw(canvas);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                if ((pixel >>> 24) == 0xFF) {
                    return pixel;
                }
            }
        }
        throw new IllegalStateException("Icon rendered with no fully opaque pixels");
    }
}
