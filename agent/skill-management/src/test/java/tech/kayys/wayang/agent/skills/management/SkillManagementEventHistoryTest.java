package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventHistoryTest {

    @Test
    void queriesAndPrunesReadableEventHistory() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementEventHistory history = new SkillManagementEventHistory(events, events);
        events.record(event(SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        events.record(event(SkillManagementEventOperation.UPDATE_SKILL, "coder", true));
        events.record(event(SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventPage plannerEvents = history.query(SkillManagementEventQuery.forSkill("planner", 10));
        SkillManagementEventPruneResult pruneResult =
                history.prune(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(history.supportsPruning()).isTrue();
        assertThat(plannerEvents.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.DELETE_SKILL);
        assertThat(pruneResult.success()).isTrue();
        assertThat(pruneResult.prunedEvents()).isEqualTo(2);
        assertThat(history.latest().events()).extracting(SkillManagementEvent::skillId)
                .containsExactly("planner");
    }

    @Test
    void reportsUnsupportedPruningForWriteOnlyEventSinks() {
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementEventHistory history = new SkillManagementEventHistory(
                SkillManagementEventReader.forSink(writeOnlySink),
                SkillManagementEventPruner.forSink(writeOnlySink));

        SkillManagementEventPruneResult result =
                history.prune(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(history.latest().events()).isEmpty();
        assertThat(history.supportsPruning()).isFalse();
        assertThat(result.success()).isFalse();
        assertThat(result.failure()).contains("Event history pruning is not supported");
    }

    private SkillManagementEvent event(
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return new SkillManagementEvent(
                Instant.now(),
                operation,
                skillId,
                success,
                Map.of());
    }
}
