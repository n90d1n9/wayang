package tech.kayys.wayang.a2ui.wayang;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Dependency-free HTTP-shaped request for A2UI bridge adapters.
 */
public record WayangA2uiHttpRequest(
        String method,
        String path,
        String body,
        Map<String, Object> headers,
        Map<String, Object> attributes) {

    public WayangA2uiHttpRequest {
        method = normalizeMethod(method);
        path = normalizePath(path);
        body = body == null ? "" : body;
        headers = WayangA2uiTransportMaps.copy(headers);
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpRequest get(String path) {
        return new WayangA2uiHttpRequest("GET", path, "", Map.of(), Map.of());
    }

    public static WayangA2uiHttpRequest postJson(String path, String body) {
        return new WayangA2uiHttpRequest(
                "POST",
                path,
                body,
                Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, WayangA2uiTransportContent.MIME_JSON),
                Map.of());
    }

    public static WayangA2uiHttpRequest exchange(String requestEnvelopeJson) {
        return postJson(WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE, requestEnvelopeJson);
    }

    public static WayangA2uiHttpRequest surfaceCatalog() {
        return get(WayangA2uiHttpBridgeAdapter.PATH_SURFACE_CATALOG);
    }

    public static WayangA2uiHttpRequest routeCatalog() {
        return get(WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG);
    }

    public static WayangA2uiHttpRequest bindingReport() {
        return get(WayangA2uiHttpBridgeAdapter.PATH_BINDING_REPORT);
    }

    public static WayangA2uiHttpRequest smoke() {
        return get(WayangA2uiHttpBridgeAdapter.PATH_SMOKE);
    }

    public static WayangA2uiHttpRequest readiness() {
        return get(WayangA2uiHttpBridgeAdapter.PATH_READINESS);
    }

    public boolean method(String expectedMethod) {
        return method.equals(normalizeMethod(expectedMethod));
    }

    public boolean path(String expectedPath) {
        return path.equals(normalizePath(expectedPath));
    }

    public Optional<String> header(String name) {
        String normalizedName = normalizeHeaderName(name);
        if (normalizedName.isBlank()) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(entry -> normalizeHeaderName(entry.getKey()).equals(normalizedName))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(value -> String.valueOf(value).trim())
                .findFirst();
    }

    public String contentType() {
        return header(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE).orElse("");
    }

    public boolean contentType(String expectedContentType) {
        String expected = normalizeMediaType(expectedContentType);
        return !expected.isBlank() && normalizeMediaType(contentType()).equals(expected);
    }

    public String accept() {
        return header(WayangA2uiHttpResponse.HEADER_ACCEPT).orElse("");
    }

    public boolean accepts(String responseContentType) {
        String expected = normalizeMediaType(responseContentType);
        if (expected.isBlank()) {
            return false;
        }
        String accepted = accept();
        if (accepted.isBlank()) {
            return true;
        }
        for (String candidate : accepted.split(",")) {
            if (acceptedMediaType(candidate, expected)) {
                return true;
            }
        }
        return false;
    }

    public WayangA2uiHttpRequest withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiHttpRequest(
                method,
                path,
                body,
                headers,
                WayangA2uiTransportMetadata.merge(attributes, WayangA2uiTransportMaps.copy(extraAttributes)));
    }

    static String normalizeMethod(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? "GET" : normalized.toUpperCase(Locale.ROOT);
    }

    static String normalizePath(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "/";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String normalizeHeaderName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeMediaType(String value) {
        String normalized = value == null ? "" : value.trim();
        int parameterStart = normalized.indexOf(';');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart);
        }
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean acceptedMediaType(String candidate, String expected) {
        String mediaType = normalizeMediaType(candidate);
        if (mediaType.isBlank() || qualityZero(candidate)) {
            return false;
        }
        if ("*/*".equals(mediaType)) {
            return true;
        }
        if (mediaType.endsWith("/*")) {
            String prefix = mediaType.substring(0, mediaType.length() - 1);
            return expected.startsWith(prefix);
        }
        return mediaType.equals(expected);
    }

    private static boolean qualityZero(String value) {
        if (value == null || !value.contains(";")) {
            return false;
        }
        for (String parameter : value.split(";")) {
            String normalized = parameter.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("q=")) {
                try {
                    return Double.parseDouble(normalized.substring(2).trim()) <= 0.0d;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
