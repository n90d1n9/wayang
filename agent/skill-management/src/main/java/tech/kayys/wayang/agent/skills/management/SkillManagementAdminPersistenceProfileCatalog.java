package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.function.Predicate;

/**
 * Stable admin-facing projection of the named skill persistence profile catalog.
 */
public record SkillManagementAdminPersistenceProfileCatalog(
        int profileCount,
        int durableProfileCount,
        int externalProfileCount,
        int compositeProfileCount,
        int mirroredProfileCount,
        int durableFallbackProfileCount,
        List<SkillManagementAdminPersistenceProfile> profiles) {

    public SkillManagementAdminPersistenceProfileCatalog(
            List<SkillManagementAdminPersistenceProfile> profiles) {
        this(0, 0, 0, 0, 0, 0, profiles);
    }

    public SkillManagementAdminPersistenceProfileCatalog {
        profiles = SkillManagementAdminValueSupport.nonNullList(profiles);
        profileCount = profiles.size();
        durableProfileCount = count(profiles, profile -> profile.persistence().fullyDurable());
        externalProfileCount = count(profiles, profile -> profile.persistence().hasExternalProvider());
        compositeProfileCount = count(profiles, profile -> profile.persistence().hasCompositeProvider());
        mirroredProfileCount = count(profiles, profile -> profile.persistence().hasMirroredProvider());
        durableFallbackProfileCount = count(profiles, profile -> profile.persistence().hasDurableFallback());
    }

    private static int count(
            List<SkillManagementAdminPersistenceProfile> profiles,
            Predicate<SkillManagementAdminPersistenceProfile> predicate) {
        return (int) profiles.stream()
                .filter(predicate)
                .count();
    }
}
