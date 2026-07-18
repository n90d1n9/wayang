package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Assembles the public skill-management service around live persistence stores.
 */
final class SkillManagementServiceAssembler {

    private final SkillDefinitionStoreInspector definitionStoreInspector;
    private final SkillLifecycleStateStoreInspector lifecycleStateStoreInspector;

    SkillManagementServiceAssembler(
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector) {
        this.definitionStoreInspector = Objects.requireNonNull(definitionStoreInspector, "definitionStoreInspector");
        this.lifecycleStateStoreInspector =
                Objects.requireNonNull(lifecycleStateStoreInspector, "lifecycleStateStoreInspector");
    }

    SkillManagementService service(SkillManagementStoreBundle stores) {
        Objects.requireNonNull(stores, "stores");
        return new SkillManagementService(
                stores.definitionStore(),
                definitionStoreInspector,
                stores.lifecycleStateStore(),
                lifecycleStateStoreInspector,
                stores.artifactStore(),
                stores.eventSink());
    }
}
