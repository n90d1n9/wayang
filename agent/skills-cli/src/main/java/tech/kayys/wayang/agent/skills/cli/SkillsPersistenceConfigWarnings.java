package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;
import tech.kayys.wayang.agent.skills.management.SkillManagementObjectStorageProviderConfigAssessment;

import java.util.ArrayList;
import java.util.List;

final class SkillsPersistenceConfigWarnings {

    private SkillsPersistenceConfigWarnings() {
    }

    static List<String> from(
            SkillsPersistenceConfigSource source,
            SkillManagementAdminPersistenceStrategy persistence) {
        return from(source, persistence, SkillManagementObjectStorageProviderConfigAssessment.fromRuntime());
    }

    static List<String> from(
            SkillsPersistenceConfigSource source,
            SkillManagementAdminPersistenceStrategy persistence,
            SkillManagementObjectStorageProviderConfigAssessment providerAssessment) {
        List<String> warnings = persistence == null
                ? new ArrayList<>()
                : new ArrayList<>(persistence.warnings());
        warnings.addAll(providerWarnings(source, persistence, providerAssessment));
        return List.copyOf(warnings);
    }

    private static List<String> providerWarnings(
            SkillsPersistenceConfigSource source,
            SkillManagementAdminPersistenceStrategy persistence,
            SkillManagementObjectStorageProviderConfigAssessment providerAssessment) {
        if (source == null || persistence == null || !source.runtime() || !persistence.hasExternalProvider()) {
            return List.of();
        }
        return providerAssessment == null ? List.of() : providerAssessment.warnings();
    }
}
