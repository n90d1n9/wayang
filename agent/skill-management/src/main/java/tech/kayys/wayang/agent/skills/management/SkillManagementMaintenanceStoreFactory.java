package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Resolves source and target stores used by maintenance workflows.
 */
final class SkillManagementMaintenanceStoreFactory {

    private final SkillDefinitionStoreFactory definitionStoreFactory;
    private final SkillArtifactStoreFactory artifactStoreFactory;

    SkillManagementMaintenanceStoreFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillArtifactStoreFactory artifactStoreFactory) {
        this.definitionStoreFactory = Objects.requireNonNull(definitionStoreFactory, "definitionStoreFactory");
        this.artifactStoreFactory = Objects.requireNonNull(artifactStoreFactory, "artifactStoreFactory");
    }

    SkillStoreConfigValidationResult validateSources(SkillManagementMaintenanceSourceConfig sourceConfig) {
        SkillManagementMaintenanceSourceConfig resolved =
                SkillManagementConfigResolution.maintenanceSource(sourceConfig);
        return SkillStoreConfigValidationResult.combine(
                resolved.hasDefinitionStore()
                        ? definitionStoreFactory.validate(resolved.definitionStore())
                        : SkillStoreConfigValidationResult.valid(),
                resolved.hasArtifactStore()
                        ? artifactStoreFactory.validate(resolved.artifactStore())
                        : SkillStoreConfigValidationResult.valid());
    }

    SkillManagementMaintenanceStores create(
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementStoreBundle targetStores) {
        Objects.requireNonNull(targetStores, "targetStores");
        SkillManagementMaintenanceSourceConfig resolved =
                SkillManagementConfigResolution.maintenanceSource(sourceConfig);
        return new SkillManagementMaintenanceStores(
                sourceDefinitionStore(resolved, targetStores),
                targetStores.definitionStore(),
                targetStores.lifecycleStateStore(),
                sourceArtifactStore(resolved, targetStores),
                targetStores.artifactStore(),
                targetStores.eventSink());
    }

    private SkillDefinitionStore sourceDefinitionStore(
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementStoreBundle targetStores) {
        return sourceConfig.hasDefinitionStore()
                ? definitionStoreFactory.create(sourceConfig.definitionStore())
                : targetStores.definitionStore();
    }

    private SkillArtifactStore sourceArtifactStore(
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementStoreBundle targetStores) {
        return sourceConfig.hasArtifactStore()
                ? artifactStoreFactory.create(sourceConfig.artifactStore())
                : targetStores.artifactStore();
    }
}
