package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Runtime component graph used by the public skill-management service facade.
 */
record SkillManagementServiceRuntime(
        SkillManagementRuntimeStores stores,
        SkillManagementRuntimeEventHooks events,
        SkillManagementRuntimeInspection inspection,
        SkillManagementRuntimeOperations operations) {

    SkillManagementServiceRuntime {
        stores = Objects.requireNonNull(stores, "stores");
        events = Objects.requireNonNull(events, "events");
        inspection = Objects.requireNonNull(inspection, "inspection");
        operations = Objects.requireNonNull(operations, "operations");
    }

    static SkillManagementServiceRuntime assemble(
            SkillDefinitionStore definitionStore,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillArtifactStore artifactStore,
            SkillManagementEventSink eventSink,
            SkillManagementEventReader eventReader) {
        SkillManagementRuntimeStores stores = SkillManagementRuntimeStores.of(
                definitionStore,
                definitionStoreInspector,
                lifecycleStateStore,
                lifecycleStateStoreInspector,
                artifactStore);
        SkillManagementRuntimeEventHooks events = SkillManagementRuntimeEventHooks.of(eventSink, eventReader);
        SkillManagementRuntimeInspection inspection = SkillManagementRuntimeInspection.of(stores, events);
        SkillManagementRuntimeOperations operations = SkillManagementRuntimeOperations.of(
                stores,
                events,
                inspection);

        return new SkillManagementServiceRuntime(stores, events, inspection, operations);
    }

    SkillDefinitionStore definitionStore() {
        return stores.definitionStore();
    }

    SkillLifecycleStateStore lifecycleStateStore() {
        return stores.lifecycleStateStore();
    }

    SkillArtifactStore artifactStore() {
        return stores.artifactStore();
    }

    SkillManagementEventSink eventSink() {
        return events.eventSink();
    }

    SkillManagementEventHistory eventHistory() {
        return events.eventHistory();
    }

    SkillManagementOperationTraceReader operationTraceReader() {
        return events.operationTraceReader();
    }

    SkillManagementInspectionReader inspectionReader() {
        return inspection.inspectionReader();
    }

    SkillDefinitionValidator definitionValidator() {
        return operations.definitionValidator();
    }

    SkillManagementDefinitionMutationRunner definitionMutationRunner() {
        return operations.definitionMutationRunner();
    }

    SkillManagementLifecycleRunner lifecycleRunner() {
        return operations.lifecycleRunner();
    }

    SkillManagementCatalogReader catalogReader() {
        return operations.catalogReader();
    }

    SkillManagementArtifactReader artifactReader() {
        return operations.artifactReader();
    }

    SkillManagementArtifactMutationRunner artifactMutationRunner() {
        return operations.artifactMutationRunner();
    }

    SkillManagementArtifactSyncWorkflow artifactSyncWorkflow() {
        return operations.artifactSyncWorkflow();
    }

    SkillManagementMaintenanceWorkflow maintenanceWorkflow() {
        return operations.maintenanceWorkflow();
    }
}
