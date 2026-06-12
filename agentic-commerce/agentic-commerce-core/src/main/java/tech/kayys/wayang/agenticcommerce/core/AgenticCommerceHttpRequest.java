package tech.kayys.wayang.agenticcommerce.core;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Dependency-free HTTP-shaped request for Agentic Commerce Protocol adapters.
 */
public record AgenticCommerceHttpRequest(
        String method,
        String path,
        String body,
        Map<String, Object> headers,
        Map<String, Object> attributes) {

    public AgenticCommerceHttpRequest {
        method = normalizeMethod(method);
        path = normalizePath(path);
        body = body == null ? "" : body;
        headers = AgenticCommerceMaps.copy(headers);
        attributes = AgenticCommerceMaps.copy(attributes);
    }

    public static AgenticCommerceHttpRequest get(String path) {
        return new AgenticCommerceHttpRequest("GET", path, "", Map.of(), Map.of());
    }

    public static AgenticCommerceHttpRequest postJson(String path, String body) {
        return new AgenticCommerceHttpRequest(
                "POST",
                path,
                body,
                Map.of(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON),
                Map.of());
    }

    public static AgenticCommerceHttpRequest patchJson(String path, String body) {
        return new AgenticCommerceHttpRequest(
                "PATCH",
                path,
                body,
                Map.of(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON),
                Map.of());
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

    public String authorization() {
        return header(AgenticCommerceProtocol.HEADER_AUTHORIZATION).orElse("");
    }

    public String contentType() {
        return header(AgenticCommerceProtocol.HEADER_CONTENT_TYPE).orElse("");
    }

    public boolean contentType(String expectedContentType) {
        String expected = normalizeMediaType(expectedContentType);
        return !expected.isBlank() && normalizeMediaType(contentType()).equals(expected);
    }

    public String apiVersion() {
        return header(AgenticCommerceProtocol.HEADER_API_VERSION).orElse("");
    }

    public AgenticCommerceHttpRequest withHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new java.util.LinkedHashMap<>(headers);
        merged.putAll(AgenticCommerceMaps.copy(extraHeaders));
        return new AgenticCommerceHttpRequest(method, path, body, merged, attributes);
    }

    public AgenticCommerceHttpRequest withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new java.util.LinkedHashMap<>(attributes);
        merged.putAll(AgenticCommerceMaps.copy(extraAttributes));
        return new AgenticCommerceHttpRequest(method, path, body, headers, merged);
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

    static String normalizeHeaderName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeMediaType(String value) {
        String normalized = value == null ? "" : value.trim();
        int parameterStart = normalized.indexOf(';');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart);
        }
        return normalized.trim().toLowerCase(Locale.ROOT);
    }
}
