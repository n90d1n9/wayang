package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpHeaderValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

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
        contentType = DecodeValues.text(contentType, WayangA2uiTransportContent.MIME_JSON);
        body = DecodeValues.rawText(body);
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
        return HttpEndpointProjection.response(this);
    }

    private static Map<String, List<String>> normalizeHeaders(Map<?, ?> values, String contentType) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((name, value) -> {
                if (name != null) {
                    List<String> headerValues = HttpHeaderValues.values(value);
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

    private static boolean containsHeader(Map<String, ?> headers, String name) {
        String normalized = normalizeHeaderName(name);
        return headers.keySet().stream()
                .map(WayangA2uiHttpEndpointResponse::normalizeHeaderName)
                .anyMatch(normalized::equals);
    }

    private static String normalizeHeaderName(String value) {
        return DecodeValues.text(value).toLowerCase(Locale.ROOT);
    }
}
