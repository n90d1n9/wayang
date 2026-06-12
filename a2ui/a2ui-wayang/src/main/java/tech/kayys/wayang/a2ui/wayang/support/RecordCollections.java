package tech.kayys.wayang.a2ui.wayang.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Small collection normalization helpers for A2UI record constructors.
 */
public final class RecordCollections {

    private RecordCollections() {
    }

    public static <T> List<T> copyList(List<? extends T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }

    public static <T> List<T> singletonOrEmpty(T value) {
        return value == null ? List.of() : List.of(value);
    }

    public static <T> Set<T> copySet(Set<? extends T> values) {
        return values == null || values.isEmpty() ? Set.of() : Set.copyOf(values);
    }

    public static <T> List<T> nonNullList(List<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<T> normalized = new ArrayList<>();
        for (T value : values) {
            if (value != null) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    @SafeVarargs
    public static <T> List<T> nonNullVarargs(T... values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        List<T> normalized = new ArrayList<>();
        for (T value : values) {
            if (value != null) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    public static List<String> nonBlankStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    public static Set<String> trimmedNonBlankStringSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        values.stream()
                .map(RecordCollections::text)
                .filter(value -> !value.isBlank())
                .forEach(normalized::add);
        return Set.copyOf(normalized);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
