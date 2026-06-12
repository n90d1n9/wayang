package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportProjectionTest {

    @Test
    void keepsOrderedDiagnosticsEnvelope() {
        WayangA2aJsonRpcDiagnosticsReport report = report();

        Map<String, Object> values = WayangA2aJsonRpcDiagnosticsReportProjection.report(report);

        assertThat(values.keySet()).containsExactly(
                "diagnosticsId",
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "issueCount",
                "checks",
                "issues",
                "attributes");
        assertThat(values)
                .containsEntry("diagnosticsId", WayangA2aJsonRpcDiagnosticsReport.DIAGNOSTICS_ID)
                .containsEntry("passed", false)
                .containsEntry("exitCode", WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(map(values.get("attributes")).keySet()).containsExactly("config", "specAlignment");
    }

    @Test
    void parsesDiagnosticsMapsWithDefaultsAndNormalizedChildren() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", true);
        values.put("exitCode", WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        values.put("checks", List.of(Map.of("probe", "bindingReport")));
        values.put("issues", List.of());
        values.put("attributes", Map.of("config", Map.of("diagnosticsReportEnabled", true)));

        WayangA2aJsonRpcDiagnosticsReport report =
                WayangA2aJsonRpcDiagnosticsReportProjection.fromMap(values);

        assertThat(report.diagnosticsId()).isEqualTo(WayangA2aJsonRpcDiagnosticsReport.DIAGNOSTICS_ID);
        assertThat(report.passed()).isTrue();
        assertThat(report.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(report.checks()).singleElement()
                .satisfies(check -> assertThat(check).containsEntry("probe", "bindingReport"));
        assertThat(map(report.attributes()).keySet()).containsExactly("config");
    }

    @Test
    void buildsStandardDiagnosticsThroughProjection() {
        WayangA2aJsonRpcDiagnosticsReport report = report();

        Map<String, Object> standard =
                WayangA2aJsonRpcDiagnosticsReportProjection.standardDiagnostics(report).toMap();

        assertThat(standard.keySet()).containsExactly(
                "diagnosticsId",
                "passed",
                "exitCode",
                "issueCount",
                "checks",
                "issues",
                "attributes");
        assertThat(standard)
                .containsEntry("diagnosticsId", WayangA2aJsonRpcDiagnosticsReport.DIAGNOSTICS_ID)
                .containsEntry("passed", false)
                .containsEntry("issueCount", 1)
                .containsEntry("checks", report.checks())
                .containsEntry("issues", report.issues())
                .containsEntry("attributes", report.attributes());
    }

    @Test
    void buildsDiagnosticsResponseThroughProjection() {
        WayangA2aJsonRpcDiagnosticsReport report = report();

        WayangA2aHttpResponse response = WayangA2aJsonRpcDiagnosticsReportProjection.response(report);

        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS);
        assertThat(WayangA2aJsonRpcDiagnosticsReport.fromJson(response.body()).toJson())
                .isEqualTo(report.toJson());
    }

    private static WayangA2aJsonRpcDiagnosticsReport report() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("config", Map.of("diagnosticsReportEnabled", true));
        attributes.put("specAlignment", Map.of("aligned", false));
        return new WayangA2aJsonRpcDiagnosticsReport(
                WayangA2aJsonRpcDiagnosticsReport.DIAGNOSTICS_ID,
                false,
                WayangA2aJsonRpcSmokeResult.EXIT_FAILURE,
                true,
                false,
                true,
                false,
                true,
                1,
                List.of(Map.of("probe", "bindingReport", "passed", true)),
                List.of(Map.of("source", "specAlignment", "code", "spec_alignment_gaps")),
                attributes);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
