package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configurable connector factory selection for Agentic Commerce Wayang runtimes.
 */
public record AgenticCommerceConnectorFactoryConfig(
        String connectorKind,
        Map<String, Object> attributes) {

    public static final String CONNECTOR_IN_MEMORY = "in-memory";
    public static final String CONNECTOR_HTTP = "http";

    public AgenticCommerceConnectorFactoryConfig {
        connectorKind = normalizeConnectorKind(connectorKind);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceConnectorFactoryConfig defaults() {
        return inMemory();
    }

    public static AgenticCommerceConnectorFactoryConfig inMemory() {
        return new AgenticCommerceConnectorFactoryConfig(CONNECTOR_IN_MEMORY, Map.of());
    }

    public static AgenticCommerceConnectorFactoryConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        String connectorKind = AgenticCommerceWayangMaps.firstText(
                resolved,
                "connectorKind",
                "connectorMode",
                "kind",
                "mode",
                "type");
        return new AgenticCommerceConnectorFactoryConfig(
                connectorKind,
                AgenticCommerceWayangMaps.copy(firstMap(
                        resolved,
                        "attributes",
                        "metadata",
                        "connectorFactoryAttributes")));
    }

    public AgenticCommerceConnector buildConnector(AgenticCommerceConnectorConfig connectorConfig) {
        return buildConnector(connectorConfig, AgenticCommerceConnectorPolicy.defaults());
    }

    public AgenticCommerceConnector buildConnector(
            AgenticCommerceConnectorConfig connectorConfig,
            AgenticCommerceConnectorPolicy connectorPolicy) {
        AgenticCommerceConnectorConfig config = connectorConfig == null
                ? AgenticCommerceConnectorConfig.defaults()
                : connectorConfig;
        AgenticCommerceConnectorPolicy policy = connectorPolicy == null
                ? AgenticCommerceConnectorPolicy.defaults()
                : connectorPolicy;
        policy.validate(this, config);
        return switch (connectorKind) {
            case CONNECTOR_IN_MEMORY -> new InMemoryAgenticCommerceConnector();
            case CONNECTOR_HTTP -> new HttpAgenticCommerceConnector(config);
            default -> throw new IllegalArgumentException(
                    "Unsupported Agentic Commerce connector kind: " + connectorKind);
        };
    }

    public boolean inMemoryConnector() {
        return CONNECTOR_IN_MEMORY.equals(connectorKind);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("connectorKind", connectorKind);
        values.put("inMemoryConnector", inMemoryConnector());
        values.put("httpConnector", CONNECTOR_HTTP.equals(connectorKind));
        values.put("attributeCount", attributes.size());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    public Map<String, Object> toStorageMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("connectorKind", connectorKind);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Map<?, ?> firstMap(Map<String, Object> values, String... keys) {
        Object value = AgenticCommerceWayangMaps.first(values, keys);
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String normalizeConnectorKind(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            return CONNECTOR_IN_MEMORY;
        }
        if ("memory".equals(normalized) || "local".equals(normalized) || "test".equals(normalized)) {
            return CONNECTOR_IN_MEMORY;
        }
        if ("seller".equals(normalized) || "seller-http".equals(normalized) || "remote".equals(normalized)) {
            return CONNECTOR_HTTP;
        }
        return normalized;
    }
}
