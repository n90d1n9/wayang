package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportBuilderTest {

    @Test
    void buildsDiagnosticsReportFromReadinessConfigAndSpecAlignment() {
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                19,
                1,
                List.of("route.SendMessage"));

        WayangA2aJsonRpcDiagnosticsReport report =
                WayangA2aJsonRpcDiagnosticsReportBuilder.from(
                                passingReadiness(),
                                WayangA2aJsonRpcHttpConfig.defaults(),
                                specAlignment)
                        .build();
        String reportJson = report.toJson();

        assertThat(report.passed()).isFalse();
        assertThat(report.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "specAlignment")
                        .containsEntry("code", "spec_alignment_gaps"));
        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) report.attributes().get("specAlignment")))
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 1);
        assertThat(reportJson).startsWith("{\"diagnosticsId\":");
        assertThat(reportJson.indexOf("\"checks\""))
                .isGreaterThan(reportJson.indexOf("\"issueCount\""));
        assertThat(reportJson.indexOf("\"attributes\""))
                .isGreaterThan(reportJson.indexOf("\"issues\""));
    }

    @Test
    void diagnosticsReportFactoryDelegatesToBuilder() {
        WayangA2aJsonRpcReadinessProbeResult readiness = passingReadiness();
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.defaults();

        assertThat(WayangA2aJsonRpcDiagnosticsReport.from(readiness, config).toMap())
                .isEqualTo(WayangA2aJsonRpcDiagnosticsReportBuilder.from(readiness, config).build().toMap());
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
}
