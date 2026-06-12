package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of one named skill persistence profile.
 */
public record SkillManagementAdminPersistenceProfile(
        String label,
        List<String> aliases,
        String description,
        SkillManagementAdminPersistenceStrategy persistence) {

    public SkillManagementAdminPersistenceProfile {
        label = SkillManagementAdminValueSupport.identifier(label);
        aliases = SkillManagementAdminValueSupport.compactStrings(aliases);
        description = SkillManagementAdminValueSupport.blankToEmpty(description);
        persistence = persistence == null
                ? SkillManagementAdminPersistenceViews.persistenceStrategy(SkillManagementServiceConfig.defaults())
                : persistence;
    }
}
