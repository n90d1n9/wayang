package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Small collection coercion helpers shared by A2UI decoders.
 */
final class WayangA2uiDecodeCollections {

    static List<String> distinctTokens(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(WayangA2uiDecodeValues::text)
                    .filter(text -> !text.isBlank())
                    .distinct()
                    .toList();
        }
        String text = WayangA2uiDecodeValues.text(value);
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    static List<String> texts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(WayangA2uiDecodeValues::text)
                    .toList();
        }
        String text = WayangA2uiDecodeValues.text(value);
        return text.isBlank() ? List.of() : List.of(text);
    }

    static List<String> rawTexts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(WayangA2uiDecodeValues::rawText)
                    .toList();
        }
        String text = WayangA2uiDecodeValues.rawText(value);
        return text.isBlank() ? List.of() : List.of(text);
    }

    static List<String> nonBlankTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangA2uiDecodeValues::text)
                .filter(text -> !text.isBlank())
                .toList();
    }

    static List<String> distinctNonBlankTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangA2uiDecodeValues::text)
                .filter(text -> !text.isBlank())
                .distinct()
                .toList();
    }

    static List<Integer> integers(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(WayangA2uiDecodeValues::integerOrNull)
                    .filter(Objects::nonNull)
                    .toList();
        }
        Integer integer = WayangA2uiDecodeValues.integerOrNull(value);
        return integer == null ? List.of() : List.of(integer);
    }

    static List<Map<String, Object>> maps(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(WayangA2uiTransportMaps::copy)
                    .toList();
        }
        if (value instanceof Map<?, ?> map) {
            return List.of(WayangA2uiTransportMaps.copy(map));
        }
        return List.of();
    }

    private WayangA2uiDecodeCollections() {
    }
}
