package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportAttributesTest {

    @Test
    void includesConfigProtocolBindingAndSpecAlignment() {
        WayangA2aJsonRpcDiagnosticsReportAttributes attributes =
                WayangA2aJsonRpcDiagnosticsReportAttributes.from(
                        passingReadiness(),
                        WayangA2aJsonRpcHttpConfig.defaults(),
                        WayangA2aSpecAlignmentSnapshot.defaults());

        assertThat(attributes.values())
                .containsKeys("config", "specAlignment")
                .containsEntry("protocol", "A2A")
                .containsEntry("binding", "JSONRPC");
    }

    @Test
    void includesMethodDispatchWhenReported() {
        WayangA2aJsonRpcDiagnosticsReportAttributes attributes =
                WayangA2aJsonRpcDiagnosticsReportAttributes.from(
                        readinessWithReportedMethodDispatch(),
                        null,
                        WayangA2aSpecAlignmentSnapshot.defaults());

        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) attributes.values().get("methodDispatch")))
                .containsEntry("reported", true)
                .containsEntry("complete", false);
        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) attributes.values().get("config"))).isEmpty();
    }

    @Test
    void includesMethodRegistryWhenReported() {
        WayangA2aJsonRpcDiagnosticsReportAttributes attributes =
                WayangA2aJsonRpcDiagnosticsReportAttributes.from(
                        readinessWithReportedMethodRegistry(),
                        null,
                        WayangA2aSpecAlignmentSnapshot.defaults());

        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) attributes.values().get("methodRegistry")))
                .containsEntry("reported", true)
                .containsEntry("groupCount", 1)
                .containsEntry("overridePolicy",
                        WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE)
                .containsEntry("overrideCount", 0);
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

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithReportedMethodDispatch() {
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                WayangA2aJsonRpcMethods.GET_TASK),
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE)));
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                null,
                false,
                null,
                false);
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithReportedMethodRegistry() {
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                null,
                WayangA2aJsonRpcMethodRegistryTestFixtures.taskRegistrySnapshot());
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                null,
                false,
                null,
                false);
    }
}
