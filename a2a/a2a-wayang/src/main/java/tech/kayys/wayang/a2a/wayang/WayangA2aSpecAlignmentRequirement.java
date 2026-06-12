package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One locally verifiable A2A spec-alignment expectation.
 */
public record WayangA2aSpecAlignmentRequirement(
        String id,
        String category,
        String title,
        boolean aligned,
        Map<String, Object> expected,
        Map<String, Object> actual,
        String message) {

    public WayangA2aSpecAlignmentRequirement {
        id = WayangA2aMaps.required(id, "A2A spec alignment requirement id");
        category = WayangA2aMaps.required(category, "A2A spec alignment requirement category");
        title = WayangA2aMaps.required(title, "A2A spec alignment requirement title");
        expected = WayangA2aMaps.copyMap(expected);
        actual = WayangA2aMaps.copyMap(actual);
        message = WayangA2aMaps.optional(message) == null ? "" : message.trim();
    }

    public static WayangA2aSpecAlignmentRequirement aligned(
            String id,
            String category,
            String title,
            Map<String, Object> expected,
            Map<String, Object> actual) {
        return new WayangA2aSpecAlignmentRequirement(
                id,
                category,
                title,
                true,
                expected,
                actual,
                "");
    }

    public static WayangA2aSpecAlignmentRequirement gap(
            String id,
            String category,
            String title,
            Map<String, Object> expected,
            Map<String, Object> actual,
            String message) {
        return new WayangA2aSpecAlignmentRequirement(
                id,
                category,
                title,
                false,
                expected,
                actual,
                message);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("category", category);
        values.put("title", title);
        values.put("aligned", aligned);
        values.put("expected", expected);
        values.put("actual", actual);
        if (!message.isBlank()) {
            values.put("message", message);
        }
        return WayangA2aMaps.copyMap(values);
    }
}
