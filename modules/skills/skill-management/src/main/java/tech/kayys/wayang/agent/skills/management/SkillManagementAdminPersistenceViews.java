package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Maps skill persistence strategy diagnostics to stable admin DTOs.
 */
final class SkillManagementAdminPersistenceViews {

    private SkillManagementAdminPersistenceViews() {
    }

    static SkillManagementAdminPersistenceStrategy persistenceStrategy(
            SkillManagementServiceConfig config) {
        return persistenceStrategy(SkillPersistenceStrategySummary.from(config));
    }

    static SkillManagementAdminPersistenceStrategy persistenceStrategy(
            SkillPersistenceContractMatrix matrix) {
        return persistenceStrategy(SkillPersistenceStrategySummary.from(matrix));
    }

    static SkillManagementAdminPersistenceStrategy persistenceStrategy(
            SkillPersistenceStrategySummary summary) {
        SkillPersistenceStrategySummary resolved = summary == null
                ? SkillPersistenceStrategySummary.from(SkillManagementServiceConfig.defaults())
                : summary;
        return new SkillManagementAdminPersistenceStrategy(
                resolved.kindLabel(),
                resolved.fullyDurable(),
                resolved.hasEphemeralRole(),
                resolved.hasDurableFallback(),
                resolved.hasExternalProvider(),
                resolved.hasCustomProvider(),
                resolved.hasCompositeProvider(),
                resolved.hasMirroredProvider(),
                resolved.warnings(),
                resolved.roles().stream()
                        .map(SkillManagementAdminPersistenceViews::role)
                        .toList());
    }

    static SkillManagementAdminPersistenceProfileCatalog persistenceProfiles() {
        return new SkillManagementAdminPersistenceProfileCatalog(
                SkillManagementServiceProfiles.profiles().stream()
                        .map(SkillManagementAdminPersistenceViews::persistenceProfile)
                        .toList());
    }

    static SkillManagementAdminPersistenceProfile persistenceProfile(
            SkillManagementServiceProfileDescriptor descriptor) {
        SkillManagementServiceProfileDescriptor resolved = descriptor == null
                ? SkillManagementServiceProfiles.profileDescriptor(SkillManagementServiceProfile.DEFAULT)
                : descriptor;
        return new SkillManagementAdminPersistenceProfile(
                resolved.label(),
                resolved.aliases(),
                resolved.description(),
                persistenceStrategy(SkillManagementServiceProfiles.config(resolved.profile())));
    }

    static SkillManagementAdminPersistenceProfile persistenceProfile(
            SkillManagementServiceProfile profile) {
        return persistenceProfile(SkillManagementServiceProfiles.profileDescriptor(profile));
    }

    static SkillManagementAdminPersistenceProfile persistenceProfile(String profileName) {
        return persistenceProfile(SkillManagementServiceProfiles.profileDescriptor(profileName));
    }

    private static SkillManagementAdminPersistenceRole role(
            SkillPersistenceStrategySummary.RoleStrategy role) {
        Objects.requireNonNull(role, "role");
        return new SkillManagementAdminPersistenceRole(
                role.roleLabel(),
                role.path(),
                role.provider(),
                role.persistenceClassLabel(),
                role.kindLabel(),
                role.disabled(),
                role.ephemeral(),
                role.durable(),
                role.durableFallback(),
                role.external(),
                role.custom(),
                role.composite(),
                role.mirrored(),
                role.capabilities(),
                role.children().stream()
                        .map(SkillManagementAdminPersistenceViews::role)
                        .toList());
    }
}
