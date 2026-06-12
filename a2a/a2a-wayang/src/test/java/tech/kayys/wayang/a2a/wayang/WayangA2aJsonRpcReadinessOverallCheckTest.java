package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessOverallCheckTest {

    @Test
    void buildsOverallReadinessRowFromBreakdown() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithFailedBindingReportProbe();
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown =
                WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness);

        assertThat(WayangA2aJsonRpcReadinessOverallCheck.from(readiness, breakdown).toMap())
                .containsEntry("probe", "readiness")
                .containsEntry("required", true)
                .containsEntry("passed", false)
                .containsEntry("statusCode", 0)
                .containsEntry("routeOperation", WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS)
                .containsEntry("issueCount", 1);
    }

    @Test
    void computesBreakdownWhenNotProvided() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithFailedBindingReportProbe();

        assertThat(WayangA2aJsonRpcReadinessOverallCheck.from(readiness, null).toMap())
                .containsEntry("probe", "readiness")
                .containsEntry("passed", false)
                .containsEntry("issueCount", 1);
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithFailedBindingReportProbe() {
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
