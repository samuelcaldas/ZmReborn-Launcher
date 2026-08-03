package org.zmreborn.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.zmreborn.CellLayout;
import org.zmreborn.Launcher;
import org.zmreborn.R;
import org.zmreborn.Workspace;

/** Verifies widget-picker catalog and preview-card behavior. */
public class WidgetPickerInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {

    /** Creates widget-picker instrumentation test for Launcher. */
    public WidgetPickerInstrumentationTest() {
        super(Launcher.class);
    }

    /** Verifies catalog Search ordering and provider-label sorting. */
    public void testCatalogStartsWithSearchAndSortsProviders() {
        Launcher launcher = getActivity();
        List<WidgetPickerEntry> entries = WidgetPickerCatalog.load(
                launcher, AppWidgetManager.getInstance(launcher));

        assertFalse(entries.isEmpty());
        assertTrue(entries.get(0).isSearch());
        String previousLabel = "";
        for (int index = 1; index < entries.size(); index++) {
            WidgetPickerEntry entry = entries.get(index);
            assertFalse(entry.isSearch());
            assertNotNull(entry.getProvider());
            String label = entry.getLabel().toString();
            assertTrue(previousLabel.compareToIgnoreCase(label) <= 0);
            previousLabel = label;
        }
    }

    /** Verifies failed post-grant verification revokes test-added bind authority. */
    public void testBindAuthorityRollsBackFailedVerification() {
        final int[] probes = new int[1];
        final int[] grants = new int[1];
        final int[] revocations = new int[1];
        WidgetBindAuthority.Operations operations =
                new WidgetBindAuthority.Operations() {
            public boolean canBind() {
                probes[0]++;
                return false;
            }

            public void grant() {
                grants[0]++;
            }

            public void revoke() {
                revocations[0]++;
            }
        };

        try {
            WidgetBindAuthority.ensure(operations,
                    "Expected post-grant verification failure");
            fail("Failed widget bind verification must throw");
        } catch (AssertionError expected) {
            assertEquals("Expected post-grant verification failure",
                    expected.getMessage());
        }
        assertEquals(2, probes[0]);
        assertEquals(1, grants[0]);
        assertEquals(1, revocations[0]);
    }

    /** Verifies newly granted bind authority revokes through its paired token once. */
    public void testBindAuthorityGrantRevokesNewAuthorityOnce() {
        final int[] probes = new int[1];
        final int[] grants = new int[1];
        final int[] revocations = new int[1];
        WidgetBindAuthority.Operations operations =
                new WidgetBindAuthority.Operations() {
            public boolean canBind() {
                probes[0]++;
                return probes[0] == 2;
            }

            public void grant() {
                grants[0]++;
            }

            public void revoke() {
                revocations[0]++;
            }
        };

        WidgetBindAuthority.Grant grant = WidgetBindAuthority.ensureGrant(operations,
                "Expected widget bind authority");

        assertEquals(2, probes[0]);
        assertEquals(1, grants[0]);
        assertEquals(0, revocations[0]);
        grant.revoke();
        grant.revoke();
        assertEquals(1, revocations[0]);
    }

    /** Verifies catalog inspection never allocates host widget IDs. */
    public void testCatalogLoadingDoesNotAllocateWidgetId() {
        Launcher launcher = getActivity();
        int[] before = launcher.getAppWidgetHost().getAppWidgetIds();

        WidgetPickerCatalog.load(launcher,
                AppWidgetManager.getInstance(launcher));

        int[] after = launcher.getAppWidgetHost().getAppWidgetIds();
        Arrays.sort(before);
        Arrays.sort(after);
        assertTrue(Arrays.equals(before, after));
    }

    /** Verifies Search card has preview and accessibility presentation. */
    public void testAdapterRendersAccessibleSearchPreviewCard() {
        Launcher launcher = getActivity();
        WidgetPickerEntry search = WidgetPickerEntry.search(launcher, "4 × 1");
        WidgetPickerAdapter adapter = new WidgetPickerAdapter(
                launcher, Arrays.asList(search));

        View row = adapter.getView(0, null, null);
        ImageView preview = (ImageView) row.findViewById(R.id.widget_preview);
        TextView label = (TextView) row.findViewById(R.id.widget_label);
        TextView detail = (TextView) row.findViewById(R.id.widget_detail);

        assertNotNull(preview.getDrawable());
        assertEquals(launcher.getString(R.string.group_search), label.getText());
        assertEquals("4 × 1", detail.getText());
        assertNotNull(row.getContentDescription());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                label.getImportantForAccessibility());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                detail.getImportantForAccessibility());
        assertFalse(row.isFocusable());
        assertFalse(row.isClickable());
    }

    /** Verifies Search selection invokes dialog callback without host allocation. */
    public void testDialogLoadsAndSelectsSearchWithoutAllocatingWidgetId()
            throws Exception {
        final Launcher launcher = getActivity();
        final int[] before = launcher.getAppWidgetHost().getAppWidgetIds();
        final AtomicReference<WidgetPickerEntry> selection = new AtomicReference<>();
        final CountDownLatch selected = new CountDownLatch(1);
        final WidgetPickerDialog[] picker = new WidgetPickerDialog[1];

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                picker[0] = new WidgetPickerDialog(launcher,
                        new WidgetPickerDialog.Callback() {
                            public void onWidgetSelected(WidgetPickerDialog dialog,
                                    WidgetPickerEntry entry) {
                                selection.set(entry);
                                selected.countDown();
                            }

                            public void onWidgetPickerDismissed(
                                    WidgetPickerDialog dialog) {
                            }
                        });
                picker[0].show(AppWidgetManager.getInstance(launcher),
                        currentLayout(launcher));
            }
        });
        waitForEntries(picker[0]);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                ListView list = (ListView) picker[0].findViewById(
                        R.id.widget_picker_list);
                View row = list.getAdapter().getView(0, null, list);
                list.performItemClick(row, 0, list.getAdapter().getItemId(0));
            }
        });

        assertTrue(selected.await(5, TimeUnit.SECONDS));
        assertNotNull(selection.get());
        assertTrue(selection.get().isSearch());
        int[] after = launcher.getAppWidgetHost().getAppWidgetIds();
        Arrays.sort(before);
        Arrays.sort(after);
        assertTrue(Arrays.equals(before, after));
    }

    /** Verifies real provider-card touch invokes selection callback and dismisses picker. */
    public void testProviderCardTouchDispatchSelectsAndDismissesPicker()
            throws Exception {
        final Launcher launcher = getActivity();
        final ComponentName provider = ExternalWidgetFixture.provider(
                getInstrumentation().getContext());
        final AppWidgetManager manager = AppWidgetManager.getInstance(launcher);
        ExternalWidgetFixture.requireProvider(manager, provider);
        final AtomicReference<WidgetPickerEntry> selection = new AtomicReference<>();
        final CountDownLatch selected = new CountDownLatch(1);
        final CountDownLatch dismissed = new CountDownLatch(1);
        final WidgetPickerDialog[] picker = new WidgetPickerDialog[1];

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                picker[0] = new WidgetPickerDialog(launcher,
                        new WidgetPickerDialog.Callback() {
                            public void onWidgetSelected(WidgetPickerDialog dialog,
                                    WidgetPickerEntry entry) {
                                selection.set(entry);
                                selected.countDown();
                            }

                            public void onWidgetPickerDismissed(
                                    WidgetPickerDialog dialog) {
                                dismissed.countDown();
                            }
                        });
                picker[0].show(manager, currentLayout(launcher));
            }
        });
        try {
            waitForEntries(picker[0]);
            WidgetPickerTouch.tapProvider(getInstrumentation(), picker[0], provider);

            assertTrue("Provider touch must invoke picker selection callback",
                    selected.await(5, TimeUnit.SECONDS));
            assertNotNull("Provider touch must retain selected entry", selection.get());
            assertEquals(provider, selection.get().getProvider().provider);
            assertTrue("Provider selection must dismiss picker",
                    dismissed.await(5, TimeUnit.SECONDS));
            assertFalse("Provider selection must close picker", picker[0].isShowing());
        } finally {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    if (picker[0] != null && picker[0].isShowing()) {
                        picker[0].dismiss();
                    }
                }
            });
        }
    }

    /** Verifies Launcher opens in-app picker without allocating a widget ID. */
    public void testLauncherWidgetActionShowsInAppPickerWithoutAllocatingId()
            throws Exception {
        final Launcher launcher = getActivity();
        final int[] before = launcher.getAppWidgetHost().getAppWidgetIds();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                setPendingTarget(launcher);
                invokeStartAddWidgets(launcher);
            }
        });

        Field field = Launcher.class.getDeclaredField("mWidgetPickerDialog");
        field.setAccessible(true);
        final WidgetPickerDialog picker = (WidgetPickerDialog) field.get(launcher);
        assertNotNull(picker);
        assertTrue(picker.isShowing());
        int[] after = launcher.getAppWidgetHost().getAppWidgetIds();
        Arrays.sort(before);
        Arrays.sort(after);
        assertTrue(Arrays.equals(before, after));
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                picker.dismiss();
            }
        });
    }

    /** Verifies saved open-picker state restores the preview dialog. */
    public void testSavedPickerStateReopensPreviewDialog() throws Exception {
        final Launcher launcher = getActivity();
        final Bundle state = new Bundle();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                setPendingTarget(launcher);
                invokeStartAddWidgets(launcher);
                invokeSaveState(launcher, state);
                currentPicker(launcher).dismiss();
                invokeRestoreState(launcher, state);
            }
        });

        assertTrue(state.getBoolean("launcher.widget_picker_open"));
        final WidgetPickerDialog picker = currentPicker(launcher);
        assertNotNull(picker);
        assertTrue(picker.isShowing());
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                picker.dismiss();
            }
        });
    }

    /** Verifies canceled widget bind removes its allocated host ID. */
    public void testCancelledBindResultDeletesAllocatedWidgetId()
            throws Exception {
        final Launcher launcher = getActivity();
        final int[] allocated = new int[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                setPendingTarget(launcher);
                allocated[0] = launcher.getAppWidgetHost().allocateAppWidgetId();
                setPendingWidgetId(launcher, allocated[0]);
                invokeWidgetResult(launcher, Activity.RESULT_CANCELED, null);
            }
        });

        assertFalse(contains(launcher.getAppWidgetHost().getAppWidgetIds(),
                allocated[0]));
    }

    /** Verifies denied external configuration launch releases pending host state. */
    public void testDeniedConfigurationLaunchReleasesAllocatedWidgetId()
            throws Exception {
        final Launcher launcher = getActivity();
        final int appWidgetId = launcher.getAppWidgetHost().allocateAppWidgetId();
        final Intent resultData = widgetResult(appWidgetId);
        final AppWidgetProviderInfo provider = deniedConfigurationProvider();

        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    setPendingWidgetId(launcher, appWidgetId);
                    invokeStartConfiguration(launcher, provider,
                            appWidgetId, resultData);
                }
            });
            assertFalse(contains(launcher.getAppWidgetHost().getAppWidgetIds(),
                    appWidgetId));
            assertEquals(-1, pendingWidgetId(launcher));
            assertFalse(waitingForResult(launcher));
        } finally {
            if (contains(launcher.getAppWidgetHost().getAppWidgetIds(), appWidgetId)) {
                launcher.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
            }
        }
    }

    private Intent widgetResult(int appWidgetId) {
        Intent resultData = new Intent();
        resultData.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return resultData;
    }

    private AppWidgetProviderInfo deniedConfigurationProvider() {
        AppWidgetProviderInfo provider = new AppWidgetProviderInfo();
        provider.provider = ExternalWidgetFixture.provider(
                getInstrumentation().getContext());
        provider.configure = new ComponentName(
                getInstrumentation().getContext(),
                DeniedWidgetConfigurationActivity.class);
        return provider;
    }

    private void invokeStartConfiguration(Launcher launcher,
            AppWidgetProviderInfo provider, int appWidgetId, Intent resultData) {
        try {
            Method method = Launcher.class.getDeclaredMethod(
                    "startAppWidgetConfiguration", AppWidgetProviderInfo.class,
                    int.class, Intent.class);
            method.setAccessible(true);
            method.invoke(launcher, provider, appWidgetId, resultData);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to invoke widget configuration", exception);
        }
    }

    private int pendingWidgetId(Launcher launcher) {
        return ((Integer) readField(launcher, "mPendingAppWidgetId")).intValue();
    }

    private boolean waitingForResult(Launcher launcher) {
        return ((Boolean) readField(launcher, "mWaitingForResult")).booleanValue();
    }

    private Object readField(Launcher launcher, String name) {
        try {
            Field field = Launcher.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(launcher);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Launcher." + name,
                    exception);
        }
    }

    private WidgetPickerDialog currentPicker(Launcher launcher) {
        try {
            Field field = Launcher.class.getDeclaredField("mWidgetPickerDialog");
            field.setAccessible(true);
            return (WidgetPickerDialog) field.get(launcher);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read widget picker", exception);
        }
    }

    private void invokeSaveState(Launcher launcher, Bundle state) {
        invokeLauncherStateMethod(launcher, "onSaveInstanceState", state);
    }

    private void invokeRestoreState(Launcher launcher, Bundle state) {
        invokeLauncherStateMethod(launcher, "restoreState", state);
    }

    private void invokeLauncherStateMethod(Launcher launcher, String name,
            Bundle state) {
        try {
            Method method = Launcher.class.getDeclaredMethod(name, Bundle.class);
            method.setAccessible(true);
            method.invoke(launcher, state);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke " + name, exception);
        }
    }

    private void setPendingWidgetId(Launcher launcher, int appWidgetId) {
        try {
            Field field = Launcher.class.getDeclaredField("mPendingAppWidgetId");
            field.setAccessible(true);
            field.setInt(launcher, appWidgetId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set pending widget ID", exception);
        }
    }

    private void invokeWidgetResult(Launcher launcher, int resultCode,
            Intent data) {
        try {
            Method method = Launcher.class.getDeclaredMethod("onActivityResult",
                    int.class, int.class, Intent.class);
            method.setAccessible(true);
            method.invoke(launcher, 9, resultCode, data);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to deliver widget result", exception);
        }
    }

    private boolean contains(int[] values, int expected) {
        for (int value : values) {
            if (value == expected) {
                return true;
            }
        }
        return false;
    }

    private void setPendingTarget(Launcher launcher) {
        try {
            Workspace workspace = (Workspace) launcher.findViewById(R.id.workspace);
            Class<?> cellInfoClass = Class.forName("org.zmreborn.CellLayout$CellInfo");
            java.lang.reflect.Constructor<?> constructor =
                    cellInfoClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object cellInfo = constructor.newInstance();
            setCellInfoField(cellInfoClass, cellInfo, "valid", true);
            setCellInfoField(cellInfoClass, cellInfo, "screen",
                    workspace.getCurrentScreen());
            setCellInfoField(cellInfoClass, cellInfo, "cellX", -1);
            setCellInfoField(cellInfoClass, cellInfo, "cellY", -1);
            Field field = Launcher.class.getDeclaredField("mAddItemCellInfo");
            field.setAccessible(true);
            field.set(launcher, cellInfo);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set widget target", exception);
        }
    }

    private void setCellInfoField(Class<?> cellInfoClass, Object cellInfo,
            String name, Object value) throws ReflectiveOperationException {
        Field field = cellInfoClass.getDeclaredField(name);
        field.setAccessible(true);
        field.set(cellInfo, value);
    }

    private void invokeStartAddWidgets(Launcher launcher) {
        try {
            Method method = Launcher.class.getDeclaredMethod("startAddWidgets");
            method.setAccessible(true);
            method.invoke(launcher);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to start widget picker", exception);
        }
    }

    private void waitForEntries(final WidgetPickerDialog picker) throws Exception {
        for (int attempt = 0; attempt < 600; attempt++) {
            final int[] count = new int[1];
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    ListView list = (ListView) picker.findViewById(
                            R.id.widget_picker_list);
                    count[0] = list.getAdapter() == null ? 0 : list.getAdapter().getCount();
                }
            });
            if (count[0] > 0) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Widget picker did not publish entries");
    }

    private CellLayout currentLayout(Launcher launcher) {
        Workspace workspace = (Workspace) launcher.findViewById(R.id.workspace);
        return (CellLayout) workspace.getChildAt(workspace.getCurrentScreen());
    }
}
