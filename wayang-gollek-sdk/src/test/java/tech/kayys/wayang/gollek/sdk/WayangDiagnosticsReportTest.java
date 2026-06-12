package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangDiagnosticsReportTest {

    @Test
    void normalizesSharedDiagnosticsEnvelope() {
        WayangDiagnosticsReport report = WayangDiagnosticsReport.from(
                " adapter.diagnostics ",
                false,
                1,
                0,
                List.of(Map.of("probe", "binding", "passed", false)),
                List.of(Map.of("code", "binding_failed")),
                Map.of("protocol", "A2A"));

        assertThat(report.diagnosticsId()).isEqualTo("adapter.diagnostics");
        assertThat(report.passed()).isFalse();
        assertThat(report.exitCode()).isEqualTo(1);
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.toMap())
                .containsEntry("diagnosticsId", "adapter.diagnostics")
                .containsEntry("passed", false)
                .containsEntry("exitCode", 1)
                .containsEntry("issueCount", 1)
                .containsEntry("attributes", Map.of("protocol", "A2A"));
    }

    @Test
    void preservesWireFriendlyFieldOrder() {
        WayangDiagnosticsReport report = WayangDiagnosticsReport.from(
                "adapter.diagnostics",
                true,
                0,
                0,
                List.of(),
                List.of(),
                Map.of());

        assertThat(report.toMap().keySet())
                .containsExactly(
                        "diagnosticsId",
                        "passed",
                        "exitCode",
                        "issueCount",
                        "checks",
                        "issues",
                        "attributes");
    }

    @Test
    void preservesBlankDiagnosticFields() {
        WayangDiagnosticsReport report = WayangDiagnosticsReport.from(
                "adapter.diagnostics",
                true,
                0,
                0,
                List.of(Map.of("probe", "routeCatalog", "routeOperation", "")),
                List.of(),
                Map.of("binding", "JSONRPC"));

        assertThat(report.checks())
                .singleElement()
                .satisfies(check -> assertThat(check).containsEntry("routeOperation", ""));
        assertThat(report.toMap())
                .containsEntry("checks", List.of(Map.of(
                        "probe", "routeCatalog",
                        "routeOperation", "")));
    }

    @Test
    void redactsSecretLikeStringsAcrossDiagnosticsReportMaps() {
        WayangDiagnosticsReport report = WayangDiagnosticsReport.from(
                "adapter.diagnostics",
                false,
                1,
                1,
                List.of(Map.of(
                        "probe", "database",
                        "attributes", Map.of(
                                "url", "jdbc:postgresql://ops:inline-password@localhost/wayang"))),
                List.of(Map.of(
                        "code", "db_failed",
                        "message", "password=inline-secret token=inline-token")),
                Map.of("nested", List.of(Map.of(
                        "credentials", "accessKeyId=inline-access secretAccessKey=inline-secret"))));

        String output = report.toMap().toString();

        assertThat(output)
                .contains("<redacted>")
                .doesNotContain("inline-password")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-token")
                .doesNotContain("inline-access");
    }
}
