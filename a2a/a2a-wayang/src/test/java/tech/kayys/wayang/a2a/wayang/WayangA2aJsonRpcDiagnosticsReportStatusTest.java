package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportStatusTest {

    @Test
    void passesWhenReadinessAndSpecAlignmentPass() {
        WayangA2aJsonRpcDiagnosticsReportStatus status =
                WayangA2aJsonRpcDiagnosticsReportStatus.from(
                        passingReadiness(),
                        WayangA2aSpecAlignmentSnapshot.defaults());

        assertThat(status.passed()).isTrue();
        assertThat(status.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
    }

    @Test
    void failsWhenReadinessFails() {
        WayangA2aJsonRpcDiagnosticsReportStatus status =
                WayangA2aJsonRpcDiagnosticsReportStatus.from(
                        failedReadiness(),
                        WayangA2aSpecAlignmentSnapshot.defaults());

        assertThat(status.passed()).isFalse();
        assertThat(status.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
    }

    @Test
    void failsWhenSpecAlignmentHasGaps() {
        WayangA2aJsonRpcDiagnosticsReportStatus status =
                WayangA2aJsonRpcDiagnosticsReportStatus.from(
                        passingReadiness(),
                        new WayangA2aSpecAlignmentSnapshot(
                                "a2a",
                                A2aProtocol.VERSION,
                                A2aProtocol.BINDING_JSONRPC,
                                false,
                                20,
                                19,
                                1,
                                List.of("route.SendMessage")));

        assertThat(status.passed()).isFalse();
        assertThat(status.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
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
