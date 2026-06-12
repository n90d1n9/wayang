package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Executes the ordered maintenance steps after preflight has accepted a plan.
 */
final class SkillManagementMaintenanceStepExecutor {

    private final SkillDefinitionStoreSynchronizer definitionStoreSynchronizer;
    private final SkillArtifactStoreSynchronizer artifactStoreSynchronizer;
    private final SkillLifecycleStateReconciler lifecycleStateReconciler;
    private final SkillManagementEventPruner eventPruner;

    SkillManagementMaintenanceStepExecutor(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillArtifactStoreSynchronizer artifactStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler,
            SkillManagementEventPruner eventPruner) {
        this.definitionStoreSynchronizer =
                Objects.requireNonNull(definitionStoreSynchronizer, "definitionStoreSynchronizer");
        this.artifactStoreSynchronizer =
                Objects.requireNonNull(artifactStoreSynchronizer, "artifactStoreSynchronizer");
        this.lifecycleStateReconciler =
                Objects.requireNonNull(lifecycleStateReconciler, "lifecycleStateReconciler");
        this.eventPruner = Objects.requireNonNull(eventPruner, "eventPruner");
    }

    SkillManagementMaintenanceResult execute(
            SkillManagementMaintenanceInputs inputs,
            SkillManagementMaintenancePlan plan) {
        Objects.requireNonNull(inputs, "inputs");
        SkillManagementMaintenancePlan resolved = SkillManagementMaintenancePreflight.plan(plan);
        SkillDefinitionStoreSyncResult syncResult = definitionStoreSynchronizer.sync(
                inputs.sourceDefinitions(),
                inputs.targetDefinitions(),
                resolved.definitionSyncOptions());
        SkillArtifactStoreSyncResult artifactSyncResult = syncArtifacts(
                inputs,
                resolved.artifactSyncOptions());
        SkillLifecycleStateReconcileResult reconcileResult = lifecycleStateReconciler.reconcile(
                inputs.targetDefinitions(),
                inputs.lifecycleStateStore(),
                resolved.lifecycleStateReconcileOptions());
        SkillManagementEventPruneResult pruneResult = pruneEvents(resolved.eventPrunePolicy());
        return new SkillManagementMaintenanceResult(
                syncResult,
                artifactSyncResult,
                reconcileResult,
                pruneResult);
    }

    private SkillArtifactStoreSyncResult syncArtifacts(
            SkillManagementMaintenanceInputs inputs,
            SkillArtifactStoreSyncOptions options) {
        if (!inputs.artifactStoresAvailable()) {
            return new SkillArtifactStoreSyncResult(true, List.of());
        }
        return artifactStoreSynchronizer.sync(inputs.sourceArtifacts(), inputs.targetArtifacts(), options);
    }

    private SkillManagementEventPruneResult pruneEvents(SkillManagementEventPrunePolicy policy) {
        SkillManagementEventPrunePolicy resolved = policy == null
                ? SkillManagementEventPrunePolicy.disabled()
                : policy;
        if (!resolved.enabled()) {
            return SkillManagementEventPruneResult.skipped(resolved.options());
        }
        return eventPruner.pruneEvents(resolved.options());
    }
}
