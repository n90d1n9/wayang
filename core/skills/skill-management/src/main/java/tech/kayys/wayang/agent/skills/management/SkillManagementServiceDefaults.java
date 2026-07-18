package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

/**
 * Default dependencies used by source-compatible service constructors.
 */
final class SkillManagementServiceDefaults {

    private SkillManagementServiceDefaults() {
    }

    static SkillDefinitionStore definitionStore(SkillRegistry registry) {
        return new RegistrySkillDefinitionStore(registry);
    }

    static SkillDefinitionStoreInspector definitionStoreInspector() {
        return new SkillDefinitionStoreInspector();
    }

    static SkillLifecycleStateStore lifecycleStateStore() {
        return new InMemorySkillLifecycleStateStore();
    }

    static SkillLifecycleStateStoreInspector lifecycleStateStoreInspector() {
        return new SkillLifecycleStateStoreInspector();
    }

    static SkillArtifactStore artifactStore() {
        return new InMemorySkillArtifactStore();
    }

    static SkillManagementEventSink eventSink() {
        return SkillManagementEventSink.noop();
    }

    static SkillManagementEventReader eventReader(SkillManagementEventSink eventSink) {
        return SkillManagementEventReader.forSink(eventSink);
    }
}
