package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementArtifactSyncRunnerTest {

    @Test
    void syncRecordsSuccessEventWithResultAttributes() {
        InMemorySkillArtifactStore source = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore target = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        source.putArtifact(SkillArtifact.text(reference, "source"));
        SkillManagementArtifactSyncRunner runner = new SkillManagementArtifactSyncRunner(eventSink);

        SkillArtifactStoreSyncResult result = runner.sync(source, target, null);

        assertThat(result.copied()).isEqualTo(1);
        assertThat(target.getArtifact(reference)).isPresent();
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.SYNC_ARTIFACTS);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("copied", "1")
                .containsEntry("changed", "1")
                .containsEntry("consistent", "true")
                .containsKey("operationId");
    }

    @Test
    void syncRecordsFailureEventBeforeRethrowing() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementArtifactSyncRunner runner = new SkillManagementArtifactSyncRunner(eventSink);

        assertThatThrownBy(() -> runner.sync(
                        new FailingListArtifactStore(),
                        new InMemorySkillArtifactStore(),
                        SkillArtifactStoreSyncOptions.bootstrap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("artifact list failed");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.SYNC_ARTIFACTS);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("errorType", IllegalStateException.class.getSimpleName())
                .containsEntry("error", "artifact list failed")
                .containsKey("operationId");
    }

    private static final class FailingListArtifactStore implements SkillArtifactStore {

        @Override
        public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
            return Optional.empty();
        }

        @Override
        public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
            throw new IllegalStateException("artifact list failed");
        }

        @Override
        public void putArtifact(SkillArtifact artifact) {
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            return false;
        }
    }
}
