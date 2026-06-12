package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunActionControlsTest {

    @Test
    void addsStatusActionControlsWithStatusLabels() {
        List<A2uiComponent> components = new ArrayList<>();
        List<String> children = new ArrayList<>();
        AgentRunStatus status = runningStatus();

        RunActionControls.addStatusActions(
                components,
                children,
                "surface-1",
                status,
                "surface-1",
                WayangA2uiSurfaceOptions.runLifecycle());

        assertThat(children)
                .containsExactly("surface-1-inspect", "surface-1-events", "surface-1-wait", "surface-1-cancel");
        assertThat(components.stream().filter(RunActionControlsTest::isText).map(
                RunActionControlsTest::text))
                .containsExactly("Inspect run", "View events", "Wait for completion", "Cancel run");
        assertThat(components.stream().filter(RunActionControlsTest::isButton).map(
                RunActionControlsTest::actionName))
                .containsExactly(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_EVENTS,
                        WayangA2uiActions.RUN_WAIT,
                        WayangA2uiActions.RUN_CANCEL);
    }

    @Test
    void hidesLifecycleControlsForTerminalHistoryRows() {
        List<A2uiComponent> components = new ArrayList<>();
        List<String> children = new ArrayList<>();
        AgentRunStatus status = completedStatus();

        RunActionControls.addHistoryRowActions(
                components,
                children,
                "history-row",
                status,
                "wayang-run-history",
                WayangA2uiSurfaceOptions.runLifecycle());

        assertThat(children).containsExactly("history-row-inspect", "history-row-events");
        assertThat(components.stream().filter(RunActionControlsTest::isText).map(
                RunActionControlsTest::text))
                .containsExactly("Inspect", "Events");
        assertThat(components.stream().filter(RunActionControlsTest::isButton).map(
                RunActionControlsTest::actionName))
                .containsExactly(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_EVENTS);
    }

    private static boolean isText(A2uiComponent component) {
        return component.component().containsKey("Text");
    }

    private static boolean isButton(A2uiComponent component) {
        return component.component().containsKey("Button");
    }

    @SuppressWarnings("unchecked")
    private static String text(A2uiComponent component) {
        Map<String, Object> text = (Map<String, Object>) component.component().get("Text");
        Map<String, Object> value = (Map<String, Object>) text.get("text");
        return (String) value.get("literalString");
    }

    @SuppressWarnings("unchecked")
    private static String actionName(A2uiComponent component) {
        Map<String, Object> button = (Map<String, Object>) component.component().get("Button");
        Map<String, Object> action = (Map<String, Object>) button.get("action");
        return (String) action.get("name");
    }

    private static AgentRunStatus runningStatus() {
        return new AgentRunStatus(
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"),
                true,
                "Run is running.",
                Map.of());
    }

    private static AgentRunStatus completedStatus() {
        return new AgentRunStatus(
                AgentRunHandle.completed("run-2", "react"),
                true,
                "Run completed.",
                Map.of());
    }
}
