package tech.kayys.wayang.agent.skills.management;

/**
 * Test fixture builder for service instances with the standard inspectors and
 * in-memory support stores.
 */
final class TestSkillManagementServices {

    private TestSkillManagementServices() {
    }

    static Builder builder() {
        return new Builder(new TestSkillRegistry());
    }

    static Builder builder(TestSkillRegistry registry) {
        return new Builder(registry);
    }

    static final class Builder {
        private TestSkillRegistry registry;
        private SkillDefinitionStore definitionStore;
        private SkillDefinitionStoreInspector definitionStoreInspector = new SkillDefinitionStoreInspector();
        private SkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        private SkillLifecycleStateStoreInspector lifecycleStateStoreInspector =
                new SkillLifecycleStateStoreInspector();
        private SkillArtifactStore artifactStore = new InMemorySkillArtifactStore();
        private SkillManagementEventSink eventSink = SkillManagementEventSink.noop();
        private SkillManagementEventReader eventReader;

        private Builder(TestSkillRegistry registry) {
            this.registry = registry;
        }

        Builder registry(TestSkillRegistry registry) {
            this.registry = registry;
            this.definitionStore = null;
            return this;
        }

        Builder definitionStore(SkillDefinitionStore definitionStore) {
            this.definitionStore = definitionStore;
            return this;
        }

        Builder definitionStoreInspector(SkillDefinitionStoreInspector definitionStoreInspector) {
            this.definitionStoreInspector = definitionStoreInspector;
            return this;
        }

        Builder lifecycleStateStore(SkillLifecycleStateStore lifecycleStateStore) {
            this.lifecycleStateStore = lifecycleStateStore;
            return this;
        }

        Builder lifecycleStateStoreInspector(SkillLifecycleStateStoreInspector lifecycleStateStoreInspector) {
            this.lifecycleStateStoreInspector = lifecycleStateStoreInspector;
            return this;
        }

        Builder artifactStore(SkillArtifactStore artifactStore) {
            this.artifactStore = artifactStore;
            return this;
        }

        Builder eventSink(SkillManagementEventSink eventSink) {
            this.eventSink = eventSink;
            return this;
        }

        Builder eventReader(SkillManagementEventReader eventReader) {
            this.eventReader = eventReader;
            return this;
        }

        SkillManagementService build() {
            SkillDefinitionStore resolvedDefinitions = definitionStore == null
                    ? new RegistrySkillDefinitionStore(registry)
                    : definitionStore;
            if (eventReader == null) {
                return new SkillManagementService(
                        resolvedDefinitions,
                        definitionStoreInspector,
                        lifecycleStateStore,
                        lifecycleStateStoreInspector,
                        artifactStore,
                        eventSink);
            }
            return new SkillManagementService(
                    resolvedDefinitions,
                    definitionStoreInspector,
                    lifecycleStateStore,
                    lifecycleStateStoreInspector,
                    artifactStore,
                    eventSink,
                    eventReader);
        }
    }
}
