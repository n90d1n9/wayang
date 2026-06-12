package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime-facing service assembly helper. It keeps CDI/runtime dependency
 * discovery outside the core factories while still honoring the deployable
 * store configuration model.
 */
public final class SkillManagementServiceRuntimeFactory {

    private SkillManagementServiceRuntimeFactory() {
    }

    public static SkillManagementService create(SkillRegistry registry) {
        return create(registry, SkillManagementServiceConfigs.fromRuntime());
    }

    public static SkillManagementService create(
            SkillRegistry registry,
            SkillManagementServiceConfig config) {
        return create(registry, config, Optional.empty(), Optional.empty());
    }

    public static SkillManagementService create(
            SkillRegistry registry,
            SkillManagementServiceConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> jdbcDataSource) {
        Objects.requireNonNull(registry, "registry");
        SkillManagementRuntimeDependencies dependencies =
                SkillManagementRuntimeDependencies.of(config, objectStorageService, jdbcDataSource);
        SkillManagementServiceFactory factory = dependencies.serviceFactory(registry);
        SkillStoreConfigValidationResult validation = factory.validate(dependencies.config());
        if (!validation.validConfiguration()) {
            throw new IllegalStateException(
                    "Invalid skill-management runtime configuration: " + validation.message());
        }
        return factory.create(dependencies.config());
    }
}
