package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcRouteCatalogProbeProjectionTest {

    @Test
    void keepsOrderedProbeEnvelope() {
        WayangA2aJsonRpcRouteCatalogProbeResult probe = passingProbe();

        Map<String, Object> values = WayangA2aJsonRpcRouteCatalogProbeProjection.probe(probe);

        assertThat(values.keySet()).containsExactly(
                "statusCode",
                "httpSuccessful",
                "routeOperation",
                "protocolVersion",
                "contentType",
                "allow",
                "routeCatalogRoute",
                "jsonContent",
                "complete",
                "passed",
                "issueCount",
                "issues",
                "routeCount",
                "enabledRouteCount",
                "endpointDescriptor",
                "smokeDescriptor",
                "routeCatalogDescriptor",
                "diagnosticsReportDescriptor",
                "specComplianceReportDescriptor",
                "bindingReportDescriptor",
                "readinessDescriptor",
                "readinessIssueSummaryDescriptor",
                "body",
                "headers");
        assertThat(values)
                .containsEntry("statusCode", 200)
                .containsEntry("httpSuccessful", true)
                .containsEntry("routeCatalogRoute", true)
                .containsEntry("jsonContent", true)
                .containsEntry("complete", true)
                .containsEntry("passed", true)
                .containsEntry("routeCount", 8)
                .containsEntry("enabledRouteCount", 8);
    }

    @Test
    void parsesProbeMapsWithDefaultsAndNestedObjects() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", "204");
        values.put("httpSuccessful", "true");
        values.put("routeOperation", " " + WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG + " ");
        values.put("protocolVersion", " 0.3.0 ");
        values.put("contentType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        values.put("allow", " GET ");
        values.put("routeCount", "3");
        values.put("enabledRouteCount", "2");
        values.put("endpointDescriptor", "true");
        values.put("smokeDescriptor", true);
        values.put("routeCatalogDescriptor", false);
        values.put("diagnosticsReportDescriptor", true);
        values.put("specComplianceReportDescriptor", true);
        values.put("bindingReportDescriptor", true);
        values.put("readinessDescriptor", true);
        values.put("readinessIssueSummaryDescriptor", true);
        values.put("issueCount", "0");
        values.put("issues", List.of(Map.of("code", "route_descriptor_missing")));
        values.put("body", Map.of("routeCount", 3));
        values.put("headers", Map.of(WayangA2aHttpResponse.HEADER_ALLOW, "GET"));

        WayangA2aJsonRpcRouteCatalogProbeResult probe =
                WayangA2aJsonRpcRouteCatalogProbeProjection.fromMap(values);

        assertThat(probe.statusCode()).isEqualTo(204);
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.routeOperation())
                .isEqualTo(WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG);
        assertThat(probe.protocolVersion()).isEqualTo("0.3.0");
        assertThat(probe.allow()).isEqualTo("GET");
        assertThat(probe.routeCount()).isEqualTo(3);
        assertThat(probe.enabledRouteCount()).isEqualTo(2);
        assertThat(probe.routeCatalogDescriptor()).isFalse();
        assertThat(probe.issueCount()).isEqualTo(1);
        assertThat(probe.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue).containsEntry("code", "route_descriptor_missing"));
        assertThat(probe.body()).containsEntry("routeCount", 3);
        assertThat(probe.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET");
    }

    @Test
    void recordDelegatesToProjectionForJsonRoundTrip() {
        WayangA2aJsonRpcRouteCatalogProbeResult probe = passingProbe();

        assertThat(probe.toMap()).isEqualTo(WayangA2aJsonRpcRouteCatalogProbeProjection.probe(probe));
        assertThat(WayangA2aJsonRpcRouteCatalogProbeResult.fromJson(probe.toJson()).toMap())
                .isEqualTo(probe.toMap());
    }

    private static WayangA2aJsonRpcRouteCatalogProbeResult passingProbe() {
        List<Map<String, Object>> routes = List.of(
                route(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC),
                route(WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE),
                route(WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG),
                route(WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS),
                route(WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE),
                route(WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT),
                route(WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS),
                route(WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY));
        Map<String, Object> body = Map.of(
                "routeCount", routes.size(),
                "enabledRouteCount", routes.size(),
                "routes", routes);
        Map<String, Object> headers = Map.of(
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        return new WayangA2aJsonRpcRouteCatalogProbeResult(
                200,
                true,
                WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
                "0.3.0",
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG,
                routes.size(),
                routes.size(),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                0,
                List.of(),
                body,
                headers);
    }

    private static Map<String, Object> route(String operation) {
        return Map.of("operation", operation, "enabled", true);
    }
}
