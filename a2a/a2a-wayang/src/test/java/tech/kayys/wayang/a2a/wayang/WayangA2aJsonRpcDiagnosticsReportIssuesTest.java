package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportIssuesTest {

    @Test
    void appendsSpecAlignmentGapIssueWhenSpecIsNotAligned() {
        WayangA2aJsonRpcDiagnosticsReportIssues issues =
                WayangA2aJsonRpcDiagnosticsReportIssues.from(
                        WayangA2aJsonRpcReadinessIssueSummary.from(passingReadiness()),
                        new WayangA2aSpecAlignmentSnapshot(
                                "a2a",
                                A2aProtocol.VERSION,
                                A2aProtocol.BINDING_JSONRPC,
                                false,
                                20,
                                19,
                                1,
                                List.of("route.SendMessage")));

        assertThat(issues.issueCount()).isEqualTo(1);
        assertThat(issues.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "specAlignment")
                        .containsEntry("code", "spec_alignment_gaps")
                        .containsEntry("actual", "1"));
    }

    @Test
    void keepsReadinessIssuesWhenSpecIsAligned() {
        WayangA2aJsonRpcDiagnosticsReportIssues issues =
                WayangA2aJsonRpcDiagnosticsReportIssues.from(
                        failedReadiness(),
                        WayangA2aSpecAlignmentSnapshot.defaults());

        assertThat(issues.issueCount()).isEqualTo(1);
        assertThat(issues.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("probe", WayangA2aJsonRpcReadinessIssueCatalog.PROBE_READINESS)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_BINDING_REPORT_PROBE_FAILED));
    }

    private static WayangA2aJsonRpcReadinessProbeResult passingReadiness() {
        WayangA2aJsonRpcBindingReport report = WayangA2aJsonRpcBindingReport.defaults();
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                null,
                false,
                null,
                false);
    }

    private static WayangA2aJsonRpcReadinessProbeResult failedReadiness() {
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                        "statusCode", 404,
                        "routeOperation", "JsonRpc")),
                null,
                false,
                null,
                false);
    }
}
