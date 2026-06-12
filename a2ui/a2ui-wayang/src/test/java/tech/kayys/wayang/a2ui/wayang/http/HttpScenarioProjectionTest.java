package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiBridgeResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpBridgeAdapter;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRequest;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRouteCatalog;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioExchange;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioIssue;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioSuiteReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpScenarioProjectionTest {

    @Test
    void projectsOrderedScenarioIssueAndRecordDelegates() {
        WayangA2uiHttpScenarioIssue issue = new WayangA2uiHttpScenarioIssue(
                "broken-exchange",
                2,
                "post",
                "api/a2ui/exchange",
                400,
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR.name(),
                "invalid_request_json",
                "Unable to decode A2UI transport request JSON",
                Map.of("traceId", "trace-1"));

        Map<String, Object> values = HttpScenarioProjection.issue(issue);

        assertThat(issue.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "scenarioId",
                "exchangeIndex",
                "method",
                "path",
                "statusCode",
                "routeOperation",
                "outcome",
                "errorCode",
                "message",
                "attributes");
        assertThat(values)
                .containsEntry("scenarioId", "broken-exchange")
                .containsEntry("exchangeIndex", 2)
                .containsEntry("method", "POST")
                .containsEntry("path", "/api/a2ui/exchange")
                .containsEntry("errorCode", "invalid_request_json");
    }

    @Test
    void projectsOrderedScenarioReportAndNestedExchangeShapes() {
        WayangA2uiHttpScenarioExchange exchange = routeCatalogExchange();
        Map<String, Object> exchangeValues = HttpScenarioProjection.exchange(exchange);
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
                List.of(exchangeValues),
                List.of(),
                Map.of("traceId", "trace-1"));

        Map<String, Object> values = HttpScenarioProjection.report(report);

        assertThat(report.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "scenarioId",
                "passed",
                "exchangeCount",
                "successfulCount",
                "clientErrorCount",
                "serverErrorCount",
                "handledCount",
                "rejectedCount",
                "transportErrors",
                "statusCodes",
                "outcomes",
                "attributes",
                "exchanges",
                "issueCount",
                "issues");
        assertThat(values)
                .containsEntry("scenarioId", "route-report")
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
        assertThat(exchangeValues.keySet()).containsExactly("index", "request", "response");
        assertThat((Map<String, Object>) exchangeValues.get("request"))
                .containsEntry("method", "GET")
                .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG)
                .containsEntry("bodyPresent", false)
                .containsEntry("bodyLength", 0);
        assertThat((Map<String, Object>) exchangeValues.get("response"))
                .containsEntry("statusCode", 200)
                .containsEntry("successful", true)
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                .containsEntry("transportError", false);
    }

    @Test
    void projectsOrderedSuiteReportAndRecordDelegates() {
        WayangA2uiHttpScenarioReport scenarioReport = new WayangA2uiHttpScenarioReport(
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
                List.of(HttpScenarioProjection.exchange(routeCatalogExchange())),
                List.of(),
                Map.of("traceId", "trace-1"));
        WayangA2uiHttpScenarioSuiteReport suiteReport = new WayangA2uiHttpScenarioSuiteReport(
                "smoke-suite",
                1,
                1,
                0,
                1,
                1,
                0,
                0,
                0,
                0,
                false,
                0,
                List.of("route-report"),
                List.of(scenarioReport.toMap()),
                List.of(),
                Map.of("suiteKind", "smoke"));

        Map<String, Object> values = HttpScenarioProjection.suiteReport(suiteReport);

        assertThat(suiteReport.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "suiteId",
                "passed",
                "scenarioCount",
                "passedScenarioCount",
                "failedScenarioCount",
                "exchangeCount",
                "successfulCount",
                "clientErrorCount",
                "serverErrorCount",
                "handledCount",
                "rejectedCount",
                "transportErrors",
                "issueCount",
                "scenarioIds",
                "attributes",
                "scenarios",
                "issues");
        assertThat(values)
                .containsEntry("suiteId", "smoke-suite")
                .containsEntry("passed", true)
                .containsEntry("scenarioCount", 1)
                .containsEntry("issueCount", 0L);
        assertThat((Iterable<String>) values.get("scenarioIds")).containsExactly("route-report");
    }

    private static WayangA2uiHttpScenarioExchange routeCatalogExchange() {
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.from(
                WayangA2uiHttpRouteCatalog.defaultCatalog());
        WayangA2uiHttpResponse response = WayangA2uiHttpResponse.fromBridge(
                WayangA2uiBridgeResponse.of(transportResponse)).withRoute(WayangA2uiHttpRoute.routeCatalog());
        return new WayangA2uiHttpScenarioExchange(
                1,
                WayangA2uiHttpRequest.routeCatalog().withAttributes(Map.of("traceId", "trace-1")),
                response);
    }
}
