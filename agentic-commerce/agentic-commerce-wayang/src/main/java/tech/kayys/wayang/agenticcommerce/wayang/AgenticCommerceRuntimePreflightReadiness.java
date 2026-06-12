package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;
import tech.kayys.wayang.gollek.sdk.WayangReadinessReports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record AgenticCommerceRuntimePreflightReadiness(AgenticCommerceRuntimePreflightReport preflight) {

    AgenticCommerceRuntimePreflightReadiness {
        preflight = Objects.requireNonNull(preflight, "preflight");
    }

    static AgenticCommerceRuntimePreflightReadiness from(AgenticCommerceRuntimePreflightReport preflight) {
        return new AgenticCommerceRuntimePreflightReadiness(preflight);
    }

    WayangReadinessReport toReport() {
        return WayangReadinessReport.from(
                AgenticCommerceRuntimePreflightReport.READINESS_ID,
                preflight.ready(),
                WayangReadinessReports.exitCode(preflight.ready()),
                preflight.errorCount(),
                probes(),
                issues(),
                attributes());
    }

    private List<Map<String, Object>> probes() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = preflight.snapshot().runtimeConfig();
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = preflight.snapshot().bootstrapConfig();
        boolean smokeRequired = bootstrapConfig.requireSmokeProbe();
        boolean smokePassed = !smokeRequired || runtimeConfig.httpConfig().smokeEnabled();
        boolean bindingRoutesRequired = bootstrapConfig.requireBindingRoutes();
        boolean bindingRoutesPassed = !bindingRoutesRequired || preflight.bindingReport().routeCount() > 0;
        boolean persistenceKnown = !AgenticCommerceWayangMaps.text(
                preflight.snapshot().storeStatus().get("storageKind")).isBlank();
        return List.of(
                WayangReadinessReports.probe(
                        "connectorSupported",
                        true,
                        preflight.connectorSupported(),
                        preflight.connectorSupported() ? 0 : 1,
                        Map.of("connectorKind", preflight.connectorKind())),
                WayangReadinessReports.probe(
                        "connectorPolicy",
                        true,
                        preflight.connectorPolicyIssues().isEmpty(),
                        preflight.connectorPolicyIssues().size(),
                        Map.of("issues", preflight.connectorPolicyIssues())),
                WayangReadinessReports.probe(
                        "smokeProbe",
                        smokeRequired,
                        smokePassed,
                        smokePassed ? 0 : 1,
                        Map.of("enabled", runtimeConfig.httpConfig().smokeEnabled())),
                WayangReadinessReports.probe(
                        "bindingRoutes",
                        bindingRoutesRequired,
                        bindingRoutesPassed,
                        bindingRoutesPassed ? 0 : 1,
                        Map.of("routeCount", preflight.bindingReport().routeCount())),
                WayangReadinessReports.probe(
                        "persistenceStore",
                        false,
                        persistenceKnown,
                        persistenceKnown ? 0 : 1,
                        persistenceProbeAttributes()));
    }

    private List<Map<String, Object>> issues() {
        return preflight.errors().stream()
                .map(this::issue)
                .toList();
    }

    private Map<String, Object> issue(String code) {
        return WayangReadinessReports.issue(code, issueSource(code), code.replace('_', ' '));
    }

    private Map<String, Object> attributes() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("warningCount", preflight.warningCount());
        values.put("warnings", preflight.warnings());
        values.put("connectorKind", preflight.connectorKind());
        values.put("connectorSupported", preflight.connectorSupported());
        values.put("connectorPolicyIssues", preflight.connectorPolicyIssues());
        values.put("httpRouteCount", preflight.bindingReport().routeCount());
        values.put("runtimeConfigSource", preflight.snapshot().runtimeConfigSource());
        values.put("bootstrapConfigSource", preflight.snapshot().bootstrapConfigSource());
        values.put("runtimeConfigPersisted", preflight.snapshot().runtimeConfigPersisted());
        values.put("bootstrapConfigPersisted", preflight.snapshot().bootstrapConfigPersisted());
        values.put("persistenceTarget", preflight.snapshot().persistenceTarget());
        values.put("persistenceCapabilities", AgenticCommerceWayangPersistenceCapabilities.fromStatus(
                preflight.snapshot().storeStatus()).toMap());
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> persistenceProbeAttributes() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                "storageKind",
                AgenticCommerceWayangMaps.text(preflight.snapshot().storeStatus().get("storageKind")));
        values.put("target", preflight.snapshot().persistenceTarget());
        values.put("capabilities", AgenticCommerceWayangPersistenceCapabilities.fromStatus(
                preflight.snapshot().storeStatus()).toMap());
        return AgenticCommerceWayangMaps.copy(values);
    }

    private static String issueSource(String code) {
        if ("connector_kind_unsupported".equals(code) || code.startsWith("connector_")) {
            return "connector";
        }
        if (code.startsWith("smoke_") || code.startsWith("binding_")) {
            return "http";
        }
        return "runtime";
    }
}
