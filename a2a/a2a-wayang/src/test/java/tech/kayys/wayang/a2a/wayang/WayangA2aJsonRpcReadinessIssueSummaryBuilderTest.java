package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessIssueSummaryBuilderTest {

    @Test
    void buildsSummaryFromReadinessAndIssueBreakdown() {
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
                WayangA2aJsonRpcReadinessIssueSummaryBuilder.from(readiness).build();
        String summaryJson = summary.toJson();

        assertThat(summary.passed()).isFalse();
        assertThat(summary.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(summary.issueCount()).isEqualTo(1);
        assertThat(summary.readinessIssueCount()).isEqualTo(1);
        assertThat(summary.bindingReportIssueCount()).isZero();
        assertThat(summary.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("probe", WayangA2aJsonRpcReadinessIssueCatalog.PROBE_READINESS)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_BINDING_REPORT_PROBE_FAILED));
        assertThat(summaryJson).startsWith("{\"passed\":");
        assertThat(summaryJson.indexOf("\"issues\""))
                .isGreaterThan(summaryJson.indexOf("\"smokeIssueCount\""));
    }

    @Test
    void summaryFactoryDelegatesToBuilder() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                                "statusCode", 404,
                                "routeOperation", "JsonRpc")),
                        null,
                        false,
                        null,
                        false);

        assertThat(WayangA2aJsonRpcReadinessIssueSummary.from(readiness).toMap())
                .isEqualTo(WayangA2aJsonRpcReadinessIssueSummaryBuilder.from(readiness).build().toMap());
    }

    @Test
    void canUsePrecomputedIssueBreakdown() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                                "statusCode", 404,
                                "routeOperation", "JsonRpc")),
                        null,
                        false,
                        null,
                        false);
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown =
                WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness);

        assertThat(WayangA2aJsonRpcReadinessIssueSummaryBuilder.from(readiness, breakdown).build().toMap())
                .isEqualTo(WayangA2aJsonRpcReadinessIssueSummaryBuilder.from(readiness).build().toMap());
    }
}
