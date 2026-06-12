package tech.kayys.wayang.a2ui.wayang.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Small collection ordering helpers for stable A2UI projections.
 */
public final class ProjectionCollections {

    private ProjectionCollections() {
    }

    public static List<String> sortedStrings(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return RecordCollections.nonNullList(values.stream().toList()).stream()
                .sorted()
                .toList();
    }

    public static List<String> referenceOrderThenSortedRemainder(
            List<String> referenceOrder,
            List<String> values) {
        List<String> normalizedValues = RecordCollections.nonNullList(values);
        if (normalizedValues.isEmpty()) {
            return List.of();
        }
        List<String> reference = RecordCollections.nonNullList(referenceOrder);
        List<String> ordered = new ArrayList<>();
        reference.stream()
                .filter(normalizedValues::contains)
                .forEach(ordered::add);
        normalizedValues.stream()
                .filter(value -> !reference.contains(value))
                .sorted()
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }
}
