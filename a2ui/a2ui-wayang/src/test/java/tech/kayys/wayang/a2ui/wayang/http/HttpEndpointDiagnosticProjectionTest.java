package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiBridgeResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointBinding;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticIssue;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticIssueCatalog;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticRun;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticSummary;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointExchange;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpEndpointDiagnosticProjectionTest {

    @Test
    void projectsOrderedDiagnosticReportEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointDiagnosticReport report = new WayangA2uiHttpEndpointDiagnosticReport(
                "projection-report",
                1,
                1,
                0,
                1,
                0,
                1,
                0,
                0,
                1,
                0,
                false,
                List.of(200),
                List.of(WayangA2uiTransportOutcome.SUCCESS.name()),
                List.of(Map.of("index", 1)),
                List.of(),
                Map.of("source", "projection"));

        Map<String, Object> values = HttpEndpointDiagnosticProjection.report(report);

        assertThat(report.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "diagnosticsId",
                WayangA2uiTransportFields.PASSED,
                "exchangeCount",
                "knownPathCount",
                "unknownPathCount",
                "matchedCount",
                "unmatchedCount",
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
                WayangA2uiTransportFields.ISSUE_COUNT,
                "issues");
        assertThat(values)
                .containsEntry("diagnosticsId", "projection-report")
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry("exchangeCount", 1)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 0);
    }

    @Test
    void projectsDiagnosticRunEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointDiagnosticResult result = new WayangA2uiHttpEndpointDiagnosticResult(
                "projection-run",
                List.of(successfulRouteCatalogExchange()),
                Map.of("source", "run"));
        WayangA2uiHttpEndpointDiagnosticRun run = new WayangA2uiHttpEndpointDiagnosticRun(result);

        Map<String, Object> values = HttpEndpointDiagnosticProjection.run(run);

        assertThat(run.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("summary", "report");
        assertThat((Map<String, Object>) values.get("summary"))
                .containsEntry("diagnosticsId", "projection-run")
                .containsEntry(WayangA2uiTransportFields.PASSED, true);
        assertThat((Map<String, Object>) values.get("report"))
                .containsEntry("diagnosticsId", "projection-run")
                .containsEntry(WayangA2uiTransportFields.PASSED, true);
    }

    @Test
    void projectsOrderedDiagnosticSummaryEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointDiagnosticSummary summary = new WayangA2uiHttpEndpointDiagnosticSummary(
                "projection-summary",
                false,
                WayangA2uiHttpSmokeResult.EXIT_FAILURE,
                2,
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                0,
                true,
                List.of(200, 404),
                List.of(WayangA2uiTransportOutcome.SUCCESS.name(), WayangA2uiTransportOutcome.TRANSPORT_ERROR.name()),
                List.of(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH),
                List.of("not_found"),
                Map.of("source", "projection"));

        Map<String, Object> values = HttpEndpointDiagnosticProjection.summary(summary);

        assertThat(summary.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "diagnosticsId",
                WayangA2uiTransportFields.PASSED,
                WayangA2uiTransportFields.EXIT_CODE,
                "successfulExit",
                "exchangeCount",
                WayangA2uiTransportFields.ISSUE_COUNT,
                "knownPathCount",
                "unknownPathCount",
                "matchedCount",
                "unmatchedCount",
                "successfulCount",
                "clientErrorCount",
                "serverErrorCount",
                "transportErrors",
                "statusCodes",
                "outcomes",
                "issueCategories",
                "errorCodes",
                "attributes");
        assertThat(values)
                .containsEntry("diagnosticsId", "projection-summary")
                .containsEntry(WayangA2uiTransportFields.PASSED, false)
                .containsEntry("successfulExit", false)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 1);
    }

    @Test
    void projectsOrderedDiagnosticIssueEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointDiagnosticIssue issue = WayangA2uiHttpEndpointDiagnosticIssue.from(
                        "projection-issue",
                        2,
                        failedUnknownPathExchange())
                .orElseThrow();

        Map<String, Object> values = HttpEndpointDiagnosticProjection.issue(issue);

        assertThat(issue.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "diagnosticsId",
                "exchangeIndex",
                "method",
                "path",
                "knownPath",
                "matched",
                "statusCode",
                "routeOperation",
                "allow",
                "outcome",
                "category",
                "errorCode",
                "message",
                "attributes");
        assertThat(values)
                .containsEntry("diagnosticsId", "projection-issue")
                .containsEntry("exchangeIndex", 2)
                .containsEntry("category", WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH)
                .containsEntry("errorCode", "not_found");
    }

    @Test
    void projectsIndexedExchangesAndDerivedIssues() {
        List<WayangA2uiHttpEndpointExchange> exchanges = List.of(
                successfulRouteCatalogExchange(),
                failedUnknownPathExchange());

        List<Map<String, Object>> exchangeMaps =
                HttpEndpointDiagnosticProjection.exchanges(exchanges);
        List<Map<String, Object>> issueMaps =
                HttpEndpointDiagnosticProjection.issues("projection-issues", exchanges);

        assertThat(exchangeMaps)
                .extracting(exchange -> exchange.get("index"))
                .containsExactly(1, 2);
        assertThat(issueMaps)
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("diagnosticsId", "projection-issues")
                        .containsEntry("exchangeIndex", 2)
                        .containsEntry("category", "unknown-path")
                        .containsEntry("errorCode", "not_found"));
    }

    @Test
    void ignoresNullEndpointExchangeEntriesInProjectionLists() {
        List<WayangA2uiHttpEndpointExchange> exchanges = new ArrayList<>();
        exchanges.add(successfulRouteCatalogExchange());
        exchanges.add(null);
        exchanges.add(failedUnknownPathExchange());

        List<Map<String, Object>> exchangeMaps =
                HttpEndpointDiagnosticProjection.exchanges(exchanges);
        List<Map<String, Object>> issueMaps =
                HttpEndpointDiagnosticProjection.issues("projection-null-exchanges", exchanges);

        assertThat(exchangeMaps)
                .extracting(exchange -> exchange.get("index"))
                .containsExactly(1, 2);
        assertThat(issueMaps)
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("diagnosticsId", "projection-null-exchanges")
                        .containsEntry("exchangeIndex", 2)
                        .containsEntry("category", "unknown-path"));
    }

    private static WayangA2uiHttpEndpointExchange successfulRouteCatalogExchange() {
        return WayangA2uiHttpEndpointExchange.from(
                endpoint(),
                "GET",
                "/api/a2ui/route-catalog",
                "",
                Map.of(),
                Map.of("traceId", "trace-1"));
    }

    private static WayangA2uiHttpEndpointExchange failedUnknownPathExchange() {
        return WayangA2uiHttpEndpointExchange.from(
                endpoint(),
                "GET",
                "/api/a2ui/missing",
                "",
                Map.of(),
                Map.of());
    }

    private static WayangA2uiHttpEndpointBinding endpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
