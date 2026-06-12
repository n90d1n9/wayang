package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

class WayangA2uiHttpEndpointDiagnosticContractTest {

    private final A2uiContractAssert contracts = new A2uiContractAssert();

    @Test
    void endpointDiagnosticConfigMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-config.json",
                TransportJson.json(
                        contractConfig().toMap(),
                        "Unable to encode endpoint diagnostic config fixture"));
    }

    @Test
    void endpointDiagnosticRequestMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-request.json",
                contractRequest().toJson());
    }

    @Test
    void endpointDiagnosticPlanMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-plan.json",
                contractPlan().toJson());
    }

    @Test
    void endpointDiagnosticReportMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-report.json",
                contractReport().toJson());
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-report.json",
                WayangA2uiHttpEndpointDiagnosticReport.fromJson(contractReport().toJson()).toJson());
    }

    @Test
    void endpointDiagnosticSummaryMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-summary.json",
                contractReport().summary().toJson());
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-summary.json",
                WayangA2uiHttpEndpointDiagnosticSummary.fromJson(contractReport().summary().toJson()).toJson());
    }

    @Test
    void endpointDiagnosticUnknownPathIssueMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-issue-unknown-path.json",
                contractUnknownPathIssue().toJson());
    }

    @Test
    void endpointDiagnosticRouteMismatchIssueMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-issue-route-mismatch.json",
                contractRouteMismatchIssue().toJson());
    }

    @Test
    void endpointDiagnosticRunMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-endpoint-diagnostic-run.json",
                contractRun().toJson());
    }

    private static WayangA2uiHttpEndpointDiagnosticRun contractRun() {
        WayangA2uiHttpEndpointDiagnosticRunner runner =
                WayangA2uiHttpEndpointDiagnosticRunner.of(contractEndpoint());
        return runner.runRequestMaps(
                "contract-run",
                List.of(Map.of(
                        "method",
                        "GET",
                        "path",
                        "/api/a2ui/missing",
                        "attributes",
                        Map.of("traceId", "trace-1"))),
                Map.of("tenant", "demo"));
    }

    private static WayangA2uiHttpEndpointBinding contractEndpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }

    private static WayangA2uiHttpEndpointDiagnosticPlan contractPlan() {
        return new WayangA2uiHttpEndpointDiagnosticPlan(
                "contract-plan",
                contractConfig(),
                List.of(contractRequest()),
                Map.of("source", "contract"));
    }

    private static WayangA2uiHttpEndpointDiagnosticReport contractReport() {
        return new WayangA2uiHttpEndpointDiagnosticReport(
                "contract-report",
                1,
                0,
                1,
                0,
                1,
                0,
                1,
                0,
                0,
                1,
                true,
                List.of(404),
                List.of(WayangA2uiTransportOutcome.TRANSPORT_ERROR.name()),
                List.of(contractReportExchange()),
                List.of(contractReportIssue()),
                Map.of("tenant", "demo"));
    }

    private static Map<String, Object> contractReportExchange() {
        return Map.of(
                "index",
                1,
                "knownPath",
                false,
                "matched",
                false,
                "statusCode",
                404,
                "successful",
                false,
                "outcome",
                WayangA2uiTransportOutcome.TRANSPORT_ERROR.name(),
                "transportError",
                true,
                "request",
                Map.of(
                        "method",
                        "GET",
                        "path",
                        "/api/a2ui/missing",
                        "attributes",
                        Map.of("traceId", "trace-1")),
                "response",
                Map.of(
                        "statusCode",
                        404,
                        "contentType",
                        WayangA2uiTransportContent.MIME_JSON,
                        "headerCount",
                        1),
                "responseEnvelope",
                Map.of(
                        WayangA2uiTransportFields.OUTCOME,
                        WayangA2uiTransportOutcome.TRANSPORT_ERROR.name(),
                        WayangA2uiTransportFields.METADATA,
                        Map.of(WayangA2uiTransportFields.ERROR_CODE, "not_found")));
    }

    private static Map<String, Object> contractReportIssue() {
        return contractUnknownPathIssue().toMap();
    }

    private static WayangA2uiHttpEndpointDiagnosticIssue contractUnknownPathIssue() {
        return new WayangA2uiHttpEndpointDiagnosticIssue(
                "contract-report",
                1,
                "GET",
                "/api/a2ui/missing",
                false,
                false,
                404,
                "",
                "",
                WayangA2uiTransportOutcome.TRANSPORT_ERROR.name(),
                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH,
                "not_found",
                "Unknown A2UI HTTP route: GET /api/a2ui/missing",
                Map.of("traceId", "trace-1"));
    }

    private static WayangA2uiHttpEndpointDiagnosticIssue contractRouteMismatchIssue() {
        return new WayangA2uiHttpEndpointDiagnosticIssue(
                "contract-report",
                2,
                "GET",
                "/api/a2ui/exchange",
                true,
                false,
                405,
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                "POST, OPTIONS",
                WayangA2uiTransportOutcome.TRANSPORT_ERROR.name(),
                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH,
                "method_not_allowed",
                "A2UI HTTP route /api/a2ui/exchange only supports POST.",
                Map.of("traceId", "trace-2"));
    }

    private static WayangA2uiHttpEndpointDiagnosticConfig contractConfig() {
        return WayangA2uiHttpEndpointDiagnosticConfig.fromMap(Map.of(
                "profile",
                "discovery",
                "headers",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT,
                        List.of(WayangA2uiTransportContent.MIME_JSON)),
                "attributes",
                Map.of("tenant", "demo")));
    }

    private static WayangA2uiHttpEndpointDiagnosticRequest contractRequest() {
        return WayangA2uiHttpEndpointDiagnosticRequest.fromMap(Map.of(
                "method",
                "GET",
                "path",
                "/api/a2ui/route-catalog?tenant=demo",
                "headers",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT,
                        List.of(WayangA2uiTransportContent.MIME_JSON)),
                "attributes",
                Map.of("traceId", "trace-1")));
    }
}
