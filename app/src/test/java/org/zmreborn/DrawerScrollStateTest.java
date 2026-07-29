package org.zmreborn;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DrawerScrollStateTest {
    @Test
    public void stableAnchorSurvivesListInsertion() {
        ApplicationItemInfo anchor = item("anchor");
        DrawerScrollState state = DrawerScrollState.capture(anchor, 1, -12);
        ArrayList<ApplicationItemInfo> updated = items(
                item("new"), item("first"), anchor, item("last"));

        assertEquals(2, state.resolvePosition(updated));
        assertEquals(-12, state.getTopOffset());
    }

    @Test
    public void missingAnchorFallsBackToClampedValidPosition() {
        DrawerScrollState state = DrawerScrollState.capture(item("removed"), 8, 4);

        assertEquals(1, state.resolvePosition(items(item("first"), item("second"))));
    }

    @Test
    public void emptyListHasNoRestorablePosition() {
        DrawerScrollState state = DrawerScrollState.capture(item("anchor"), 0, 0);

        assertEquals(-1, state.resolvePosition(new ArrayList<ApplicationItemInfo>()));
    }

    private static ApplicationItemInfo item(String component) {
        ApplicationItemInfo item = new ApplicationItemInfo();
        item.componentName = component;
        item.title = component;
        return item;
    }

    private static ArrayList<ApplicationItemInfo> items(ApplicationItemInfo... items) {
        return new ArrayList<ApplicationItemInfo>(Arrays.asList(items));
    }
}
