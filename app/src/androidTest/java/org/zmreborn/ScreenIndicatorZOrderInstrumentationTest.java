package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import java.lang.reflect.Method;

/** Verifies the paging drawer's page indicator draws above the drawer backdrop. */
public class ScreenIndicatorZOrderInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {
    private static final String HORIZONTAL_PAGING = "2";
    private SharedPreferences mPreferences;
    private String mDrawerTypeKey;
    private Object mSavedDrawerType;

    public ScreenIndicatorZOrderInstrumentationTest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getTargetContext();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mDrawerTypeKey = context.getString(R.string.preferences_key_apps_grid_type);
        this.mSavedDrawerType = this.mPreferences.contains(this.mDrawerTypeKey)
                ? this.mPreferences.getAll().get(this.mDrawerTypeKey) : null;
        assertTrue("Drawer type preference must commit", this.mPreferences.edit()
                .putString(this.mDrawerTypeKey, HORIZONTAL_PAGING).commit());
        getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SharedPreferences.Editor editor = this.mPreferences.edit();
            if (this.mSavedDrawerType == null) {
                editor.remove(this.mDrawerTypeKey);
            } else {
                editor.putString(this.mDrawerTypeKey, (String) this.mSavedDrawerType);
            }
            assertTrue("Drawer type preference must restore", editor.commit());
        } finally {
            super.tearDown();
        }
    }

    public void testIndicatorElevationMeetsOrExceedsPagingDrawerWhenOpen() throws Throwable {
        final Launcher launcher = getActivity();
        getInstrumentation().waitForIdleSync();
        assertTrue("Paging drawer must be active",
                launcher.mApplicationsView instanceof ApplicationsPagingView);

        try {
            invokeApplicationsGridMethodOnMainThread(launcher, "openApplicationsGrid", false);
            getInstrumentation().waitForIdleSync();

            View drawerView = launcher.mApplicationsView.getImplementingView();
            ScreenIndicator indicator = launcher.getScreenIndicator();
            assertNotNull("Screen indicator must exist", indicator);

            assertTrue("Indicator elevation must meet or exceed the drawer's so it draws on top",
                    indicator.getElevation() >= drawerView.getElevation());
        } finally {
            invokeApplicationsGridMethodOnMainThread(launcher, "closeApplicationsGrid", false);
        }
    }

    private void invokeApplicationsGridMethodOnMainThread(
            final Launcher launcher, final String methodName, final boolean animated)
            throws Throwable {
        final Throwable[] failure = new Throwable[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    Method method = Launcher.class.getDeclaredMethod(methodName, Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(launcher, Boolean.valueOf(animated));
                } catch (Throwable throwable) {
                    failure[0] = throwable;
                }
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
    }
}
