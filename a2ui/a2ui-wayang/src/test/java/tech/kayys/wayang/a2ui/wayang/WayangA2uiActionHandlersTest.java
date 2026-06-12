package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiActionHandlersTest {

    @Test
    void exposesBuiltInActionNames() {
        WayangA2uiActionHandlers handlers = WayangA2uiActionHandlers.standard(
                new RecordingWayangGollekSdk(),
                WayangA2uiSurfaceRegistry.readOnly());

        assertThat(handlers.actionNames())
                .containsExactly(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS,
                        WayangA2uiActions.RUN_WAIT,
                        WayangA2uiActions.RUN_CANCEL);
    }

    @Test
    void routesSupportedActionsThroughSdkAndResponseAssembly() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionHandlers handlers = WayangA2uiActionHandlers.standard(
                sdk,
                WayangA2uiSurfaceRegistry.readOnly());

        WayangA2uiActionResult result = handlers.route(
                action(WayangA2uiActions.RUN_EVENTS, Map.of("type", "run.completed", "limit", 10)),
                "run-1");

        assertThat(result.handled()).isTrue();
        assertThat(result.actionName()).isEqualTo(WayangA2uiActions.RUN_EVENTS);
        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.metadata())
                .containsEntry("totalEvents", 2)
                .containsEntry("returnedEvents", 2)
                .containsEntry("filtered", true);
        assertThat(sdk.eventsQueried).isEqualTo(1);
        assertThat(sdk.lastEventsQuery.type()).isEqualTo("run.completed");
        assertThat(sdk.lastEventsQuery.limit()).isEqualTo(10);
    }

    @Test
    void canExtendBuiltInHandlersWithCustomActions() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionHandlers handlers = WayangA2uiActionHandlers.standard(
                        sdk,
                        WayangA2uiSurfaceRegistry.readOnly())
                .toBuilder()
                .register(" custom.action ", (action, runId) -> WayangA2uiActionResult.handled(
                        action.name(),
                        runId,
                        "Custom action handled.",
                        List.of(),
                        Map.of("custom", true)))
                .build();

        WayangA2uiActionResult result = handlers.route(action("custom.action", Map.of()), "run-custom");

        assertThat(handlers.supports(" custom.action ")).isTrue();
        assertThat(handlers.supports(" ")).isFalse();
        assertThat(handlers.supports(null)).isFalse();
        assertThat(result.handled()).isTrue();
        assertThat(result.actionName()).isEqualTo("custom.action");
        assertThat(result.runId()).isEqualTo("run-custom");
        assertThat(result.metadata()).containsEntry("custom", true);
        assertThat(sdk.inspected).isZero();
        assertThat(sdk.historyQueried).isZero();
    }

    @Test
    void builderCanReplaceAndRemoveHandlers() {
        WayangA2uiActionHandlers handlers = WayangA2uiActionHandlers.builder()
                .register("demo.action", (action, runId) -> WayangA2uiActionResult.rejected(
                        action.name(),
                        runId,
                        "Old handler."))
                .replace("demo.action", (action, runId) -> WayangA2uiActionResult.handled(
                        action.name(),
                        runId,
                        "Replacement handler.",
                        List.of(),
                        Map.of()))
                .register("removed.action", (action, runId) -> WayangA2uiActionResult.handled(
                        action.name(),
                        runId,
                        "Removed handler.",
                        List.of(),
                        Map.of()))
                .without("removed.action")
                .build();

        assertThat(handlers.actionNames()).containsExactly("demo.action");
        assertThat(handlers.route(action("demo.action", Map.of()), "").message())
                .isEqualTo("Replacement handler.");
        assertThat(handlers.route(action("removed.action", Map.of()), "").handled()).isFalse();
    }

    @Test
    void rejectsBlankHandlerNames() {
        WayangA2uiActionHandlers.Builder builder = WayangA2uiActionHandlers.builder();

        assertThatThrownBy(() -> builder.register(" ", (action, runId) -> WayangA2uiActionResult.rejected(
                action.name(),
                runId,
                "unused")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI action handler name must not be blank");
        assertThatThrownBy(() -> builder.without(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI action handler name must not be blank");
    }

    @Test
    void rejectsUnsupportedActionsWithoutCallingSdk() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionHandlers handlers = WayangA2uiActionHandlers.standard(
                sdk,
                WayangA2uiSurfaceRegistry.readOnly());

        WayangA2uiActionResult result = handlers.route(action("custom.action", Map.of()), "");

        assertThat(result.handled()).isFalse();
        assertThat(result.actionName()).isEqualTo("custom.action");
        assertThat(result.message()).isEqualTo("Unsupported A2UI action.");
        assertThat(sdk.inspected).isZero();
        assertThat(sdk.historyQueried).isZero();
        assertThat(sdk.eventsQueried).isZero();
        assertThat(sdk.waited).isZero();
        assertThat(sdk.cancelled).isZero();
    }

    private static A2uiUserAction action(String name, Map<String, Object> context) {
        return new A2uiUserAction(name, "main", "button", Instant.parse("2026-05-31T00:00:00Z"), context);
    }
}
