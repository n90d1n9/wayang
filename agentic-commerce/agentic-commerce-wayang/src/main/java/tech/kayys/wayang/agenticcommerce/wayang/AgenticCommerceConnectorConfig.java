package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequestOptions;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Connector-level defaults for seller Agentic Commerce calls.
 */
public record AgenticCommerceConnectorConfig(
        String baseUrl,
        String bearerToken,
        String apiVersion,
        Map<String, Object> headers,
        Map<String, Object> attributes) {

    public AgenticCommerceConnectorConfig {
        baseUrl = normalizeBaseUrl(baseUrl);
        bearerToken = AgenticCommerceWayangMaps.text(bearerToken);
        apiVersion = defaultApiVersion(apiVersion);
        headers = AgenticCommerceWayangMaps.copy(headers);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceConnectorConfig defaults() {
        return new AgenticCommerceConnectorConfig("", "", AgenticCommerceProtocol.SPEC_VERSION, Map.of(), Map.of());
    }

    public static AgenticCommerceConnectorConfig bearer(String bearerToken) {
        return new AgenticCommerceConnectorConfig("", bearerToken, AgenticCommerceProtocol.SPEC_VERSION, Map.of(), Map.of());
    }

    public static AgenticCommerceConnectorConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Builder builder = builder();
        AgenticCommerceWayangMaps.optional(AgenticCommerceWayangMaps.first(
                resolved,
                "baseUrl",
                "baseURL",
                "sellerBaseUrl",
                "sellerUrl",
                "endpointBaseUrl")).ifPresent(builder::baseUrl);
        AgenticCommerceWayangMaps.optional(AgenticCommerceWayangMaps.first(
                resolved,
                "bearerToken",
                "token",
                "accessToken",
                "sellerToken",
                "apiKey")).ifPresent(builder::bearerToken);
        AgenticCommerceWayangMaps.optional(AgenticCommerceWayangMaps.first(
                resolved,
                "apiVersion",
                "specVersion",
                "version")).ifPresent(builder::apiVersion);
        firstMap(resolved, "headers", "defaultHeaders", "sellerHeaders").ifPresent(builder::headers);
        firstMap(resolved, "attributes", "metadata", "connectorAttributes").ifPresent(builder::attributes);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public AgenticCommerceConnectorConfig withBaseUrl(String baseUrl) {
        return new AgenticCommerceConnectorConfig(baseUrl, bearerToken, apiVersion, headers, attributes);
    }

    public AgenticCommerceConnectorConfig withHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new java.util.LinkedHashMap<>(headers);
        merged.putAll(AgenticCommerceWayangMaps.copy(extraHeaders));
        return new AgenticCommerceConnectorConfig(baseUrl, bearerToken, apiVersion, merged, attributes);
    }

    public AgenticCommerceConnectorConfig withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new java.util.LinkedHashMap<>(attributes);
        merged.putAll(AgenticCommerceWayangMaps.copy(extraAttributes));
        return new AgenticCommerceConnectorConfig(baseUrl, bearerToken, apiVersion, headers, merged);
    }

    public AgenticCommerceHttpRequestOptions requestOptions() {
        return new AgenticCommerceHttpRequestOptions(
                bearerToken,
                apiVersion,
                "",
                "",
                "",
                "",
                headers,
                attributes);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("baseUrl", baseUrl);
        values.put("apiVersion", apiVersion);
        values.put("bearerTokenConfigured", !bearerToken.isBlank());
        values.put("headerCount", headers.size());
        values.put("attributeCount", attributes.size());
        return Map.copyOf(values);
    }

    public Map<String, Object> toStorageMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("baseUrl", baseUrl);
        values.put("bearerToken", bearerToken);
        values.put("apiVersion", apiVersion);
        values.put("headers", headers);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Optional<Map<?, ?>> firstMap(Map<String, ?> values, String... keys) {
        Object value = AgenticCommerceWayangMaps.first(values, keys);
        if (value instanceof Map<?, ?> map) {
            return Optional.of(map);
        }
        return Optional.empty();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String defaultApiVersion(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        return normalized.isBlank() ? AgenticCommerceProtocol.SPEC_VERSION : normalized;
    }

    public static final class Builder {

        private String baseUrl = "";
        private String bearerToken = "";
        private String apiVersion = AgenticCommerceProtocol.SPEC_VERSION;
        private final Map<String, Object> headers = new LinkedHashMap<>();
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder bearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder headers(Map<?, ?> headers) {
            this.headers.putAll(AgenticCommerceWayangMaps.copy(headers));
            return this;
        }

        public Builder attributes(Map<?, ?> attributes) {
            this.attributes.putAll(AgenticCommerceWayangMaps.copy(attributes));
            return this;
        }

        public AgenticCommerceConnectorConfig build() {
            return new AgenticCommerceConnectorConfig(
                    baseUrl,
                    bearerToken,
                    apiVersion,
                    headers,
                    attributes);
        }
    }
}
