package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Store inputs used by maintenance execution.
 */
record SkillManagementMaintenanceInputs(
        SkillDefinitionStore sourceDefinitions,
        SkillDefinitionStore targetDefinitions,
        SkillLifecycleStateStore lifecycleStateStore,
        SkillArtifactStore sourceArtifacts,
        SkillArtifactStore targetArtifacts) {

    SkillManagementMaintenanceInputs {
        sourceDefinitions = Objects.requireNonNull(sourceDefinitions, "sourceDefinitions");
        targetDefinitions = Objects.requireNonNull(targetDefinitions, "targetDefinitions");
        lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
    }

    static SkillManagementMaintenanceInputs definitionsOnly(
            SkillDefinitionStore sourceDefinitions,
            SkillDefinitionStore targetDefinitions,
            SkillLifecycleStateStore lifecycleStateStore) {
        return new SkillManagementMaintenanceInputs(
                sourceDefinitions,
                targetDefinitions,
                lifecycleStateStore,
                null,
                null);
    }

    static SkillManagementMaintenanceInputs withArtifacts(
            SkillDefinitionStore sourceDefinitions,
            SkillDefinitionStore targetDefinitions,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillArtifactStore sourceArtifacts,
            SkillArtifactStore targetArtifacts) {
        return new SkillManagementMaintenanceInputs(
                sourceDefinitions,
                targetDefinitions,
                lifecycleStateStore,
                sourceArtifacts,
                targetArtifacts);
    }

    boolean artifactStoresAvailable() {
        return sourceArtifacts != null && targetArtifacts != null;
    }
}
