package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadResult;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnostics;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnosticsSummary;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceCapability;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceDiagnostics;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourcePolicy;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceSpec;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceSpecs;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSources;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class WayangA2uiSessionConfigSourceContractTest {

    private final A2uiContractAssert contracts = new A2uiContractAssert();

    @Test
    void fallbackSourceSpecMatchesContractFixture() throws IOException {
        SessionConfigSourceSpec spec = SessionConfigSourceSpec.firstAvailable(
                SessionConfigSourceSpecs.database("tenant-a", "operators"),
                SessionConfigSourceSpecs.s3("wayang-config", "tenants/tenant-a/a2ui-session.json"),
                SessionConfigSourceSpec.file(Path.of("/etc/wayang/a2ui-session-fallback.json")));

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-source-fallback.json",
                TransportJson.json(spec.toMap(), "Unable to encode A2UI session config source fixture"));
    }

    @Test
    void redactedSourceSpecMatchesContractFixture() throws IOException {
        Map<String, Object> sourceValues = new LinkedHashMap<>();
        sourceValues.put("bucket", "wayang-config");
        sourceValues.put("key", "tenants/tenant-a/a2ui-session.json");
        sourceValues.put("endpointUrl", "https://object-store.internal");
        sourceValues.put("accessKeyId", "AKIA_TEST");
        sourceValues.put("secretAccessKey", "secret");
        SessionConfigSourceSpec spec = SessionConfigSourceSpec.provider("s3", sourceValues);

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-source-redacted.json",
                TransportJson.json(
                        spec.toDiagnosticMap(),
                        "Unable to encode A2UI redacted session config source fixture"));
    }

    @Test
    void fallbackLoadResultTraceMatchesContractFixture() throws IOException {
        SessionConfigLoadResult result = SessionConfigSources.loadFirstResult(
                SessionConfigSources.json("database:tenant-a", Optional::empty),
                SessionConfigSources.classpath("a2ui/session-config-readonly.json"));

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-load-result-fallback-trace.json",
                result.toJson());
    }

    @Test
    void sourceDiagnosticsMatchesContractFixture() throws IOException {
        SessionConfigSourceCapability inlineCapability = SessionConfigSourceCapability.inline("inline");
        SessionConfigSourceDiagnostics diagnostics = new SessionConfigSourceDiagnostics(
                SessionConfigSourceDiagnostics.DIAGNOSTICS_ID,
                "inline",
                SessionConfigSourceSpec
                        .inlineJson("contract-inline", "{ \"mode\": \"read-only\" }")
                        .toDiagnosticMap(),
                SessionConfigLoadResult.failed(
                        "session-config-source-policy",
                        new IllegalArgumentException("source source type inline is not allowed")),
                SessionConfigSourcePolicy.deny("inline").toMap(),
                List.of(),
                List.of("source source type inline is not allowed"),
                inlineCapability.toMap(),
                Map.of("inline", inlineCapability.toMap()));
        String diagnosticsJson = diagnostics.toJson();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-source-diagnostics-policy-rejected.json",
                diagnosticsJson);
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-source-diagnostics-policy-rejected.json",
                SessionConfigSourceDiagnostics.fromJson(diagnosticsJson).toJson());
    }

    @Test
    void requestDiagnosticsMatchesContractFixture() throws IOException {
        SessionConfigRequestDiagnostics diagnostics = new SessionConfigRequestDiagnostics(
                SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                "a2ui",
                "sessionConfig",
                "sessionConfigSource",
                true,
                true,
                false,
                SessionConfigRequestDiagnostics.ACTIVE_DIRECT_CONFIG,
                SessionConfigLoadResult.loaded("a2ui.sessionConfig", WayangA2uiSessionConfig.readOnly()),
                null);
        String diagnosticsJson = diagnostics.toJson();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-request-diagnostics-direct.json",
                diagnosticsJson);
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-request-diagnostics-direct.json",
                SessionConfigRequestDiagnostics.fromJson(diagnosticsJson).toJson());
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-request-diagnostics-summary-direct.json",
                diagnostics.summary().toJson());
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-request-diagnostics-summary-direct.json",
                SessionConfigRequestDiagnosticsSummary.fromJson(diagnostics.summary().toJson()).toJson());
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-session-config-request-diagnostics-summary-direct.json",
                SessionConfigRequestDiagnosticsSummary.fromDiagnosticsJson(diagnosticsJson).toJson());
    }
}
