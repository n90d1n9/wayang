package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportPartsTest {

    @Test
    void derivesReportPartsFromContext() {
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                19,
                1,
                List.of("route.SendMessage"));
        WayangA2aJsonRpcDiagnosticsReportContext context =
                WayangA2aJsonRpcDiagnosticsReportContext.from(
                        passingReadiness(),
                        WayangA2aJsonRpcHttpConfig.defaults(),
                        specAlignment);

        WayangA2aJsonRpcDiagnosticsReportParts parts =
                WayangA2aJsonRpcDiagnosticsReportParts.from(context);

        assertThat(parts.context()).isSameAs(context);
        assertThat(parts.readinessState().toMap())
                .containsEntry("bindingReportPassed", true)
                .containsEntry("routeCatalogRequired", false)
                .containsEntry("routeCatalogPassed", true)
                .containsEntry("smokeRequired", false)
                .containsEntry("smokePassed", true);
        assertThat(parts.status().passed()).isFalse();
        assertThat(parts.status().exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(parts.issueCount()).isEqualTo(1);
        assertThat(parts.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "specAlignment")
                        .containsEntry("code", "spec_alignment_gaps"));
        assertThat(parts.diagnosticChecks())
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment")
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1));
        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) parts.attributeValues().get("specAlignment")))
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 1);
    }

    @Test
    void exposesCopiedCollections() {
        WayangA2aJsonRpcDiagnosticsReportParts parts =
                WayangA2aJsonRpcDiagnosticsReportParts.from(
                        WayangA2aJsonRpcDiagnosticsReportContext.from(
                                passingReadiness(),
                                null,
                                null));

        assertThat(parts.diagnosticChecks()).isNotSameAs(
                WayangA2aJsonRpcReadinessDiagnosticChecks.from(passingReadiness()).toMaps());
        assertThat(parts.issues()).isEqualTo(parts.reportIssues().issues());
        assertThat(parts.attributeValues()).isEqualTo(parts.attributes().values());
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
