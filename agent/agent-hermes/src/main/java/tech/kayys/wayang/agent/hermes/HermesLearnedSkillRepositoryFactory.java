package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Assembles the learned-skill repository from Hermes persistence configuration
 * and available runtime storage resources.
 */
final class HermesLearnedSkillRepositoryFactory {

    private HermesLearnedSkillRepositoryFactory() {
    }

    static HermesLearnedSkillRepository create(
            SkillManagementService skillManagementService,
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        return createWithResources(
                skillManagementService,
                config,
                HermesPersistenceResources.of(objectStorageService, dataSource),
                new HermesSkillMarkdownRenderer());
    }

    static HermesLearnedSkillRepository createWithResources(
            SkillManagementService skillManagementService,
            HermesAgentModeConfig config,
            HermesPersistenceResources resources,
            HermesSkillMarkdownRenderer markdownRenderer) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceResources effectiveResources = resources == null
                ? HermesPersistenceResources.empty()
                : resources;
        return new HermesLearnedSkillRepository(
                HermesLearnedSkillPersistenceAdapterResolver.resolve(
                        skillManagementService,
                        effectiveConfig,
                        effectiveResources.objectStorageService(),
                        effectiveResources.dataSource()),
                markdownRenderer);
    }
}
