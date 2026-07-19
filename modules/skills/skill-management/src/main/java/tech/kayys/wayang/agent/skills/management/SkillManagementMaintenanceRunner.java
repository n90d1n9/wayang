package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Runs definition-store synchronization, artifact synchronization,
 * lifecycle-state repair, and optional event-history pruning.
 */
public final class SkillManagementMaintenanceRunner {

    private final SkillManagementMaintenanceStepExecutor stepExecutor;
    private final SkillManagementEventRecorder eventRecorder;
    private final SkillManagementEventPruner eventPruner;

    public SkillManagementMaintenanceRunner() {
        this(
                new SkillDefinitionStoreSynchronizer(),
                new SkillArtifactStoreSynchronizer(),
                new SkillLifecycleStateReconciler());
    }

    public SkillManagementMaintenanceRunner(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler) {
        this(definitionStoreSynchronizer, new SkillArtifactStoreSynchronizer(), lifecycleStateReconciler);
    }

    public SkillManagementMaintenanceRunner(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillArtifactStoreSynchronizer artifactStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler) {
        this(definitionStoreSynchronizer, artifactStoreSynchronizer, lifecycleStateReconciler,
                SkillManagementEventSink.noop());
    }

    public SkillManagementMaintenanceRunner(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillArtifactStoreSynchronizer artifactStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler,
            SkillManagementEventSink eventSink) {
        this(
                definitionStoreSynchronizer,
                artifactStoreSynchronizer,
                lifecycleStateReconciler,
                eventSink,
                SkillManagementEventPruner.forSink(eventSink));
    }

    public SkillManagementMaintenanceRunner(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler,
            SkillManagementEventSink eventSink) {
        this(
                definitionStoreSynchronizer,
                new SkillArtifactStoreSynchronizer(),
                lifecycleStateReconciler,
                eventSink,
                SkillManagementEventPruner.forSink(eventSink));
    }

    public SkillManagementMaintenanceRunner(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler,
            SkillManagementEventSink eventSink,
            SkillManagementEventPruner eventPruner) {
        this(
                definitionStoreSynchronizer,
                new SkillArtifactStoreSynchronizer(),
                lifecycleStateReconciler,
                eventSink,
                eventPruner);
    }

    public SkillManagementMaintenanceRunner(
            SkillDefinitionStoreSynchronizer definitionStoreSynchronizer,
            SkillArtifactStoreSynchronizer artifactStoreSynchronizer,
            SkillLifecycleStateReconciler lifecycleStateReconciler,
            SkillManagementEventSink eventSink,
            SkillManagementEventPruner eventPruner) {
        this.eventRecorder = new SkillManagementEventRecorder(eventSink);
        this.eventPruner = Objects.requireNonNull(eventPruner, "eventPruner");
        this.stepExecutor = new SkillManagementMaintenanceStepExecutor(
                definitionStoreSynchronizer,
                artifactStoreSynchronizer,
                lifecycleStateReconciler,
                this.eventPruner);
    }

    public SkillManagementMaintenanceResult run(
            SkillDefinitionStore sourceDefinitions,
            SkillDefinitionStore targetDefinitions,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillManagementMaintenancePlan plan) {
        return run(
                SkillManagementMaintenanceInputs.definitionsOnly(
                        sourceDefinitions,
                        targetDefinitions,
                        lifecycleStateStore),
                plan,
                SkillManagementOperationContext.root());
    }

    public SkillManagementMaintenanceResult run(
            SkillDefinitionStore sourceDefinitions,
            SkillDefinitionStore targetDefinitions,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillArtifactStore sourceArtifacts,
            SkillArtifactStore targetArtifacts,
            SkillManagementMaintenancePlan plan) {
        return run(
                SkillManagementMaintenanceInputs.withArtifacts(
                        sourceDefinitions,
                        targetDefinitions,
                        lifecycleStateStore,
                        sourceArtifacts,
                        targetArtifacts),
                plan,
                SkillManagementOperationContext.root());
    }

    SkillManagementMaintenanceResult run(
            SkillManagementMaintenanceInputs inputs,
            SkillManagementMaintenancePlan plan,
            SkillManagementOperationContext context) {
        Objects.requireNonNull(inputs, "inputs");
        SkillManagementMaintenancePlan resolved = SkillManagementMaintenancePreflight.plan(plan);
        SkillManagementOperationContext resolvedContext = context == null
                ? SkillManagementOperationContext.root()
                : context;
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.MAINTENANCE,
                "",
                resolvedContext,
                () -> {
                    SkillManagementPreflightEnforcer.enforce(
                            SkillManagementEventOperation.MAINTENANCE,
                            preflight(resolved));
                    return stepExecutor.execute(inputs, resolved);
                },
                result -> SkillManagementEventAttributes.maintenance(result, resolved));
    }

    public SkillManagementPreflightReport preflight(SkillManagementMaintenancePlan plan) {
        return SkillManagementMaintenancePreflight.report(plan, eventPruner);
    }

}
