package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodDispatchIssuesTest {

    @Test
    void emitsSummaryAndGroupIssuesForIncompleteDispatchCoverage() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                WayangA2aJsonRpcMethods.GET_TASK),
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE));

        assertThat(WayangA2aJsonRpcMethodDispatchIssues.from(coverage))
                .hasSize(2)
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE)
                        .containsEntry(
                                "field",
                                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_COMPLETE)
                        .containsEntry("actual", "missing=[GetTask], orphan=[]"))
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE)
                        .containsEntry(
                                "field",
                                WayangA2aJsonRpcReadinessIssueCatalog.methodDispatchGroupCompleteField(
                                        WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY))
                        .containsEntry("actual", "missing=[GetTask], orphan=[]")
                        .containsEntry(
                                "message",
                                "A2A JSON-RPC taskQuery method dispatch coverage was incomplete."));
    }

    @Test
    void omitsIssuesForCompleteOrUnreportedCoverage() {
        assertThat(WayangA2aJsonRpcMethodDispatchIssues.from(null)).isEmpty();
        assertThat(WayangA2aJsonRpcMethodDispatchIssues.from(
                        WayangA2aJsonRpcMethodDispatchCoverage.from(
                                WayangA2aJsonRpcMethods.methods(),
                                WayangA2aJsonRpcMethods.methods())))
                .isEmpty();
    }
}
