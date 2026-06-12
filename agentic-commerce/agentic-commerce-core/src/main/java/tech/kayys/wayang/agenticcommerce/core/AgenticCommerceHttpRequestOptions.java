package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared HTTP request metadata for Agentic Commerce checkout request builders.
 */
public record AgenticCommerceHttpRequestOptions(
        String bearerToken,
        String apiVersion,
        String idempotencyKey,
        String requestId,
        String acceptLanguage,
        String userAgent,
        Map<String, Object> headers,
        Map<String, Object> attributes) {

    public AgenticCommerceHttpRequestOptions {
        bearerToken = AgenticCommerceValues.textValue(bearerToken);
        apiVersion = defaultApiVersion(apiVersion);
        idempotencyKey = AgenticCommerceValues.textValue(idempotencyKey);
        requestId = AgenticCommerceValues.textValue(requestId);
        acceptLanguage = AgenticCommerceValues.textValue(acceptLanguage);
        userAgent = AgenticCommerceValues.textValue(userAgent);
        headers = AgenticCommerceMaps.copy(headers);
        attributes = AgenticCommerceMaps.copy(attributes);
    }

    public static AgenticCommerceHttpRequestOptions defaults() {
        return new AgenticCommerceHttpRequestOptions("", AgenticCommerceProtocol.SPEC_VERSION, "", "", "", "", Map.of(), Map.of());
    }

    public static AgenticCommerceHttpRequestOptions bearer(String bearerToken) {
        return defaults().withBearerToken(bearerToken);
    }

    public AgenticCommerceHttpRequestOptions withBearerToken(String bearerToken) {
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                idempotencyKey,
                requestId,
                acceptLanguage,
                userAgent,
                headers,
                attributes);
    }

    public AgenticCommerceHttpRequestOptions withIdempotencyKey(String idempotencyKey) {
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                idempotencyKey,
                requestId,
                acceptLanguage,
                userAgent,
                headers,
                attributes);
    }

    public AgenticCommerceHttpRequestOptions withRequestId(String requestId) {
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                idempotencyKey,
                requestId,
                acceptLanguage,
                userAgent,
                headers,
                attributes);
    }

    public AgenticCommerceHttpRequestOptions withAcceptLanguage(String acceptLanguage) {
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                idempotencyKey,
                requestId,
                acceptLanguage,
                userAgent,
                headers,
                attributes);
    }

    public AgenticCommerceHttpRequestOptions withUserAgent(String userAgent) {
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                idempotencyKey,
                requestId,
                acceptLanguage,
                userAgent,
                headers,
                attributes);
    }

    public AgenticCommerceHttpRequestOptions withHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(headers);
        merged.putAll(AgenticCommerceMaps.copy(extraHeaders));
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                idempotencyKey,
                requestId,
                acceptLanguage,
                userAgent,
                merged,
                attributes);
    }

    public AgenticCommerceHttpRequestOptions withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(attributes);
        merged.putAll(AgenticCommerceMaps.copy(extraAttributes));
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                idempotencyKey,
                requestId,
                acceptLanguage,
                userAgent,
                headers,
                merged);
    }

    Map<String, Object> requestHeaders(boolean bodyPresent) {
        Map<String, Object> values = new LinkedHashMap<>();
        putHeader(values, AgenticCommerceProtocol.HEADER_ACCEPT, AgenticCommerceProtocol.MIME_JSON);
        putHeader(values, AgenticCommerceProtocol.HEADER_API_VERSION, apiVersion);
        if (!bearerToken.isBlank()) {
            putHeader(values, AgenticCommerceProtocol.HEADER_AUTHORIZATION, authorizationHeader());
        }
        if (bodyPresent) {
            putHeader(values, AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON);
        }
        putHeader(values, AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY, idempotencyKey);
        putHeader(values, AgenticCommerceProtocol.HEADER_REQUEST_ID, requestId);
        putHeader(values, "Accept-Language", acceptLanguage);
        putHeader(values, "User-Agent", userAgent);
        values.putAll(headers);
        return AgenticCommerceMaps.copy(values);
    }

    private String authorizationHeader() {
        if (bearerToken.startsWith(AgenticCommerceProtocol.BEARER_PREFIX)) {
            return bearerToken;
        }
        return AgenticCommerceProtocol.BEARER_PREFIX + bearerToken;
    }

    private static void putHeader(Map<String, Object> headers, String key, String value) {
        String normalized = AgenticCommerceValues.textValue(value);
        if (!normalized.isBlank()) {
            headers.put(key, normalized);
        }
    }

    private static String defaultApiVersion(String value) {
        String normalized = AgenticCommerceValues.textValue(value);
        return normalized.isBlank() ? AgenticCommerceProtocol.SPEC_VERSION : normalized;
    }
}
