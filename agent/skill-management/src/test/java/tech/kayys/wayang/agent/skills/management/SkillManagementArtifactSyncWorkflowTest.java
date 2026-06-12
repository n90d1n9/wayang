package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementArtifactSyncWorkflowTest {

    @Test
    void syncUsesManagedTargetArtifactStore() {
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore targetArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        sourceArtifacts.putArtifact(SkillArtifact.text(reference, "source"));
        SkillManagementArtifactSyncWorkflow workflow = new SkillManagementArtifactSyncWorkflow(
                targetArtifacts,
                new SkillManagementArtifactSyncRunner(eventSink));

        SkillArtifactStoreSyncResult result = workflow.sync(sourceArtifacts, SkillArtifactStoreSyncOptions.bootstrap());

        assertThat(result.copied()).isEqualTo(1);
        assertThat(targetArtifacts.getArtifact(reference)).isPresent();
        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.SYNC_ARTIFACTS);
    }

    @Test
    void syncRejectsMissingSourceArtifacts() {
        SkillManagementArtifactSyncWorkflow workflow = new SkillManagementArtifactSyncWorkflow(
                new InMemorySkillArtifactStore(),
                new SkillManagementArtifactSyncRunner(new InMemorySkillManagementEventSink()));

        assertThatThrownBy(() -> workflow.sync(null, SkillArtifactStoreSyncOptions.bootstrap()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("sourceArtifacts");
    }

    @Test
    void syncKeepsFailureEventWhenSourceListingFails() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementArtifactSyncWorkflow workflow = new SkillManagementArtifactSyncWorkflow(
                new InMemorySkillArtifactStore(),
                new SkillManagementArtifactSyncRunner(eventSink));

        assertThatThrownBy(() -> workflow.sync(
                        new FailingListArtifactStore(),
                        SkillArtifactStoreSyncOptions.bootstrap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("artifact list failed");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.SYNC_ARTIFACTS);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("errorType", IllegalStateException.class.getSimpleName())
                .containsEntry("error", "artifact list failed");
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
