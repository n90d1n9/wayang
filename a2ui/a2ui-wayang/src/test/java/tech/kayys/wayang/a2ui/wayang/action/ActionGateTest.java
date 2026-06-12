package tech.kayys.wayang.a2ui.wayang.action;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionPolicy;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ActionGateTest {

    @Test
    void defaultPolicyNamesInspectOnlyFallback() {
        WayangA2uiActionPolicy policy = WayangA2uiActionPolicy.defaultPolicy();

        assertThat(policy).isEqualTo(WayangA2uiActionPolicy.inspectOnly());
        assertThat(policy.allowedActions()).containsExactly(WayangA2uiActions.RUN_INSPECT);
    }

    @Test
    void acceptsAllowedActionsAndReturnsTrimmedRunId() {
        ActionGate.Decision decision = ActionGate.evaluate(
                WayangA2uiActionPolicy.readOnly(),
                action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", " run-1 ")));

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.runId()).isEqualTo("run-1");
        assertThat(decision.rejection()).isNull();
    }

    @Test
    void acceptsHistoryWithoutRunId() {
        ActionGate.Decision decision = ActionGate.evaluate(
                WayangA2uiActionPolicy.readOnly(),
                action(WayangA2uiActions.RUN_HISTORY, Map.of()));

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.runId()).isEmpty();
    }

    @Test
    void rejectsDisallowedActionBeforeRunChecks() {
        ActionGate.Decision decision = ActionGate.evaluate(
                WayangA2uiActionPolicy.inspectOnly(),
                action(WayangA2uiActions.RUN_CANCEL, Map.of("runId", "run-1")));

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.rejection().actionName()).isEqualTo(WayangA2uiActions.RUN_CANCEL);
        assertThat(decision.rejection().runId()).isEqualTo("run-1");
        assertThat(decision.rejection().message()).isEqualTo("A2UI action is not allowed.");
    }

    @Test
    void rejectsRunActionsWithoutRunId() {
        ActionGate.Decision decision = ActionGate.evaluate(
                WayangA2uiActionPolicy.readOnly(),
                action(WayangA2uiActions.RUN_EVENTS, Map.of()));

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.rejection().message()).isEqualTo("A2UI run action requires context.runId.");
    }

    @Test
    void rejectsRunIdsAndContextOutsidePolicy() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(
                Set.of(WayangA2uiActions.RUN_INSPECT),
                Set.of("run-1"),
                Map.of("tenantId", "tenant-a"));

        ActionGate.Decision wrongRun = ActionGate.evaluate(
                policy,
                action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-2", "tenantId", "tenant-a")));
        ActionGate.Decision wrongContext = ActionGate.evaluate(
                policy,
                action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1", "tenantId", "tenant-b")));

        assertThat(wrongRun.accepted()).isFalse();
        assertThat(wrongRun.rejection().message()).isEqualTo("A2UI action is not allowed for this run.");
        assertThat(wrongContext.accepted()).isFalse();
        assertThat(wrongContext.rejection().message()).isEqualTo("A2UI action context does not match policy.");
    }

    @Test
    void allowsPolicyAllowedUnknownActionsForRouterFallback() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(Set.of("custom.action"), Set.of(), Map.of());

        ActionGate.Decision decision = ActionGate.evaluate(
                policy,
                action("custom.action", Map.of()));

        assertThat(decision.accepted()).isTrue();
    }

    private static A2uiUserAction action(String name, Map<String, Object> context) {
        return new A2uiUserAction(
                name,
                "main",
                "button",
                Instant.parse("2026-05-31T00:00:00Z"),
                context);
    }
}
