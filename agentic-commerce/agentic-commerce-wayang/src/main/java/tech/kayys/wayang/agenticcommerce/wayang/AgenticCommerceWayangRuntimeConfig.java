package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified bootstrap configuration for Agentic Commerce Wayang runtime wiring.
 */
public record AgenticCommerceWayangRuntimeConfig(
        AgenticCommerceConnectorFactoryConfig connectorFactoryConfig,
        AgenticCommerceConnectorConfig connectorConfig,
        AgenticCommerceHttpAdapterConfig httpConfig,
        AgenticCommerceConnectorPolicy connectorPolicy) {

    public AgenticCommerceWayangRuntimeConfig {
        connectorFactoryConfig = connectorFactoryConfig == null
                ? AgenticCommerceConnectorFactoryConfig.defaults()
                : connectorFactoryConfig;
        connectorConfig = connectorConfig == null ? AgenticCommerceConnectorConfig.defaults() : connectorConfig;
        httpConfig = httpConfig == null ? AgenticCommerceHttpAdapterConfig.defaults() : httpConfig;
        connectorPolicy = connectorPolicy == null ? AgenticCommerceConnectorPolicy.defaults() : connectorPolicy;
    }

    public static AgenticCommerceWayangRuntimeConfig defaults() {
        return builder().build();
    }

    public static AgenticCommerceWayangRuntimeConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Map<String, Object> connectorFactoryValues = mergedWithNested(
                resolved,
                "connectorFactoryConfig",
                "connectorFactory",
                "connectorRuntime",
                "connectorSelection");
        Map<String, Object> connectorValues = mergedWithNested(
                resolved,
                "connectorConfig",
                "connector",
                "seller",
                "sellerConnector");
        Map<String, Object> httpValues = mergedWithNested(
                resolved,
                "httpConfig",
                "http",
                "httpAdapter",
                "binding");
        Map<String, Object> connectorPolicyValues = mergedWithNested(
                resolved,
                "connectorPolicy",
                "policy",
                "sellerPolicy",
                "connectorSecurity");
        return builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(connectorFactoryValues))
                .connectorConfig(AgenticCommerceConnectorConfig.fromMap(connectorValues))
                .httpConfig(AgenticCommerceHttpAdapterConfig.fromMap(httpValues))
                .connectorPolicy(AgenticCommerceConnectorPolicy.fromMap(connectorPolicyValues))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public AgenticCommerceWayangRuntime build() {
        return build(connectorFactoryConfig.buildConnector(connectorConfig, connectorPolicy));
    }

    public AgenticCommerceWayangRuntime build(AgenticCommerceConnector connector) {
        return new AgenticCommerceWayangRuntime(connector, this);
    }

    public AgenticCommerceWayangRuntime buildInMemory() {
        return build(new InMemoryAgenticCommerceConnector());
    }

    public AgenticCommerceWayangRuntimeConfig withConnectorFactoryConfig(
            AgenticCommerceConnectorFactoryConfig connectorFactoryConfig) {
        return new AgenticCommerceWayangRuntimeConfig(connectorFactoryConfig, connectorConfig, httpConfig, connectorPolicy);
    }

    public AgenticCommerceWayangRuntimeConfig withConnectorConfig(AgenticCommerceConnectorConfig connectorConfig) {
        return new AgenticCommerceWayangRuntimeConfig(connectorFactoryConfig, connectorConfig, httpConfig, connectorPolicy);
    }

    public AgenticCommerceWayangRuntimeConfig withHttpConfig(AgenticCommerceHttpAdapterConfig httpConfig) {
        return new AgenticCommerceWayangRuntimeConfig(connectorFactoryConfig, connectorConfig, httpConfig, connectorPolicy);
    }

    public AgenticCommerceWayangRuntimeConfig withConnectorPolicy(AgenticCommerceConnectorPolicy connectorPolicy) {
        return new AgenticCommerceWayangRuntimeConfig(connectorFactoryConfig, connectorConfig, httpConfig, connectorPolicy);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("connectorFactoryConfig", connectorFactoryConfig.toMap());
        values.put("connectorConfig", connectorConfig.toMap());
        values.put("httpConfig", httpConfig.toMap());
        values.put("connectorPolicy", connectorPolicy.toMap());
        return Map.copyOf(values);
    }

    public Map<String, Object> toStorageMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("connectorFactoryConfig", connectorFactoryConfig.toStorageMap());
        values.put("connectorConfig", connectorConfig.toStorageMap());
        values.put("httpConfig", httpConfig.toMap());
        values.put("connectorPolicy", connectorPolicy.toStorageMap());
        return Map.copyOf(values);
    }

    private static Map<String, Object> mergedWithNested(Map<String, Object> values, String... nestedKeys) {
        Map<String, Object> merged = new LinkedHashMap<>(values);
        if (nestedKeys != null) {
            for (String nestedKey : nestedKeys) {
                Object nested = values.get(nestedKey);
                if (nested instanceof Map<?, ?> nestedMap) {
                    merged.putAll(AgenticCommerceWayangMaps.copy(nestedMap));
                }
            }
        }
        return Map.copyOf(merged);
    }

    public static final class Builder {

        private AgenticCommerceConnectorFactoryConfig connectorFactoryConfig =
                AgenticCommerceConnectorFactoryConfig.defaults();
        private AgenticCommerceConnectorConfig connectorConfig = AgenticCommerceConnectorConfig.defaults();
        private AgenticCommerceHttpAdapterConfig httpConfig = AgenticCommerceHttpAdapterConfig.defaults();
        private AgenticCommerceConnectorPolicy connectorPolicy = AgenticCommerceConnectorPolicy.defaults();

        private Builder() {
        }

        public Builder connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig connectorFactoryConfig) {
            this.connectorFactoryConfig = connectorFactoryConfig;
            return this;
        }

        public Builder connectorConfig(AgenticCommerceConnectorConfig connectorConfig) {
            this.connectorConfig = connectorConfig;
            return this;
        }

        public Builder httpConfig(AgenticCommerceHttpAdapterConfig httpConfig) {
            this.httpConfig = httpConfig;
            return this;
        }

        public Builder connectorPolicy(AgenticCommerceConnectorPolicy connectorPolicy) {
            this.connectorPolicy = connectorPolicy;
            return this;
        }

        public AgenticCommerceWayangRuntimeConfig build() {
            return new AgenticCommerceWayangRuntimeConfig(
                    connectorFactoryConfig,
                    connectorConfig,
                    httpConfig,
                    connectorPolicy);
        }
    }
}
