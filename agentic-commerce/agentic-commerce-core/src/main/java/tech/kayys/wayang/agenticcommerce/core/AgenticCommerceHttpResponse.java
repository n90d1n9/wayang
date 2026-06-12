package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dependency-free HTTP-shaped response for Agentic Commerce Protocol adapters.
 */
public record AgenticCommerceHttpResponse(
        int statusCode,
        String body,
        Map<String, Object> headers,
        Map<String, Object> attributes) {

    public AgenticCommerceHttpResponse {
        statusCode = statusCode <= 0 ? 200 : statusCode;
        body = body == null ? "" : body;
        headers = AgenticCommerceMaps.copy(headers);
        attributes = AgenticCommerceMaps.copy(attributes);
    }

    public static AgenticCommerceHttpResponse json(int statusCode, String body) {
        return new AgenticCommerceHttpResponse(
                statusCode,
                body,
                Map.of(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON),
                Map.of());
    }

    public Optional<String> header(String name) {
        String normalizedName = AgenticCommerceHttpRequest.normalizeHeaderName(name);
        if (normalizedName.isBlank()) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(entry -> AgenticCommerceHttpRequest.normalizeHeaderName(entry.getKey()).equals(normalizedName))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(value -> String.valueOf(value).trim())
                .findFirst();
    }

    public String contentType() {
        return header(AgenticCommerceProtocol.HEADER_CONTENT_TYPE).orElse("");
    }

    public boolean contentType(String expectedContentType) {
        String expected = AgenticCommerceHttpRequest.normalizeMediaType(expectedContentType);
        return !expected.isBlank()
                && AgenticCommerceHttpRequest.normalizeMediaType(contentType()).equals(expected);
    }

    public String requestId() {
        return header(AgenticCommerceProtocol.HEADER_REQUEST_ID).orElse("");
    }

    public String idempotencyKey() {
        return header(AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY).orElse("");
    }

    public AgenticCommerceHttpResponse withHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(headers);
        merged.putAll(AgenticCommerceMaps.copy(extraHeaders));
        return new AgenticCommerceHttpResponse(statusCode, body, merged, attributes);
    }

    public AgenticCommerceHttpResponse withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(attributes);
        merged.putAll(AgenticCommerceMaps.copy(extraAttributes));
        return new AgenticCommerceHttpResponse(statusCode, body, headers, merged);
    }
}
