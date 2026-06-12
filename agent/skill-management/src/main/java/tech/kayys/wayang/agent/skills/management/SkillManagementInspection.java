package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Service-level operational view of skill-management persistence.
 */
public record SkillManagementInspection(
        SkillDefinitionStoreInspection definitionStore,
        SkillLifecycleStateStoreInspection lifecycleStateStore,
        SkillManagementEventStoreInspection eventStore,
        SkillArtifactStoreInspection artifactStore,
        SkillLifecycleStateReconcileResult lifecycleStateReconciliation,
        String lifecycleStateReconciliationFailure) {

    public SkillManagementInspection {
        definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        eventStore = Objects.requireNonNull(eventStore, "eventStore");
        artifactStore = Objects.requireNonNull(artifactStore, "artifactStore");
        lifecycleStateReconciliation = lifecycleStateReconciliation == null
                ? emptyReconciliation()
                : lifecycleStateReconciliation;
        lifecycleStateReconciliationFailure =
                lifecycleStateReconciliationFailure == null ? "" : lifecycleStateReconciliationFailure;
    }

    public boolean ready() {
        return definitionStore.ready()
                && lifecycleStateStore.ready()
                && eventStore.ready()
                && artifactStore.ready()
                && lifecycleStateReconciliationFailure.isBlank();
    }

    public boolean lifecycleStateConsistent() {
        return lifecycleStateReconciliationFailure.isBlank()
                && lifecycleStateReconciliation.consistent();
    }

    private static SkillLifecycleStateReconcileResult emptyReconciliation() {
        return new SkillLifecycleStateReconcileResult(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
