package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Concrete source and target stores for one maintenance run.
 */
record SkillManagementMaintenanceStores(
        SkillDefinitionStore sourceDefinitions,
        SkillDefinitionStore targetDefinitions,
        SkillLifecycleStateStore lifecycleStateStore,
        SkillArtifactStore sourceArtifacts,
        SkillArtifactStore targetArtifacts,
        SkillManagementEventSink eventSink) {

    SkillManagementMaintenanceStores {
        Objects.requireNonNull(sourceDefinitions, "sourceDefinitions");
        Objects.requireNonNull(targetDefinitions, "targetDefinitions");
        Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        Objects.requireNonNull(sourceArtifacts, "sourceArtifacts");
        Objects.requireNonNull(targetArtifacts, "targetArtifacts");
        Objects.requireNonNull(eventSink, "eventSink");
    }
}
