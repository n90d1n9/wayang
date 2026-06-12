package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Read-side coordinator for skill-management operational inspections.
 */
final class SkillManagementInspectionReader {

    private final SkillDefinitionStore definitionStore;
    private final SkillDefinitionStoreInspector definitionStoreInspector;
    private final SkillLifecycleStateStore lifecycleStateStore;
    private final SkillLifecycleStateStoreInspector lifecycleStateStoreInspector;
    private final SkillArtifactStore artifactStore;
    private final SkillArtifactStoreInspector artifactStoreInspector;
    private final SkillManagementEventReader eventReader;
    private final SkillManagementInspector managementInspector;

    SkillManagementInspectionReader(
            SkillDefinitionStore definitionStore,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillArtifactStore artifactStore,
            SkillArtifactStoreInspector artifactStoreInspector,
            SkillManagementEventReader eventReader,
            SkillManagementInspector managementInspector) {
        this.definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        this.definitionStoreInspector =
                Objects.requireNonNull(definitionStoreInspector, "definitionStoreInspector");
        this.lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        this.lifecycleStateStoreInspector =
                Objects.requireNonNull(lifecycleStateStoreInspector, "lifecycleStateStoreInspector");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore");
        this.artifactStoreInspector = Objects.requireNonNull(artifactStoreInspector, "artifactStoreInspector");
        this.eventReader = Objects.requireNonNull(eventReader, "eventReader");
        this.managementInspector = Objects.requireNonNull(managementInspector, "managementInspector");
    }

    SkillDefinitionStoreInspection definitionStore() {
        return definitionStoreInspector.inspect("skills", definitionStore);
    }

    SkillLifecycleStateStoreInspection lifecycleStateStore() {
        return lifecycleStateStoreInspector.inspect("lifecycle", lifecycleStateStore);
    }

    SkillArtifactStoreInspection artifactStore() {
        return artifactStoreInspector.inspect("artifacts", artifactStore);
    }

    SkillManagementInspection management() {
        return managementInspector.inspect(
                definitionStore,
                lifecycleStateStore,
                eventReader,
                artifactStore);
    }
}
