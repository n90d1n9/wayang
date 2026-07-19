package tech.kayys.wayang.agent.skills.management;

/**
 * Options for a combined definition sync, artifact sync, lifecycle
 * reconciliation, and event-history maintenance run.
 */
public record SkillManagementMaintenancePlan(
        SkillDefinitionStoreSyncOptions definitionSyncOptions,
        SkillArtifactStoreSyncOptions artifactSyncOptions,
        SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions,
        SkillManagementEventPrunePolicy eventPrunePolicy) {

    public SkillManagementMaintenancePlan(
            SkillDefinitionStoreSyncOptions definitionSyncOptions,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        this(
                definitionSyncOptions,
                SkillArtifactStoreSyncOptions.bootstrap(),
                lifecycleStateReconcileOptions,
                SkillManagementEventPrunePolicy.disabled());
    }

    public SkillManagementMaintenancePlan(
            SkillDefinitionStoreSyncOptions definitionSyncOptions,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions,
            SkillManagementEventPrunePolicy eventPrunePolicy) {
        this(definitionSyncOptions, SkillArtifactStoreSyncOptions.bootstrap(), lifecycleStateReconcileOptions,
                eventPrunePolicy);
    }

    public SkillManagementMaintenancePlan(
            SkillDefinitionStoreSyncOptions definitionSyncOptions,
            SkillArtifactStoreSyncOptions artifactSyncOptions,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        this(
                definitionSyncOptions,
                artifactSyncOptions,
                lifecycleStateReconcileOptions,
                SkillManagementEventPrunePolicy.disabled());
    }

    public SkillManagementMaintenancePlan {
        definitionSyncOptions = definitionSyncOptions == null
                ? SkillDefinitionStoreSyncOptions.bootstrap()
                : definitionSyncOptions;
        artifactSyncOptions = artifactSyncOptions == null
                ? SkillArtifactStoreSyncOptions.bootstrap()
                : artifactSyncOptions;
        lifecycleStateReconcileOptions = lifecycleStateReconcileOptions == null
                ? SkillLifecycleStateReconcileOptions.createMissing()
                : lifecycleStateReconcileOptions;
        eventPrunePolicy = eventPrunePolicy == null
                ? SkillManagementEventPrunePolicy.disabled()
                : eventPrunePolicy;
    }

    public static SkillManagementMaintenancePlan bootstrap() {
        return new SkillManagementMaintenancePlan(
                SkillDefinitionStoreSyncOptions.bootstrap(),
                SkillArtifactStoreSyncOptions.bootstrap(),
                SkillLifecycleStateReconcileOptions.createMissing());
    }

    public static SkillManagementMaintenancePlan mirrorAndRepair() {
        return new SkillManagementMaintenancePlan(
                SkillDefinitionStoreSyncOptions.mirror(),
                SkillArtifactStoreSyncOptions.mirror(),
                SkillLifecycleStateReconcileOptions.createMissingAndRemoveOrphans());
    }

    public static SkillManagementMaintenancePlan inspectOnly() {
        return new SkillManagementMaintenancePlan(
                SkillDefinitionStoreSyncOptions.mirror().asDryRun(),
                SkillArtifactStoreSyncOptions.mirror().asDryRun(),
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

    public SkillManagementMaintenancePlan asDryRun() {
        return new SkillManagementMaintenancePlan(
                definitionSyncOptions.asDryRun(),
                artifactSyncOptions.asDryRun(),
                SkillLifecycleStateReconcileOptions.inspectOnly(),
                eventPrunePolicy.asDryRun());
    }

    public SkillManagementMaintenancePlan withEventPruning(SkillManagementEventPruneOptions options) {
        return new SkillManagementMaintenancePlan(
                definitionSyncOptions,
                artifactSyncOptions,
                lifecycleStateReconcileOptions,
                new SkillManagementEventPrunePolicy(true, options));
    }
}
