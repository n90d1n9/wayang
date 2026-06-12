package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaces;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunInspectionSurfaceTest {

    private final SurfaceJsonlTestSupport jsonl = new SurfaceJsonlTestSupport();

    @Test
    void rendersStatusAndEventsSurfacesAndFacadeDelegates() {
        AgentRunInspection inspection = runInspection();

        List<A2uiServerMessage> messages = RunInspectionSurface.render(
                inspection,
                WayangA2uiSurfaceOptions.readOnly());

        assertThat(messages).isEqualTo(WayangA2uiSurfaces.runInspection(inspection, WayangA2uiSurfaceOptions.readOnly()));
        assertThat(messages).hasSize(6);
        assertThat(jsonl.stream(messages))
                .contains("wayang-run-run-1")
                .contains("wayang-run-events-run-1")
                .contains("wayang.run.events");
    }

    @Test
    void rendersOnlyStatusWhenEventsAreEmptyOrHidden() {
        AgentRunInspection emptyEvents = runInspection(emptyEvents());
        AgentRunInspection hiddenEvents = runInspection();

        String emptyJsonl = jsonl.stream(RunInspectionSurface.render(
                emptyEvents,
                WayangA2uiSurfaceOptions.readOnly()));
        String hiddenJsonl = jsonl.stream(RunInspectionSurface.render(
                hiddenEvents,
                WayangA2uiSurfaceOptions.inspectOnly()));

        assertThat(RunInspectionSurface.render(emptyEvents, WayangA2uiSurfaceOptions.readOnly()))
                .hasSize(3);
        assertThat(RunInspectionSurface.render(hiddenEvents, WayangA2uiSurfaceOptions.inspectOnly()))
                .hasSize(3);
        assertThat(emptyJsonl)
                .contains("wayang-run-run-1")
                .doesNotContain("wayang-run-events-run-1");
        assertThat(hiddenJsonl)
                .contains("wayang-run-run-1")
                .doesNotContain("wayang-run-events-run-1")
                .doesNotContain("wayang.run.events");
    }

    private static AgentRunInspection runInspection() {
        return runInspection(singleRunningEventPage());
    }

    private static AgentRunInspection runInspection(AgentRunEvents events) {
        return new AgentRunInspection("run-1", runningStatus(), events, "Inspected run.");
    }

    private static AgentRunStatus runningStatus() {
        return new AgentRunStatus(
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"),
                true,
                "Run is running.",
                Map.of());
    }

    private static AgentRunEvents singleRunningEventPage() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.all(),
                List.of(runningEvent(1)),
                1,
                "Loaded run events.");
    }

    private static AgentRunEvents emptyEvents() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.all(),
                List.of(),
                0,
                "");
    }

    private static AgentRunEvent runningEvent(long sequence) {
        return new AgentRunEvent(
                "run-1",
                sequence,
                "run.running",
                AgentRunState.RUNNING,
                "Run is running.",
                Map.of());
    }
}
