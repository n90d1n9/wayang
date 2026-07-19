package tech.kayys.wayang.agent.skills.management;

/**
 * Optional source persistence used by configured maintenance runs.
 *
 * <p>Null source stores intentionally mean "reuse the managed target store" so
 * mirror-mode maintenance cannot accidentally prune target data from an empty
 * default source.</p>
 */
public record SkillManagementMaintenanceSourceConfig(
        SkillDefinitionStoreConfig definitionStore,
        SkillArtifactStoreConfig artifactStore) {

    public SkillManagementMaintenanceSourceConfig {
        validate(definitionStore, artifactStore).throwIfInvalid();
    }

    public boolean hasDefinitionStore() {
        return definitionStore != null;
    }

    public boolean hasArtifactStore() {
        return artifactStore != null;
    }

    public SkillStoreConfigValidationResult validate() {
        return validate(definitionStore, artifactStore);
    }

    public static SkillStoreConfigValidationResult validate(
            SkillDefinitionStoreConfig definitionStore,
            SkillArtifactStoreConfig artifactStore) {
        return SkillStoreConfigValidationResult.combine(
                definitionStore == null ? SkillStoreConfigValidationResult.valid() : definitionStore.validate(),
                artifactStore == null ? SkillStoreConfigValidationResult.valid() : artifactStore.validate());
    }

    public static SkillManagementMaintenanceSourceConfig none() {
        return new SkillManagementMaintenanceSourceConfig(null, null);
    }

    public static SkillManagementMaintenanceSourceConfig of(
            SkillDefinitionStoreConfig definitionStore,
            SkillArtifactStoreConfig artifactStore) {
        return new SkillManagementMaintenanceSourceConfig(definitionStore, artifactStore);
    }

    public static SkillManagementMaintenanceSourceConfig definitions(SkillDefinitionStoreConfig definitionStore) {
        return new SkillManagementMaintenanceSourceConfig(definitionStore, null);
    }

    public static SkillManagementMaintenanceSourceConfig artifacts(SkillArtifactStoreConfig artifactStore) {
        return new SkillManagementMaintenanceSourceConfig(null, artifactStore);
    }
}
