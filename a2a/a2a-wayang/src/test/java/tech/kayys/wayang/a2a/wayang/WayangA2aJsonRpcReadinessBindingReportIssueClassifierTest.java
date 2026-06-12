package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessBindingReportIssueClassifierTest {

    @Test
    void classifiesKnownCoverageIssuesIntoConcreteProbeBuckets() {
        WayangA2aJsonRpcReadinessBindingReportIssueClassifier classifier =
                WayangA2aJsonRpcReadinessBindingReportIssueClassifier.defaults();

        assertThat(classifier.bucket(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog
                                .ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                        "field",
                        WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                        "actual",
                        "missing=[readiness], orphan=[]")))
                .isEqualTo(WayangA2aJsonRpcReadinessIssueCatalog.PROBE_DIAGNOSTIC_HANDLERS);
        assertThat(classifier.bucket(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog
                                .ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE,
                        "field",
                        "methodDispatch.methodGroups.taskQuery.complete")))
                .isEqualTo(WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_DISPATCH);
        assertThat(classifier.bucket(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_COUNT_MISSING,
                        "field",
                        "methodCount")))
                .isEqualTo(WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT);
    }

    @Test
    void supportsScopedRuleSetsForFutureBuckets() {
        WayangA2aJsonRpcReadinessBindingReportIssueClassifier classifier =
                new WayangA2aJsonRpcReadinessBindingReportIssueClassifier(
                        List.of(WayangA2aJsonRpcReadinessBindingReportIssueBucketRule.methodDispatch()));

        assertThat(classifier.bucket(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE,
                        "field",
                        WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_COMPLETE)))
                .isEqualTo(WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_DISPATCH);
        assertThat(classifier.bucket(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog
                                .ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                        "field",
                        WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                        "actual",
                        "missing=[readiness], orphan=[]")))
                .isEqualTo(WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT);
    }
}
