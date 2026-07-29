package org.zmreborn;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.zmreborn.compat.BackGestureCompat;

public class PredictiveBackE2ETest extends ActivityInstrumentationTestCase2<Launcher> {

    public PredictiveBackE2ETest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testRegisteredPlatformBackCallbackClosesDrawerOnApi33AndAbove() throws Exception {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        final Launcher launcher = getActivity();
        final Object registration = getBackGestureRegistration(launcher);
        assertNotNull("API 33+ must retain a registered platform back callback", registration);
        runOnUiThread(new UiAction() {
            public void run() throws Exception {
                openApplicationsGrid(launcher);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue("Drawer must be open before registered platform callback",
                launcher.isApplicationsGridOpen());
        runOnUiThread(new UiAction() {
            public void run() throws Exception {
                invokeRegisteredBackCallback(registration);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertFalse("Registered platform callback must close the drawer",
                getApplicationsGridOpen(launcher));
    }

    public void testBackHandlerClosesDrawerAndRestoresPreview() throws Exception {
        final Launcher launcher = getActivity();
        runOnUiThread(new UiAction() {
            public void run() throws Exception {
                openApplicationsGrid(launcher);
            }
        });
        getInstrumentation().waitForIdleSync();

        final View drawer = launcher.mApplicationsView.getImplementingView();
        final BackGestureCompat.BackHandler handler = getBackHandler(launcher);
        assertTrue("Drawer must be open before predictive back", launcher.isApplicationsGridOpen());
        runOnUiThread(new UiAction() {
            public void run() {
                handler.onBackProgressed(0.5f);
            }
        });
        assertEquals(0.825f, drawer.getAlpha(), 0.001f);
        assertEquals(0.96f, drawer.getScaleX(), 0.001f);

        runOnUiThread(new UiAction() {
            public void run() {
                handler.onBackCancelled();
            }
        });
        assertEquals(1.0f, drawer.getAlpha(), 0.0f);
        assertEquals(1.0f, drawer.getScaleX(), 0.0f);

        runOnUiThread(new UiAction() {
            public void run() {
                handler.onBackInvoked();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse("Predictive back must close the drawer", launcher.isApplicationsGridOpen());
        assertEquals(1.0f, drawer.getAlpha(), 0.0f);
        assertEquals(1.0f, drawer.getScaleX(), 0.0f);
    }

    public void testBackHandlerClosesFolderAndRestoresPreview() throws Exception {
        final Launcher launcher = getActivity();
        final UserFolderInfo folderInfo = new UserFolderInfo();
        folderInfo.container = -200;
        folderInfo.title = "Predictive back folder";
        runOnUiThread(new UiAction() {
            public void run() throws Exception {
                openFolder(launcher, folderInfo);
            }
        });
        getInstrumentation().waitForIdleSync();

        final Folder folder = launcher.mWorkspace.getOpenFolder();
        final BackGestureCompat.BackHandler handler = getBackHandler(launcher);
        assertNotNull("Folder must be open before predictive back", folder);
        runOnUiThread(new UiAction() {
            public void run() {
                handler.onBackProgressed(0.5f);
            }
        });
        assertEquals(0.825f, folder.getAlpha(), 0.001f);
        assertEquals(0.96f, folder.getScaleX(), 0.001f);

        runOnUiThread(new UiAction() {
            public void run() {
                handler.onBackCancelled();
            }
        });
        assertEquals(1.0f, folder.getAlpha(), 0.0f);
        assertEquals(1.0f, folder.getScaleX(), 0.0f);

        runOnUiThread(new UiAction() {
            public void run() {
                handler.onBackInvoked();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertNull("Predictive back must close the folder", launcher.mWorkspace.getOpenFolder());
    }

    public void testBackHandlerLeavesHomeScreenVisible() throws Exception {
        final Launcher launcher = getActivity();
        final BackGestureCompat.BackHandler handler = getBackHandler(launcher);
        assertFalse("Drawer must start closed", launcher.isApplicationsGridOpen());
        assertNull("No folder must start open", launcher.mWorkspace.getOpenFolder());

        runOnUiThread(new UiAction() {
            public void run() {
                handler.onBackInvoked();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(View.VISIBLE, launcher.mWorkspace.getVisibility());
        assertFalse("Back on home must not open the drawer", launcher.isApplicationsGridOpen());
        assertNull("Back on home must not create a folder", launcher.mWorkspace.getOpenFolder());
    }

    public void testWorkspaceExcludesBoundOpenApplicationsEdges() throws Exception {
        if (Build.VERSION.SDK_INT < 29) {
            return;
        }
        final Launcher launcher = getActivity();
        final Workspace workspace = launcher.mWorkspace;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(launcher);
        String upKey = launcher.getString(R.string.preferences_key_action_swipe_up);
        String downKey = launcher.getString(R.string.preferences_key_action_swipe_down);
        String originalUp = preferences.getString(upKey, null);
        String originalDown = preferences.getString(downKey, null);
        try {
            assertTrue(preferences.edit().putString(upKey, "1").putString(downKey, "1").commit());
            updateGestureExclusions(workspace, new Rect(0, 16, 0, 32));
            assertTrue("Unbound workspace gestures must leave back edges to the OS",
                    getGestureExclusionRects(workspace).isEmpty());

            assertTrue(preferences.edit().putString(downKey, "2").commit());
            updateGestureExclusions(workspace, new Rect(0, 16, 0, 32));
            List<Rect> rects = getGestureExclusionRects(workspace);
            assertEquals(1, rects.size());
            Rect topEdge = rects.get(0);
            assertEquals(0, topEdge.left);
            assertEquals(workspace.getWidth(), topEdge.right);
            assertEquals(0, topEdge.top);
            assertEquals(16, topEdge.bottom);

            assertTrue(preferences.edit().putString(upKey, "2").commit());
            updateGestureExclusions(workspace, new Rect(0, 16, 0, 32));
            rects = getGestureExclusionRects(workspace);
            assertEquals(2, rects.size());
            topEdge = rects.get(0);
            assertEquals(0, topEdge.left);
            assertEquals(workspace.getWidth(), topEdge.right);
            assertEquals(0, topEdge.top);
            assertEquals(16, topEdge.bottom);
            Rect bottomEdge = rects.get(1);
            assertEquals(0, bottomEdge.left);
            assertEquals(workspace.getWidth(), bottomEdge.right);
            assertEquals(workspace.getHeight() - 32, bottomEdge.top);
            assertEquals(workspace.getHeight(), bottomEdge.bottom);
        } finally {
            SharedPreferences.Editor editor = preferences.edit();
            restorePreference(editor, upKey, originalUp);
            restorePreference(editor, downKey, originalDown);
            assertTrue(editor.commit());
            updateGestureExclusions(workspace, new Rect(0, 16, 0, 32));
        }
    }

    private static void restorePreference(
            SharedPreferences.Editor editor, String key, String value) {
        if (value == null) {
            editor.remove(key);
            return;
        }
        editor.putString(key, value);
    }

    private void runOnUiThread(final UiAction action) throws Exception {
        final Exception[] failures = new Exception[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    action.run();
                } catch (Exception exception) {
                    failures[0] = exception;
                }
            }
        });
        if (failures[0] != null) {
            throw failures[0];
        }
    }

    private static Object getBackGestureRegistration(Launcher launcher) throws Exception {
        Field field = Launcher.class.getDeclaredField("mBackGestureRegistration");
        field.setAccessible(true);
        return field.get(launcher);
    }

    private static void invokeRegisteredBackCallback(Object registration) throws Exception {
        Method method = registration.getClass().getDeclaredMethod("onBackInvoked");
        method.setAccessible(true);
        method.invoke(registration);
    }

    private static boolean getApplicationsGridOpen(Launcher launcher) throws Exception {
        Field field = Launcher.class.getDeclaredField("mApplicationsGridOpen");
        field.setAccessible(true);
        return field.getBoolean(launcher);
    }

    private static BackGestureCompat.BackHandler getBackHandler(Launcher launcher) throws Exception {
        Field field = Launcher.class.getDeclaredField("mBackHandler");
        field.setAccessible(true);
        return (BackGestureCompat.BackHandler) field.get(launcher);
    }

    private static void openApplicationsGrid(Launcher launcher) throws Exception {
        Method method = Launcher.class.getDeclaredMethod("openApplicationsGrid", Boolean.TYPE);
        method.setAccessible(true);
        method.invoke(launcher, Boolean.FALSE);
    }

    private static void openFolder(Launcher launcher, FolderInfo folderInfo) throws Exception {
        Method method = Launcher.class.getDeclaredMethod("openFolder", FolderInfo.class);
        method.setAccessible(true);
        method.invoke(launcher, folderInfo);
    }

    private void updateGestureExclusions(final Workspace workspace, final Rect insets)
            throws Exception {
        runOnUiThread(new UiAction() {
            public void run() {
                workspace.setSystemGestureInsets(insets);
                workspace.updateSystemGestureExclusionRects();
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    @SuppressWarnings("unchecked")
    private static List<Rect> getGestureExclusionRects(View view) throws Exception {
        Method method = View.class.getMethod("getSystemGestureExclusionRects");
        return (List<Rect>) method.invoke(view);
    }

    private interface UiAction {
        void run() throws Exception;
    }
}
