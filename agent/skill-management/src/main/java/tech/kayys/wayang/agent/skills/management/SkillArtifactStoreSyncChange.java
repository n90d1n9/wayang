package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * One artifact-level change discovered during store synchronization.
 */
public record SkillArtifactStoreSyncChange(
        SkillArtifactReference reference,
        SkillArtifactStoreSyncAction action,
        String detail) {

    public SkillArtifactStoreSyncChange {
        reference = Objects.requireNonNull(reference, "reference");
        action = Objects.requireNonNull(action, "action");
        detail = SkillManagementValueSupport.text(detail);
    }

    public boolean changed() {
        return action == SkillArtifactStoreSyncAction.COPIED
                || action == SkillArtifactStoreSyncAction.UPDATED
                || action == SkillArtifactStoreSyncAction.DELETED;
    }
}
