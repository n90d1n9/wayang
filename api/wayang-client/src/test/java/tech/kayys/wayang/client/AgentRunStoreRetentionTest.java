package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.event.AgentRunEvent;
import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStatus;
import tech.kayys.wayang.agent.store.AgentRunStoreRetention;
import tech.kayys.wayang.agent.store.AgentRunStoreRetentionAssessment;
import tech.kayys.wayang.agent.store.AgentRunStoreRetentionPolicy;
import tech.kayys.wayang.agent.store.AgentRunStoreSnapshot;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunStoreRetentionTest {

    @Test
    void assessesRunAndEventPruningForSnapshot() {
        AgentRunStoreSnapshot snapshot = new AgentRunStoreSnapshot(
                List.of(
                        status("run-retention-assess-1"),
                        status("run-retention-assess-2"),
                        status("run-retention-assess-3")),
                List.of(
                        event("run-retention-assess-1", 1),
                        event("run-retention-assess-2", 1),
                        event("run-retention-assess-2", 2),
                        event("run-retention-assess-3", 1),
                        event("run-retention-assess-3", 2),
                        event("run-retention-assess-3", 3)));

        AgentRunStoreRetentionAssessment assessment = AgentRunStoreRetention.assess(
                snapshot,
                AgentRunStoreRetentionPolicy.of(2, 2));
        AgentRunStoreSnapshot retained = AgentRunStoreRetention.apply(
                snapshot,
                AgentRunStoreRetentionPolicy.of(2, 2));

        assertThat(assessment.pruned()).isTrue();
        assertThat(assessment.totalRuns()).isEqualTo(3);
        assertThat(assessment.retainedRuns()).isEqualTo(2);
        assertThat(assessment.prunedRuns()).isEqualTo(1);
        assertThat(assessment.totalStatuses()).isEqualTo(3);
        assertThat(assessment.retainedStatuses()).isEqualTo(2);
        assertThat(assessment.prunedStatuses()).isEqualTo(1);
        assertThat(assessment.totalEvents()).isEqualTo(6);
        assertThat(assessment.retainedEvents()).isEqualTo(4);
        assertThat(assessment.prunedEvents()).isEqualTo(2);
        assertThat(assessment.retainedRunIds())
                .containsExactly("run-retention-assess-2", "run-retention-assess-3");
        assertThat(assessment.prunedRunIds()).containsExactly("run-retention-assess-1");
        assertThat(assessment.prunedEventsByRun())
                .containsEntry("run-retention-assess-1", 1)
                .containsEntry("run-retention-assess-3", 1);
        assertThat(retained.statuses())
                .extracting(status -> status.handle().runId())
                .containsExactly("run-retention-assess-2", "run-retention-assess-3");
        assertThat(retained.events())
                .extracting(AgentRunEvent::sequence)
                .containsExactly(1L, 2L, 2L, 3L);
    }

    @Test
    void reportsNoPruningForUnlimitedPolicy() {
        AgentRunStoreSnapshot snapshot = new AgentRunStoreSnapshot(
                List.of(status("run-retention-unlimited-1")),
                List.of(
                        event("run-retention-unlimited-1", 1),
                        event("run-retention-unlimited-1", 2)));

        AgentRunStoreRetentionAssessment assessment = AgentRunStoreRetention.assess(
                snapshot,
                AgentRunStoreRetentionPolicy.unlimited());
        Map<String, Object> values = assessment.toMap();

        assertThat(assessment.pruned()).isFalse();
        assertThat(assessment.totalRuns()).isEqualTo(1);
        assertThat(assessment.retainedRuns()).isEqualTo(1);
        assertThat(assessment.prunedRuns()).isZero();
        assertThat(assessment.totalEvents()).isEqualTo(2);
        assertThat(assessment.retainedEvents()).isEqualTo(2);
        assertThat(assessment.prunedEvents()).isZero();
        assertThat(assessment.prunedRunIds()).isEmpty();
        assertThat(assessment.prunedEventsByRun()).isEmpty();
        assertThat(values)
                .containsEntry("pruned", false)
                .containsEntry("totalRuns", 1)
                .containsEntry("retainedEvents", 2)
                .containsEntry("prunedEvents", 0);
        assertThat(values.get("policy").toString())
                .contains("mode=unlimited")
                .contains("unlimited=true");
    }

    private static AgentRunStatus status(String runId) {
        return new AgentRunStatus(
                AgentRunHandle.completed(runId, "strategy-a"),
                true,
                "done",
                Map.of());
    }

    private static AgentRunEvent event(String runId, long sequence) {
        return new AgentRunEvent(
                runId,
                sequence,
                "run.audit",
                AgentRunState.RUNNING,
                "event-" + sequence,
                Map.of());
    }
}
