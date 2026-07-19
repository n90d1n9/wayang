package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Binds ad hoc maintenance runs to the managed service stores.
 */
final class SkillManagementMaintenanceWorkflow {

    private final SkillDefinitionStore targetDefinitions;
    private final SkillLifecycleStateStore lifecycleStateStore;
    private final SkillArtifactStore targetArtifacts;
    private final SkillManagementEventSink eventSink;
    private final SkillManagementEventPruner eventPruner;
    private final SkillManagementMaintenanceRunnerFactory runnerFactory;

    SkillManagementMaintenanceWorkflow(
            SkillDefinitionStore targetDefinitions,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillArtifactStore targetArtifacts,
            SkillManagementEventSink eventSink,
            SkillManagementEventPruner eventPruner,
            SkillManagementMaintenanceRunnerFactory runnerFactory) {
        this.targetDefinitions = Objects.requireNonNull(targetDefinitions, "targetDefinitions");
        this.lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        this.targetArtifacts = Objects.requireNonNull(targetArtifacts, "targetArtifacts");
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink");
        this.eventPruner = Objects.requireNonNull(eventPruner, "eventPruner");
        this.runnerFactory = Objects.requireNonNull(runnerFactory, "runnerFactory");
    }

    SkillManagementMaintenanceResult run(
            SkillDefinitionStore sourceDefinitions,
            SkillManagementMaintenancePlan plan) {
        Objects.requireNonNull(sourceDefinitions, "sourceDefinitions");
        return runner().run(sourceDefinitions, targetDefinitions, lifecycleStateStore, plan);
    }

    SkillManagementMaintenanceResult run(
            SkillDefinitionStore sourceDefinitions,
            SkillArtifactStore sourceArtifacts,
            SkillManagementMaintenancePlan plan) {
        Objects.requireNonNull(sourceDefinitions, "sourceDefinitions");
        Objects.requireNonNull(sourceArtifacts, "sourceArtifacts");
        return runner().run(
                sourceDefinitions,
                targetDefinitions,
                lifecycleStateStore,
                sourceArtifacts,
                targetArtifacts,
                plan);
    }

    private SkillManagementMaintenanceRunner runner() {
        return runnerFactory.create(eventSink, eventPruner);
    }
}
