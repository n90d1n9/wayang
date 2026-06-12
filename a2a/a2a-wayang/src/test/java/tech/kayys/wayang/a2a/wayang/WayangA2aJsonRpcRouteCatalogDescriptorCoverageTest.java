package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcRouteCatalogDescriptorCoverageTest {

    @Test
    void reportsCompleteCoverageForRequiredRouteOperations() {
        WayangA2aJsonRpcRouteCatalogDescriptorCoverage coverage =
                WayangA2aJsonRpcRouteCatalogDescriptorCoverage.fromRoutes(List.of(
                        route(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC),
                        route(WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE),
                        route(WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG),
                        route(WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS),
                        route(WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE),
                        route(WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT),
                        route(WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS),
                        route(WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY)));

        assertThat(coverage.complete()).isTrue();
        assertThat(coverage.endpointDescriptor()).isTrue();
        assertThat(coverage.readinessIssueSummaryDescriptor()).isTrue();
        assertThat(coverage.missingIssues()).isEmpty();
    }

    @Test
    void reportsMissingDescriptorIssuesInProbeOrder() {
        WayangA2aJsonRpcRouteCatalogDescriptorCoverage coverage =
                WayangA2aJsonRpcRouteCatalogDescriptorCoverage.fromRoutes(List.of(
                        route(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC),
                        route(WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE),
                        route(WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS),
                        route(WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE),
                        route(WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT),
                        route(WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS)));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.missingIssues())
                .extracting(issue -> issue.get("field"))
                .containsExactly("routeCatalogDescriptor", "readinessIssueSummaryDescriptor");
        assertThat(coverage.missingIssues().get(0))
                .containsEntry("source", "routeCatalog")
                .containsEntry("code", "route_descriptor_missing")
                .containsEntry("expected", WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry("actual", "missing");
    }

    private static Map<String, Object> route(String operation) {
        return Map.of("operation", operation, "enabled", true);
    }
}
