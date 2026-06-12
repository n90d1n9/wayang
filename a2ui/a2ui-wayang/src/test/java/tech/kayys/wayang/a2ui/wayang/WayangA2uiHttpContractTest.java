package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractBindingReport;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractBindingReportHttpResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractReadinessHttpResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractReadinessProbeResult;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractSmokeHttpResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractSmokeResult;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractSmokeTransportResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.failedContractSmokeHttpResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.failedContractSmokeTransportResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.incompleteContractBindingReportHttpResponse;

class WayangA2uiHttpContractTest {

    private final A2uiContractAssert contracts = new A2uiContractAssert();

    @Test
    void httpRouteCatalogBodyMatchesContractFixture() throws IOException {
        String body = WayangA2uiTransportResponse.from(WayangA2uiHttpRouteCatalog.defaultCatalog()).body();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-route-catalog-body.json",
                body);
        assertThat(body).startsWith("{\"routeCount\":");
        assertThat(body.indexOf("\"routes\""))
                .isGreaterThan(body.indexOf("\"routeCount\""));
    }

    @Test
    void httpBindingReportBodyMatchesContractFixture() throws IOException {
        String body = WayangA2uiTransportResponse.from(contractBindingReport()).body();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-binding-report-body.json",
                body);
        assertThat(body).startsWith("{\"complete\":");
        assertThat(body.indexOf("\"handlerOperationCount\""))
                .isGreaterThan(body.indexOf("\"routeOperationCount\""));
    }

    @Test
    void httpBindingReportProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-binding-report-probe-result.json",
                WayangA2uiHttpBindingReportProbeResult.from(contractBindingReportHttpResponse()).toJson());
    }

    @Test
    void incompleteHttpBindingReportProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-binding-report-probe-result-incomplete.json",
                WayangA2uiHttpBindingReportProbeResult.from(incompleteContractBindingReportHttpResponse()).toJson());
    }

    @Test
    void httpScenarioReportMatchesContractFixture() throws IOException {
        WayangA2uiHttpScenarioReport report = new WayangA2uiHttpScenarioReport(
                "route-report",
                1,
                1,
                0,
                0,
                0,
                0,
                false,
                List.of(200),
                List.of(WayangA2uiTransportOutcome.SUCCESS.name()),
                List.of(Map.of(
                        "index",
                        1,
                        "request",
                        Map.of(
                                "method",
                                "GET",
                                "path",
                                WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG),
                        "response",
                        Map.of(
                                "statusCode",
                                200,
                                "routeOperation",
                                WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                                "outcome",
                                WayangA2uiTransportOutcome.SUCCESS.name()))),
                List.of(),
                Map.of("traceId", "trace-1"));

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-scenario-report.json",
                report.toJson());
    }

    @Test
    void httpScenarioIssueMatchesContractFixture() throws IOException {
        WayangA2uiHttpScenarioIssue issue = new WayangA2uiHttpScenarioIssue(
                "broken-exchange",
                1,
                "POST",
                WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                400,
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR.name(),
                "invalid_request_json",
                "Unable to decode A2UI transport request JSON",
                Map.of("traceId", "trace-1"));

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-scenario-issue.json",
                TransportJson.json(issue.toMap(), "Unable to encode issue fixture"));
    }

    @Test
    void httpScenarioSuiteReportMatchesContractFixture() throws IOException {
        WayangA2uiHttpScenarioSuiteReport report = new WayangA2uiHttpScenarioSuiteReport(
                WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                3,
                3,
                0,
                17,
                17,
                0,
                0,
                0,
                0,
                false,
                0,
                List.of(
                        WayangA2uiHttpScenarios.DISCOVERY_ID,
                        WayangA2uiHttpScenarios.ROUTE_OPTIONS_ID,
                        WayangA2uiHttpScenarios.DIAGNOSTICS_ID),
                List.of(
                        Map.of(
                                "scenarioId",
                                WayangA2uiHttpScenarios.DISCOVERY_ID,
                                "passed",
                                true,
                                "issueCount",
                                0),
                        Map.of(
                                "scenarioId",
                                WayangA2uiHttpScenarios.ROUTE_OPTIONS_ID,
                                "passed",
                                true,
                                "issueCount",
                                0),
                        Map.of(
                                "scenarioId",
                                WayangA2uiHttpScenarios.DIAGNOSTICS_ID,
                                "passed",
                                true,
                                "issueCount",
                                0)),
                List.of(),
                Map.of("suiteKind", "smoke"));

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-suite-report.json",
                report.toJson());
    }

    @Test
    void httpExpectationResultMatchesContractFixture() throws IOException {
        WayangA2uiHttpExpectationResult result = WayangA2uiHttpExpectationResult.of(
                "broken-exchange",
                "a2ui-http-scenario-pass",
                List.of(WayangA2uiHttpExpectationIssue.of(
                        "broken-exchange",
                        "statusCodes",
                        List.of(200),
                        List.of(400),
                        "Expected statusCodes to match exactly.")),
                Map.of("source", "contract"));

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-expectation-result.json",
                result.toJson());
    }

    @Test
    void httpSmokeResultMatchesContractFixture() throws IOException {
        String smokeJson = contractSmokeResult().toJson();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-smoke-result.json",
                smokeJson);
        assertThat(smokeJson).startsWith("{\"passed\":");
        assertThat(smokeJson.indexOf("\"expectationResult\""))
                .isGreaterThan(smokeJson.indexOf("\"suiteReport\""));
    }

    @Test
    void httpSmokeTransportResponseProjectionMatchesContractFixtures() throws IOException {
        WayangA2uiTransportResponse response = WayangA2uiTransportResponse.from(contractSmokeResult());

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-smoke-result.json",
                response.body());
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-smoke-response-metadata.json",
                TransportJson.json(
                        response.metadata(),
                        "Unable to encode A2UI HTTP smoke response metadata fixture"));
    }

    @Test
    void httpSmokeSummaryMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-smoke-summary.json",
                WayangA2uiHttpSmokeSummary.from(contractSmokeTransportResponse()).toJson());
    }

    @Test
    void httpSmokeProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-smoke-probe-result.json",
                WayangA2uiHttpSmokeProbeResult.from(contractSmokeHttpResponse()).toJson());
    }

    @Test
    void httpReadinessProbeResultMatchesContractFixture() throws IOException {
        String readinessJson = contractReadinessProbeResult().toJson();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-readiness-probe-result.json",
                readinessJson);
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-readiness-probe-result.json",
                WayangA2uiHttpReadinessProbeResult.fromJson(readinessJson).toJson());
        assertThat(readinessJson).startsWith("{\"passed\":");
        assertThat(readinessJson.indexOf("\"bindingReportProbe\""))
                .isGreaterThan(readinessJson.indexOf("\"issues\""));
    }

    @Test
    void httpOperationalDiagnosticsSummaryMatchesContractFixture() throws IOException {
        WayangA2uiHttpOperationalDiagnostics diagnostics =
                new WayangA2uiHttpOperationalDiagnostics(contractReadinessProbeResult());
        String summaryJson = diagnostics.summary().toJson();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-operational-diagnostics-summary.json",
                summaryJson);
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-operational-diagnostics-summary.json",
                WayangA2uiHttpOperationalDiagnosticsSummary.fromJson(summaryJson).toJson());
    }

    @Test
    void httpReadinessEndpointDecodeMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-readiness-probe-result.json",
                WayangA2uiHttpReadinessProbeResult.from(contractReadinessHttpResponse()).toJson());
    }

    @Test
    void failedHttpSmokeSummaryMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-smoke-summary-failed.json",
                WayangA2uiHttpSmokeSummary.from(failedContractSmokeTransportResponse()).toJson());
    }

    @Test
    void failedHttpSmokeProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-http-smoke-probe-result-failed.json",
                WayangA2uiHttpSmokeProbeResult.from(failedContractSmokeHttpResponse()).toJson());
    }
}
