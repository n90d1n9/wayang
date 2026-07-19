package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementMaintenanceStoreFactoryTest {

    @Test
    void fallsBackToTargetStoresWhenSourcesAreNotConfigured() {
        SkillManagementMaintenanceStoreFactory factory = factory(Map.of(), Map.of());
        SkillManagementStoreBundle targetStores = targetStores();

        SkillManagementMaintenanceStores stores = factory.create(null, targetStores);

        assertThat(stores.sourceDefinitions()).isSameAs(targetStores.definitionStore());
        assertThat(stores.targetDefinitions()).isSameAs(targetStores.definitionStore());
        assertThat(stores.sourceArtifacts()).isSameAs(targetStores.artifactStore());
        assertThat(stores.targetArtifacts()).isSameAs(targetStores.artifactStore());
        assertThat(stores.lifecycleStateStore()).isSameAs(targetStores.lifecycleStateStore());
        assertThat(stores.eventSink()).isSameAs(targetStores.eventSink());
    }

    @Test
    void createsConfiguredSourceStores() {
        TestSkillDefinitionStore sourceDefinitions = new TestSkillDefinitionStore();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        SkillManagementMaintenanceStoreFactory factory = factory(
                Map.of("source-definitions", sourceDefinitions),
                Map.of("source-artifacts", sourceArtifacts));
        SkillManagementStoreBundle targetStores = targetStores();

        SkillManagementMaintenanceStores stores = factory.create(
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("source-definitions"),
                        SkillArtifactStoreConfig.custom("source-artifacts")),
                targetStores);

        assertThat(stores.sourceDefinitions()).isSameAs(sourceDefinitions);
        assertThat(stores.sourceArtifacts()).isSameAs(sourceArtifacts);
        assertThat(stores.targetDefinitions()).isSameAs(targetStores.definitionStore());
        assertThat(stores.targetArtifacts()).isSameAs(targetStores.artifactStore());
    }

    @Test
    void validatesConfiguredSourceStoresOnly() {
        SkillManagementMaintenanceStoreFactory factory = factory(Map.of(), Map.of());

        SkillStoreConfigValidationResult emptySourceValidation = factory.validateSources(null);
        SkillStoreConfigValidationResult missingSourceValidation = factory.validateSources(
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("missing-definitions"),
                        SkillArtifactStoreConfig.custom("missing-artifacts")));

        assertThat(emptySourceValidation.validConfiguration()).isTrue();
        assertThat(missingSourceValidation.errors())
                .contains(
                        "No custom skill definition store registered for: missing-definitions",
                        "No custom artifact store registered for: missing-artifacts");
    }

    private SkillManagementMaintenanceStoreFactory factory(
            Map<String, SkillDefinitionStore> definitionStores,
            Map<String, SkillArtifactStore> artifactStores) {
        return new SkillManagementMaintenanceStoreFactory(
                new SkillDefinitionStoreFactory(
                        null,
                        (SkillManagementObjectStore) null,
                        null,
                        definitionStores),
                new SkillArtifactStoreFactory(artifactStores));
    }

    private SkillManagementStoreBundle targetStores() {
        return new SkillManagementStoreBundle(
                new TestSkillDefinitionStore(),
                new InMemorySkillLifecycleStateStore(),
                new InMemorySkillArtifactStore(),
                new InMemorySkillManagementEventSink());
    }
}
