package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessIssueSummaryProjectionTest {

    @Test
    void keepsOrderedSummaryEnvelopeWithoutOptionalIssueCounts() {
        WayangA2aJsonRpcReadinessIssueSummary summary = new WayangA2aJsonRpcReadinessIssueSummary(
                true,
                WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS,
                true,
                false,
                true,
                false,
                true,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                List.of());

        Map<String, Object> values = WayangA2aJsonRpcReadinessIssueSummaryProjection.summary(summary);

        assertThat(values.keySet()).containsExactly(
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "issueCount",
                "readinessIssueCount",
                "bindingReportIssueCount",
                "routeCatalogIssueCount",
                "smokeIssueCount",
                "issues");
        assertThat(values)
                .containsEntry("passed", true)
                .containsEntry("exitCode", WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS)
                .doesNotContainKeys("diagnosticHandlerIssueCount", "methodDispatchIssueCount");
    }

    @Test
    void keepsOptionalIssueCountsBeforeRouteAndSmokeCounts() {
        WayangA2aJsonRpcReadinessIssueSummary summary = new WayangA2aJsonRpcReadinessIssueSummary(
                false,
                WayangA2aJsonRpcSmokeResult.EXIT_FAILURE,
                false,
                true,
                false,
                true,
                false,
                2,
                1,
                2,
                3,
                4,
                5,
                6,
                List.of(Map.of("probe", "readiness"), Map.of("probe", "methodDispatch")));

        Map<String, Object> values = WayangA2aJsonRpcReadinessIssueSummaryProjection.summary(summary);

        assertThat(values.keySet()).containsExactly(
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "issueCount",
                "readinessIssueCount",
                "bindingReportIssueCount",
                "diagnosticHandlerIssueCount",
                "methodDispatchIssueCount",
                "routeCatalogIssueCount",
                "smokeIssueCount",
                "issues");
        assertThat(values)
                .containsEntry("diagnosticHandlerIssueCount", 3)
                .containsEntry("methodDispatchIssueCount", 4)
                .containsEntry("routeCatalogIssueCount", 5)
                .containsEntry("smokeIssueCount", 6);
    }

    @Test
    void parsesReadinessProbePayloadsThroughBuilder() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                                "statusCode", 404,
                                "routeOperation", "JsonRpc")),
                        null,
                        false,
                        null,
                        false);

        WayangA2aJsonRpcReadinessIssueSummary summary =
                WayangA2aJsonRpcReadinessIssueSummaryProjection.fromMap(readiness.toMap());

        assertThat(summary.passed()).isFalse();
        assertThat(summary.issueCount()).isEqualTo(1);
        assertThat(summary.readinessIssueCount()).isEqualTo(1);
        assertThat(summary.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("probe", WayangA2aJsonRpcReadinessIssueCatalog.PROBE_READINESS)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_BINDING_REPORT_PROBE_FAILED));
    }

    @Test
    void buildsIssueSummaryResponseThroughProjection() {
        WayangA2aJsonRpcReadinessIssueSummary summary = new WayangA2aJsonRpcReadinessIssueSummary(
                true,
                WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS,
                true,
                false,
                true,
                false,
                true,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                List.of());

        WayangA2aHttpResponse response = WayangA2aJsonRpcReadinessIssueSummaryProjection.response(summary);

        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY);
        assertThat(WayangA2aJsonRpcReadinessIssueSummary.from(response).toJson())
                .isEqualTo(summary.toJson());
    }
}
