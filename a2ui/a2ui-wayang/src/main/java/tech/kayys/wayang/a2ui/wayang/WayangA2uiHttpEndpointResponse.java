package tech.kayys.wayang.a2ui.wayang;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Framework-friendly projection of a dependency-free A2UI HTTP response.
 */
public record WayangA2uiHttpEndpointResponse(
        int statusCode,
        String contentType,
        String body,
        Map<String, List<String>> headers) {

    public WayangA2uiHttpEndpointResponse {
        statusCode = statusCode <= 0 ? 200 : statusCode;
        contentType = WayangA2uiDecodeValues.text(contentType, WayangA2uiTransportContent.MIME_JSON);
        body = WayangA2uiDecodeValues.rawText(body);
        headers = normalizeHeaders(headers, contentType);
    }

    public static WayangA2uiHttpEndpointResponse from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        return new WayangA2uiHttpEndpointResponse(
                resolved.statusCode(),
                resolved.contentType(),
                resolved.body(),
                normalizeHeaders(resolved.headers(), resolved.contentType()));
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public Optional<List<String>> header(String name) {
        String normalized = normalizeHeaderName(name);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(entry -> normalizeHeaderName(entry.getKey()).equals(normalized))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<String> firstHeader(String name) {
        return header(name)
                .flatMap(values -> values.stream().findFirst());
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpEndpointProjection.response(this);
    }

    private static Map<String, List<String>> normalizeHeaders(Map<?, ?> values, String contentType) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((name, value) -> {
                if (name != null) {
                    List<String> headerValues = headerValues(value);
                    if (!headerValues.isEmpty()) {
                        headers.put(String.valueOf(name), headerValues);
                    }
                }
            });
        }
        if (!containsHeader(headers, WayangA2uiHttpResponse.HEADER_CONTENT_TYPE)) {
            headers.put(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, List.of(contentType));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    private static List<String> headerValues(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(WayangA2uiHttpEndpointResponse::headerValues).orElse(List.of());
        }
        if (value instanceof Iterable<?> values) {
            return iterableValues(values);
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            return arrayValues(value);
        }
        String normalized = WayangA2uiDecodeValues.text(value);
        return normalized.isBlank() ? List.of() : List.of(normalized);
    }

    private static List<String> iterableValues(Iterable<?> values) {
        java.util.ArrayList<String> normalized = new java.util.ArrayList<>();
        for (Object value : values) {
            normalized.addAll(headerValues(value));
        }
        return List.copyOf(normalized);
    }

    private static List<String> arrayValues(Object values) {
        java.util.ArrayList<String> normalized = new java.util.ArrayList<>();
        int length = Array.getLength(values);
        for (int i = 0; i < length; i++) {
            normalized.addAll(headerValues(Array.get(values, i)));
        }
        return List.copyOf(normalized);
    }

    private static boolean containsHeader(Map<String, ?> headers, String name) {
        String normalized = normalizeHeaderName(name);
        return headers.keySet().stream()
                .map(WayangA2uiHttpEndpointResponse::normalizeHeaderName)
                .anyMatch(normalized::equals);
    }

    private static String normalizeHeaderName(String value) {
        return WayangA2uiDecodeValues.text(value).toLowerCase(Locale.ROOT);
    }
}
