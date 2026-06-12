package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiDataPart;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiProtocol;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiSessionTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    @SuppressWarnings("unchecked")
    void handlesJsonlActionsAndReturnsEncodedResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSession session = new WayangA2uiSession(sdk, WayangA2uiActionPolicy.runLifecycle());

        WayangA2uiSessionResult result = session.handleJsonl(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1")))
                        + "\n\n"
                        + codec.line(action(WayangA2uiActions.RUN_WAIT, Map.of("runId", "run-1")))
                        + "\n");

        assertThat(result.actionResults()).hasSize(2);
        assertThat(result.handledCount()).isEqualTo(2);
        assertThat(result.rejectedCount()).isZero();
        assertThat(result.responseMessages()).hasSize(6);
        assertThat(result.responseJsonl())
                .contains("RUNNING")
                .contains("COMPLETED");
        assertThat(result.responseDataParts())
                .hasSameSizeAs(result.responseMessages())
                .allSatisfy(dataPart -> {
                    Map<String, Object> metadata = (Map<String, Object>) dataPart.get("metadata");
                    assertThat(metadata).containsEntry("mimeType", A2uiProtocol.MIME_TYPE);
                });
        assertThat(sdk.inspected).isEqualTo(1);
        assertThat(sdk.waited).isEqualTo(1);
    }

    @Test
    void handlesA2aDataPartPayloadMaps() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSession session = new WayangA2uiSession(sdk);
        A2uiUserAction action = action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1"));

        WayangA2uiSessionResult result = session.handleDataPart(A2uiDataPart.of(action).toPayload());

        assertThat(result.handledCount()).isEqualTo(1);
        assertThat(result.responseJsonl()).contains("wayang.run.inspect");
        assertThat(result.responseDataParts()).hasSize(3);
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void ignoresNullMessagesInBatches() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSession session = new WayangA2uiSession(sdk);
        List<A2uiUserAction> messages = new ArrayList<>();
        messages.add(null);
        messages.add(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1")));

        WayangA2uiSessionResult result = session.handle(messages);

        assertThat(result.actionResults()).hasSize(1);
        assertThat(result.handledCount()).isEqualTo(1);
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void remembersRunEventCursorAcrossRequests() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSession session = new WayangA2uiSession(sdk, WayangA2uiActionPolicy.readOnly());

        session.handle(action(WayangA2uiActions.RUN_EVENTS, Map.of("runId", "run-1")));

        assertThat(sdk.lastEventsQuery.afterSequence()).isZero();
        assertThat(session.state().eventCursor("run-1").isPresent()).isTrue();
        assertThat(session.state().eventCursor("run-1").getAsLong()).isEqualTo(2L);

        session.handle(action(WayangA2uiActions.RUN_EVENTS, Map.of("runId", "run-1")));

        assertThat(sdk.lastEventsQuery.afterSequence()).isEqualTo(2L);

        session.handle(action(WayangA2uiActions.RUN_EVENTS, Map.of("runId", "run-1", "afterSequence", 0L)));

        assertThat(sdk.lastEventsQuery.afterSequence()).isZero();
    }

    @Test
    void materializesRejectedActionsAsFeedbackSurfaces() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSession session = new WayangA2uiSession(sdk);
        A2uiUserAction action = action(WayangA2uiActions.RUN_CANCEL, Map.of("runId", "run-1"));

        WayangA2uiSessionResult result = session.handle(action);

        assertThat(result.handledCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.responseMessages()).hasSize(3);
        assertThat(result.responseJsonl())
                .contains("wayang-action-result-1-wayang-run-cancel")
                .contains("Action rejected")
                .contains("A2UI action is not allowed.");
        assertThat(result.responseDataParts()).hasSize(3);
        assertThat(sdk.cancelled).isZero();
    }

    @Test
    void materializesRejectedActionsThroughCustomFeedbackSurface() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.readOnly()
                .toBuilder()
                .replace(
                        WayangA2uiSurfaceRegistry.ACTION_RESULT,
                        WayangA2uiActionFeedback.class,
                        feedback -> WayangA2uiResultSurfaces.actionResult(
                                WayangA2uiActionResult.rejected(
                                        "custom.feedback",
                                        feedback.result().runId(),
                                        "Custom feedback " + feedback.sequence() + ": "
                                                + feedback.result().message()),
                                feedback.sequence() + 20))
                .build();
        WayangA2uiSession session = new WayangA2uiSession(
                sdk,
                WayangA2uiActionPolicy.inspectOnly(),
                registry);

        WayangA2uiSessionResult result = session.handle(List.of(
                action(WayangA2uiActions.RUN_CANCEL, Map.of("runId", "run-1")),
                action(WayangA2uiActions.RUN_WAIT, Map.of("runId", "run-1"))));

        assertThat(result.handledCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(2);
        assertThat(result.responseJsonl())
                .contains("wayang-action-result-21-custom-feedback")
                .contains("Custom feedback 1: A2UI action is not allowed.")
                .contains("wayang-action-result-22-custom-feedback")
                .contains("Custom feedback 2: A2UI action is not allowed.");
        assertThat(sdk.cancelled).isZero();
        assertThat(sdk.waited).isZero();
    }

    @Test
    void disabledConfigRejectsBeforeCallingSdk() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSession session = new WayangA2uiSession(sdk, WayangA2uiSessionConfig.disabled());

        WayangA2uiSessionResult result = session.handle(
                action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1")));

        assertThat(result.handledCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.actionResults().getFirst().message()).contains("disabled");
        assertThat(result.responseJsonl())
                .contains("Action rejected")
                .contains("A2UI session is disabled.");
        assertThat(sdk.inspected).isZero();
    }

    private static A2uiUserAction action(String name, Map<String, Object> context) {
        return new A2uiUserAction(name, "main", "button", Instant.parse("2026-05-31T00:00:00Z"), context);
    }
}
