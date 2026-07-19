package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Normalized store and inspector inputs for service runtime assembly.
 */
record SkillManagementRuntimeStores(
        SkillDefinitionStore definitionStore,
        SkillDefinitionStoreInspector definitionStoreInspector,
        SkillLifecycleStateStore lifecycleStateStore,
        SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
        SkillArtifactStore artifactStore) {

    SkillManagementRuntimeStores {
        definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        definitionStoreInspector = Objects.requireNonNull(definitionStoreInspector, "definitionStoreInspector");
        lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        lifecycleStateStoreInspector =
                Objects.requireNonNull(lifecycleStateStoreInspector, "lifecycleStateStoreInspector");
        artifactStore = Objects.requireNonNull(artifactStore, "artifactStore");
    }

    static SkillManagementRuntimeStores of(
            SkillDefinitionStore definitionStore,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillArtifactStore artifactStore) {
        return new SkillManagementRuntimeStores(
                definitionStore,
                definitionStoreInspector,
                lifecycleStateStore,
                lifecycleStateStoreInspector,
                artifactStore);
    }
}
