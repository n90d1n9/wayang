package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessIssueBreakdownTest {

    @Test
    void buildsCountsFromRequiredProbeIssueGroups() {
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

        assertThat(breakdown.issueCount()).isEqualTo(1);
        assertThat(breakdown.readinessIssueCount()).isEqualTo(1);
        assertThat(breakdown.bindingReportIssueCount()).isZero();
        assertThat(breakdown.diagnosticHandlerIssueCount()).isZero();
        assertThat(breakdown.methodDispatchIssueCount()).isZero();
        assertThat(breakdown.routeCatalogIssueCount()).isZero();
        assertThat(breakdown.smokeIssueCount()).isZero();
        assertThat(breakdown.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("probe", "readiness")
                        .containsEntry("code", "binding_report_probe_failed")
                        .containsEntry("statusCode", 404));
    }

    @Test
    void classifiesMethodDispatchCoverageIssuesSeparatelyFromBindingReportIssues() {
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                WayangA2aJsonRpcMethods.GET_TASK),
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE)));
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                        null,
                        false,
                        null,
                        false);

        WayangA2aJsonRpcReadinessIssueBreakdown breakdown =
                WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness);

        assertThat(breakdown.issueCount()).isEqualTo(3);
        assertThat(breakdown.readinessIssueCount()).isEqualTo(1);
        assertThat(breakdown.bindingReportIssueCount()).isZero();
        assertThat(breakdown.diagnosticHandlerIssueCount()).isZero();
        assertThat(breakdown.methodDispatchIssueCount()).isEqualTo(2);
        assertThat(breakdown.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("probe", "methodDispatch")
                        .containsEntry("source", "bindingReport")
                        .containsEntry("code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE))
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("probe", "methodDispatch")
                        .containsEntry("source", "bindingReport")
                        .containsEntry("field", "methodDispatch.methodGroups.taskQuery.complete")
                        .containsEntry("code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE))
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("probe", "readiness")
                        .containsEntry("code", "binding_report_probe_failed"));
    }

    @Test
    void classifiesConcreteDiagnosticHandlerIssuesSeparatelyFromBindingReportIssues() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                                "issues",
                                List.of(Map.of(
                                        "source",
                                        "bindingReport",
                                        "code",
                                        WayangA2aJsonRpcReadinessIssueCatalog
                                                .ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                                        "field",
                                        WayangA2aJsonRpcReadinessIssueCatalog
                                                .FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                                        "actual",
                                        "missing=[readiness], orphan=[]")),
                                "issueCount",
                                1)),
                        null,
                        false,
                        null,
                        false);

        WayangA2aJsonRpcReadinessIssueBreakdown breakdown =
                WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness);

        assertThat(breakdown.issueCount()).isEqualTo(2);
        assertThat(breakdown.readinessIssueCount()).isEqualTo(1);
        assertThat(breakdown.bindingReportIssueCount()).isZero();
        assertThat(breakdown.diagnosticHandlerIssueCount()).isEqualTo(1);
        assertThat(breakdown.methodDispatchIssueCount()).isZero();
        assertThat(breakdown.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("probe", "diagnosticHandlers")
                        .containsEntry("source", "bindingReport")
                        .containsEntry("code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE));
        assertThat(WayangA2aJsonRpcReadinessIssueSummary.from(readiness).toMap())
                .containsEntry("diagnosticHandlerIssueCount", 1)
                .doesNotContainKey("methodDispatchIssueCount");
    }
}
