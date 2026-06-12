package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Live persistence stores assembled for a skill-management service.
 */
record SkillManagementStoreBundle(
        SkillDefinitionStore definitionStore,
        SkillLifecycleStateStore lifecycleStateStore,
        SkillArtifactStore artifactStore,
        SkillManagementEventSink eventSink) {

    SkillManagementStoreBundle {
        Objects.requireNonNull(definitionStore, "definitionStore");
        Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        Objects.requireNonNull(artifactStore, "artifactStore");
        Objects.requireNonNull(eventSink, "eventSink");
    }
}
