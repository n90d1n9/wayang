package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.function.Predicate;

/**
 * Stable admin-facing projection of skill persistence strategy posture.
 */
public record SkillManagementAdminPersistenceStrategy(
        String strategy,
        boolean fullyDurable,
        boolean hasEphemeralRole,
        boolean hasDurableFallback,
        boolean hasExternalProvider,
        boolean hasCustomProvider,
        boolean hasCompositeProvider,
        boolean hasMirroredProvider,
        int roleCount,
        int durableRoleCount,
        int ephemeralRoleCount,
        int disabledRoleCount,
        int customRoleCount,
        int warningCount,
        List<String> warnings,
        List<SkillManagementAdminPersistenceRole> roles) {

    public SkillManagementAdminPersistenceStrategy(
            String strategy,
            boolean fullyDurable,
            boolean hasEphemeralRole,
            boolean hasDurableFallback,
            boolean hasExternalProvider,
            boolean hasCustomProvider,
            boolean hasCompositeProvider,
            boolean hasMirroredProvider,
            List<String> warnings,
            List<SkillManagementAdminPersistenceRole> roles) {
        this(
                strategy,
                fullyDurable,
                hasEphemeralRole,
                hasDurableFallback,
                hasExternalProvider,
                hasCustomProvider,
                hasCompositeProvider,
                hasMirroredProvider,
                0,
                0,
                0,
                0,
                0,
                0,
                warnings,
                roles);
    }

    public SkillManagementAdminPersistenceStrategy {
        strategy = strategy == null || strategy.isBlank() ? "unknown" : strategy.trim();
        warnings = SkillManagementAdminValueSupport.compactStrings(warnings);
        roles = SkillManagementAdminValueSupport.nonNullList(roles);
        roleCount = roles.size();
        durableRoleCount = count(roles, SkillManagementAdminPersistenceRole::durable);
        ephemeralRoleCount = count(roles, SkillManagementAdminPersistenceRole::ephemeral);
        disabledRoleCount = count(roles, SkillManagementAdminPersistenceRole::disabled);
        customRoleCount = count(roles, SkillManagementAdminPersistenceRole::custom);
        warningCount = warnings.size();
    }

    private static int count(
            List<SkillManagementAdminPersistenceRole> roles,
            Predicate<SkillManagementAdminPersistenceRole> predicate) {
        return (int) roles.stream()
                .filter(predicate)
                .count();
    }
}
