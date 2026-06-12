package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpConfigDecoderTest {

    @Test
    void decodesLegacyAliasesIntoCanonicalConfig() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfigDecoder.fromMap(Map.ofEntries(
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

        assertThat(config.toMap())
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
    void configRecordDelegatesMapDecodingToDecoder() {
        Map<String, Object> values = Map.of(
                "endpointPath", "a2a/rpc",
                "smokePath", "internal/a2a/smoke",
                "smokeEnabled", "false");

        assertThat(WayangA2aJsonRpcHttpConfig.fromMap(values).toMap())
                .isEqualTo(WayangA2aJsonRpcHttpConfigDecoder.fromMap(values).toMap());
    }

    @Test
    void parsesLenientBooleanTokensInKeyOrder() {
        assertThat(WayangA2aJsonRpcHttpConfigDecoder.firstBoolean(
                Map.of("enabled", "yes"), "enabled"))
                .contains(true);
        assertThat(WayangA2aJsonRpcHttpConfigDecoder.firstBoolean(
                Map.of("enabled", "1"), "enabled"))
                .contains(true);
        assertThat(WayangA2aJsonRpcHttpConfigDecoder.firstBoolean(
                Map.of("enabled", "no"), "enabled"))
                .contains(false);
        assertThat(WayangA2aJsonRpcHttpConfigDecoder.firstBoolean(
                Map.of("enabled", "0"), "enabled"))
                .contains(false);
        assertThat(WayangA2aJsonRpcHttpConfigDecoder.firstBoolean(
                Map.of("primary", "maybe", "fallback", false), "primary", "fallback"))
                .contains(false);
        assertThat(WayangA2aJsonRpcHttpConfigDecoder.firstBoolean(
                Map.of("enabled", "maybe"), "enabled"))
                .isEmpty();
        assertThat(WayangA2aJsonRpcHttpConfigDecoder.firstBoolean(
                Map.of("primary", true, "fallback", false), "primary", "fallback"))
                .contains(true);
    }
}
