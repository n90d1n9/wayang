package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminViews;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillStoreConfigValidationResult;

final class SkillsPersistenceConfigResolutionService {

    private final SkillManagementServiceConfig defaultConfig;

    SkillsPersistenceConfigResolutionService(SkillManagementServiceConfig defaultConfig) {
        this.defaultConfig = defaultConfig == null
                ? SkillManagementServiceConfig.defaults()
                : defaultConfig;
    }

    SkillsPersistenceConfigResolution resolve(String profileName, boolean runtimeConfig) {
        SkillsPersistenceConfigSource source = SkillsPersistenceConfigSource.resolve(
                profileName,
                runtimeConfig,
                defaultConfig);
        SkillStoreConfigValidationResult validation = source.config().validate();
        SkillManagementAdminPersistenceStrategy persistence =
                SkillManagementAdminViews.persistenceStrategy(source.config());
        return new SkillsPersistenceConfigResolution(
                source.source(),
                source.profile(),
                source.runtime(),
                validation.validConfiguration(),
                validation.errors(),
                SkillsPersistenceConfigDiagnostics.from(source.config()),
                SkillsPersistenceConfigWarnings.from(source, persistence),
                persistence);
    }
}
