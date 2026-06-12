package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiClientMessage;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiSessionStateTest {

    @Test
    void appliesRememberedEventCursorWhenActionOmitsCursor() {
        WayangA2uiSessionState state = new WayangA2uiSessionState();
        state.rememberEventCursor("run-1", 7);

        A2uiClientMessage enriched = state.apply(action(Map.of("runId", "run-1")));

        assertThat(enriched).isInstanceOf(A2uiUserAction.class);
        assertThat(((A2uiUserAction) enriched).context())
                .containsEntry("runId", "run-1")
                .containsEntry("afterSequence", 7L);
    }

    @Test
    void keepsExplicitEventCursor() {
        WayangA2uiSessionState state = new WayangA2uiSessionState();
        state.rememberEventCursor("run-1", 7);

        A2uiClientMessage enriched = state.apply(action(Map.of("runId", "run-1", "afterSequence", 1L)));

        assertThat(((A2uiUserAction) enriched).context())
                .containsEntry("afterSequence", 1L);
    }

    @Test
    void observesHandledRunEventResultsOnly() {
        WayangA2uiSessionState state = new WayangA2uiSessionState();

        state.observe(WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_EVENTS,
                "run-1",
                "Loaded events.",
                List.of(),
                Map.of("nextAfterSequence", 5L)));
        state.observe(WayangA2uiActionResult.rejected(
                WayangA2uiActions.RUN_EVENTS,
                "run-2",
                "Rejected."));

        assertThat(state.eventCursor("run-1").isPresent()).isTrue();
        assertThat(state.eventCursor("run-1").getAsLong()).isEqualTo(5L);
        assertThat(state.eventCursor("run-2").isPresent()).isFalse();
    }

    private static A2uiUserAction action(Map<String, Object> context) {
        return new A2uiUserAction(
                WayangA2uiActions.RUN_EVENTS,
                "main",
                "events",
                Instant.parse("2026-05-31T00:00:00Z"),
                context);
    }
}
