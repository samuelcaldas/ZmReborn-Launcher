package org.zmreborn;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Normalizes queries and filters application items by prefix and substring matching with diacritics removal.
 */
final class DrawerSearchFilter {
    private DrawerSearchFilter() {
    }

    /**
     * Filters the given list of applications matching the query text.
     */
    static ArrayList<ApplicationItemInfo> filter(
            List<ApplicationItemInfo> source,
            CharSequence query) {
        ArrayList<ApplicationItemInfo> snapshot = snapshot(source);
        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() == 0) {
            return snapshot;
        }
        ArrayList<ApplicationItemInfo> prefixMatches = new ArrayList<>();
        ArrayList<ApplicationItemInfo> containsMatches = new ArrayList<>();
        for (ApplicationItemInfo item : snapshot) {
            addMatch(item, normalizedQuery, prefixMatches, containsMatches);
        }
        prefixMatches.addAll(containsMatches);
        return prefixMatches;
    }

    /**
     * Returns true if the query string is empty or contains only whitespace/diacritics.
     */
    static boolean isEmptyQuery(CharSequence query) {
        return normalize(query).length() == 0;
    }

    /**
     * Normalizes text by decomposing diacritics, lowercasing, and stripping whitespace.
     */
    static String normalize(CharSequence value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        StringBuilder normalized = new StringBuilder(decomposed.length());
        for (int index = 0; index < decomposed.length(); index++) {
            appendSearchCharacter(normalized, decomposed.charAt(index));
        }
        return normalized.toString().toLowerCase(Locale.ROOT).trim();
    }

    private static void addMatch(
            ApplicationItemInfo item,
            String query,
            ArrayList<ApplicationItemInfo> prefixMatches,
            ArrayList<ApplicationItemInfo> containsMatches) {
        String title = normalize(item == null ? null : item.title);
        if (title.startsWith(query)) {
            prefixMatches.add(item);
            return;
        }
        if (title.contains(query)) {
            containsMatches.add(item);
        }
    }

    private static void appendSearchCharacter(StringBuilder normalized, char character) {
        int type = Character.getType(character);
        if (type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK) {
            return;
        }
        normalized.append(character);
    }

    private static ArrayList<ApplicationItemInfo> snapshot(
            List<ApplicationItemInfo> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(source);
    }
}
