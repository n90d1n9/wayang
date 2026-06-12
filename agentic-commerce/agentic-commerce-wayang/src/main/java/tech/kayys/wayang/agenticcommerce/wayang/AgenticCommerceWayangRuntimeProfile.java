package tech.kayys.wayang.agenticcommerce.wayang;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Named runtime/bootstrap profile for common Agentic Commerce deployment modes.
 */
public record AgenticCommerceWayangRuntimeProfile(
        String profileName,
        AgenticCommerceWayangRuntimeConfig runtimeConfig,
        AgenticCommerceWayangBootstrapConfig bootstrapConfig,
        Map<String, Object> attributes) {

    public static final String PROFILE_LOCAL = "local";
    public static final String PROFILE_SELLER_HTTP = "seller-http";
    public static final String PROFILE_STAGING = "staging";
    public static final String PROFILE_PRODUCTION = "production";

    public AgenticCommerceWayangRuntimeProfile {
        profileName = normalizeProfileName(profileName);
        runtimeConfig = runtimeConfig == null ? AgenticCommerceWayangRuntimeConfig.defaults() : runtimeConfig;
        bootstrapConfig = bootstrapConfig == null ? AgenticCommerceWayangBootstrapConfig.defaults() : bootstrapConfig;
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangRuntimeProfile local() {
        return new AgenticCommerceWayangRuntimeProfile(
                PROFILE_LOCAL,
                AgenticCommerceWayangRuntimeConfig.defaults(),
                AgenticCommerceWayangBootstrapConfig.defaults(),
                Map.of("profileKind", PROFILE_LOCAL));
    }

    public static AgenticCommerceWayangRuntimeProfile sellerHttp(String baseUrl) {
        return new AgenticCommerceWayangRuntimeProfile(
                PROFILE_SELLER_HTTP,
                AgenticCommerceWayangRuntimeConfig.builder()
                        .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                                "mode",
                                "seller-http")))
                        .connectorConfig(AgenticCommerceConnectorConfig.defaults().withBaseUrl(baseUrl))
                        .connectorPolicy(AgenticCommerceConnectorPolicy.defaults())
                        .build(),
                AgenticCommerceWayangBootstrapConfig.defaults(),
                Map.of("profileKind", PROFILE_SELLER_HTTP));
    }

    public static AgenticCommerceWayangRuntimeProfile staging(String baseUrl) {
        return hosted(PROFILE_STAGING, baseUrl, hostFromBaseUrl(baseUrl));
    }

    public static AgenticCommerceWayangRuntimeProfile production(String baseUrl) {
        return hosted(PROFILE_PRODUCTION, baseUrl, hostFromBaseUrl(baseUrl));
    }

    public static AgenticCommerceWayangRuntimeProfile hosted(
            String profileName,
            String baseUrl,
            List<String> allowedHosts) {
        String normalizedName = normalizeProfileName(profileName);
        return new AgenticCommerceWayangRuntimeProfile(
                normalizedName,
                AgenticCommerceWayangRuntimeConfig.builder()
                        .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                                "mode",
                                "seller-http")))
                        .connectorConfig(AgenticCommerceConnectorConfig.defaults().withBaseUrl(baseUrl))
                        .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(allowedHosts))
                        .build(),
                AgenticCommerceWayangBootstrapConfig.defaults(),
                Map.of("profileKind", normalizedName));
    }

    public static AgenticCommerceWayangRuntimeProfile fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        String profileName = AgenticCommerceWayangMaps.firstText(
                resolved,
                "profileName",
                "profile",
                "environment",
                "env");
        String normalizedName = normalizeProfileName(profileName);
        AgenticCommerceWayangRuntimeProfile baseProfile = baseProfile(normalizedName, resolved);
        AgenticCommerceWayangRuntimeConfig runtimeConfig = baseProfile.runtimeConfig();
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = baseProfile.bootstrapConfig();
        if (hasAny(resolved, "connectorFactoryConfig", "connectorFactory", "connectorRuntime", "connectorSelection",
                "connectorKind", "connectorMode", "mode")) {
            runtimeConfig = runtimeConfig.withConnectorFactoryConfig(
                    AgenticCommerceConnectorFactoryConfig.fromMap(mergedWithNested(
                            resolved,
                            "connectorFactoryConfig",
                            "connectorFactory",
                            "connectorRuntime",
                            "connectorSelection")));
        }
        if (hasAny(resolved, "connectorConfig", "connector", "seller", "sellerConnector",
                "baseUrl", "baseURL", "sellerBaseUrl", "sellerUrl", "endpointBaseUrl",
                "bearerToken", "token", "accessToken", "sellerToken", "apiKey",
                "apiVersion", "specVersion", "version", "headers", "defaultHeaders", "sellerHeaders")) {
            runtimeConfig = runtimeConfig.withConnectorConfig(AgenticCommerceConnectorConfig.fromMap(
                    mergedWithNested(resolved, "connectorConfig", "connector", "seller", "sellerConnector")));
        }
        if (hasAny(resolved, "httpConfig", "http", "httpAdapter", "binding",
                "checkoutBasePath", "basePath", "endpointPath", "path",
                "smokePath", "smokeEndpointPath", "smokeEnabled", "enableSmoke",
                "bindingReportPath", "bindingPath", "diagnosticsPath",
                "bindingReportEnabled", "enableBindingReport", "diagnosticsEnabled")) {
            runtimeConfig = runtimeConfig.withHttpConfig(AgenticCommerceHttpAdapterConfig.fromMap(
                    mergedWithNested(resolved, "httpConfig", "http", "httpAdapter", "binding")));
        }
        if (hasAny(resolved, "connectorPolicy", "policy", "sellerPolicy", "connectorSecurity",
                "allowedConnectorKinds", "allowedConnectors", "connectorKinds", "connectors",
                "requireHttps", "httpsOnly", "requireTls")) {
            runtimeConfig = runtimeConfig.withConnectorPolicy(AgenticCommerceConnectorPolicy.fromMap(
                    mergedWithNested(resolved, "connectorPolicy", "policy", "sellerPolicy", "connectorSecurity")));
        }
        if (hasAny(resolved, "bootstrapConfig", "bootstrap", "skills", "skillIds", "selectedSkillIds",
                "checkoutSkillIds", "includeDefinitions", "definitions", "registerDefinitions",
                "includeRuntimeSkills", "runtimeSkills", "registerRuntimeSkills",
                "requireSkillRegistration", "requireSkills", "skillsRequired",
                "requireSmokeProbe", "requireSmoke", "smokeRequired",
                "requireBindingRoutes", "requireBinding", "bindingRequired")) {
            bootstrapConfig = AgenticCommerceWayangBootstrapConfig.fromMap(
                    mergedWithNested(resolved, "bootstrapConfig", "bootstrap"));
        }
        return new AgenticCommerceWayangRuntimeProfile(
                normalizedName,
                runtimeConfig,
                bootstrapConfig,
                AgenticCommerceWayangMaps.copy(firstMap(resolved, "attributes", "metadata", "profileAttributes")));
    }

    public AgenticCommerceWayangConfigSnapshot snapshot() {
        return new AgenticCommerceWayangConfigSnapshot(
                runtimeConfig,
                bootstrapConfig,
                true,
                true,
                Map.of(
                        "storageKind",
                        "profile",
                        "profileName",
                        profileName));
    }

    public AgenticCommerceRuntimePreflightReport preflight() {
        return snapshot().preflight();
    }

    public AgenticCommerceWayangRuntime runtime() {
        return snapshot().buildRuntime();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("profileName", profileName);
        values.put("runtimeConfig", runtimeConfig.toMap());
        values.put("bootstrapConfig", bootstrapConfig.toMap());
        values.put("attributes", attributes);
        values.put("preflight", preflight().toMap());
        return Map.copyOf(values);
    }

    public Map<String, Object> toStorageMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("profileName", profileName);
        values.put("runtimeConfig", runtimeConfig.toStorageMap());
        values.put("bootstrapConfig", bootstrapConfig.toMap());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangRuntimeProfile baseProfile(
            String profileName,
            Map<String, Object> values) {
        Map<String, Object> connectorValues = mergedWithNested(
                values,
                "connectorConfig",
                "connector",
                "seller",
                "sellerConnector");
        String baseUrl = AgenticCommerceWayangMaps.firstText(
                connectorValues,
                "baseUrl",
                "baseURL",
                "sellerBaseUrl",
                "sellerUrl",
                "endpointBaseUrl");
        List<String> hosts = AgenticCommerceWayangMaps.stringList(AgenticCommerceWayangMaps.first(
                values,
                "allowedBaseHosts",
                "allowedHosts",
                "hosts",
                "outboundHosts"));
        return switch (profileName) {
            case PROFILE_SELLER_HTTP -> sellerHttp(baseUrl);
            case PROFILE_STAGING -> hosted(PROFILE_STAGING, baseUrl, hosts.isEmpty() ? hostFromBaseUrl(baseUrl) : hosts);
            case PROFILE_PRODUCTION -> hosted(PROFILE_PRODUCTION, baseUrl, hosts.isEmpty() ? hostFromBaseUrl(baseUrl) : hosts);
            default -> local();
        };
    }

    private static String normalizeProfileName(String profileName) {
        String normalized = AgenticCommerceWayangMaps.text(profileName).toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank() || "memory".equals(normalized) || "test".equals(normalized)) {
            return PROFILE_LOCAL;
        }
        if ("seller".equals(normalized) || "http".equals(normalized) || "remote".equals(normalized)) {
            return PROFILE_SELLER_HTTP;
        }
        if ("prod".equals(normalized) || "hosted".equals(normalized)) {
            return PROFILE_PRODUCTION;
        }
        if ("stage".equals(normalized)) {
            return PROFILE_STAGING;
        }
        return normalized;
    }

    private static List<String> hostFromBaseUrl(String baseUrl) {
        String normalized = AgenticCommerceWayangMaps.text(baseUrl);
        if (normalized.isBlank()) {
            return List.of();
        }
        try {
            String host = AgenticCommerceWayangMaps.text(URI.create(normalized).getHost());
            return host.isBlank() ? List.of() : List.of(host);
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private static boolean hasAny(Map<String, Object> values, String... keys) {
        if (values == null || values.isEmpty() || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (key != null && values.containsKey(key)) {
                return true;
            }
        }
        return false;
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

    private static Map<?, ?> firstMap(Map<String, Object> values, String... keys) {
        Object value = AgenticCommerceWayangMaps.first(values, keys);
        return value instanceof Map<?, ?> map ? map : Map.of();
    }
}
