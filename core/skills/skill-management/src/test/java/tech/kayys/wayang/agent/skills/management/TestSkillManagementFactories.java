package tech.kayys.wayang.agent.skills.management;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test fixture builder for service factories with optional custom stores and
 * event sinks.
 */
final class TestSkillManagementFactories {

    private TestSkillManagementFactories() {
    }

    static Builder builder() {
        return new Builder(new TestSkillRegistry());
    }

    static Builder builder(TestSkillRegistry registry) {
        return new Builder(registry);
    }

    static final class Builder {
        private TestSkillRegistry registry;
        private SkillManagementObjectStore objectStore;
        private DataSource dataSource;
        private final Map<String, SkillDefinitionStore> definitionStores = new LinkedHashMap<>();
        private final Map<String, SkillLifecycleStateStore> lifecycleStateStores = new LinkedHashMap<>();
        private final Map<String, SkillArtifactStore> artifactStores = new LinkedHashMap<>();
        private final Map<String, SkillManagementEventSink> eventStores = new LinkedHashMap<>();
        private SkillManagementEventSink eventSinkOverride;

        private Builder(TestSkillRegistry registry) {
            this.registry = registry;
        }

        Builder registry(TestSkillRegistry registry) {
            this.registry = registry;
            return this;
        }

        Builder objectStore(SkillManagementObjectStore objectStore) {
            this.objectStore = objectStore;
            return this;
        }

        Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        Builder customDefinitionStore(String name, SkillDefinitionStore store) {
            definitionStores.put(name, store);
            return this;
        }

        Builder customLifecycleStateStore(String name, SkillLifecycleStateStore store) {
            lifecycleStateStores.put(name, store);
            return this;
        }

        Builder customArtifactStore(String name, SkillArtifactStore store) {
            artifactStores.put(name, store);
            return this;
        }

        Builder customEventStore(String name, SkillManagementEventSink store) {
            eventStores.put(name, store);
            return this;
        }

        Builder eventSink(SkillManagementEventSink eventSink) {
            this.eventSinkOverride = eventSink;
            return this;
        }

        SkillManagementServiceFactory build() {
            if (eventSinkOverride != null || !eventStores.isEmpty()) {
                return new SkillManagementServiceFactory(
                        new SkillDefinitionStoreFactory(registry, objectStore, dataSource, definitionStores),
                        new SkillLifecycleStateStoreFactory(dataSource, objectStore, lifecycleStateStores),
                        new SkillDefinitionStoreInspector(),
                        new SkillLifecycleStateStoreInspector(),
                        new SkillManagementEventStoreFactory(dataSource, objectStore, eventStores),
                        new SkillArtifactStoreFactory(objectStore, dataSource, artifactStores),
                        eventSinkOverride);
            }
            return new SkillManagementServiceFactory(
                    registry,
                    objectStore,
                    dataSource,
                    definitionStores,
                    lifecycleStateStores,
                    artifactStores);
        }
    }
}
