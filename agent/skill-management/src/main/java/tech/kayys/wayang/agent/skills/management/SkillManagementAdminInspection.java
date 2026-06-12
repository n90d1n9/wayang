package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Stable admin-facing projection of aggregate skill-management diagnostics.
 */
public record SkillManagementAdminInspection(
        boolean ready,
        boolean lifecycleStateConsistent,
        SkillManagementAdminStoreStatus definitionStore,
        SkillManagementAdminStoreStatus lifecycleStateStore,
        SkillManagementAdminStoreStatus eventStore,
        SkillManagementAdminStoreStatus artifactStore,
        SkillManagementAdminReconcileStatus lifecycleStateReconciliation) {

    public SkillManagementAdminInspection {
        definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        eventStore = Objects.requireNonNull(eventStore, "eventStore");
        artifactStore = Objects.requireNonNull(artifactStore, "artifactStore");
        lifecycleStateReconciliation =
                Objects.requireNonNull(lifecycleStateReconciliation, "lifecycleStateReconciliation");
    }
}
