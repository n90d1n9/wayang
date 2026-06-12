package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiActionRouterTest {

    private final A2uiJsonlTestSupport jsonl = new A2uiJsonlTestSupport();

    @Test
    void routesInspectActionToSdkAndResponseSurface() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk);

        WayangA2uiActionResult result = router.route(action(
                WayangA2uiActions.RUN_INSPECT,
                Map.of("runId", "run-1")));

        assertThat(result.handled()).isTrue();
        assertThat(result.actionName()).isEqualTo(WayangA2uiActions.RUN_INSPECT);
        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.responseMessages()).hasSize(3);
        assertThat(sdk.inspected).isEqualTo(1);
        assertThat(jsonl.stream(result.responseMessages()))
                .contains("wayang.run.inspect")
                .doesNotContain("wayang.run.events")
                .contains("RUNNING");
    }

    @Test
    void rejectsDisallowedActionsBeforeCallingSdk() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk);

        WayangA2uiActionResult result = router.route(action(
                WayangA2uiActions.RUN_CANCEL,
                Map.of("runId", "run-1")));

        assertThat(result.handled()).isFalse();
        assertThat(result.message()).contains("not allowed");
        assertThat(sdk.cancelled).isZero();
    }

    @Test
    void routesLifecycleActionsWhenPolicyAllowsThem() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(
                Set.of(WayangA2uiActions.RUN_CANCEL, WayangA2uiActions.RUN_WAIT),
                Set.of("run-1"),
                Map.of("tenantId", "tenant-a"));
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk, policy);

        WayangA2uiActionResult cancel = router.route(action(
                WayangA2uiActions.RUN_CANCEL,
                Map.of("runId", "run-1", "tenantId", "tenant-a", "reason", "No longer needed")));
        WayangA2uiActionResult wait = router.route(action(
                WayangA2uiActions.RUN_WAIT,
                Map.of("runId", "run-1", "tenantId", "tenant-a", "timeoutSeconds", 0, "pollMillis", 1)));

        assertThat(cancel.handled()).isTrue();
        assertThat(cancel.metadata()).containsEntry("cancelled", true);
        assertThat(wait.handled()).isTrue();
        assertThat(wait.metadata()).containsEntry("terminal", true);
        assertThat(sdk.cancelled).isEqualTo(1);
        assertThat(sdk.waited).isEqualTo(1);
        assertThat(sdk.lastCancelReason).isEqualTo("No longer needed");
    }

    @Test
    void routesHistoryActionWithoutRunId() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk, WayangA2uiActionPolicy.readOnly());

        WayangA2uiActionResult result = router.route(action(
                WayangA2uiActions.RUN_HISTORY,
                Map.of("tenantId", "tenant-a", "surfaceId", "coding-agent", "limit", 2, "offset", 1)));

        assertThat(result.handled()).isTrue();
        assertThat(result.actionName()).isEqualTo(WayangA2uiActions.RUN_HISTORY);
        assertThat(result.runId()).isEmpty();
        assertThat(result.responseMessages()).hasSize(3);
        assertThat(result.metadata())
                .containsEntry("totalRuns", 3)
                .containsEntry("returnedRuns", 2)
                .containsEntry("filtered", true);
        assertThat(sdk.historyQueried).isEqualTo(1);
        assertThat(sdk.lastHistoryQuery.tenantId()).isEqualTo("tenant-a");
        assertThat(sdk.lastHistoryQuery.surfaceId()).isEqualTo("coding-agent");
        assertThat(sdk.lastHistoryQuery.limit()).isEqualTo(2);
        assertThat(sdk.lastHistoryQuery.offset()).isEqualTo(1);
        assertThat(jsonl.stream(result.responseMessages()))
                .contains("wayang-run-history")
                .contains("wayang.run.inspect");
    }

    @Test
    void routesEventsActionToSdkAndResponseSurface() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk, WayangA2uiActionPolicy.readOnly());

        WayangA2uiActionResult result = router.route(action(
                WayangA2uiActions.RUN_EVENTS,
                Map.of("runId", "run-1", "state", "completed", "type", "run.completed",
                        "afterSequence", 1L, "limit", 10)));

        assertThat(result.handled()).isTrue();
        assertThat(result.actionName()).isEqualTo(WayangA2uiActions.RUN_EVENTS);
        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.responseMessages()).hasSize(3);
        assertThat(result.metadata())
                .containsEntry("totalEvents", 2)
                .containsEntry("returnedEvents", 2)
                .containsEntry("filtered", true);
        assertThat(sdk.eventsQueried).isEqualTo(1);
        assertThat(sdk.lastEventsQuery.state().name()).isEqualTo("COMPLETED");
        assertThat(sdk.lastEventsQuery.type()).isEqualTo("run.completed");
        assertThat(sdk.lastEventsQuery.afterSequence()).isEqualTo(1L);
        assertThat(sdk.lastEventsQuery.limit()).isEqualTo(10);
        assertThat(jsonl.stream(result.responseMessages()))
                .contains("wayang-run-events-run-1")
                .contains("wayang.run.events")
                .contains("run.completed");
    }

    @Test
    void propagatesRequiredPolicyContextToResponseActions() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(
                Set.of(
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_EVENTS),
                Set.of(),
                Map.of("tenantId", "tenant-a"));
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk, policy);

        WayangA2uiActionResult result = router.route(action(
                WayangA2uiActions.RUN_HISTORY,
                Map.of("tenantId", "tenant-a", "limit", 2)));

        assertThat(result.handled()).isTrue();
        assertThat(jsonl.stream(result.responseMessages()))
                .contains("tenantId")
                .contains("tenant-a")
                .contains("wayang.run.inspect")
                .contains("wayang.run.events");
    }

    @Test
    void rejectsRunOrContextOutsidePolicy() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(
                Set.of(WayangA2uiActions.RUN_INSPECT),
                Set.of("run-1"),
                Map.of("tenantId", "tenant-a"));
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk, policy);

        assertThat(router.route(action(
                WayangA2uiActions.RUN_INSPECT,
                Map.of("runId", "run-2", "tenantId", "tenant-a"))).handled())
                .isFalse();
        assertThat(router.route(action(
                WayangA2uiActions.RUN_INSPECT,
                Map.of("runId", "run-1", "tenantId", "tenant-b"))).handled())
                .isFalse();
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void rejectsUnsupportedActionsAfterPolicyGateAllowsThem() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(Set.of("custom.action"), Set.of(), Map.of());
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(sdk, policy);

        WayangA2uiActionResult result = router.route(action("custom.action", Map.of()));

        assertThat(result.handled()).isFalse();
        assertThat(result.message()).isEqualTo("Unsupported A2UI action.");
        assertThat(sdk.inspected).isZero();
        assertThat(sdk.historyQueried).isZero();
        assertThat(sdk.eventsQueried).isZero();
    }

    @Test
    void routesPolicyAllowedCustomActionsThroughCustomHandlers() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(Set.of("custom.action"), Set.of(), Map.of());
        WayangA2uiActionHandlers handlers = WayangA2uiActionHandlers.builder()
                .register("custom.action", (handledAction, runId) -> WayangA2uiActionResult.handled(
                        handledAction.name(),
                        runId,
                        "Custom action handled.",
                        List.of(),
                        Map.of("surface", handledAction.surfaceId())))
                .build();
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(
                policy,
                WayangA2uiSurfaceRegistry.readOnly(),
                handlers);

        WayangA2uiActionResult result = router.route(action("custom.action", Map.of("runId", "run-1")));

        assertThat(result.handled()).isTrue();
        assertThat(result.actionName()).isEqualTo("custom.action");
        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.metadata()).containsEntry("surface", "main");
    }

    private static A2uiUserAction action(String name, Map<String, Object> context) {
        return new A2uiUserAction(name, "main", "button", Instant.parse("2026-05-31T00:00:00Z"), context);
    }

}
