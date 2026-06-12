package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One locally verifiable Agentic Commerce spec-alignment expectation.
 */
public record AgenticCommerceSpecAlignmentRequirement(
        String id,
        String category,
        String title,
        boolean aligned,
        Map<String, Object> expected,
        Map<String, Object> actual,
        String message) {

    public AgenticCommerceSpecAlignmentRequirement {
        id = required(id, "spec alignment requirement id");
        category = required(category, "spec alignment requirement category");
        title = required(title, "spec alignment requirement title");
        expected = AgenticCommerceMaps.copy(expected);
        actual = AgenticCommerceMaps.copy(actual);
        message = text(message);
    }

    public static AgenticCommerceSpecAlignmentRequirement aligned(
            String id,
            String category,
            String title,
            Map<String, Object> expected,
            Map<String, Object> actual) {
        return new AgenticCommerceSpecAlignmentRequirement(
                id,
                category,
                title,
                true,
                expected,
                actual,
                "");
    }

    public static AgenticCommerceSpecAlignmentRequirement gap(
            String id,
            String category,
            String title,
            Map<String, Object> expected,
            Map<String, Object> actual,
            String message) {
        return new AgenticCommerceSpecAlignmentRequirement(
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
        return AgenticCommerceMaps.copy(values);
    }

    private static String required(String value, String name) {
        String normalized = text(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
