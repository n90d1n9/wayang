package tech.kayys.wayang.a2ui.wayang.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Small collection coercion helpers shared by A2UI decoders.
 */
public final class DecodeCollections {

    public static List<String> distinctTokens(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(DecodeValues::text)
                    .filter(text -> !text.isBlank())
                    .distinct()
                    .toList();
        }
        String text = DecodeValues.text(value);
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    public static List<String> texts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(DecodeValues::text)
                    .toList();
        }
        String text = DecodeValues.text(value);
        return text.isBlank() ? List.of() : List.of(text);
    }

    public static List<String> rawTexts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(DecodeValues::rawText)
                    .toList();
        }
        String text = DecodeValues.rawText(value);
        return text.isBlank() ? List.of() : List.of(text);
    }

    public static List<String> nonBlankTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(DecodeValues::text)
                .filter(text -> !text.isBlank())
                .toList();
    }

    public static List<String> distinctNonBlankTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(DecodeValues::text)
                .filter(text -> !text.isBlank())
                .distinct()
                .toList();
    }

    public static Set<String> commaSeparatedTextSet(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(DecodeValues::text)
                    .filter(text -> !text.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Arrays.stream(DecodeValues.text(value).split(","))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static List<Integer> integers(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(DecodeValues::integerOrNull)
                    .filter(Objects::nonNull)
                    .toList();
        }
        Integer integer = DecodeValues.integerOrNull(value);
        return RecordCollections.singletonOrEmpty(integer);
    }

    public static List<Map<String, Object>> maps(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(DecodeCollections::copyMap)
                    .toList();
        }
        if (value instanceof Map<?, ?> map) {
            return List.of(copyMap(map));
        }
        return List.of();
    }

    private static Map<String, Object> copyMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(String.valueOf(key), copyValue(value));
            }
        });
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(DecodeCollections::copyValue)
                    .toList();
        }
        return value;
    }

    private DecodeCollections() {
    }
}
