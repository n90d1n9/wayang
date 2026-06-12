package tech.kayys.wayang.agenticcommerce.wayang;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Policy guard for Agentic Commerce connector creation.
 */
public record AgenticCommerceConnectorPolicy(
        List<String> allowedConnectorKinds,
        List<String> allowedBaseHosts,
        boolean requireHttps) {

    public AgenticCommerceConnectorPolicy {
        allowedConnectorKinds = normalizeConnectorKinds(allowedConnectorKinds);
        allowedBaseHosts = normalizeHosts(allowedBaseHosts);
    }

    public static AgenticCommerceConnectorPolicy defaults() {
        return new AgenticCommerceConnectorPolicy(List.of(), List.of(), false);
    }

    public static AgenticCommerceConnectorPolicy strictHosted(List<String> allowedBaseHosts) {
        return new AgenticCommerceConnectorPolicy(
                List.of(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP),
                allowedBaseHosts,
                true);
    }

    public static AgenticCommerceConnectorPolicy fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Object connectorKinds = AgenticCommerceWayangMaps.first(
                resolved,
                "allowedConnectorKinds",
                "allowedConnectors",
                "connectorKinds",
                "connectors");
        Object baseHosts = AgenticCommerceWayangMaps.first(
                resolved,
                "allowedBaseHosts",
                "allowedHosts",
                "hosts",
                "outboundHosts");
        return new AgenticCommerceConnectorPolicy(
                AgenticCommerceWayangMaps.stringList(connectorKinds),
                AgenticCommerceWayangMaps.stringList(baseHosts),
                AgenticCommerceWayangMaps.firstBoolean(
                        resolved,
                        "requireHttps",
                        "httpsOnly",
                        "requireTls").orElse(false));
    }

    public boolean allows(
            AgenticCommerceConnectorFactoryConfig connectorFactoryConfig,
            AgenticCommerceConnectorConfig connectorConfig) {
        return issues(connectorFactoryConfig, connectorConfig).isEmpty();
    }

    public void validate(
            AgenticCommerceConnectorFactoryConfig connectorFactoryConfig,
            AgenticCommerceConnectorConfig connectorConfig) {
        List<String> issues = issues(connectorFactoryConfig, connectorConfig);
        if (!issues.isEmpty()) {
            throw new IllegalArgumentException("Agentic Commerce connector policy rejected: " + String.join(", ", issues));
        }
    }

    public List<String> issues(
            AgenticCommerceConnectorFactoryConfig connectorFactoryConfig,
            AgenticCommerceConnectorConfig connectorConfig) {
        AgenticCommerceConnectorFactoryConfig factoryConfig = connectorFactoryConfig == null
                ? AgenticCommerceConnectorFactoryConfig.defaults()
                : connectorFactoryConfig;
        AgenticCommerceConnectorConfig config = connectorConfig == null
                ? AgenticCommerceConnectorConfig.defaults()
                : connectorConfig;
        List<String> issues = new ArrayList<>();
        if (!allowedConnectorKinds.isEmpty() && !allowedConnectorKinds.contains(factoryConfig.connectorKind())) {
            issues.add("connector_kind_not_allowed");
        }
        if (AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP.equals(factoryConfig.connectorKind())) {
            URI baseUri = baseUri(config.baseUrl(), issues);
            if (baseUri != null) {
                String scheme = AgenticCommerceWayangMaps.text(baseUri.getScheme()).toLowerCase(java.util.Locale.ROOT);
                String host = AgenticCommerceWayangMaps.text(baseUri.getHost()).toLowerCase(java.util.Locale.ROOT);
                if (host.isBlank()) {
                    issues.add("connector_base_host_required");
                }
                if (requireHttps && !"https".equals(scheme)) {
                    issues.add("connector_https_required");
                }
                if (!allowedBaseHosts.isEmpty() && !allowedBaseHosts.contains(host)) {
                    issues.add("connector_host_not_allowed");
                }
            }
        }
        return List.copyOf(issues);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("allowedConnectorKinds", allowedConnectorKinds);
        values.put("allowedBaseHosts", allowedBaseHosts);
        values.put("requireHttps", requireHttps);
        values.put("connectorKindRestricted", !allowedConnectorKinds.isEmpty());
        values.put("baseHostRestricted", !allowedBaseHosts.isEmpty());
        return Map.copyOf(values);
    }

    public Map<String, Object> toStorageMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("allowedConnectorKinds", allowedConnectorKinds);
        values.put("allowedBaseHosts", allowedBaseHosts);
        values.put("requireHttps", requireHttps);
        return Map.copyOf(values);
    }

    private static URI baseUri(String baseUrl, List<String> issues) {
        String normalized = AgenticCommerceWayangMaps.text(baseUrl);
        if (normalized.isBlank()) {
            issues.add("connector_base_url_required");
            return null;
        }
        try {
            return URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            issues.add("connector_base_url_invalid");
            return null;
        }
    }

    private static List<String> normalizeConnectorKinds(List<String> connectorKinds) {
        return AgenticCommerceWayangMaps.stringList(connectorKinds).stream()
                .map(kind -> AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("connectorKind", kind)).connectorKind())
                .distinct()
                .toList();
    }

    private static List<String> normalizeHosts(List<String> hosts) {
        return AgenticCommerceWayangMaps.stringList(hosts).stream()
                .map(host -> host.toLowerCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }
}
