package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Builds maintenance runners with the shared synchronization components.
 */
final class SkillManagementMaintenanceRunnerFactory {

    private final SkillDefinitionStoreSynchronizer definitionStoreSynchronizer;
    private final SkillArtifactStoreSynchronizer artifactStoreSynchronizer;
    private final SkillLifecycleStateReconciler lifecycleStateReconciler;

    SkillManagementMaintenanceRunnerFactory() {
        this(new SkillLifecycleStateReconciler());
    }

    SkillManagementMaintenanceRunnerFactory(SkillLifecycleStateReconciler lifecycleStateReconciler) {
        this(
                new SkillDefinitionStoreSynchronizer(),
                new SkillArtifactStoreSynchronizer(),
                lifecycleStateReconciler);
    }

    SkillManagementMaintenanceRunnerFactory(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillArtifactStoreSynchronizer artifactStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler) {
        this.definitionStoreSynchronizer =
                Objects.requireNonNull(definitionStoreSynchronizer, "definitionStoreSynchronizer");
        this.artifactStoreSynchronizer =
                Objects.requireNonNull(artifactStoreSynchronizer, "artifactStoreSynchronizer");
        this.lifecycleStateReconciler =
                Objects.requireNonNull(lifecycleStateReconciler, "lifecycleStateReconciler");
    }

    SkillManagementMaintenanceRunner create(SkillManagementEventSink eventSink) {
        return create(eventSink, SkillManagementEventPruner.forSink(eventSink));
    }

    SkillManagementMaintenanceRunner create(
            SkillManagementEventSink eventSink,
            SkillManagementEventPruner eventPruner) {
        return new SkillManagementMaintenanceRunner(
                definitionStoreSynchronizer,
                artifactStoreSynchronizer,
                lifecycleStateReconciler,
                eventSink,
                eventPruner);
    }
}
