package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of one skill persistence strategy role.
 */
public record SkillManagementAdminPersistenceRole(
        String role,
        String path,
        String provider,
        String persistenceClass,
        String strategy,
        boolean disabled,
        boolean ephemeral,
        boolean durable,
        boolean durableFallback,
        boolean external,
        boolean custom,
        boolean composite,
        boolean mirrored,
        List<String> capabilities,
        List<SkillManagementAdminPersistenceRole> children) {

    public SkillManagementAdminPersistenceRole {
        role = label(role);
        path = label(path);
        provider = label(provider);
        persistenceClass = label(persistenceClass);
        strategy = label(strategy);
        capabilities = SkillManagementAdminValueSupport.compactStrings(capabilities);
        children = SkillManagementAdminValueSupport.nonNullList(children);
    }

    private static String label(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
