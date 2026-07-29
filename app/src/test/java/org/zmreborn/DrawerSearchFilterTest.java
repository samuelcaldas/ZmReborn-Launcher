package org.zmreborn;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class DrawerSearchFilterTest {
    @Test
    public void filteringIgnoresCaseAndAccents() {
        ArrayList<ApplicationItemInfo> source = items(
                item("Câmera", "camera"),
                item("Calculadora", "calculator"),
                item("Mapa da cidade", "map"));

        ArrayList<ApplicationItemInfo> filtered = DrawerSearchFilter.filter(source, "CAMERA");

        assertEquals(1, filtered.size());
        assertEquals("component:camera", filtered.get(0).getStableKey());
    }

    @Test
    public void prefixMatchesPrecedeContainsMatchesWithoutMutatingSource() {
        ApplicationItemInfo contains = item("My Camera", "contains");
        ApplicationItemInfo prefix = item("Camera", "prefix");
        ApplicationItemInfo secondPrefix = item("Camera Pro", "second-prefix");
        ArrayList<ApplicationItemInfo> source = items(contains, prefix, secondPrefix);

        ArrayList<ApplicationItemInfo> filtered = DrawerSearchFilter.filter(source, "cam");

        assertEquals(Arrays.asList(prefix, secondPrefix, contains), filtered);
        assertEquals(Arrays.asList(contains, prefix, secondPrefix), source);
    }

    @Test
    public void clearingSearchReturnsIndependentSnapshot() {
        ArrayList<ApplicationItemInfo> source = items(item("Clock", "clock"));

        ArrayList<ApplicationItemInfo> filtered = DrawerSearchFilter.filter(source, "  ");

        assertEquals(source, filtered);
        assertNotSame(source, filtered);
        filtered.clear();
        assertEquals(1, source.size());
    }

    @Test
    public void nullTitlesDoNotMatchOrCrash() {
        ApplicationItemInfo untitled = item(null, "untitled");

        assertEquals(0, DrawerSearchFilter.filter(items(untitled), "app").size());
    }

    private static ApplicationItemInfo item(String title, String component) {
        ApplicationItemInfo item = new ApplicationItemInfo();
        item.title = title;
        item.componentName = component;
        return item;
    }

    @SafeVarargs
    private static ArrayList<ApplicationItemInfo> items(ApplicationItemInfo... items) {
        return new ArrayList<ApplicationItemInfo>(Arrays.asList(items));
    }
}
