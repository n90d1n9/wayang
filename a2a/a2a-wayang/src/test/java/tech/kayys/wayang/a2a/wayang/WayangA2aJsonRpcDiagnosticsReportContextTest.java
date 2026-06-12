package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportContextTest {

    @Test
    void precomputesBreakdownAndSummaryFromReadiness() {
        WayangA2aJsonRpcReadinessProbeResult readiness = failedReadiness();

        WayangA2aJsonRpcDiagnosticsReportContext context =
                WayangA2aJsonRpcDiagnosticsReportContext.from(readiness, null, null);

        assertThat(context.readiness()).isSameAs(readiness);
        assertThat(context.config()).isNull();
        assertThat(context.breakdown().issueCount()).isEqualTo(1);
        assertThat(context.breakdown().readinessIssueCount()).isEqualTo(1);
        assertThat(context.summary().toMap())
                .isEqualTo(WayangA2aJsonRpcReadinessIssueSummaryBuilder
                        .from(readiness, context.breakdown())
                        .build()
                        .toMap());
    }

    @Test
    void normalizesMissingSpecAlignmentToDefaults() {
        WayangA2aJsonRpcDiagnosticsReportContext context =
                WayangA2aJsonRpcDiagnosticsReportContext.from(failedReadiness(), null, null);

        assertThat(context.specAlignment().toMap())
                .isEqualTo(WayangA2aSpecAlignmentSnapshot.defaults().toMap());
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
