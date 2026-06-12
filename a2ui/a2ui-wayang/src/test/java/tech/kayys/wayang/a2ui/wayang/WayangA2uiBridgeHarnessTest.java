package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiBridgeHarnessTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void runsBridgeScenariosAndSummarizesResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiBridge bridge = WayangA2uiBridge.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiBridgeHarness harness = WayangA2uiBridgeHarness.of(bridge);
        WayangA2uiBridgeScenario scenario = WayangA2uiBridgeScenario.of(
                        "inspect-and-catalog",
                        WayangA2uiBridgeRequest.of(WayangA2uiTransportRequest.jsonLine(
                                codec.line(action(WayangA2uiActions.RUN_INSPECT)))),
                        WayangA2uiBridgeRequest.envelope(WayangA2uiTransportRequest.surfaceCatalog().toMap()))
                .withAttributes(Map.of("tenant", "demo"));

        WayangA2uiBridgeScenarioResult result = harness.run(scenario);

        assertThat(result.scenarioId()).isEqualTo("inspect-and-catalog");
        assertThat(result.attributes()).containsEntry("tenant", "demo");
        assertThat(result.exchangeCount()).isEqualTo(2);
        assertThat(result.handledCount()).isEqualTo(1L);
        assertThat(result.rejectedCount()).isZero();
        assertThat(result.hasTransportErrors()).isFalse();
        assertThat(result.outcomes()).containsExactly(
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.SUCCESS);
        assertThat(result.exchanges())
                .extracting(WayangA2uiBridgeScenarioExchange::index)
                .containsExactly(1, 2);
        assertThat(result.exchanges().get(0).requestEnvelope())
                .containsEntry(WayangA2uiTransportFields.KIND, WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThat(result.responseEnvelopes().get(0))
                .containsEntry(WayangA2uiTransportFields.HANDLED_COUNT, 1L);
        assertThat(result.responseEnvelopes().get(1))
                .containsEntry(WayangA2uiTransportFields.BODY_ENCODING, WayangA2uiTransportContent.ENCODING_JSON);
        assertThat(result.exchanges().get(1).response().transportResponse().body())
                .contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void buildsAndRunsJsonEnvelopeScenarios() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiBridgeHarness harness = new WayangA2uiBridgeHarness(
                new WayangA2uiTransportBridge(new WayangA2uiTransportAdapter(
                        sdk,
                        WayangA2uiSessionConfig.readOnly())));
        WayangA2uiBridgeScenario scenario = WayangA2uiBridgeScenario.envelopeJson(
                "catalog-json",
                List.of(WayangA2uiTransportRequest.surfaceCatalog().toJson()));

        WayangA2uiBridgeScenarioResult result = harness.run(scenario);

        assertThat(scenario.size()).isEqualTo(1);
        assertThat(scenario.empty()).isFalse();
        assertThat(result.exchangeCount()).isEqualTo(1);
        assertThat(result.handledCount()).isZero();
        assertThat(result.rejectedCount()).isZero();
        assertThat(result.hasTransportErrors()).isFalse();
        assertThat(result.exchanges().get(0).responseEnvelope())
                .containsEntry(WayangA2uiTransportFields.OUTCOME, WayangA2uiTransportOutcome.SUCCESS.name());
        assertThat(result.exchanges().get(0).response().transportEnvelopeJson())
                .contains(WayangA2uiSurfaceRegistry.RUN_STATUS);
        assertThat(sdk.inspected).isZero();
    }

    private static A2uiUserAction action(String name) {
        return new A2uiUserAction(
                name,
                "main",
                "button",
                Instant.parse("2026-05-31T00:00:00Z"),
                Map.of("runId", "run-1"));
    }
}
