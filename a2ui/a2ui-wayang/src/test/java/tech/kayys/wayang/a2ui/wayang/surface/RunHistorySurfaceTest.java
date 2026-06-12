package tech.kayys.wayang.a2ui.wayang.surface;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaces;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunHistorySurfaceTest {

    private final SurfaceJsonlTestSupport jsonl = new SurfaceJsonlTestSupport();

    @Test
    void rendersRunHistorySurfaceAndFacadeDelegates() throws Exception {
        AgentRunHistory history = runHistory(4);

        List<A2uiServerMessage> messages = RunHistorySurface.render(
                history,
                WayangA2uiSurfaceOptions.readOnly());

        assertThat(messages).isEqualTo(WayangA2uiSurfaces.runHistory(history, WayangA2uiSurfaceOptions.readOnly()));
        assertThat(messages).hasSize(3);

        JsonNode data = jsonl.dataModelUpdate(messages);
        JsonNode update = jsonl.surfaceUpdate(messages);
        JsonNode begin = jsonl.beginRendering(messages);

        assertThat(data.at("/dataModelUpdate/surfaceId").asText()).isEqualTo("wayang-run-history");
        assertThat(data.at("/dataModelUpdate/contents/0/valueNumber").asInt()).isEqualTo(4);
        assertThat(data.at("/dataModelUpdate/contents/1/valueNumber").asInt()).isEqualTo(2);
        assertThat(data.at("/dataModelUpdate/contents/3/valueBoolean").asBoolean()).isTrue();
        assertThat(data.toString()).contains("run0").contains("RUNNING").contains("run1").contains("COMPLETED");

        assertThat(update.at("/surfaceUpdate/components/0/component/Column/children/explicitList").size())
                .isEqualTo(5);
        assertThat(update.at("/surfaceUpdate/components").size()).isEqualTo(16);
        assertThat(update.toString())
                .contains("Wayang runs")
                .contains("run-1")
                .contains("run-2")
                .contains("wayang.run.inspect")
                .contains("wayang.run.events");
        assertThat(begin.at("/beginRendering/root").asText()).isEqualTo("wayang-run-history-root");
    }

    @Test
    void hidesLifecycleActionsForTerminalHistoryRows() {
        String jsonlStream = jsonl.stream(RunHistorySurface.render(
                runHistory(2),
                WayangA2uiSurfaceOptions.runLifecycle()));

        assertThat(jsonlStream)
                .contains("wayang.run.wait")
                .contains("wayang.run.cancel")
                .doesNotContain("wayang-run-history-run-run-2-wait")
                .doesNotContain("wayang-run-history-run-run-2-cancel");
    }

    private static AgentRunHistory runHistory(int totalRuns) {
        return new AgentRunHistory(
                AgentRunHistoryQuery.of("", 2, "", "", "", 0),
                List.of(runningStatusWithSurface(), completedStatusWithSurface()),
                totalRuns,
                "Loaded run history.");
    }

    private static AgentRunStatus runningStatusWithSurface() {
        return new AgentRunStatus(
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"),
                true,
                "Run is running.",
                Map.of("surface", "coding-agent"));
    }

    private static AgentRunStatus completedStatusWithSurface() {
        return new AgentRunStatus(
                AgentRunHandle.completed("run-2", "react"),
                true,
                "Run completed.",
                Map.of("surface", "coding-agent"));
    }
}
