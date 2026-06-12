package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Compact, redacted connector health surface for operator diagnostics.
 */
public record AgenticCommerceConnectorDiagnostics(
        AgenticCommerceWayangConfigSnapshot snapshot,
        AgenticCommerceRuntimePreflightReport preflight,
        AgenticCommerceConnectorContractReport contractReport) {

    public static final String READINESS_ID = "agentic-commerce.connector.readiness";

    public AgenticCommerceConnectorDiagnostics {
        snapshot = snapshot == null ? emptySnapshot() : snapshot;
        preflight = preflight == null ? snapshot.preflight() : preflight;
    }

    public static AgenticCommerceConnectorDiagnostics from(AgenticCommerceWayangConfigSnapshot snapshot) {
        return new AgenticCommerceConnectorDiagnostics(snapshot, null, null);
    }

    public static AgenticCommerceConnectorDiagnostics from(
            AgenticCommerceWayangConfigSnapshot snapshot,
            AgenticCommerceConnectorContractReport contractReport) {
        return new AgenticCommerceConnectorDiagnostics(snapshot, null, contractReport);
    }

    public static AgenticCommerceConnectorDiagnostics from(AgenticCommerceWayangRuntime runtime) {
        return new AgenticCommerceConnectorDiagnostics(runtimeSnapshot(runtime), null, null);
    }

    public static AgenticCommerceConnectorDiagnostics from(
            AgenticCommerceWayangRuntime runtime,
            AgenticCommerceConnectorContractReport contractReport) {
        return new AgenticCommerceConnectorDiagnostics(runtimeSnapshot(runtime), null, contractReport);
    }

    public boolean ready() {
        return preflight.ready() && (contractReport == null || contractReport.passed());
    }

    public boolean contractAvailable() {
        return contractReport != null;
    }

    public int issueCount() {
        return preflight.errorCount() + (contractReport == null ? 0 : contractReport.issueCount());
    }

    public WayangReadinessReport standardReadiness() {
        return AgenticCommerceConnectorDiagnosticsReadiness.from(this).toReport();
    }

    public Map<String, Object> toMap() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = snapshot.runtimeConfig();
        AgenticCommerceConnectorFactoryConfig factoryConfig = runtimeConfig.connectorFactoryConfig();
        AgenticCommerceConnectorConfig connectorConfig = runtimeConfig.connectorConfig();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("issueCount", issueCount());
        values.put("warningCount", preflight.warningCount());
        values.put("connectorKind", factoryConfig.connectorKind());
        values.put("connectorSupported", preflight.connectorSupported());
        values.put("contractAvailable", contractAvailable());
        values.put("sources", sources());
        values.put("storage", storageSummary());
        values.put("transport", transport(factoryConfig, connectorConfig));
        values.put("auth", auth(connectorConfig));
        values.put("policy", policy(runtimeConfig.connectorPolicy(), factoryConfig, connectorConfig));
        values.put("preflight", preflightSummary());
        if (contractReport != null) {
            values.put("contract", contractSummary(contractReport));
        }
        return Map.copyOf(values);
    }

    private Map<String, Object> sources() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("runtimeConfigPersisted", snapshot.runtimeConfigPersisted());
        values.put("bootstrapConfigPersisted", snapshot.bootstrapConfigPersisted());
        values.put("runtimeConfigSource", snapshot.runtimeConfigSource());
        values.put("bootstrapConfigSource", snapshot.bootstrapConfigSource());
        return Map.copyOf(values);
    }

    private Map<String, Object> storageSummary() {
        Map<String, Object> values = new LinkedHashMap<>();
        copyKnownStoreValue(values, "storageKind");
        AgenticCommerceWayangPersistenceDocuments.ALL.forEach(
                document -> copyKnownStoreValue(values, document.availabilityStatusKey()));
        copyKnownStoreValue(values, "primaryStorageKind");
        copyKnownStoreValue(values, "fallbackStorageKind");
        copyKnownStoreValue(values, "mirrorWritesToFallback");
        copyKnownStoreValue(values, "connectorClass");
        values.put("capabilities", AgenticCommerceWayangPersistenceCapabilities.fromStatus(
                snapshot.storeStatus()).toMap());
        return Map.copyOf(values);
    }

    private Map<String, Object> transport(
            AgenticCommerceConnectorFactoryConfig factoryConfig,
            AgenticCommerceConnectorConfig connectorConfig) {
        URI baseUri = baseUri(connectorConfig.baseUrl());
        String scheme = baseUri == null
                ? ""
                : AgenticCommerceWayangMaps.text(baseUri.getScheme()).toLowerCase(Locale.ROOT);
        String host = baseUri == null
                ? ""
                : AgenticCommerceWayangMaps.text(baseUri.getHost()).toLowerCase(Locale.ROOT);
        String path = baseUri == null ? "" : AgenticCommerceWayangMaps.text(baseUri.getPath());
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("connectorKind", factoryConfig.connectorKind());
        values.put("inMemoryConnector", factoryConfig.inMemoryConnector());
        values.put("httpConnector", AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP.equals(
                factoryConfig.connectorKind()));
        values.put("baseUrlConfigured", !connectorConfig.baseUrl().isBlank());
        values.put("baseUrlValid", !scheme.isBlank() && !host.isBlank());
        AgenticCommerceWayangMaps.putText(values, "baseUrlScheme", scheme);
        AgenticCommerceWayangMaps.putText(values, "baseUrlHost", host);
        if (baseUri != null && baseUri.getPort() >= 0) {
            values.put("baseUrlPort", baseUri.getPort());
        }
        values.put("baseUrlPathConfigured", !path.isBlank() && !"/".equals(path));
        return Map.copyOf(values);
    }

    private Map<String, Object> auth(AgenticCommerceConnectorConfig connectorConfig) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bearerTokenConfigured", !connectorConfig.bearerToken().isBlank());
        values.put("bearerTokenRedacted", !connectorConfig.bearerToken().isBlank());
        values.put("headerCount", connectorConfig.headers().size());
        values.put("attributeCount", connectorConfig.attributes().size());
        values.put("apiVersion", connectorConfig.apiVersion());
        return Map.copyOf(values);
    }

    private Map<String, Object> policy(
            AgenticCommerceConnectorPolicy connectorPolicy,
            AgenticCommerceConnectorFactoryConfig factoryConfig,
            AgenticCommerceConnectorConfig connectorConfig) {
        Map<String, Object> values = new LinkedHashMap<>(connectorPolicy.toMap());
        values.put("allowsConnector", connectorPolicy.allows(factoryConfig, connectorConfig));
        values.put("issues", connectorPolicy.issues(factoryConfig, connectorConfig));
        return Map.copyOf(values);
    }

    private Map<String, Object> preflightSummary() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", preflight.ready());
        values.put("errorCount", preflight.errorCount());
        values.put("warningCount", preflight.warningCount());
        values.put("errors", preflight.errors());
        values.put("warnings", preflight.warnings());
        values.put("connectorPolicyIssues", preflight.connectorPolicyIssues());
        values.put("httpRouteCount", preflight.bindingReport().routeCount());
        return Map.copyOf(values);
    }

    private Map<String, Object> contractSummary(AgenticCommerceConnectorContractReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", report.passed());
        values.put("connectorName", report.connectorName());
        values.put("exchangeCount", report.exchangeCount());
        values.put("issueCount", report.issueCount());
        values.put("scenarioId", report.scenarioResult().scenario().id());
        values.put("expectationId", report.expectationResult().expectation().id());
        AgenticCommerceWayangMaps.putText(
                values,
                "contractId",
                AgenticCommerceWayangMaps.text(report.attributes().get("contractId")));
        return Map.copyOf(values);
    }

    private void copyKnownStoreValue(Map<String, Object> values, String key) {
        if (snapshot.storeStatus().containsKey(key)) {
            values.put(key, snapshot.storeStatus().get(key));
        }
    }

    private static URI baseUri(String baseUrl) {
        String normalized = AgenticCommerceWayangMaps.text(baseUrl);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static AgenticCommerceWayangConfigSnapshot runtimeSnapshot(AgenticCommerceWayangRuntime runtime) {
        AgenticCommerceWayangRuntime resolved = Objects.requireNonNull(runtime, "runtime");
        Map<String, Object> storeStatus = new LinkedHashMap<>();
        storeStatus.put("storageKind", "runtime");
        storeStatus.put("runtimeConfigAvailable", true);
        storeStatus.put("bootstrapConfigAvailable", false);
        storeStatus.put("connectorClass", resolved.connector().getClass().getName());
        return new AgenticCommerceWayangConfigSnapshot(
                resolved.runtimeConfig(),
                AgenticCommerceWayangBootstrapConfig.defaults(),
                true,
                false,
                storeStatus);
    }

    private static AgenticCommerceWayangConfigSnapshot emptySnapshot() {
        return new AgenticCommerceWayangConfigSnapshot(
                AgenticCommerceWayangRuntimeConfig.defaults(),
                AgenticCommerceWayangBootstrapConfig.defaults(),
                false,
                false,
                Map.of());
    }
}
