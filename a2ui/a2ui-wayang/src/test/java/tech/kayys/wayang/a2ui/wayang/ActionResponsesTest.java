package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.action.ActionResponses;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitOptions;

import static org.assertj.core.api.Assertions.assertThat;

class ActionResponsesTest {

    private final A2uiJsonlTestSupport jsonl = new A2uiJsonlTestSupport();
    private final WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.fromPolicy(
            WayangA2uiActionPolicy.runLifecycle());

    @Test
    void buildsInspectionActionResultWithSurfaceAndMetadata() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();

        WayangA2uiActionResult result = ActionResponses.inspection(
                registry,
                "run-1",
                sdk.inspectRun("run-1"));

        assertThat(result.handled()).isTrue();
        assertThat(result.actionName()).isEqualTo(WayangA2uiActions.RUN_INSPECT);
        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.message()).isEqualTo("Inspected run.");
        assertThat(result.metadata()).containsEntry("known", true).containsEntry("empty", false);
        assertThat(jsonl.stream(result.responseMessages()))
                .contains("wayang-run-run-1")
                .contains("wayang.run.events");
    }

    @Test
    void buildsHistoryAndEventsActionResultsWithMetadata() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();

        WayangA2uiActionResult history = ActionResponses.history(
                registry,
                sdk.runHistory(AgentRunHistoryQuery.of("", 2, "tenant-a", "", "", 1)));
        WayangA2uiActionResult events = ActionResponses.events(
                registry,
                sdk.runEvents("run-1", AgentRunEventsQuery.of("completed", "run.completed", 1L, 10)));

        assertThat(history.actionName()).isEqualTo(WayangA2uiActions.RUN_HISTORY);
        assertThat(history.runId()).isEmpty();
        assertThat(history.metadata()).containsEntry("filtered", true).containsEntry("offset", 1);
        assertThat(jsonl.stream(history.responseMessages())).contains("wayang-run-history");

        assertThat(events.actionName()).isEqualTo(WayangA2uiActions.RUN_EVENTS);
        assertThat(events.runId()).isEqualTo("run-1");
        assertThat(events.metadata()).containsEntry("filtered", true).containsEntry("afterSequence", 1L);
        assertThat(jsonl.stream(events.responseMessages())).contains("wayang-run-events-run-1");
    }

    @Test
    void buildsWaitAndCancelActionResultsFromStatusSurfaces() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();

        WayangA2uiActionResult wait = ActionResponses.waitResult(
                registry,
                sdk.waitForRun("run-1", AgentRunWaitOptions.of(0, 1)));
        AgentRunCancelResult cancelled = sdk.cancelRun("run-1", "No longer needed");
        WayangA2uiActionResult cancel = ActionResponses.cancelResult(registry, cancelled);

        assertThat(wait.actionName()).isEqualTo(WayangA2uiActions.RUN_WAIT);
        assertThat(wait.metadata()).containsEntry("terminal", true).containsEntry("attempts", 1);
        assertThat(jsonl.stream(wait.responseMessages())).contains("State: COMPLETED");

        assertThat(cancel.actionName()).isEqualTo(WayangA2uiActions.RUN_CANCEL);
        assertThat(cancel.runId()).isEqualTo("run-1");
        assertThat(cancel.metadata()).containsEntry("cancelled", true);
        assertThat(jsonl.stream(cancel.responseMessages()))
                .contains("State: CANCELLED")
                .contains("Run was cancelled.");
    }
}
