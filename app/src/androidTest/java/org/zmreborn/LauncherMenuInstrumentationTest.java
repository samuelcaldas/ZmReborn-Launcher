package org.zmreborn;

import android.test.ActivityInstrumentationTestCase2;

/** Verifies launcher action-menu entries exposed to users. */
public class LauncherMenuInstrumentationTest
        extends ActivityInstrumentationTestCase2<Launcher> {

    public LauncherMenuInstrumentationTest() {
        super(Launcher.class);
    }

    public void testHomeLongPressMenuExposesAddActionsDirectly() {
        Launcher launcher = getActivity();
        LauncherDialogAdapter adapter = new LauncherDialogAdapter(launcher);

        assertEquals(5, adapter.getCount());
        assertItem(adapter, 0, R.string.group_add_widgets, 0);
        assertItem(adapter, 1, R.string.group_add_shortcuts, 1);
        assertItem(adapter, 2, R.string.group_add_folders, 2);
        assertItem(adapter, 3, R.string.menu_wallpaper, 3);
        assertItem(adapter, 4, R.string.menu_preferences, 4);
    }

    public void testOptionsAddMenuRetainsThreeAddActions() {
        Launcher launcher = getActivity();
        AddDialogAdapter adapter = new AddDialogAdapter(launcher);

        assertEquals(3, adapter.getCount());
        assertItem(adapter, 0, R.string.group_add_widgets, 0);
        assertItem(adapter, 1, R.string.group_add_shortcuts, 1);
        assertItem(adapter, 2, R.string.group_add_folders, 2);
    }

    private void assertItem(LauncherDialogAdapter adapter, int position,
            int textResourceId, int actionTag) {
        LauncherDialogAdapter.ListItem item =
                (LauncherDialogAdapter.ListItem) adapter.getItem(position);
        assertEquals(getActivity().getString(textResourceId), item.mText);
        assertEquals(actionTag, item.mActionTag);
    }

    private void assertItem(AddDialogAdapter adapter, int position,
            int textResourceId, int actionTag) {
        AddDialogAdapter.ListItem item =
                (AddDialogAdapter.ListItem) adapter.getItem(position);
        assertEquals(getActivity().getString(textResourceId), item.mText);
        assertEquals(actionTag, item.mActionTag);
    }
}
