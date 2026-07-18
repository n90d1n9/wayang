package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Runtime inspection graph shared by service health checks and workflows.
 */
record SkillManagementRuntimeInspection(
        SkillLifecycleStateReconciler lifecycleStateReconciler,
        SkillArtifactStoreInspector artifactStoreInspector,
        SkillManagementInspector managementInspector,
        SkillManagementInspectionReader inspectionReader) {

    SkillManagementRuntimeInspection {
        lifecycleStateReconciler = Objects.requireNonNull(lifecycleStateReconciler, "lifecycleStateReconciler");
        artifactStoreInspector = Objects.requireNonNull(artifactStoreInspector, "artifactStoreInspector");
        managementInspector = Objects.requireNonNull(managementInspector, "managementInspector");
        inspectionReader = Objects.requireNonNull(inspectionReader, "inspectionReader");
    }

    static SkillManagementRuntimeInspection of(
            SkillManagementRuntimeStores stores,
            SkillManagementRuntimeEventHooks events) {
        Objects.requireNonNull(stores, "stores");
        Objects.requireNonNull(events, "events");
        SkillLifecycleStateReconciler reconciler = new SkillLifecycleStateReconciler();
        SkillArtifactStoreInspector artifactInspector = new SkillArtifactStoreInspector();
        SkillManagementInspector managementInspector = new SkillManagementInspector(
                stores.definitionStoreInspector(),
                stores.lifecycleStateStoreInspector(),
                new SkillManagementEventStoreInspector(),
                artifactInspector,
                reconciler);
        SkillManagementInspectionReader inspectionReader = new SkillManagementInspectionReader(
                stores.definitionStore(),
                stores.definitionStoreInspector(),
                stores.lifecycleStateStore(),
                stores.lifecycleStateStoreInspector(),
                stores.artifactStore(),
                artifactInspector,
                events.eventReader(),
                managementInspector);
        return new SkillManagementRuntimeInspection(
                reconciler,
                artifactInspector,
                managementInspector,
                inspectionReader);
    }
}
