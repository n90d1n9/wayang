package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Service operation components assembled from normalized runtime dependencies.
 */
record SkillManagementRuntimeOperations(
        SkillDefinitionValidator definitionValidator,
        SkillManagementDefinitionMutationRunner definitionMutationRunner,
        SkillManagementLifecycleRunner lifecycleRunner,
        SkillManagementCatalogReader catalogReader,
        SkillManagementArtifactReader artifactReader,
        SkillManagementArtifactMutationRunner artifactMutationRunner,
        SkillManagementArtifactSyncWorkflow artifactSyncWorkflow,
        SkillManagementMaintenanceWorkflow maintenanceWorkflow) {

    SkillManagementRuntimeOperations {
        definitionValidator = Objects.requireNonNull(definitionValidator, "definitionValidator");
        definitionMutationRunner = Objects.requireNonNull(definitionMutationRunner, "definitionMutationRunner");
        lifecycleRunner = Objects.requireNonNull(lifecycleRunner, "lifecycleRunner");
        catalogReader = Objects.requireNonNull(catalogReader, "catalogReader");
        artifactReader = Objects.requireNonNull(artifactReader, "artifactReader");
        artifactMutationRunner = Objects.requireNonNull(artifactMutationRunner, "artifactMutationRunner");
        artifactSyncWorkflow = Objects.requireNonNull(artifactSyncWorkflow, "artifactSyncWorkflow");
        maintenanceWorkflow = Objects.requireNonNull(maintenanceWorkflow, "maintenanceWorkflow");
    }

    static SkillManagementRuntimeOperations of(
            SkillManagementRuntimeStores stores,
            SkillManagementRuntimeEventHooks events,
            SkillManagementRuntimeInspection inspection) {
        Objects.requireNonNull(stores, "stores");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(inspection, "inspection");
        SkillDefinitionValidator validator = new SkillDefinitionValidator();
        SkillManagementDefinitionMutationRunner definitionRunner =
                new SkillManagementDefinitionMutationRunner(
                        stores.definitionStore(),
                        stores.lifecycleStateStore(),
                        events.eventRecorder());
        SkillManagementLifecycleRunner lifecycleRunner = new SkillManagementLifecycleRunner(
                stores.definitionStore(),
                stores.lifecycleStateStore(),
                inspection.lifecycleStateReconciler(),
                events.eventRecorder());
        SkillManagementArtifactMutationRunner artifactRunner =
                new SkillManagementArtifactMutationRunner(
                        stores.artifactStore(),
                        events.eventRecorder());
        SkillManagementMaintenanceRunnerFactory maintenanceRunnerFactory =
                new SkillManagementMaintenanceRunnerFactory(inspection.lifecycleStateReconciler());
        return new SkillManagementRuntimeOperations(
                validator,
                definitionRunner,
                lifecycleRunner,
                new SkillManagementCatalogReader(stores.definitionStore(), lifecycleRunner),
                new SkillManagementArtifactReader(stores.artifactStore()),
                artifactRunner,
                new SkillManagementArtifactSyncWorkflow(
                        stores.artifactStore(),
                        new SkillManagementArtifactSyncRunner(events.eventRecorder())),
                new SkillManagementMaintenanceWorkflow(
                        stores.definitionStore(),
                        stores.lifecycleStateStore(),
                        stores.artifactStore(),
                        events.eventSink(),
                        events.eventPruner(),
                        maintenanceRunnerFactory));
    }
}
