package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessBindingReportIssueBucketsTest {

    @Test
    void keepsGenericIssuesInBindingReportBucket() {
        WayangA2aJsonRpcReadinessBindingReportIssueBuckets buckets =
                WayangA2aJsonRpcReadinessBindingReportIssueBuckets.from(List.of(Map.of(
                        "code", "method_count_missing",
                        "field", "methodCount")));

        assertThat(buckets.bindingReportIssues())
                .singleElement()
                .satisfies(issue -> assertThat(issue).containsEntry("code", "method_count_missing"));
        assertThat(buckets.diagnosticHandlerIssues()).isEmpty();
        assertThat(buckets.methodDispatchIssues()).isEmpty();
    }

    @Test
    void routesConcreteDiagnosticHandlerCoverageIssuesToDiagnosticHandlerBucket() {
        WayangA2aJsonRpcReadinessBindingReportIssueBuckets buckets =
                WayangA2aJsonRpcReadinessBindingReportIssueBuckets.from(List.of(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                        "field",
                        WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                        "actual",
                        "missing=[readiness], orphan=[]")));

        assertThat(buckets.bindingReportIssues()).isEmpty();
        assertThat(buckets.diagnosticHandlerIssues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("field",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .FIELD_DIAGNOSTIC_HANDLERS_COMPLETE));
        assertThat(buckets.methodDispatchIssues()).isEmpty();
    }

    @Test
    void keepsUnreportedDiagnosticHandlerCoverageIssuesInBindingReportBucket() {
        WayangA2aJsonRpcReadinessBindingReportIssueBuckets buckets =
                WayangA2aJsonRpcReadinessBindingReportIssueBuckets.from(List.of(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                        "field",
                        WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                        "actual",
                        "missing=[], orphan=[]")));

        assertThat(buckets.bindingReportIssues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("actual", "missing=[], orphan=[]"));
        assertThat(buckets.diagnosticHandlerIssues()).isEmpty();
        assertThat(buckets.methodDispatchIssues()).isEmpty();
    }

    @Test
    void routesMethodDispatchCoverageIssuesToMethodDispatchBucket() {
        WayangA2aJsonRpcReadinessBindingReportIssueBuckets buckets =
                WayangA2aJsonRpcReadinessBindingReportIssueBuckets.from(List.of(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE,
                        "field",
                        WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_COMPLETE)));

        assertThat(buckets.bindingReportIssues()).isEmpty();
        assertThat(buckets.diagnosticHandlerIssues()).isEmpty();
        assertThat(buckets.methodDispatchIssues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE));
    }

    @Test
    void keepsSameCodeWithDifferentFieldInBindingReportBucket() {
        WayangA2aJsonRpcReadinessBindingReportIssueBuckets buckets =
                WayangA2aJsonRpcReadinessBindingReportIssueBuckets.from(List.of(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE,
                        "field",
                        "methodDispatch")));

        assertThat(buckets.bindingReportIssues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("field", "methodDispatch"));
        assertThat(buckets.diagnosticHandlerIssues()).isEmpty();
        assertThat(buckets.methodDispatchIssues()).isEmpty();
    }
}
