package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One locally verifiable A2UI spec-alignment expectation.
 */
public record WayangA2uiSpecAlignmentRequirement(
        String id,
        String category,
        String title,
        boolean aligned,
        Map<String, Object> expected,
        Map<String, Object> actual,
        String message) {

    public WayangA2uiSpecAlignmentRequirement {
        id = required(id, "A2UI spec alignment requirement id");
        category = required(category, "A2UI spec alignment requirement category");
        title = required(title, "A2UI spec alignment requirement title");
        expected = copy(expected);
        actual = copy(actual);
        message = WayangA2uiDecodeValues.text(message);
    }

    public static WayangA2uiSpecAlignmentRequirement aligned(
            String id,
            String category,
            String title,
            Map<String, Object> expected,
            Map<String, Object> actual) {
        return new WayangA2uiSpecAlignmentRequirement(
                id,
                category,
                title,
                true,
                expected,
                actual,
                "");
    }

    public static WayangA2uiSpecAlignmentRequirement gap(
            String id,
            String category,
            String title,
            Map<String, Object> expected,
            Map<String, Object> actual,
            String message) {
        return new WayangA2uiSpecAlignmentRequirement(
                id,
                category,
                title,
                false,
                expected,
                actual,
                message);
    }

    public Map<String, Object> toMap() {
        return WayangA2uiSpecAlignmentProjection.requirement(this);
    }

    private static String required(String value, String name) {
        String normalized = WayangA2uiDecodeValues.text(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static Map<String, Object> copy(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(String.valueOf(key), value);
            }
        });
        return WayangA2uiTransportMaps.freeze(copy);
    }
}
