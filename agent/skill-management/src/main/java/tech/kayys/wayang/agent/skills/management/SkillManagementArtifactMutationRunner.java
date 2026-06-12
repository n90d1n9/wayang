package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Runs direct artifact mutations with management event recording.
 */
final class SkillManagementArtifactMutationRunner {

    private final SkillArtifactStore artifactStore;
    private final SkillManagementEventRecorder eventRecorder;

    SkillManagementArtifactMutationRunner(
            SkillArtifactStore artifactStore,
            SkillManagementEventRecorder eventRecorder) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore");
        this.eventRecorder = Objects.requireNonNull(eventRecorder, "eventRecorder");
    }

    SkillArtifact put(SkillArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact");
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.PUT_ARTIFACT,
                artifact.reference().skillId(),
                context,
                () -> {
                    artifactStore.putArtifact(artifact);
                    return artifact;
                },
                SkillManagementEventAttributes::artifact,
                error -> SkillManagementEventAttributes.artifact(artifact.reference()),
                error -> artifactWriteFailure("put", artifact.reference(), error));
    }

    boolean delete(SkillArtifactReference reference) {
        Objects.requireNonNull(reference, "reference");
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.DELETE_ARTIFACT,
                reference.skillId(),
                context,
                () -> artifactStore.deleteArtifact(reference),
                deleted -> SkillManagementEventAttributes.artifactDeleted(reference, deleted),
                error -> SkillManagementEventAttributes.artifact(reference),
                error -> artifactWriteFailure("delete", reference, error));
    }

    private SkillManagementWriteException artifactWriteFailure(
            String operationName,
            SkillArtifactReference reference,
            RuntimeException error) {
        return new SkillManagementWriteException(
                "Failed to " + operationName + " skill artifact consistently: " + reference.qualifiedName(),
                error);
    }
}
