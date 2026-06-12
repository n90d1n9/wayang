package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiBeginRendering;
import tech.kayys.wayang.a2ui.core.A2uiClientCapabilities;
import tech.kayys.wayang.a2ui.core.A2uiProtocol;
import tech.kayys.wayang.a2ui.core.A2uiServerCapabilities;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadResult;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLookupProvider;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnostics;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnosticsSummary;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceDiagnostics;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourcePolicy;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceRegistry;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceSpec;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiTest {

    @Test
    void exposesA2uiAgentExtensionBlock() {
        Map<String, Object> extension = WayangA2ui.agentExtension(A2uiServerCapabilities.standard());
        String json = TransportJson.json(extension, "Unable to encode A2UI extension fixture");

        assertThat(extension)
                .containsEntry("uri", A2uiProtocol.EXTENSION_URI)
                .containsEntry("required", false);
        assertThat(extension.get("params")).isInstanceOf(Map.class);
        assertThat(json).startsWith("{\"uri\":");
        assertThat(json.indexOf("\"params\""))
                .isGreaterThan(json.indexOf("\"required\""));
    }

    @Test
    void storesClientCapabilitiesInAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();

        AgentRequest enriched = WayangA2ui.activate(WayangA2ui.withClientCapabilities(
                request,
                A2uiClientCapabilities.standard()));

        assertThat(enriched.context()).containsKey(WayangA2ui.CONTEXT_KEY);
        assertThat(TransportJson.json(
                (Map<String, Object>) enriched.context().get(WayangA2ui.CONTEXT_KEY),
                "Unable to encode A2UI context fixture"))
                .startsWith("{\"clientCapabilities\":");
        assertThat(WayangA2ui.clientCapabilities(enriched))
                .isPresent()
                .get()
                .matches(capabilities -> capabilities.supports(A2uiProtocol.STANDARD_CATALOG_ID));
    }

    @Test
    void storesSessionConfigInAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        WayangA2uiSessionConfig config = WayangA2uiSessionConfig.runLifecycle();

        AgentRequest enriched = WayangA2ui.withSessionConfig(request, config);

        assertThat(WayangA2ui.sessionConfig(enriched))
                .isPresent()
                .get()
                .matches(resolved -> resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
    }

    @Test
    void reportsDirectSessionConfigLoadResultFromAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfig(request, WayangA2uiSessionConfig.runLifecycle());

        SessionConfigLoadResult result = WayangA2ui.sessionConfigLoadResult(enriched);

        assertThat(result.loaded()).isTrue();
        assertThat(result.sourceDescription()).isEqualTo("a2ui.sessionConfig");
        assertThat(result.config().actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL)).isTrue();
    }

    @Test
    void reportsRequestDiagnosticsForDirectSessionConfig() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfig(request, WayangA2uiSessionConfig.runLifecycle());

        SessionConfigRequestDiagnostics diagnostics = WayangA2ui.sessionConfigDiagnostics(enriched);

        assertThat(diagnostics.contextPresent()).isTrue();
        assertThat(diagnostics.configPresent()).isTrue();
        assertThat(diagnostics.sourcePresent()).isFalse();
        assertThat(diagnostics.activeInput()).isEqualTo(SessionConfigRequestDiagnostics.ACTIVE_DIRECT_CONFIG);
        assertThat(diagnostics.directConfigActive()).isTrue();
        assertThat(diagnostics.loaded()).isTrue();
        assertThat(diagnostics.sourceDiagnosticsPresent()).isFalse();
        assertThat(diagnostics.loadResult().sourceDescription()).isEqualTo("a2ui.sessionConfig");
        assertThat(SessionConfigRequestDiagnostics.fromJson(diagnostics.toJson())).isEqualTo(diagnostics);

        SessionConfigRequestDiagnosticsSummary summary = WayangA2ui.sessionConfigDiagnosticsSummary(enriched);
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.activeInput()).isEqualTo(SessionConfigRequestDiagnostics.ACTIVE_DIRECT_CONFIG);
        assertThat(summary.sourceDiagnosticsPresent()).isFalse();
    }

    @Test
    void resolvesDirectSessionConfigJsonFromAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigJson(request, """
                {
                  "mode": "read-only"
                }
                """);

        SessionConfigLoadResult result = WayangA2ui.sessionConfigLoadResult(enriched);

        assertThat(WayangA2ui.sessionConfig(enriched))
                .isPresent()
                .get()
                .matches(resolved -> resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_HISTORY))
                .matches(resolved -> !resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
        assertThat(result.loaded()).isTrue();
        assertThat(result.sourceDescription()).isEqualTo("a2ui.sessionConfig");
        assertThat(result.config()).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void resolvesSessionConfigSourceFromAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(request, Map.of(
                "type", "inline",
                "json", """
                        {
                          "mode": "run-lifecycle"
                        }
                        """));

        assertThat(WayangA2ui.sessionConfig(enriched))
                .isPresent()
                .get()
                .matches(resolved -> resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
    }

    @Test
    void reportsSessionConfigSourceLoadResultFromAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(
                request,
                SessionConfigSourceSpec.inlineJson("request-inline", """
                        {
                          "mode": "read-only"
                        }
                        """));

        SessionConfigLoadResult result = WayangA2ui.sessionConfigLoadResult(enriched);

        assertThat(result.loaded()).isTrue();
        assertThat(result.sourceDescription()).isEqualTo("request-inline");
        assertThat(result.config()).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void reportsSessionConfigSourceDiagnosticsFromAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(
                request,
                SessionConfigSourceSpec.inlineJson("request-inline", """
                        {
                          "mode": "read-only"
                        }
                        """));
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .policy(SessionConfigSourcePolicy.deny("inline"))
                .build();

        SessionConfigSourceDiagnostics diagnostics =
                WayangA2ui.sessionConfigSourceDiagnostics(enriched, registry).orElseThrow();

        assertThat(diagnostics.sourceType()).isEqualTo("inline");
        assertThat(diagnostics.valid()).isTrue();
        assertThat(diagnostics.allowed()).isFalse();
        assertThat(diagnostics.failed()).isTrue();
        assertThat(diagnostics.policyErrors())
                .containsExactly("source source type inline is not allowed");
        assertThat(diagnostics.toJson()).contains(SessionConfigSourceDiagnostics.DIAGNOSTICS_ID);
    }

    @Test
    void reportsRequestDiagnosticsForSessionConfigSource() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(
                request,
                SessionConfigSourceSpec.inlineJson("request-inline", """
                        {
                          "mode": "read-only"
                        }
                        """));

        SessionConfigRequestDiagnostics diagnostics = WayangA2ui.sessionConfigDiagnostics(enriched);

        assertThat(diagnostics.contextPresent()).isTrue();
        assertThat(diagnostics.configPresent()).isFalse();
        assertThat(diagnostics.sourcePresent()).isTrue();
        assertThat(diagnostics.activeInput()).isEqualTo(SessionConfigRequestDiagnostics.ACTIVE_SOURCE);
        assertThat(diagnostics.sourceActive()).isTrue();
        assertThat(diagnostics.loaded()).isTrue();
        assertThat(diagnostics.sourceDiagnosticsPresent()).isTrue();
        assertThat(diagnostics.sourceDiagnostics().sourceType()).isEqualTo("inline");
        assertThat(diagnostics.toMap())
                .containsEntry("diagnosticsId", SessionConfigRequestDiagnostics.DIAGNOSTICS_ID)
                .containsEntry("sourceDiagnosticsPresent", true);

        SessionConfigRequestDiagnosticsSummary summary = WayangA2ui.sessionConfigDiagnosticsSummary(enriched);
        assertThat(summary.passed()).isTrue();
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.activeInput()).isEqualTo(SessionConfigRequestDiagnostics.ACTIVE_SOURCE);
        assertThat(summary.sourceType()).isEqualTo("inline");
    }

    @Test
    void reportsFailedSourceDiagnosticsForMalformedSourceJson() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSourceJson(request, "{not-json");

        SessionConfigSourceDiagnostics diagnostics =
                WayangA2ui.sessionConfigSourceDiagnostics(enriched).orElseThrow();

        assertThat(diagnostics.sourceType()).isEqualTo("unknown");
        assertThat(diagnostics.valid()).isFalse();
        assertThat(diagnostics.failed()).isTrue();
        assertThat(diagnostics.loadResult().sourceDescription()).isEqualTo("a2ui.sessionConfigSource");
        assertThat(diagnostics.loadResult().message())
                .contains("Unable to decode a2ui.sessionConfigSource JSON");
        assertThat(diagnostics.validationErrors())
                .containsExactly("Unable to decode a2ui.sessionConfigSource JSON");
    }

    @Test
    void resolvesSessionConfigSourceJsonFromAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSourceJson(request, """
                {
                  "type": "inline",
                  "description": "request-source-json",
                  "json": "{ \\"mode\\": \\"run-lifecycle\\" }"
                }
                """);

        SessionConfigLoadResult result = WayangA2ui.sessionConfigLoadResult(enriched);

        assertThat(WayangA2ui.sessionConfig(enriched))
                .isPresent()
                .get()
                .matches(resolved -> resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
        assertThat(result.loaded()).isTrue();
        assertThat(result.sourceDescription()).isEqualTo("request-source-json");
    }

    @Test
    void omitsSourceDiagnosticsWhenDirectConfigTakesPrecedence() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(
                WayangA2ui.withSessionConfig(request, WayangA2uiSessionConfig.readOnly()),
                SessionConfigSourceSpec.inlineJson("""
                        {
                          "mode": "run-lifecycle"
                        }
                        """));

        assertThat(WayangA2ui.sessionConfigSourceDiagnostics(enriched)).isEmpty();
    }

    @Test
    void reportsRequestDiagnosticsWhenNoA2uiContextExists() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();

        SessionConfigRequestDiagnostics diagnostics = WayangA2ui.sessionConfigDiagnostics(request);

        assertThat(diagnostics.contextPresent()).isFalse();
        assertThat(diagnostics.configPresent()).isFalse();
        assertThat(diagnostics.sourcePresent()).isFalse();
        assertThat(diagnostics.activeInput()).isEqualTo(SessionConfigRequestDiagnostics.ACTIVE_NONE);
        assertThat(diagnostics.missing()).isTrue();
        assertThat(diagnostics.sourceDiagnosticsPresent()).isFalse();
    }

    @Test
    void reportsMissingOrFailedRequestSessionConfigSources() {
        AgentRequest missing = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest unknownProvider = WayangA2ui.withSessionConfigSource(missing, Map.of(
                "type", "database",
                "tenantId", "tenant-a"));

        assertThat(WayangA2ui.sessionConfigLoadResult(missing).missing()).isTrue();
        assertThat(WayangA2ui.sessionConfigLoadResult(unknownProvider).failed()).isTrue();
        assertThat(WayangA2ui.sessionConfigLoadResult(unknownProvider).message())
                .contains("database");
    }

    @Test
    void storesStructuredSessionConfigSourceSpecsInAgentRequestContext() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(
                request,
                SessionConfigSourceSpec.inlineJson("""
                        {
                          "mode": "read-only"
                        }
                        """));

        assertThat(WayangA2ui.sessionConfig(enriched))
                .isPresent()
                .get()
                .matches(resolved -> resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_HISTORY))
                .matches(resolved -> !resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
    }

    @Test
    void resolvesSessionConfigSourceWithCallerSuppliedRegistry() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(request, Map.of(
                "type", "database",
                "tenantId", "tenant-a"));
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database((tenantId, lookupKey, values) -> Optional.of("""
                                {
                                  "mode": "read-only"
                                }
                                """)))
                .build();

        assertThat(WayangA2ui.sessionConfig(enriched, registry))
                .isPresent()
                .get()
                .matches(resolved -> resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_HISTORY))
                .matches(resolved -> !resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
    }

    @Test
    void prefersDirectSessionConfigOverSourceSpec() {
        AgentRequest request = AgentRequest.builder()
                .prompt("Render an interactive UI")
                .build();
        AgentRequest enriched = WayangA2ui.withSessionConfigSource(
                WayangA2ui.withSessionConfig(request, WayangA2uiSessionConfig.readOnly()),
                Map.of(
                        "type", "inline",
                        "json", """
                                {
                                  "mode": "run-lifecycle"
                                }
                                """));

        assertThat(WayangA2ui.sessionConfig(enriched))
                .isPresent()
                .get()
                .matches(resolved -> resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_HISTORY))
                .matches(resolved -> !resolved.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
    }

    @Test
    @SuppressWarnings("unchecked")
    void wrapsServerMessagesAsA2aDataParts() {
        Map<String, Object> dataPart = WayangA2ui.dataPart(A2uiBeginRendering.standard("main", "root"));

        assertThat(dataPart)
                .containsEntry("kind", "data")
                .containsKey("data")
                .containsKey("metadata");
        assertThat((Map<String, Object>) dataPart.get("metadata"))
                .containsEntry("mimeType", A2uiProtocol.MIME_TYPE);
    }
}
