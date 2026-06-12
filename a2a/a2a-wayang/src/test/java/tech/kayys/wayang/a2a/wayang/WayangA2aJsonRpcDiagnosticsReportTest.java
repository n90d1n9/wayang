package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportTest {

    @Test
    void specAlignmentGapsContributeDiagnosticsIssue() {
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                18,
                2,
                List.of("route.SendMessage", "agent_card.top_level_fields"));

        WayangA2aJsonRpcDiagnosticsReport report = WayangA2aJsonRpcDiagnosticsReport.from(
                passingReadiness(),
                WayangA2aJsonRpcHttpConfig.defaults(),
                specAlignment);
        Map<String, Object> attributes = map(report.toMap().get("attributes"));

        assertThat(report.passed()).isFalse();
        assertThat(report.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues()).singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "specAlignment")
                        .containsEntry("code", "spec_alignment_gaps")
                        .containsEntry("field", "gapCount")
                        .containsEntry("expected", "0")
                        .containsEntry("actual", "2"));
        assertThat(map(attributes.get("specAlignment")))
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 2)
                .containsEntry("gapCategories", List.of())
                .containsEntry("gapIds", List.of("route.SendMessage", "agent_card.top_level_fields"))
                .containsEntry("categorySummaries", List.of());
        assertThat(maps(report.toMap().get("checks")))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 2));
    }

    @Test
    void exposesSharedDiagnosticsContractView() {
        WayangA2aJsonRpcDiagnosticsReport report = WayangA2aJsonRpcDiagnosticsReport.from(
                passingReadiness(),
                WayangA2aJsonRpcHttpConfig.defaults());

        assertThat(report.standardDiagnostics().toMap())
                .containsEntry("diagnosticsId", WayangA2aJsonRpcDiagnosticsReport.DIAGNOSTICS_ID)
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0)
                .containsEntry("checks", report.checks())
                .containsEntry("issues", report.issues())
                .containsEntry("attributes", report.attributes());
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

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }

    private static List<Map<String, Object>> maps(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return ((List<?>) value).stream()
                .map(entry -> {
                    assertThat(entry).isInstanceOf(Map.class);
                    return WayangA2aMaps.copyMap((Map<?, ?>) entry);
                })
                .toList();
    }
}
