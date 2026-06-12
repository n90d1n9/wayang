package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunLifecycleServiceTest {

    @Test
    void recordsRunResultsAndServesLifecycleViews() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunLifecycleService lifecycle = AgentRunLifecycleService.create(store);
        AgentRunResult result = new AgentRunResult(
                "run-lifecycle-1",
                "done",
                true,
                "strategy-a",
                List.of("prepare", "run"),
                Map.of("tenant", "tenant-a"));

        AgentRunStatus saved = lifecycle.record(result);
        AgentRunEvents events = lifecycle.events(" run-lifecycle-1 ");
        AgentRunInspection inspection = lifecycle.inspect(
                "run-lifecycle-1",
                AgentRunEventsQuery.of("completed", "run.completed", 10));
        AgentRunWaitResult wait = lifecycle.waitForRun("run-lifecycle-1", new AgentRunWaitOptions(0, 1));

        assertThat(saved.handle()).isEqualTo(result.handle());
        assertThat(saved.metadata())
                .containsEntry("tenant", "tenant-a")
                .containsEntry("successful", true)
                .containsEntry("stepCount", 2);
        assertThat(lifecycle.status("run-lifecycle-1")).isEqualTo(saved);
        assertThat(store.status("run-lifecycle-1")).isEqualTo(saved);
        assertThat(lifecycle.history().runs()).containsExactly(saved);
        assertThat(lifecycle.history().outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(events.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(events.events())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.sequence()).isEqualTo(1);
                    assertThat(event.type()).isEqualTo("run.completed");
                    assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                });
        assertThat(events.stateSummaries())
                .containsExactly(new AgentRunEventFacetSummary("completed", 1));
        assertThat(events.typeSummaries())
                .containsExactly(new AgentRunEventFacetSummary("run.completed", 1));
        assertThat(inspection.known()).isTrue();
        assertThat(inspection.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(inspection.events().query().filtered()).isTrue();
        assertThat(inspection.message()).isEqualTo("Inspected Wayang run lifecycle.");
        assertThat(wait.terminal()).isTrue();
        assertThat(wait.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(wait.timedOut()).isFalse();
        assertThat(wait.attempts()).isEqualTo(1);
    }

    @Test
    void followsRunEventsUntilTerminalEventThroughSharedBoundary() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunLifecycleService lifecycle = AgentRunLifecycleService.create(store);
        lifecycle.record(new AgentRunResult(
                "run-follow-1",
                "done",
                true,
                "strategy-a",
                List.of("prepare", "run"),
                Map.of("tenant", "tenant-a")));
        List<AgentRunEvents> windows = new ArrayList<>();

        AgentRunEventsFollowResult follow = lifecycle.followEvents(
                "run-follow-1",
                AgentRunEventsFollowOptions.of(AgentRunEventsQuery.all(), 3, 1L),
                windows::add);

        assertThat(follow.successful()).isTrue();
        assertThat(follow.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(follow.terminal()).isTrue();
        assertThat(follow.terminalState()).isEqualTo("completed");
        assertThat(follow.terminalEventType()).isEqualTo("run.completed");
        assertThat(follow.terminalSequence()).isEqualTo(1);
        assertThat(follow.maxPollsReached()).isFalse();
        assertThat(follow.polls()).isEqualTo(1);
        assertThat(follow.nextAfterSequence()).isEqualTo(1);
        assertThat(follow.nextQuery().afterSequence()).isEqualTo(1);
        assertThat(follow.metadata())
                .containsEntry("maxPolls", 3)
                .containsEntry("pollMillis", 1L);
        assertThat(windows)
                .singleElement()
                .satisfies(events -> assertThat(events.events())
                        .singleElement()
                        .satisfies(event -> {
                            assertThat(event.type()).isEqualTo("run.completed");
                            assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                        }));
    }

    @Test
    void reportsMaxPollsOutcomeWhenFollowDoesNotReachTerminalEvent() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunLifecycleService lifecycle = AgentRunLifecycleService.create(store);
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-follow-running-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "Run is still running.",
                Map.of("tenant", "tenant-a")));
        List<AgentRunEvents> windows = new ArrayList<>();

        AgentRunEventsFollowResult follow = lifecycle.followEvents(
                "run-follow-running-1",
                AgentRunEventsFollowOptions.of(AgentRunEventsQuery.all(), 1, 1L),
                windows::add);

        assertThat(follow.successful()).isFalse();
        assertThat(follow.outcome()).isEqualTo(AgentRunOutcomes.MAX_POLLS);
        assertThat(follow.terminal()).isFalse();
        assertThat(follow.terminalState()).isEmpty();
        assertThat(follow.terminalEventType()).isEmpty();
        assertThat(follow.terminalSequence()).isZero();
        assertThat(follow.maxPollsReached()).isTrue();
        assertThat(follow.polls()).isEqualTo(1);
        assertThat(follow.nextAfterSequence()).isEqualTo(1);
        assertThat(follow.message()).isEqualTo("Run events did not reach a terminal state before max polls.");
        assertThat(windows)
                .singleElement()
                .satisfies(events -> {
                    assertThat(events.outcome()).isEqualTo(AgentRunOutcomes.PENDING);
                    assertThat(events.events())
                            .singleElement()
                            .satisfies(event -> assertThat(event.state()).isEqualTo(AgentRunState.RUNNING));
                });
    }

    @Test
    void cancelsAndForgetsMutableRunsThroughTheSharedLifecycleBoundary() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunLifecycleService lifecycle = AgentRunLifecycleService.create(store);
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-mutable-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of("tenant", "tenant-a")));

        AgentRunCancelResult cancel = lifecycle.cancel(" run-mutable-1 ", "operator stop");

        assertThat(cancel.cancelled()).isTrue();
        assertThat(cancel.outcome()).isEqualTo(AgentRunOutcomes.CANCELLED);
        assertThat(cancel.handle().state()).isEqualTo(AgentRunState.CANCELLED);
        assertThat(cancel.metadata())
                .containsEntry("tenant", "tenant-a")
                .containsEntry("previousState", "RUNNING")
                .containsEntry("reason", "operator stop");
        assertThat(lifecycle.events("run-mutable-1").events())
                .extracting(AgentRunEvent::state)
                .containsExactly(AgentRunState.RUNNING, AgentRunState.CANCELLED);
        AgentRunForgetResult forget = lifecycle.forget("run-mutable-1");

        assertThat(forget.forgotten()).isTrue();
        assertThat(forget.outcome()).isEqualTo(AgentRunOutcomes.FORGOTTEN);
        assertThat(lifecycle.status("run-mutable-1").known()).isFalse();
        assertThat(lifecycle.events("run-mutable-1").empty()).isTrue();
    }
}
