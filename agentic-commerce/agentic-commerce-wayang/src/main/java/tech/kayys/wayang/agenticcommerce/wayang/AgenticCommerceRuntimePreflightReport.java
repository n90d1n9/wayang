package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-network readiness report for Agentic Commerce runtime construction.
 */
public record AgenticCommerceRuntimePreflightReport(
        AgenticCommerceWayangConfigSnapshot snapshot,
        AgenticCommerceHttpBindingReport bindingReport) {

    public static final String READINESS_ID = "agentic-commerce.runtime.readiness";

    public AgenticCommerceRuntimePreflightReport {
        snapshot = snapshot == null ? emptySnapshot() : snapshot;
        bindingReport = bindingReport == null
                ? AgenticCommerceHttpBindingReport.fromConfig(snapshot.runtimeConfig().httpConfig())
                : bindingReport;
    }

    public static AgenticCommerceRuntimePreflightReport from(AgenticCommerceWayangConfigSnapshot snapshot) {
        AgenticCommerceWayangConfigSnapshot resolved = snapshot == null ? emptySnapshot() : snapshot;
        return new AgenticCommerceRuntimePreflightReport(
                resolved,
                AgenticCommerceHttpBindingReport.fromConfig(resolved.runtimeConfig().httpConfig()));
    }

    public boolean ready() {
        return errors().isEmpty();
    }

    public int errorCount() {
        return errors().size();
    }

    public int warningCount() {
        return warnings().size();
    }

    public String connectorKind() {
        return snapshot.runtimeConfig().connectorFactoryConfig().connectorKind();
    }

    public boolean connectorSupported() {
        return supportedConnector(snapshot.runtimeConfig().connectorFactoryConfig());
    }

    public List<String> connectorPolicyIssues() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = snapshot.runtimeConfig();
        return runtimeConfig.connectorPolicy().issues(
                runtimeConfig.connectorFactoryConfig(),
                runtimeConfig.connectorConfig());
    }

    public List<String> errors() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = snapshot.runtimeConfig();
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = snapshot.bootstrapConfig();
        List<String> errors = new ArrayList<>();
        if (!connectorSupported()) {
            errors.add("connector_kind_unsupported");
        }
        errors.addAll(connectorPolicyIssues());
        if (bootstrapConfig.requireSmokeProbe() && !runtimeConfig.httpConfig().smokeEnabled()) {
            errors.add("smoke_probe_required_but_disabled");
        }
        if (bootstrapConfig.requireBindingRoutes() && bindingReport.routeCount() == 0) {
            errors.add("binding_routes_required_but_missing");
        }
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = snapshot.runtimeConfig();
        AgenticCommerceConnectorFactoryConfig connectorFactoryConfig = runtimeConfig.connectorFactoryConfig();
        List<String> warnings = new ArrayList<>();
        if (!snapshot.runtimeConfigPersisted()) {
            warnings.add("runtime_config_defaulted");
        }
        if (!snapshot.bootstrapConfigPersisted()) {
            warnings.add("bootstrap_config_defaulted");
        }
        if (connectorFactoryConfig.inMemoryConnector() && !runtimeConfig.connectorConfig().baseUrl().isBlank()) {
            warnings.add("connector_base_url_ignored_for_in_memory");
        }
        if (AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP.equals(connectorFactoryConfig.connectorKind())
                && runtimeConfig.connectorConfig().bearerToken().isBlank()) {
            warnings.add("seller_bearer_token_missing");
        }
        if (!runtimeConfig.httpConfig().bindingReportEnabled()) {
            warnings.add("binding_report_disabled");
        }
        if (AgenticCommerceWayangMaps.text(snapshot.storeStatus().get("storageKind")).isBlank()) {
            warnings.add("persistence_store_unknown");
        }
        return List.copyOf(warnings);
    }

    public Map<String, Object> toMap() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = snapshot.runtimeConfig();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("errorCount", errorCount());
        values.put("warningCount", warningCount());
        values.put("errors", errors());
        values.put("warnings", warnings());
        values.put("runtimeConfigSource", snapshot.runtimeConfigSource());
        values.put("bootstrapConfigSource", snapshot.bootstrapConfigSource());
        values.put("connectorKind", connectorKind());
        values.put("connectorSupported", connectorSupported());
        values.put("connectorPolicyIssues", connectorPolicyIssues());
        values.put("httpRouteCount", bindingReport.routeCount());
        values.put("bindingReport", bindingReport.toMap());
        values.put("runtimeConfig", runtimeConfig.toMap());
        values.put("bootstrapConfig", snapshot.bootstrapConfig().toMap());
        values.put("store", snapshot.storeStatus());
        values.put("persistenceTarget", snapshot.persistenceTarget());
        values.put("persistenceCapabilities", AgenticCommerceWayangPersistenceCapabilities.fromStatus(
                snapshot.storeStatus()).toMap());
        return Map.copyOf(values);
    }

    public WayangReadinessReport standardReadiness() {
        return AgenticCommerceRuntimePreflightReadiness.from(this).toReport();
    }

    private static boolean supportedConnector(AgenticCommerceConnectorFactoryConfig connectorFactoryConfig) {
        String connectorKind = connectorFactoryConfig == null
                ? AgenticCommerceConnectorFactoryConfig.defaults().connectorKind()
                : connectorFactoryConfig.connectorKind();
        return AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY.equals(connectorKind)
                || AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP.equals(connectorKind);
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
