package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointDiagnosticsTest {

    @Test
    @SuppressWarnings("unchecked")
    void runsExplicitRawEndpointDiagnosticsAndReportsIssues() {
        WayangA2uiHttpEndpointDiagnostics diagnostics = WayangA2uiHttpEndpointDiagnostics.of(errorEndpoint());

        WayangA2uiHttpEndpointDiagnosticResult result = diagnostics.run(
                "mounted-endpoint-problems",
                List.of(
                        WayangA2uiHttpEndpointDiagnosticRequest.get("/api/a2ui/route-catalog?tenant=demo")
                                .withAttributes(Map.of("traceId", "trace-1")),
                        WayangA2uiHttpEndpointDiagnosticRequest.of("GET", "/api/a2ui/exchange"),
                        WayangA2uiHttpEndpointDiagnosticRequest.get("/api/a2ui/missing")),
                Map.of("tenant", "demo"));

        assertThat(result.diagnosticsId()).isEqualTo("mounted-endpoint-problems");
        assertThat(result.exchangeCount()).isEqualTo(3);
        assertThat(result.knownPathCount()).isEqualTo(2L);
        assertThat(result.unknownPathCount()).isEqualTo(1L);
        assertThat(result.matchedCount()).isEqualTo(1L);
        assertThat(result.unmatchedCount()).isEqualTo(2L);
        assertThat(result.successfulCount()).isEqualTo(1L);
        assertThat(result.clientErrorCount()).isEqualTo(2L);
        assertThat(result.serverErrorCount()).isZero();
        assertThat(result.handledCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(2L);
        assertThat(result.hasTransportErrors()).isTrue();
        assertThat(result.statusCodes()).containsExactly(200, 405, 404);
        assertThat(result.outcomes()).containsExactly(
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR);

        WayangA2uiHttpEndpointDiagnosticReport report = result.report();
        Map<String, Object> reportMap = report.toMap();
        assertThat(report.passed()).isFalse();
        assertThat(report.issueCount()).isEqualTo(2);
        assertThat(reportMap)
                .containsEntry("diagnosticsId", "mounted-endpoint-problems")
                .containsEntry("passed", false)
                .containsEntry("knownPathCount", 2L)
                .containsEntry("unknownPathCount", 1L)
                .containsEntry("matchedCount", 1L)
                .containsEntry("unmatchedCount", 2L)
                .containsEntry("issueCount", 2);
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("tenant", "demo");

        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) reportMap.get("exchanges");
        Map<String, Object> firstRequest = (Map<String, Object>) exchanges.get(0).get("request");
        assertThat(exchanges.get(0)).containsEntry("index", 1);
        assertThat(firstRequest)
                .containsEntry("path", "/api/a2ui/route-catalog")
                .containsEntry("matched", true);
        assertThat((Map<String, Object>) firstRequest.get("attributes"))
                .containsEntry("traceId", "trace-1");
        assertThat((Map<String, Object>) exchanges.get(0).get("responseEnvelope"))
                .containsEntry(WayangA2uiTransportFields.OUTCOME, WayangA2uiTransportOutcome.SUCCESS.name());

        List<Map<String, Object>> issues = (List<Map<String, Object>>) reportMap.get("issues");
        assertThat(issues)
                .extracting(issue -> issue.get("category"))
                .containsExactly("route-mismatch", "unknown-path");
        assertThat(issues)
                .extracting(issue -> issue.get("errorCode"))
                .containsExactly("method_not_allowed", "not_found");
        assertThat(result.toJson())
                .contains("\"diagnosticsId\":\"mounted-endpoint-problems\"")
                .contains("\"errorCode\":\"method_not_allowed\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsEndpointDiagnosticsFromRequestMaps() {
        WayangA2uiHttpEndpointDiagnostics diagnostics = WayangA2uiHttpEndpointDiagnostics.of(
                errorEndpoint(),
                WayangA2uiHttpEndpointDiagnosticConfig.discoveryOnly()
                        .withDefaultAttributes(Map.of("tenant", "demo")));

        WayangA2uiHttpEndpointDiagnosticResult result = diagnostics.runFromMaps(
                "mapped-requests",
                List.of(
                        Map.of(
                                "method",
                                "GET",
                                "path",
                                "/api/a2ui/route-catalog?tenant=demo",
                                "attributes",
                                Map.of("traceId", "trace-1")),
                        Map.of(
                                "method",
                                "GET",
                                "rawPath",
                                "/api/a2ui/exchange")),
                Map.of("source", "cli"));

        assertThat(result.exchangeCount()).isEqualTo(2);
        assertThat(result.knownPathCount()).isEqualTo(2L);
        assertThat(result.matchedCount()).isEqualTo(1L);
        assertThat(result.unmatchedCount()).isEqualTo(1L);
        assertThat(result.statusCodes()).containsExactly(200, 405);
        assertThat(result.report().passed()).isFalse();

        Map<String, Object> reportMap = result.toMap();
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("source", "cli");
        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) reportMap.get("exchanges");
        Map<String, Object> firstRequest = (Map<String, Object>) exchanges.get(0).get("request");
        assertThat(firstRequest)
                .containsEntry("path", "/api/a2ui/route-catalog")
                .containsEntry("matched", true);
        assertThat((Map<String, Object>) firstRequest.get("attributes"))
                .containsEntry("tenant", "demo")
                .containsEntry("traceId", "trace-1");
        assertThat((List<Map<String, Object>>) reportMap.get("issues"))
                .extracting(issue -> issue.get("errorCode"))
                .containsExactly("method_not_allowed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsEndpointDiagnosticsFromPlanMaps() {
        WayangA2uiHttpEndpointDiagnostics diagnostics = WayangA2uiHttpEndpointDiagnostics.of(errorEndpoint());

        WayangA2uiHttpEndpointDiagnosticResult result = diagnostics.runPlanMap(Map.of(
                "diagnosticsId",
                "plan-map",
                "config",
                Map.of(
                        "profile",
                        "discovery",
                        "attributes",
                        Map.of("tenant", "demo")),
                "requests",
                List.of(
                        Map.of(
                                "method",
                                "GET",
                                "path",
                                "/api/a2ui/route-catalog?tenant=demo"),
                        Map.of(
                                "method",
                                "GET",
                                "rawPath",
                                "/api/a2ui/exchange")),
                "attributes",
                Map.of("source", "plan")));

        assertThat(result.diagnosticsId()).isEqualTo("plan-map");
        assertThat(result.exchangeCount()).isEqualTo(2);
        assertThat(result.statusCodes()).containsExactly(200, 405);
        assertThat(result.report().passed()).isFalse();

        Map<String, Object> reportMap = result.toMap();
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("source", "plan");
        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) reportMap.get("exchanges");
        Map<String, Object> firstRequest = (Map<String, Object>) exchanges.get(0).get("request");
        assertThat((Map<String, Object>) firstRequest.get("attributes"))
                .containsEntry("tenant", "demo");
        assertThat((List<Map<String, Object>>) reportMap.get("issues"))
                .extracting(issue -> issue.get("errorCode"))
                .containsExactly("method_not_allowed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsDefaultEndpointDiagnosticsFromPlanJson() {
        WayangA2uiHttpEndpointDiagnosticPlan plan =
                WayangA2uiHttpEndpointDiagnosticPlan.fromMap(Map.of(
                        "diagnosticsId",
                        "plan-default-json",
                        "profile",
                        "discovery",
                        "defaultAttributes",
                        Map.of("tenant", "demo"),
                        "attributes",
                        Map.of("source", "json-plan")));
        WayangA2uiHttpEndpointDiagnostics diagnostics = WayangA2uiHttpEndpointDiagnostics.of(errorEndpoint());

        WayangA2uiHttpEndpointDiagnosticResult result = diagnostics.runPlanJson(plan.toJson());
        Map<String, Object> reportMap = result.toMap();

        assertThat(result.diagnosticsId()).isEqualTo("plan-default-json");
        assertThat(result.exchangeCount()).isEqualTo(2);
        assertThat(result.statusCodes()).containsExactly(200, 200);
        assertThat(result.report().passed()).isTrue();
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("source", "json-plan")
                .containsEntry("tenant", "demo")
                .containsEntry("diagnosticKind", "endpoint-default");
        assertThat((List<Map<String, Object>>) reportMap.get("issues")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsDefaultMountedEndpointDiagnostics() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpEndpointBinding endpoint = WayangA2uiHttpEndpointBinding.from(
                new WayangA2uiTransportAdapter(sdk),
                "/api/a2ui");
        WayangA2uiHttpEndpointDiagnostics diagnostics = WayangA2uiHttpEndpointDiagnostics.of(endpoint);

        WayangA2uiHttpEndpointDiagnosticResult result = diagnostics.runDefault();
        Map<String, Object> reportMap = result.toMap();

        assertThat(diagnostics.defaultRequests()).hasSize(10);
        assertThat(result.diagnosticsId()).isEqualTo(WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        assertThat(result.exchangeCount()).isEqualTo(10);
        assertThat(result.knownPathCount()).isEqualTo(10L);
        assertThat(result.unknownPathCount()).isZero();
        assertThat(result.matchedCount()).isEqualTo(10L);
        assertThat(result.unmatchedCount()).isZero();
        assertThat(result.successfulCount()).isEqualTo(10L);
        assertThat(result.clientErrorCount()).isZero();
        assertThat(result.serverErrorCount()).isZero();
        assertThat(result.hasTransportErrors()).isFalse();
        assertThat(result.statusCodes()).containsOnly(200);
        assertThat(result.report().passed()).isTrue();
        assertThat(reportMap)
                .containsEntry("diagnosticsId", WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID)
                .containsEntry("passed", true)
                .containsEntry("exchangeCount", 10)
                .containsEntry("issueCount", 0);
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("diagnosticKind", "endpoint-default")
                .containsEntry("routeCount", 6);

        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) reportMap.get("exchanges");
        Map<String, Object> firstRequest = (Map<String, Object>) exchanges.get(0).get("request");
        assertThat(firstRequest)
                .containsEntry("method", "GET")
                .containsEntry("path", "/api/a2ui/route-catalog")
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG);
        assertThat((Map<String, Object>) ((Map<String, Object>) exchanges.get(0)
                .get("responseEnvelope")).get(WayangA2uiTransportFields.METADATA))
                .containsEntry(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_ROUTE_CATALOG);
        assertThat(result.toJson())
                .contains("\"diagnosticsId\":\"" + WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID + "\"")
                .contains(WayangA2uiHttpRoute.OPERATION_READINESS);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void appliesConfigurableDefaultProbeSelectionAndContext() {
        WayangA2uiHttpEndpointDiagnosticConfig config =
                WayangA2uiHttpEndpointDiagnosticConfig.fromMap(Map.of(
                        "profile",
                        "discovery",
                        "probes",
                        Map.of("routeOptions", "false"),
                        "headers",
                        Map.of(
                                WayangA2uiHttpResponse.HEADER_ACCEPT,
                                List.of(WayangA2uiTransportContent.MIME_JSON)),
                        "attributes",
                        Map.of("tenant", "demo")));
        WayangA2uiHttpEndpointDiagnostics diagnostics =
                WayangA2uiHttpEndpointDiagnostics.of(errorEndpoint(), config);

        List<WayangA2uiHttpEndpointDiagnosticRequest> defaultRequests = diagnostics.defaultRequests();
        WayangA2uiHttpEndpointDiagnosticResult result = diagnostics.runDefault();
        Map<String, Object> reportMap = result.toMap();

        assertThat(diagnostics.config()).isEqualTo(config);
        assertThat(defaultRequests).hasSize(2);
        assertThat(defaultRequests)
                .extracting(WayangA2uiHttpEndpointDiagnosticRequest::rawPath)
                .containsExactly("/api/a2ui/route-catalog", "/api/a2ui/binding-report");
        assertThat(defaultRequests)
                .allSatisfy(request -> {
                    assertThat(request.headers())
                            .containsEntry(WayangA2uiHttpResponse.HEADER_ACCEPT,
                                    List.of(WayangA2uiTransportContent.MIME_JSON));
                    assertThat(request.attributes()).containsEntry("tenant", "demo");
                });
        assertThat(result.exchangeCount()).isEqualTo(2);
        assertThat(result.statusCodes()).containsExactly(200, 200);
        assertThat(result.report().passed()).isTrue();
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("tenant", "demo")
                .containsEntry("diagnosticKind", "endpoint-default")
                .containsEntry("routeCount", 6);
        assertThat((Map<String, Object>) ((Map<String, Object>) reportMap.get("attributes"))
                .get("diagnosticConfig"))
                .containsEntry("routeCatalogProbe", true)
                .containsEntry("bindingReportProbe", true)
                .containsEntry("smokeProbe", false)
                .containsEntry("readinessProbe", false)
                .containsEntry("routeOptionsProbe", false);

        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) reportMap.get("exchanges");
        Map<String, Object> firstRequest = (Map<String, Object>) exchanges.get(0).get("request");
        assertThat((Map<String, Object>) firstRequest.get("headers"))
                .containsEntry(WayangA2uiHttpResponse.HEADER_ACCEPT, WayangA2uiTransportContent.MIME_JSON);
        assertThat((Map<String, Object>) firstRequest.get("attributes"))
                .containsEntry("tenant", "demo");
        assertThat(result.toJson())
                .contains("\"smokeProbe\":false")
                .contains("\"tenant\":\"demo\"");
    }

    private static WayangA2uiHttpEndpointBinding errorEndpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
