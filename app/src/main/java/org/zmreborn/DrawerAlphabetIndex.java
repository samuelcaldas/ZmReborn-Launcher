package org.zmreborn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Alphabetical index mapping letter sections to first-match application list positions.
 */
final class DrawerAlphabetIndex {
    private static final String OTHER_SECTION = "#";

    private final ArrayList<String> sections;
    private final ArrayList<Integer> positions;

    private DrawerAlphabetIndex(
            ArrayList<String> sections, ArrayList<Integer> positions) {
        this.sections = sections;
        this.positions = positions;
    }

    /**
     * Builds an alphabet index from an ordered application list.
     */
    static DrawerAlphabetIndex from(List<ApplicationItemInfo> applications) {
        TreeMap<String, Integer> positions = new TreeMap<>();
        if (applications != null) {
            indexApplications(applications, positions);
        }
        return create(positions);
    }

    private static void indexApplications(
            List<ApplicationItemInfo> applications, Map<String, Integer> positions) {
        for (int position = 0; position < applications.size(); position++) {
            String section = sectionFor(applications.get(position));
            if (!positions.containsKey(section)) {
                positions.put(section, position);
            }
        }
    }

    private static DrawerAlphabetIndex create(Map<String, Integer> positions) {
        ArrayList<String> sections = new ArrayList<>(positions.keySet());
        Collections.sort(sections);
        ArrayList<Integer> indexedPositions = new ArrayList<>();
        for (String section : sections) {
            indexedPositions.add(positions.get(section));
        }
        return new DrawerAlphabetIndex(sections, indexedPositions);
    }

    private static String sectionFor(ApplicationItemInfo application) {
        String normalized = DrawerSearchFilter.normalize(
                application == null ? null : application.title);
        if (normalized.length() == 0) {
            return OTHER_SECTION;
        }
        char firstCharacter = normalized.charAt(0);
        if (firstCharacter < 'a' || firstCharacter > 'z') {
            return OTHER_SECTION;
        }
        return String.valueOf((char) (firstCharacter - ('a' - 'A')));
    }

    DrawerAlphabetIndex compact(int maximumSections) {
        return compact(maximumSections, "");
    }

    DrawerAlphabetIndex compact(int maximumSections, String retainedSection) {
        if (maximumSections >= this.sections.size()) {
            return this;
        }
        if (maximumSections < 2) {
            return from(null);
        }
        DrawerAlphabetIndex compactIndex = compactWithoutRetention(maximumSections);
        return compactIndex.retainSection(this, retainedSection);
    }

    private DrawerAlphabetIndex compactWithoutRetention(int maximumSections) {
        ArrayList<String> compactedSections = new ArrayList<>();
        ArrayList<Integer> compactedPositions = new ArrayList<>();
        for (int slot = 0; slot < maximumSections; slot++) {
            int index = Math.round(slot * (this.sections.size() - 1)
                    / (float) (maximumSections - 1));
            addSection(index, compactedSections, compactedPositions);
        }
        return new DrawerAlphabetIndex(compactedSections, compactedPositions);
    }

    private DrawerAlphabetIndex retainSection(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        if (!canRetainSection(sourceIndex, retainedSection)) {
            return this;
        }
        TreeMap<String, Integer> currentPositions = indexedPositions();
        currentPositions.remove(getSectionAt(sectionToReplaceIndex(
                sourceIndex, retainedSection)));
        currentPositions.put(retainedSection, sourceIndex.getPositionAt(
                sourceIndex.indexOf(retainedSection)));
        return create(currentPositions);
    }

    private boolean canRetainSection(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        return retainedSection != null && retainedSection.length() > 0
                && this.sections.size() >= 2
                && this.indexOf(retainedSection) < 0
                && sourceIndex.indexOf(retainedSection) >= 0;
    }

    private TreeMap<String, Integer> indexedPositions() {
        TreeMap<String, Integer> indexed = new TreeMap<>();
        for (int index = 0; index < this.sections.size(); index++) {
            indexed.put(getSectionAt(index), getPositionAt(index));
        }
        return indexed;
    }

    private int sectionToReplaceIndex(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        if (this.sections.size() == 2) {
            return farthestEndpointIndex(sourceIndex, retainedSection);
        }
        return closestInteriorSectionIndex(sourceIndex, retainedSection);
    }

    private int farthestEndpointIndex(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        int retainedIndex = sourceIndex.indexOf(retainedSection);
        int firstDistance = Math.abs(sourceIndex.indexOf(getSectionAt(0)) - retainedIndex);
        int lastIndex = this.sections.size() - 1;
        int lastDistance = Math.abs(sourceIndex.indexOf(getSectionAt(lastIndex)) - retainedIndex);
        return firstDistance > lastDistance ? 0 : lastIndex;
    }

    private int closestInteriorSectionIndex(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        int retainedIndex = sourceIndex.indexOf(retainedSection);
        int closestIndex = 1;
        int minimumDistance = Integer.MAX_VALUE;
        for (int index = 1; index < this.sections.size() - 1; index++) {
            int sourceDistance = Math.abs(sourceIndex.indexOf(getSectionAt(index))
                    - retainedIndex);
            if (sourceDistance < minimumDistance) {
                closestIndex = index;
                minimumDistance = sourceDistance;
            }
        }
        return closestIndex;
    }

    private void addSection(
            int index, ArrayList<String> targetSections, ArrayList<Integer> targetPositions) {
        String section = this.sections.get(index);
        if (targetSections.contains(section)) {
            return;
        }
        targetSections.add(section);
        targetPositions.add(this.positions.get(index));
    }

    boolean hasMultipleSections() {
        return this.sections.size() > 1;
    }

    int size() {
        return this.sections.size();
    }

    String getSectionAt(int index) {
        return this.sections.get(index);
    }

    int getPositionAt(int index) {
        return this.positions.get(index);
    }

    int indexOf(String section) {
        return this.sections.indexOf(section);
    }
}
