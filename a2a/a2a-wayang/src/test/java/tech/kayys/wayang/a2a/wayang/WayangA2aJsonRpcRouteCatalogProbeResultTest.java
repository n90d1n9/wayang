package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcRouteCatalogProbeResultTest {

    @Test
    void reportsMissingRouteDescriptorWithCanonicalIssueShape() {
        WayangA2aJsonRpcRouteCatalogProbeResult probe =
                WayangA2aJsonRpcRouteCatalogProbeResult.from(routeCatalogResponseWithoutRouteCatalogDescriptor());

        assertThat(probe.passed()).isFalse();
        assertThat(probe.routeCatalogDescriptor()).isFalse();
        assertThat(probe.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("source", "routeCatalog")
                        .containsEntry("code", "route_descriptor_missing")
                        .containsEntry("field", "routeCatalogDescriptor")
                        .containsEntry(
                                "expected",
                                WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                        .containsEntry("actual", "missing")
                        .containsEntry(
                                "message",
                                "A2A JSON-RPC route catalog did not expose the "
                                        + WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG
                                        + " descriptor."));
    }

    private static WayangA2aHttpResponse routeCatalogResponseWithoutRouteCatalogDescriptor() {
        List<Map<String, Object>> routes = List.of(
                route(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC),
                route(WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE),
                route(WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS),
                route(WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE),
                route(WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT),
                route(WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS),
                route(WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY));
        Map<String, Object> body = Map.of(
                "routeCount", routes.size(),
                "enabledRouteCount", routes.size(),
                "routes", routes);
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(body),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
                        WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG));
    }

    private static Map<String, Object> route(String operation) {
        return Map.of("operation", operation, "enabled", true);
    }
}
