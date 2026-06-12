package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessBindingReportIssueBucketRuleTest {

    @Test
    void matchesMethodDispatchByCodeAndField() {
        assertThat(WayangA2aJsonRpcReadinessBindingReportIssueBucketRule.methodDispatch()
                .matches(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE,
                        "field",
                        WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_COMPLETE)))
                .isTrue();
    }

    @Test
    void matchesMethodDispatchGroupByCodeAndFieldPrefix() {
        assertThat(WayangA2aJsonRpcReadinessBindingReportIssueBucketRule.methodDispatchGroup()
                .matches(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE,
                        "field",
                        "methodDispatch.methodGroups.taskQuery.complete")))
                .isTrue();
        assertThat(WayangA2aJsonRpcReadinessBindingReportIssueBucketRule.methodDispatchGroup()
                .matches(Map.of(
                        "code",
                        WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE,
                        "field",
                        "methodDispatch.complete")))
                .isFalse();
    }

    @Test
    void requiresConcreteDiagnosticHandlerCoverageActual() {
        WayangA2aJsonRpcReadinessBindingReportIssueBucketRule rule =
                WayangA2aJsonRpcReadinessBindingReportIssueBucketRule.diagnosticHandlers();

        assertThat(rule.matches(Map.of(
                "code",
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                "field",
                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                "actual",
                "missing=[readiness], orphan=[]")))
                .isTrue();
        assertThat(rule.matches(Map.of(
                "code",
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                "field",
                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                "actual",
                "missing=[], orphan=[]")))
                .isFalse();
    }
}
