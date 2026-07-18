package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementDefinitionWriteFailureTest {

    @Test
    void recordsFailureEventAndReturnsWriteException() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementDefinitionWriteFailure writeFailure = new SkillManagementDefinitionWriteFailure(
                new SkillManagementEventRecorder(eventSink));
        RuntimeException cause = new IllegalStateException("store failed");

        SkillManagementWriteException error = writeFailure.record(
                SkillManagementEventOperation.CREATE_SKILL,
                "create",
                "planner",
                cause,
                SkillManagementOperationContext.of("create-1"));

        assertThat(error)
                .hasMessageContaining("Failed to create skill consistently: planner")
                .hasCause(cause);
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(event.skillId()).isEqualTo("planner");
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("operationId", "create-1")
                .containsEntry("errorType", "IllegalStateException")
                .containsEntry("error", "store failed");
    }
}
