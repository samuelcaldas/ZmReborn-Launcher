package org.zmreborn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class DrawerAlphabetIndex {
    private static final String OTHER_SECTION = "#";

    private final ArrayList<String> mSections;
    private final ArrayList<Integer> mPositions;

    private DrawerAlphabetIndex(
            ArrayList<String> sections, ArrayList<Integer> positions) {
        this.mSections = sections;
        this.mPositions = positions;
    }

    static DrawerAlphabetIndex from(List<ApplicationItemInfo> applications) {
        TreeMap<String, Integer> positions = new TreeMap<String, Integer>();
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
                positions.put(section, Integer.valueOf(position));
            }
        }
    }

    private static DrawerAlphabetIndex create(Map<String, Integer> positions) {
        ArrayList<String> sections = new ArrayList<String>(positions.keySet());
        Collections.sort(sections);
        ArrayList<Integer> indexedPositions = new ArrayList<Integer>();
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
        if (maximumSections >= this.mSections.size()) {
            return this;
        }
        if (maximumSections < 2) {
            return from(null);
        }
        DrawerAlphabetIndex compactIndex = compactWithoutRetention(maximumSections);
        return compactIndex.retainSection(this, retainedSection);
    }

    private DrawerAlphabetIndex compactWithoutRetention(int maximumSections) {
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        for (int slot = 0; slot < maximumSections; slot++) {
            int index = Math.round(slot * (this.mSections.size() - 1)
                    / (float) (maximumSections - 1));
            addSection(index, sections, positions);
        }
        return new DrawerAlphabetIndex(sections, positions);
    }

    private DrawerAlphabetIndex retainSection(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        if (!canRetainSection(sourceIndex, retainedSection)) {
            return this;
        }
        TreeMap<String, Integer> positions = indexedPositions();
        positions.remove(getSectionAt(sectionToReplaceIndex(
                sourceIndex, retainedSection)));
        positions.put(retainedSection, Integer.valueOf(sourceIndex.getPositionAt(
                sourceIndex.indexOf(retainedSection))));
        return create(positions);
    }

    private boolean canRetainSection(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        return retainedSection != null && retainedSection.length() > 0
                && this.mSections.size() >= 2
                && this.indexOf(retainedSection) < 0
                && sourceIndex.indexOf(retainedSection) >= 0;
    }

    private TreeMap<String, Integer> indexedPositions() {
        TreeMap<String, Integer> positions = new TreeMap<String, Integer>();
        for (int index = 0; index < this.mSections.size(); index++) {
            positions.put(getSectionAt(index), Integer.valueOf(getPositionAt(index)));
        }
        return positions;
    }

    private int sectionToReplaceIndex(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        if (this.mSections.size() == 2) {
            return farthestEndpointIndex(sourceIndex, retainedSection);
        }
        return closestInteriorSectionIndex(sourceIndex, retainedSection);
    }

    private int farthestEndpointIndex(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        int retainedIndex = sourceIndex.indexOf(retainedSection);
        int firstDistance = Math.abs(sourceIndex.indexOf(getSectionAt(0)) - retainedIndex);
        int lastIndex = this.mSections.size() - 1;
        int lastDistance = Math.abs(sourceIndex.indexOf(getSectionAt(lastIndex)) - retainedIndex);
        return firstDistance > lastDistance ? 0 : lastIndex;
    }

    private int closestInteriorSectionIndex(
            DrawerAlphabetIndex sourceIndex, String retainedSection) {
        int retainedIndex = sourceIndex.indexOf(retainedSection);
        int closestIndex = 1;
        int minimumDistance = Integer.MAX_VALUE;
        for (int index = 1; index < this.mSections.size() - 1; index++) {
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
            int index, ArrayList<String> sections, ArrayList<Integer> positions) {
        String section = this.mSections.get(index);
        if (sections.contains(section)) {
            return;
        }
        sections.add(section);
        positions.add(this.mPositions.get(index));
    }

    boolean hasMultipleSections() {
        return this.mSections.size() > 1;
    }

    int size() {
        return this.mSections.size();
    }

    String getSectionAt(int index) {
        return this.mSections.get(index);
    }

    int getPositionAt(int index) {
        return this.mPositions.get(index).intValue();
    }

    int indexOf(String section) {
        return this.mSections.indexOf(section);
    }
}
