package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime-provided dependencies used to choose the service factory wiring.
 */
record SkillManagementRuntimeDependencies(
        SkillManagementServiceConfig config,
        Optional<ObjectStorageService> objectStorageService,
        Optional<DataSource> jdbcDataSource) {

    SkillManagementRuntimeDependencies {
        config = config == null ? SkillManagementServiceConfigs.fromRuntime() : config;
        objectStorageService = optional(objectStorageService);
        jdbcDataSource = optional(jdbcDataSource);
    }

    static SkillManagementRuntimeDependencies of(
            SkillManagementServiceConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> jdbcDataSource) {
        return new SkillManagementRuntimeDependencies(config, objectStorageService, jdbcDataSource);
    }

    SkillManagementServiceFactory serviceFactory(SkillRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return objectStorageService
                .map(storage -> new SkillManagementServiceFactory(registry, storage, jdbcDataSource.orElse(null)))
                .orElseGet(() -> jdbcDataSource
                        .map(dataSource -> new SkillManagementServiceFactory(registry, dataSource))
                        .orElseGet(() -> new SkillManagementServiceFactory(registry)));
    }

    private static <T> Optional<T> optional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}
