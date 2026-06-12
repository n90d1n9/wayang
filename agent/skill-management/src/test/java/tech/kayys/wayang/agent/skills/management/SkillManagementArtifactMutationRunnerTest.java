package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementArtifactMutationRunnerTest {

    @Test
    void putRecordsSuccessEventWithArtifactAttributes() {
        InMemorySkillArtifactStore artifactStore = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementArtifactMutationRunner runner = new SkillManagementArtifactMutationRunner(
                artifactStore,
                new SkillManagementEventRecorder(eventSink));
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifact artifact = SkillArtifact.text(reference, "hello");

        SkillArtifact result = runner.put(artifact);

        assertThat(result).isEqualTo(artifact);
        assertThat(artifactStore.getArtifact(reference))
                .hasValueSatisfying(stored -> {
                    assertThat(stored.reference()).isEqualTo(reference);
                    assertThat(new String(stored.content(), StandardCharsets.UTF_8)).isEqualTo("hello");
                });
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.PUT_ARTIFACT);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("kind", "resource")
                .containsEntry("name", "prompt")
                .containsEntry("version", "v1")
                .containsEntry("sizeBytes", "5")
                .containsKey("operationId");
    }

    @Test
    void deleteRecordsSuccessEventWithDeleteAttributes() {
        InMemorySkillArtifactStore artifactStore = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementArtifactMutationRunner runner = new SkillManagementArtifactMutationRunner(
                artifactStore,
                new SkillManagementEventRecorder(eventSink));
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        artifactStore.putArtifact(SkillArtifact.text(reference, "hello"));

        boolean deleted = runner.delete(reference);

        assertThat(deleted).isTrue();
        assertThat(artifactStore.getArtifact(reference)).isEmpty();
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.DELETE_ARTIFACT);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("deleted", "true")
                .containsEntry("kind", "resource")
                .containsEntry("name", "prompt")
                .containsEntry("version", "v1")
                .containsKey("operationId");
    }

    @Test
    void putRecordsFailureEventBeforeThrowingWriteException() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementArtifactMutationRunner runner = new SkillManagementArtifactMutationRunner(
                new FailingArtifactMutationStore(),
                new SkillManagementEventRecorder(eventSink));
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        assertThatThrownBy(() -> runner.put(SkillArtifact.text(reference, "hello")))
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("Failed to put skill artifact consistently")
                .hasMessageContaining(reference.qualifiedName());

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.PUT_ARTIFACT);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("errorType", IllegalStateException.class.getSimpleName())
                .containsEntry("error", "artifact put failed")
                .containsEntry("kind", "resource")
                .containsKey("operationId");
    }

    @Test
    void deleteRecordsFailureEventBeforeThrowingWriteException() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementArtifactMutationRunner runner = new SkillManagementArtifactMutationRunner(
                new FailingArtifactMutationStore(),
                new SkillManagementEventRecorder(eventSink));
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        assertThatThrownBy(() -> runner.delete(reference))
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("Failed to delete skill artifact consistently")
                .hasMessageContaining(reference.qualifiedName());

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.DELETE_ARTIFACT);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("errorType", IllegalStateException.class.getSimpleName())
                .containsEntry("error", "artifact delete failed")
                .containsEntry("kind", "resource")
                .containsKey("operationId");
    }

    private static final class FailingArtifactMutationStore implements SkillArtifactStore {

        @Override
        public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
            return Optional.empty();
        }

        @Override
        public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
            return List.of();
        }

        @Override
        public void putArtifact(SkillArtifact artifact) {
            throw new IllegalStateException("artifact put failed");
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            throw new IllegalStateException("artifact delete failed");
        }
    }
}
