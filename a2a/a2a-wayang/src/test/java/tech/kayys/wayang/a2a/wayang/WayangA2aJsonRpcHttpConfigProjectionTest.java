package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpConfigProjectionTest {

    @Test
    void keepsOrderedConfigEnvelope() {
        WayangA2aJsonRpcHttpConfig config = customConfig();

        Map<String, Object> values = WayangA2aJsonRpcHttpConfigProjection.config(config);

        assertThat(values.keySet()).containsExactly(
                "endpointPath",
                "smokePath",
                "smokeEnabled",
                "routeCatalogPath",
                "routeCatalogEnabled",
                "diagnosticsReportPath",
                "diagnosticsReportEnabled",
                "specComplianceReportPath",
                "specComplianceReportEnabled",
                "bindingReportPath",
                "bindingReportEnabled",
                "readinessPath",
                "readinessEnabled",
                "readinessIssueSummaryPath",
                "readinessIssueSummaryEnabled");
        assertThat(values)
                .containsEntry("endpointPath", "/a2a/rpc")
                .containsEntry("smokePath", "/internal/a2a/smoke")
                .containsEntry("smokeEnabled", false)
                .containsEntry("routeCatalogPath", "/internal/a2a/routes")
                .containsEntry("routeCatalogEnabled", false)
                .containsEntry("diagnosticsReportPath", "/internal/a2a/diagnostics")
                .containsEntry("diagnosticsReportEnabled", false)
                .containsEntry("specComplianceReportPath", "/internal/a2a/spec")
                .containsEntry("specComplianceReportEnabled", false)
                .containsEntry("bindingReportPath", "/internal/a2a/binding")
                .containsEntry("bindingReportEnabled", false)
                .containsEntry("readinessPath", "/internal/a2a/readiness")
                .containsEntry("readinessEnabled", false)
                .containsEntry("readinessIssueSummaryPath", "/internal/a2a/readiness/issues")
                .containsEntry("readinessIssueSummaryEnabled", false);
    }

    @Test
    void configRecordDelegatesToProjection() {
        WayangA2aJsonRpcHttpConfig config = customConfig();

        assertThat(config.toMap()).isEqualTo(WayangA2aJsonRpcHttpConfigProjection.config(config));
    }

    @Test
    void parsesLegacyAliasesBeforeProjectingCanonicalEnvelope() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.fromMap(Map.ofEntries(
                Map.entry("endpoint", "a2a/rpc"),
                Map.entry("smokeEndpointPath", "internal/a2a/smoke"),
                Map.entry("enableSmoke", "no"),
                Map.entry("catalogPath", "internal/a2a/routes"),
                Map.entry("routesEnabled", "0"),
                Map.entry("jsonRpcDiagnosticsPath", "internal/a2a/diagnostics"),
                Map.entry("jsonRpcDiagnosticsEnabled", "false"),
                Map.entry("complianceReportPath", "internal/a2a/spec"),
                Map.entry("specComplianceEnabled", false),
                Map.entry("diagnosticsPath", "internal/a2a/binding"),
                Map.entry("diagnosticsEnabled", "no"),
                Map.entry("healthPath", "internal/a2a/readiness"),
                Map.entry("healthEnabled", "false"),
                Map.entry("issueSummaryPath", "internal/a2a/readiness/issues"),
                Map.entry("issueSummaryEnabled", "0")));

        assertThat(WayangA2aJsonRpcHttpConfigProjection.config(config))
                .containsEntry("endpointPath", "/a2a/rpc")
                .containsEntry("smokePath", "/internal/a2a/smoke")
                .containsEntry("smokeEnabled", false)
                .containsEntry("routeCatalogPath", "/internal/a2a/routes")
                .containsEntry("routeCatalogEnabled", false)
                .containsEntry("diagnosticsReportPath", "/internal/a2a/diagnostics")
                .containsEntry("diagnosticsReportEnabled", false)
                .containsEntry("specComplianceReportPath", "/internal/a2a/spec")
                .containsEntry("specComplianceReportEnabled", false)
                .containsEntry("bindingReportPath", "/internal/a2a/binding")
                .containsEntry("bindingReportEnabled", false)
                .containsEntry("readinessPath", "/internal/a2a/readiness")
                .containsEntry("readinessEnabled", false)
                .containsEntry("readinessIssueSummaryPath", "/internal/a2a/readiness/issues")
                .containsEntry("readinessIssueSummaryEnabled", false);
    }

    private static WayangA2aJsonRpcHttpConfig customConfig() {
        return WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokePath("/internal/a2a/smoke")
                .smokeEnabled(false)
                .routeCatalogPath("/internal/a2a/routes")
                .routeCatalogEnabled(false)
                .diagnosticsReportPath("/internal/a2a/diagnostics")
                .diagnosticsReportEnabled(false)
                .specComplianceReportPath("/internal/a2a/spec")
                .specComplianceReportEnabled(false)
                .bindingReportPath("/internal/a2a/binding")
                .bindingReportEnabled(false)
                .readinessPath("/internal/a2a/readiness")
                .readinessEnabled(false)
                .readinessIssueSummaryPath("/internal/a2a/readiness/issues")
                .readinessIssueSummaryEnabled(false)
                .build();
    }
}
