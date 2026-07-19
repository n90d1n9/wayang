package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SkillManagementEventRecorderTest {

    @Test
    void recordOperationReturnsResultAndRecordsSuccessWithContext() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementEventRecorder recorder = new SkillManagementEventRecorder(eventSink);

        String result = recorder.recordOperation(
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                SkillManagementOperationContext.of("create-1"),
                () -> "created",
                value -> Map.of("result", value));

        assertThat(result).isEqualTo("created");
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(event.skillId()).isEqualTo("planner");
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("result", "created")
                .containsEntry("operationId", "create-1");
    }

    @Test
    void recordOperationRethrowsAndRecordsFailureWithContext() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementEventRecorder recorder = new SkillManagementEventRecorder(eventSink);
        RuntimeException error = new IllegalStateException("broken");

        Throwable thrown = catchThrowable(() -> recorder.recordOperation(
                SkillManagementEventOperation.DELETE_SKILL,
                "planner",
                SkillManagementOperationContext.of("delete-1"),
                () -> {
                    throw error;
                },
                value -> Map.of()));

        assertThat(thrown).isSameAs(error);
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.DELETE_SKILL);
        assertThat(event.skillId()).isEqualTo("planner");
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("operationId", "delete-1")
                .containsEntry("errorType", "IllegalStateException")
                .containsEntry("error", "broken");
    }

    @Test
    void recordOperationRecordsFailureAttributesAndRethrowsMappedError() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementEventRecorder recorder = new SkillManagementEventRecorder(eventSink);
        RuntimeException storageError = new IllegalStateException("storage failed");
        RuntimeException mappedError = new SkillManagementWriteException("write failed", storageError);

        Throwable thrown = catchThrowable(() -> recorder.recordOperation(
                SkillManagementEventOperation.PUT_ARTIFACT,
                "planner",
                SkillManagementOperationContext.of("artifact-1"),
                () -> {
                    throw storageError;
                },
                value -> Map.of(),
                error -> Map.of("kind", "resource"),
                error -> mappedError));

        assertThat(thrown).isSameAs(mappedError);
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.PUT_ARTIFACT);
        assertThat(event.skillId()).isEqualTo("planner");
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("operationId", "artifact-1")
                .containsEntry("kind", "resource")
                .containsEntry("errorType", "IllegalStateException")
                .containsEntry("error", "storage failed");
    }
}
