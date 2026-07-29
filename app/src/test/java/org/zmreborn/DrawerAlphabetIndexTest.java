package org.zmreborn;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawerAlphabetIndexTest {
    @Test
    public void indexNormalizesAccentsAndRetainsOriginalPositions() {
        ArrayList<ApplicationItemInfo> applications = items(
                item("Beta"), item("Álbum"), item("2Factor"), item(null), item("Zulu"));

        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(applications);

        assertEquals(4, index.size());
        assertEquals("#", index.getSectionAt(0));
        assertEquals(2, index.getPositionAt(0));
        assertEquals("A", index.getSectionAt(1));
        assertEquals(1, index.getPositionAt(1));
        assertEquals("B", index.getSectionAt(2));
        assertEquals(0, index.getPositionAt(2));
        assertEquals("Z", index.getSectionAt(3));
        assertEquals(4, index.getPositionAt(3));
        assertEquals(5, applications.size());
    }

    @Test
    public void indexUsesFirstPositionForRepeatedSection() {
        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(items(
                item("Alpha"), item("Apple"), item("Beta")));

        assertTrue(index.hasMultipleSections());
        assertEquals(0, index.getPositionAt(index.indexOf("A")));
        assertEquals(2, index.getPositionAt(index.indexOf("B")));
    }

    @Test
    public void compactIndexKeepsFirstAndLastReachableSections() {
        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(items(
                item("Alpha"), item("Bravo"), item("Charlie"),
                item("Delta"), item("Echo"), item("Zulu")));

        DrawerAlphabetIndex compact = index.compact(3);

        assertEquals(3, compact.size());
        assertEquals("A", compact.getSectionAt(0));
        assertEquals("Z", compact.getSectionAt(2));
        assertEquals(5, compact.getPositionAt(2));
    }

    @Test
    public void compactIndexRetainsSelectedReachableSection() {
        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(items(
                item("Alpha"), item("Bravo"), item("Charlie"),
                item("Delta"), item("Echo"), item("Zulu")));

        DrawerAlphabetIndex compact = index.compact(3, "D");

        assertEquals(3, compact.size());
        assertEquals("A", compact.getSectionAt(0));
        assertEquals("D", compact.getSectionAt(1));
        assertEquals("Z", compact.getSectionAt(2));
    }

    @Test
    public void compactTwoSectionsRetainsActiveInteriorSection() {
        DrawerAlphabetIndex index = DrawerAlphabetIndex.from(items(
                item("Alpha"), item("Bravo"), item("Zulu")));

        DrawerAlphabetIndex compact = index.compact(2, "B");

        assertEquals(2, compact.size());
        assertEquals("A", compact.getSectionAt(0));
        assertEquals("B", compact.getSectionAt(1));
        assertEquals(1, compact.getPositionAt(1));
    }

    @Test
    public void emptyAndSingleSectionIndexesDoNotOfferFastScroll() {
        assertFalse(DrawerAlphabetIndex.from(null).hasMultipleSections());
        assertFalse(DrawerAlphabetIndex.from(items(item("Alpha"), item("Apple")))
                .hasMultipleSections());
    }

    private static ApplicationItemInfo item(String title) {
        ApplicationItemInfo item = new ApplicationItemInfo();
        item.title = title;
        item.componentName = title == null ? "untitled" : title;
        return item;
    }

    private static ArrayList<ApplicationItemInfo> items(ApplicationItemInfo... applications) {
        return new ArrayList<ApplicationItemInfo>(Arrays.asList(applications));
    }
}
