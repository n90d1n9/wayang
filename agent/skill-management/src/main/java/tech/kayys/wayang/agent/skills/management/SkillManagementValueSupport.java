package tech.kayys.wayang.agent.skills.management;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared normalization helpers for skill-management value objects.
 */
final class SkillManagementValueSupport {

    private SkillManagementValueSupport() {
    }

    static String text(String value) {
        return value == null ? "" : value;
    }

    static String identifier(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    static int nonNegative(int value) {
        return Math.max(0, value);
    }

    static long nonNegative(long value) {
        return Math.max(0, value);
    }

    static int atLeast(int value, int minimum) {
        return Math.max(nonNegative(value), nonNegative(minimum));
    }

    static Map<String, Integer> nonNegativeCounts(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, Integer> copy = new TreeMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(key, nonNegative(value));
            }
        });
        return Collections.unmodifiableMap(copy);
    }

    static boolean booleanAttribute(Map<String, String> attributes, String name) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(attributes.getOrDefault(name, "false"));
    }

    static int nonNegativeIntAttribute(Map<String, String> attributes, String name) {
        if (attributes == null || attributes.isEmpty()) {
            return 0;
        }
        String value = attributes.get(name);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return nonNegative(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static boolean hasAttributePrefix(Map<String, String> attributes, String prefix) {
        if (attributes == null || attributes.isEmpty() || prefix == null || prefix.isBlank()) {
            return false;
        }
        return attributes.keySet().stream()
                .filter(Objects::nonNull)
                .anyMatch(name -> name.startsWith(prefix));
    }

    static List<String> compactStrings(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    static List<String> sortedDistinctStrings(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    static List<String> sortedDistinctCompactStrings(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    static <T> List<T> nonNullList(List<T> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    static String joinedMessage(List<String> values) {
        return String.join("; ", values == null ? List.of() : values);
    }

    static <T> long countMatching(List<T> values, Predicate<T> predicate) {
        return nonNullList(values).stream()
                .filter(predicate)
                .count();
    }

    static <T, R> long countBy(List<T> values, Function<T, R> valueExtractor, R expected) {
        return countMatching(values, value -> Objects.equals(valueExtractor.apply(value), expected));
    }
}
