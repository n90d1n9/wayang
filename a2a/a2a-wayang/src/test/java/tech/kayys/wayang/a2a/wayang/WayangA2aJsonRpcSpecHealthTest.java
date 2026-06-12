package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcSpecHealthTest {

    @Test
    void sharesLiveSpecAlignmentSnapshotAcrossAdapterHealthReports() {
        WayangA2aJsonRpcHttpAdapter adapter = adapter();
        WayangA2aSpecAlignmentSnapshot snapshot = adapter.specAlignmentSnapshot();

        WayangA2aJsonRpcDiagnosticsReport diagnostics = adapter.diagnosticsReport();
        WayangA2aJsonRpcSpecComplianceReport compliance = adapter.specComplianceReport();

        assertThat(adapter.specHealth().specAlignmentSnapshot().toMap()).isEqualTo(snapshot.toMap());
        assertThat(adapter.specAlignmentReport().aligned()).isTrue();
        assertThat(map(diagnostics.attributes().get("specAlignment"))).isEqualTo(snapshot.toMap());
        assertThat(map(compliance.attributes().get("specAlignment"))).isEqualTo(snapshot.toMap());
        assertThat(diagnostics.passed()).isTrue();
        assertThat(compliance.passed()).isTrue();
    }

    @Test
    void appliesInjectedSpecAlignmentGapsToDiagnosticsAndCompliance() {
        WayangA2aJsonRpcSpecHealth health = adapter().specHealth();
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                19,
                1,
                List.of("route.SendMessage"),
                List.of(new WayangA2aSpecAlignmentCategorySummary(
                        "route",
                        12,
                        11,
                        1,
                        List.of("route.SendMessage"))));

        WayangA2aJsonRpcDiagnosticsReport diagnostics = health.diagnosticsReport(specAlignment);
        WayangA2aJsonRpcSpecComplianceReport compliance = health.specComplianceReport(specAlignment);

        assertThat(diagnostics.passed()).isFalse();
        assertThat(compliance.passed()).isFalse();
        assertThat(map(diagnostics.attributes().get("specAlignment")))
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 1)
                .containsEntry("gapCategories", List.of("route"));
        assertThat(map(compliance.attributes().get("specAlignment")))
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 1)
                .containsEntry("gapCategories", List.of("route"));
        assertThat(diagnostics.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("source", "specAlignment")
                        .containsEntry("code", "spec_alignment_gaps")
                        .containsEntry("actual", "1"));
        assertThat(diagnostics.checks())
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment:route")
                        .containsEntry("category", "route")
                        .containsEntry("passed", false)
                        .containsEntry("gapIds", List.of("route.SendMessage")));
        assertThat(compliance.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("source", "specAlignment")
                        .containsEntry("code", "spec_alignment_gaps")
                        .containsEntry("actual", "1"));
    }

    private static WayangA2aJsonRpcHttpAdapter adapter() {
        return WayangA2aJsonRpcHttpAdapter.configured(
                WayangA2aJsonRpcDispatcher.forExecution(
                        card(),
                        new InMemoryWayangA2aTaskStore(),
                        request -> AgentResponse.builder()
                                .runId("run-spec-health")
                                .requestId(request.requestId())
                                .answer("ok")
                                .strategy("test")
                                .build()),
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokeEnabled(false)
                        .build());
    }

    private static A2aAgentCard card() {
        return A2aAgentCard.minimal(
                "Wayang",
                "A2A JSON-RPC spec-health endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
