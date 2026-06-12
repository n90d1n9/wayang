package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfigs;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceRuntimeFactory;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Resolves the skill-management service used by Hermes mode from an existing
 * service bean or a registry-backed runtime service.
 */
final class HermesSkillManagementServiceResolver {

    private HermesSkillManagementServiceResolver() {
    }

    static SkillManagementService resolve(
            Optional<SkillManagementService> skillManagementService,
            Optional<SkillRegistry> skillRegistry,
            Optional<SkillManagementServiceConfig> config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        Optional<SkillManagementService> existingService = HermesOptionals.orEmpty(skillManagementService);
        if (existingService.isPresent()) {
            return existingService.orElseThrow();
        }
        SkillRegistry registry = HermesOptionals.orEmpty(skillRegistry)
                .orElseThrow(() -> new IllegalStateException(
                        "Hermes mode requires a SkillRegistry or SkillManagementService bean"));
        SkillManagementServiceConfig effectiveConfig = HermesOptionals.orEmpty(config)
                .orElseGet(SkillManagementServiceConfigs::fromRuntime);
        return SkillManagementServiceRuntimeFactory.create(
                registry,
                effectiveConfig,
                HermesOptionals.orEmpty(objectStorageService),
                HermesOptionals.orEmpty(dataSource));
    }
}
